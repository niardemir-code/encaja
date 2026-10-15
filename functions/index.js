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

const { onDocumentCreated, onDocumentWritten, onDocumentUpdated, onDocumentDeleted } = require("firebase-functions/v2/firestore");
const { onCall, HttpsError } = require("firebase-functions/v2/https");
const { onSchedule } = require("firebase-functions/v2/scheduler");
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
    const autor = anuncio.autorNombre || "";
    // "autor" lo usa la app para redactar el título en el idioma del móvil; "titulo" se mantiene
    // sólo para las versiones anteriores de la app, que lo mostraban tal cual.
    await enviar(await tokensDe(uids), {
      tipo: "anuncio",
      autor,
      titulo: `${autor || "Alguien de la familia"} en el tablón`,
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
  // recursiveDelete: quita también lo que cuelgue de users/{uid} (tokens) para que no quede
  // un documento "fantasma" en cursiva en la consola.
  await db.recursiveDelete(db.collection("users").doc(uid));
}

// Días que se conserva una familia sin ninguna cuenta vinculada antes de borrarla del todo.
const DIAS_CONSERVAR_HUERFANA = 30;

/** Borra la familia completa (documento y todas sus subcolecciones) y sus invitaciones. */
async function borrarFamiliaCompleta(familyId) {
  await db.recursiveDelete(db.collection("families").doc(familyId));
  const invitaciones = await db.collection("invites").where("familyId", "==", familyId).get();
  await Promise.all(invitaciones.docs.map((d) => d.ref.delete()));
}

// Código de invitación de 6 caracteres (sin 0/O ni 1/I), igual que el que genera la app.
const CARACTERES_CODIGO = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";
async function crearInvitacion(familyId, caregiverId) {
  for (let intento = 0; intento < 5; intento++) {
    let codigo = "";
    for (let i = 0; i < 6; i++) codigo += CARACTERES_CODIGO[Math.floor(Math.random() * CARACTERES_CODIGO.length)];
    const ref = db.collection("invites").doc(codigo);
    if (!(await ref.get()).exists) {
      // El código de reenganche tras borrar la última cuenta dura lo mismo que se conservan
      // los datos de la familia (DIAS_CONSERVAR_HUERFANA); el TTL de Firestore (campo
      // "caducaEn" de "invites") lo borrará después.
      const caducaEn = admin.firestore.Timestamp.fromMillis(Date.now() + DIAS_CONSERVAR_HUERFANA * 86400000);
      await ref.set({ familyId, caregiverId, usado: false, caducaEn });
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
  // Si es la única cuenta de la familia, la persona puede pedir que se borren también todos
  // los datos de la familia. Si hay más cuentas, la familia se conserva para ellas.
  const borrarDatos = !!(request.data && request.data.borrarDatos === true);
  await desvincular(uid);

  let codigo = null;
  let datosBorrados = false;
  if (familyId) {
    const familia = db.collection("families").doc(familyId);
    const quedan = await familia.collection("caregiverLinks").limit(1).get();
    if (quedan.empty) {
      if (borrarDatos) {
        await borrarFamiliaCompleta(familyId);
        datosBorrados = true;
      } else {
        // Sin cuentas: se conserva DIAS_CONSERVAR_HUERFANA días por si alguien vuelve con el
        // código, y la limpieza diaria la borra después si nadie la recupera.
        await familia.set({ huerfanaDesde: admin.firestore.FieldValue.serverTimestamp() }, { merge: true });
        const cuidador = caregiverId ? await familia.collection("caregivers").doc(caregiverId).get() : null;
        if (cuidador && cuidador.exists) codigo = await crearInvitacion(familyId, caregiverId);
      }
    }
  }
  try {
    await admin.auth().deleteUser(uid);
  } catch (e) {
    if (e.code !== "auth/user-not-found") throw new HttpsError("internal", "No se pudo borrar la cuenta.");
  }
  // Por si el móvil volvió a registrarse (token de notificaciones) mientras se borraba.
  await db.recursiveDelete(db.collection("users").doc(uid));
  return { ok: true, codigo, datosBorrados };
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
  // Y que mi cuenta sea de verdad la vinculada a esa ficha (no basta con apuntar a ella en users/{uid}).
  const miEnlace = await familia.collection("caregiverLinks").doc(miCuidador).get();
  if (miEnlace.get("uid") !== miUid) throw new HttpsError("permission-denied", "Solo un administrador puede hacerlo.");

  const enlace = await familia.collection("caregiverLinks").doc(cuidadorObjetivo).get();
  const uidObjetivo = enlace.get("uid");
  if (!enlace.exists || !uidObjetivo) throw new HttpsError("not-found", "Ese cuidador no tiene cuenta vinculada.");
  if (uidObjetivo === miUid) throw new HttpsError("failed-precondition", "Para salir tú, usa Borrar mi cuenta.");

  await enlace.ref.delete();
  await borrarTokens(uidObjetivo);
  const usuarioObjetivo = await db.collection("users").doc(uidObjetivo).get();
  if (usuarioObjetivo.exists && usuarioObjetivo.get("familyId") === familyId) {
    await db.recursiveDelete(usuarioObjetivo.ref);
  }
  // La cuenta de esa persona se elimina también de Authentication (no solo se desvincula):
  // si quiere volver, crea su cuenta de nuevo y canjea otra invitación.
  try {
    await admin.auth().deleteUser(uidObjetivo);
  } catch (e) {
    if (e.code !== "auth/user-not-found") throw new HttpsError("internal", "No se pudo eliminar la cuenta.");
  }
  await db.recursiveDelete(db.collection("users").doc(uidObjetivo));
  return { ok: true };
});

// Comprueba si el enlace de un cuidador está "huérfano" (la cuenta ya no existe, por ejemplo
// porque se borró a mano en Firestore/Authentication) y, si lo está, lo limpia para poder invitar
// de nuevo. Solo un administrador de la familia puede llamarla.
exports.repararVinculo = onCall({ region: REGION }, async (request) => {
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
  // Y que mi cuenta sea de verdad la vinculada a esa ficha (no basta con apuntar a ella en users/{uid}).
  const miEnlace = await familia.collection("caregiverLinks").doc(miCuidador).get();
  if (miEnlace.get("uid") !== miUid) throw new HttpsError("permission-denied", "Solo un administrador puede hacerlo.");

  const enlace = await familia.collection("caregiverLinks").doc(cuidadorObjetivo).get();
  if (!enlace.exists) return { huerfano: false, existe: false };
  const uid = enlace.get("uid");

  let huerfano = !uid;
  if (uid) {
    let cuentaExiste = true;
    try {
      await admin.auth().getUser(uid);
    } catch (e) {
      if (e && e.code === "auth/user-not-found") cuentaExiste = false;
      else throw new HttpsError("internal", "No se pudo comprobar la cuenta.");
    }
    if (!cuentaExiste) {
      huerfano = true;
    } else {
      const usuario = await db.collection("users").doc(uid).get();
      huerfano = !usuario.exists ||
        usuario.get("familyId") !== familyId ||
        usuario.get("caregiverId") !== cuidadorObjetivo;
    }
  }

  if (!huerfano) return { huerfano: false, existe: true };
  await enlace.ref.delete();
  if (uid) {
    await borrarTokens(uid);
    const usuario = await db.collection("users").doc(uid).get();
    if (usuario.exists && usuario.get("familyId") === familyId) await usuario.ref.delete();
  }
  return { huerfano: true, existe: true };
});

// Cuando se crea una invitación, se borran las demás aún sin usar del mismo cuidador: cada vez
// que se pulsa "Invitar" se genera un código nuevo, y los anteriores solo estorbarían (la app no
// puede listar "invites", así que no puede limpiarlos ella).
exports.limpiarInvitacionesAnteriores = onDocumentCreated(
  { document: "invites/{codigo}", region: REGION },
  async (event) => {
    const nueva = event.data && event.data.data();
    if (!nueva || !nueva.familyId || !nueva.caregiverId) return;
    const anteriores = await db.collection("invites")
      .where("familyId", "==", nueva.familyId)
      .where("caregiverId", "==", nueva.caregiverId)
      .get();
    await Promise.all(anteriores.docs.filter((d) => d.id !== event.params.codigo).map((d) => d.ref.delete()));
  }
);

/** Borra todos los documentos de una consulta, en lotes. */
async function borrarConsulta(consulta) {
  const snap = await consulta.get();
  for (let i = 0; i < snap.docs.length; i += 400) {
    const lote = db.batch();
    snap.docs.slice(i, i + 400).forEach((d) => lote.delete(d.ref));
    await lote.commit();
  }
  return snap.size;
}

// Limpieza diaria (4:00, hora de Madrid):
//  1) Familias sin ninguna cuenta vinculada: la primera vez que se ve una así se anota
//     "huerfanaDesde"; si pasados DIAS_CONSERVAR_HUERFANA días sigue sin cuentas, se borra por
//     completo (datos, invitaciones y el propio documento). Si alguien se vincula en el plazo,
//     se quita la marca y no se toca nada. Las familias de demostración ("demo-...") se saltan.
//  2) Invitaciones caducadas o ya usadas.
//  3) Documentos de users/{uid} cuya cuenta ya no existe en Authentication.
exports.limpiarFamiliasHuerfanas = onSchedule(
  { schedule: "every day 04:00", timeZone: "Europe/Madrid", region: REGION, timeoutSeconds: 540 },
  async () => {
    // 1) Familias sin cuentas
    try {
      const limite = Date.now() - DIAS_CONSERVAR_HUERFANA * 86400000;
      const familias = await db.collection("families").listDocuments();
      for (const ref of familias) {
        if (ref.id.startsWith("demo-")) continue;
        try {
          const conCuentas = !(await ref.collection("caregiverLinks").limit(1).get()).empty;
          const doc = await ref.get();
          const desde = doc.get("huerfanaDesde");
          if (conCuentas) {
            if (desde) await ref.update({ huerfanaDesde: admin.firestore.FieldValue.delete() });
          } else if (!desde) {
            await ref.set({ huerfanaDesde: admin.firestore.FieldValue.serverTimestamp() }, { merge: true });
          } else if (desde.toMillis() < limite) {
            await borrarFamiliaCompleta(ref.id);
          }
        } catch (e) {
          console.error("limpieza: fallo con la familia " + ref.id, e);
        }
      }
    } catch (e) {
      console.error("limpieza: familias", e);
    }

    // 2) Invitaciones caducadas o usadas
    try {
      const ahora = admin.firestore.Timestamp.now();
      const caducadas = await borrarConsulta(db.collection("invites").where("caducaEn", "<", ahora));
      const usadas = await borrarConsulta(db.collection("invites").where("usado", "==", true));
      console.log("limpieza: invitaciones borradas (caducadas, usadas) =", caducadas, usadas);
    } catch (e) {
      console.error("limpieza: invitaciones", e);
    }

    // 3) users/{uid} de cuentas que ya no existen (incluye los documentos "fantasma")
    try {
      const usuarios = await db.collection("users").listDocuments();
      for (let i = 0; i < usuarios.length; i += 100) {
        const lote = usuarios.slice(i, i + 100);
        const r = await admin.auth().getUsers(lote.map((u) => ({ uid: u.id })));
        const inexistentes = new Set(r.notFound.map((n) => n.uid));
        for (const u of lote) {
          if (inexistentes.has(u.id)) await db.recursiveDelete(u);
        }
      }
    } catch (e) {
      console.error("limpieza: usuarios", e);
    }
  }
);

// Una invitación usada ya no sirve para nada: se borra en cuanto alguien la canjea.
exports.borrarInvitacionUsada = onDocumentUpdated(
  { document: "invites/{codigo}", region: REGION },
  async (event) => {
    const despues = event.data && event.data.after && event.data.after.data();
    if (despues && despues.usado === true) await event.data.after.ref.delete();
  }
);

// Cuando desaparece el vínculo de un cuidador con su cuenta (desvincularlo, eliminar al cuidador,
// borrar la cuenta, o a mano en la consola), la cuenta de esa persona se elimina también de
// Authentication y se limpia su users/{uid}. Así no quedan cuentas sueltas que ya no dan acceso a
// nada, sea cual sea el camino por el que se haya borrado el vínculo.
exports.limpiarCuentaSinVinculo = onDocumentDeleted(
  { document: "families/{familyId}/caregiverLinks/{caregiverId}", region: REGION },
  async (event) => {
    const uid = event.data && event.data.get("uid");
    if (!uid) return;
    const { familyId, caregiverId } = event.params;
    // Si esa cuenta ya apunta a otra familia o ficha (se vinculó a otra), no se toca.
    const usuario = await db.collection("users").doc(uid).get();
    if (usuario.exists) {
      const otraFamilia = usuario.get("familyId") && usuario.get("familyId") !== familyId;
      const otraFicha = usuario.get("caregiverId") && usuario.get("caregiverId") !== caregiverId;
      if (otraFamilia || otraFicha) return;
    }
    try {
      await admin.auth().deleteUser(uid);
    } catch (e) {
      if (e.code !== "auth/user-not-found") console.error("limpiarCuentaSinVinculo: no se pudo borrar " + uid, e);
    }
    await db.recursiveDelete(db.collection("users").doc(uid));
  }
);
