// Notificaciones push de Encaja. Se despliegan con `firebase deploy --only functions`.
//
//  1) Tablón: al publicarse un anuncio, avisa a todos los miembros de la familia menos al autor.
//  2) Actividades: al crear o cambiar una actividad con avisos, manda a los móviles a los que
//     afecta (creador, quien lleva, quien recoge; las unidades familiares cuentan por sus
//     miembros) un mensaje silencioso para que reprogramen sus avisos sin abrir la app.
//
// Estructura de Firestore que usa (la misma que la app):
//   families/{familyId}/anuncios/{id}            { autorNombre, texto, autorUid, ... }
//   families/{familyId}/coverageNeeds/{id}       { quienLlevaId, quienRecogeId, creadoPorId, fecha, ... }
//   families/{familyId}/familyUnits/{id}         { miembros: [caregiverId, ...] }
//   families/{familyId}/caregiverLinks/{cuidadorId} { uid }
//   users/{uid}/tokens/{token}                   (los registra la app)

const { onDocumentCreated, onDocumentWritten } = require("firebase-functions/v2/firestore");
const admin = require("firebase-admin");

admin.initializeApp();
const db = admin.firestore();

// IMPORTANTE: poner aquí la misma región que tu base de datos de Firestore
// (Firebase console > Firestore > pestaña "Datos"; arriba o en "Configuración" se ve la ubicación).
// Ejemplos: "europe-west1" (Bélgica), "europe-west3" (Fráncfort), "eur3" (Europa multi-región).
const REGION = "europe-west1";

/** uids con cuenta vinculada en la familia, opcionalmente solo de unos cuidadores. */
async function uidsDeFamilia(familyId, soloCuidadores) {
  const snap = await db.collection("families").doc(familyId).collection("caregiverLinks").get();
  const uids = new Set();
  snap.forEach((doc) => {
    if (soloCuidadores && !soloCuidadores.has(doc.id)) return;
    const uid = doc.get("uid");
    if (uid) uids.add(uid);
  });
  return uids;
}

/** Resuelve ids de cuidador o de unidad familiar a un conjunto de ids de cuidador. */
async function cuidadoresDe(familyId, ids) {
  const resultado = new Set();
  for (const id of ids) {
    if (!id) continue;
    resultado.add(id); // por si es un cuidador
    const unidad = await db.collection("families").doc(familyId).collection("familyUnits").doc(id).get();
    if (unidad.exists) (unidad.get("miembros") || []).forEach((m) => resultado.add(m));
  }
  return resultado;
}

async function tokensDe(uids) {
  const tokens = [];
  for (const uid of uids) {
    const snap = await db.collection("users").doc(uid).collection("tokens").get();
    snap.forEach((doc) => tokens.push({ uid, token: doc.id }));
  }
  return tokens;
}

/** Envía [data] (solo cadenas) a los tokens y borra los que ya no existen. */
async function enviar(tokens, data, opciones = {}) {
  if (tokens.length === 0) return;
  for (let i = 0; i < tokens.length; i += 500) {
    const lote = tokens.slice(i, i + 500);
    const respuesta = await admin.messaging().sendEachForMulticast({
      tokens: lote.map((t) => t.token),
      data,
      android: { priority: "high", ...opciones },
    });
    const borrados = [];
    respuesta.responses.forEach((r, idx) => {
      const codigo = r.error && r.error.code;
      if (codigo === "messaging/registration-token-not-registered" ||
          codigo === "messaging/invalid-registration-token") {
        const { uid, token } = lote[idx];
        borrados.push(db.collection("users").doc(uid).collection("tokens").doc(token).delete());
      }
    });
    await Promise.all(borrados);
  }
}

// 1) Tablón ---------------------------------------------------------------------------------
exports.avisarAnuncio = onDocumentCreated(
  { document: "families/{familyId}/anuncios/{anuncioId}", region: REGION },
  async (event) => {
    const anuncio = event.data && event.data.data();
    if (!anuncio) return;
    const uids = await uidsDeFamilia(event.params.familyId);
    if (anuncio.autorUid) uids.delete(anuncio.autorUid); // el autor ya lo sabe
    const texto = String(anuncio.texto || "").slice(0, 200);
    const autor = anuncio.autorNombre || "Alguien de la familia";
    await enviar(await tokensDe(uids), {
      tipo: "anuncio",
      titulo: `${autor} en el tablón`,
      texto,
    });
  }
);

// 2) Actividades ----------------------------------------------------------------------------
exports.avisarCambioActividad = onDocumentWritten(
  { document: "families/{familyId}/coverageNeeds/{needId}", region: REGION },
  async (event) => {
    const despues = event.data.after && event.data.after.exists ? event.data.after.data() : null;
    if (!despues) return; // borrada: no hay nada que programar
    const hoy = new Date().toISOString().slice(0, 10);
    if (despues.fecha && despues.fecha < hoy) return; // pasada
    const antes = event.data.before && event.data.before.exists ? event.data.before.data() : {};

    const campos = ["quienLlevaId", "quienRecogeId", "creadoPorId", "avisoLlevarMin", "avisoRecogerMin",
      "fecha", "horaInicio", "horaFin", "descripcion"];
    if (event.data.before.exists && campos.every((c) => antes[c] === despues[c])) return; // nada relevante

    const familyId = event.params.familyId;
    // Afectados ahora y antes (quien deja de estar asignado también debe quitar su aviso).
    const ids = [despues.creadoPorId, despues.quienLlevaId, despues.quienRecogeId,
      antes.quienLlevaId, antes.quienRecogeId];
    const cuidadores = await cuidadoresDe(familyId, ids);
    const uids = await uidsDeFamilia(familyId, cuidadores);
    await enviar(await tokensDe(uids), { tipo: "sync_avisos", familyId }, { collapseKey: "sync_avisos" });
  }
);
