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
const { onCall, HttpsError } = require("firebase-functions/v2/https");
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

// 3) Dar de baja ----------------------------------------------------------------------------
// Se hacen en el servidor (Admin SDK) porque quien se da de baja o echa a otro necesita tocar
// documentos que las reglas de Firestore no le dejan: los tokens y la membresía de otra cuenta.

/** Borra los tokens de notificaciones de una cuenta. */
async function borrarTokens(uid) {
  const snap = await db.collection("users").doc(uid).collection("tokens").get();
  await Promise.all(snap.docs.map((d) => d.ref.delete()));
}

/** Quita el vínculo de una cuenta con su familia (no borra al cuidador de la familia). */
async function desvincular(uid) {
  const usuario = await db.collection("users").doc(uid).get();
  const familyId = usuario.get("familyId");
  const caregiverId = usuario.get("caregiverId");
  if (familyId && caregiverId) {
    const enlace = db.collection("families").doc(familyId).collection("caregiverLinks").doc(caregiverId);
    const actual = await enlace.get();
    if (actual.exists && actual.get("uid") === uid) await enlace.delete();
  }
  await borrarTokens(uid);
  if (usuario.exists) await usuario.ref.delete();
}

// Código de invitación de 6 caracteres (sin 0/O ni 1/I), igual que el que genera la app.
const CARACTERES_CODIGO = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";
async function crearInvitacion(familyId, caregiverId) {
  for (let intento = 0; intento < 5; intento++) {
    let codigo = "";
    for (let i = 0; i < 6; i++) codigo += CARACTERES_CODIGO[Math.floor(Math.random() * CARACTERES_CODIGO.length)];
    const ref = db.collection("invites").doc(codigo);
    if (!(await ref.get()).exists) {
      await ref.set({ familyId, caregiverId, usado: false });
      return codigo;
    }
  }
  return null;
}

// El usuario borra su propia cuenta: se desvincula, se borran sus tokens y su cuenta de acceso.
// El cuidador y todo su historial se quedan tal cual en la familia. Si era la última cuenta
// vinculada de la familia (nadie más podría generar una invitación), se deja creado un código
// para ese cuidador y se devuelve, para que quien llegue después pueda retomar todo con él.
exports.borrarMiCuenta = onCall({ region: REGION }, async (request) => {
  if (!request.auth) throw new HttpsError("unauthenticated", "Hay que iniciar sesión.");
  const uid = request.auth.uid;
  const usuario = await db.collection("users").doc(uid).get();
  const familyId = usuario.get("familyId");
  const caregiverId = usuario.get("caregiverId");
  await desvincular(uid);

  let codigo = null;
  if (familyId && caregiverId) {
    const familia = db.collection("families").doc(familyId);
    const quedan = await familia.collection("caregiverLinks").limit(1).get();
    const cuidador = await familia.collection("caregivers").doc(caregiverId).get();
    if (quedan.empty && cuidador.exists) codigo = await crearInvitacion(familyId, caregiverId);
  }
  try {
    await admin.auth().deleteUser(uid);
  } catch (e) {
    if (e.code !== "auth/user-not-found") throw new HttpsError("internal", "No se pudo borrar la cuenta.");
  }
  return { ok: true, codigo };
});

// Un administrador desvincula la cuenta de otro cuidador de su familia: esa persona pierde el
// acceso a los datos de la familia, pero conserva su cuenta y puede volver con otra invitación.
exports.desvincularCuenta = onCall({ region: REGION }, async (request) => {
  if (!request.auth) throw new HttpsError("unauthenticated", "Hay que iniciar sesión.");
  const miUid = request.auth.uid;
  const cuidadorObjetivo = request.data && request.data.caregiverId;
  if (!cuidadorObjetivo || typeof cuidadorObjetivo !== "string") {
    throw new HttpsError("invalid-argument", "Falta el cuidador.");
  }

  const yo = await db.collection("users").doc(miUid).get();
  const familyId = yo.get("familyId");
  const miCuidador = yo.get("caregiverId");
  if (!familyId || !miCuidador) throw new HttpsError("permission-denied", "No perteneces a ninguna familia.");

  const familia = db.collection("families").doc(familyId);
  const miFicha = await familia.collection("caregivers").doc(miCuidador).get();
  if (miFicha.get("rol") !== "ADMIN") throw new HttpsError("permission-denied", "Solo un administrador puede hacerlo.");

  const enlace = await familia.collection("caregiverLinks").doc(cuidadorObjetivo).get();
  const uidObjetivo = enlace.get("uid");
  if (!enlace.exists || !uidObjetivo) throw new HttpsError("not-found", "Ese cuidador no tiene cuenta vinculada.");
  if (uidObjetivo === miUid) throw new HttpsError("failed-precondition", "Para salir tú, usa Borrar mi cuenta.");

  await enlace.ref.delete();
  await borrarTokens(uidObjetivo);
  const usuarioObjetivo = await db.collection("users").doc(uidObjetivo).get();
  if (usuarioObjetivo.exists && usuarioObjetivo.get("familyId") === familyId) await usuarioObjetivo.ref.delete();
  return { ok: true };
});
