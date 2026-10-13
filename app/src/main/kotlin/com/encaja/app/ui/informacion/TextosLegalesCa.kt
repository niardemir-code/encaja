package com.encaja.app.ui.informacion

/**
 * Textos legals en català. Ver [documentosLegales].
 * Traducció de partida: convé que la revisi una persona catalanoparlant i, abans de publicar,
 * que els tres idiomes els repasse algú amb coneixements jurídics.
 */
internal val DOCUMENTOS_LEGALES_CA: List<DocumentoLegal> = listOf(
    DocumentoLegal(
        titulo = "Política de privadesa",
        resumen = "Quines dades guarda Encaixa, per a què i quins són els teus drets.",
        apartados = listOf(
            ApartadoLegal(
                titulo = "1. Qui és el responsable",
                parrafos = listOf(
                    "El responsable del tractament de les teves dades és ${DatosLegales.TITULAR} (${DatosLegales.DOCUMENTO_IDENTIFICACION}), amb domicili a ${DatosLegales.DOMICILIO}.",
                    "Ens pots escriure a ${DatosLegales.EMAIL_CONTACTO} per a qualsevol qüestió sobre les teves dades."
                )
            ),
            ApartadoLegal(
                titulo = "2. Quines dades tractem",
                parrafos = listOf("Només les necessàries perquè l'app funcioni:"),
                puntos = listOf(
                    "Dades del teu compte: el teu correu electrònic i, si entres amb Google, l'identificador d'aquest compte. Si et registres amb contrasenya, la gestiona el servei d'autenticació de Google i nosaltres no la veiem.",
                    "Dades de les persones de la família: nom i cognoms dels cuidadors, i només el nom dels nens. No demanem cognoms dels nens, data de naixement, telèfon, adreça, fotos ni ubicació.",
                    "Dades que tu introdueixes: activitats dels nens (descripció, data, hores, qui els porta i qui els recull, avisos), ocupacions dels cuidadors, menús, llista de la compra i anuncis del tauler.",
                    "Dades tècniques de notificacions: un identificador del mòbil (token) per poder enviar-te avisos.",
                    "Al teu mòbil es guarda una còpia local d'aquestes dades i de les teves preferències (tema, ajudes vistes)."
                )
            ),
            ApartadoLegal(
                titulo = "3. Per a què i amb quina base legal",
                parrafos = listOf(
                    "Fem servir les teves dades únicament per prestar-te el servei: organitzar la logística de la teva família i enviar-te els avisos que et corresponen. La base legal és l'execució del servei que has sol·licitat en crear el teu compte (art. 6.1.b del RGPD).",
                    "No fem servir les teves dades per a publicitat, no les venem, no elaborem perfils i l'app no inclou eines d'analítica ni de seguiment."
                )
            ),
            ApartadoLegal(
                titulo = "4. Dades de menors",
                parrafos = listOf(
                    "L'app està pensada perquè la facin servir els adults de la família. Dels nens només es guarda el nom i les activitats que els seus adults hi apunten. En introduir aquestes dades, declares que en tens la pàtria potestat o l'autoritat per fer-ho.",
                    "Els menors no han de crear un compte propi."
                )
            ),
            ApartadoLegal(
                titulo = "5. Qui més accedeix a les dades",
                parrafos = listOf(
                    "Les dades es guarden als serveis de Google Firebase (autenticació, base de dades, notificacions i funcions al núvol), que actua com a encarregat del tractament. La base de dades i les funcions estan allotjades a la Unió Europea; alguns serveis de Google, com l'autenticació i les notificacions, poden implicar transferències fora d'ella, que es fan amb les garanties que Google ofereix.",
                    "Les dades d'una família només són accessibles per als comptes vinculats a aquesta família. No se cedeixen a altres tercers tret d'obligació legal."
                )
            ),
            ApartadoLegal(
                titulo = "6. Quant de temps les conservem",
                puntos = listOf(
                    "Les activitats i ocupacions amb més d'un mes d'antiguitat s'esborren automàticament.",
                    "La resta de dades de la família es conserven mentre la família faci servir l'app.",
                    "Si esborres el teu compte, s'eliminen el teu accés, el teu vincle amb la família i els identificadors dels teus mòbils. La teva persona (nom i cognoms) i el seu historial es mantenen a la família, perquè són informació compartida amb la resta dels seus membres i permeten que un altre compte reprengui aquesta persona.",
                    "Si vols que eliminem també aquesta informació, o totes les dades d'una família, escriu-nos i ho farem."
                )
            ),
            ApartadoLegal(
                titulo = "7. Els teus drets",
                parrafos = listOf(
                    "Pots exercir els drets d'accés, rectificació, supressió, limitació del tractament, oposició i portabilitat escrivint a ${DatosLegales.EMAIL_CONTACTO}. Moltes coses les pots fer tu mateix des de l'app: corregir o esborrar les teves dades, desvincular comptes i esborrar el teu compte.",
                    "Si creus que les teves dades no es tracten correctament, pots reclamar davant l'Agència Espanyola de Protecció de Dades (www.aepd.es)."
                )
            ),
            ApartadoLegal(
                titulo = "8. Seguretat",
                parrafos = listOf(
                    "Les comunicacions amb els servidors van xifrades, i les regles d'accés de la base de dades impedeixen que una família vegi les dades d'una altra. Tot i això, cap sistema és infal·lible: avisa'ns si detectes qualsevol problema."
                )
            ),
            ApartadoLegal(
                titulo = "9. Permisos del mòbil",
                puntos = listOf(
                    "Notificacions: per al tauler i els avisos d'activitats.",
                    "Alarmes i recordatoris: perquè els avisos sonin a la seva hora.",
                    "Estalvi de bateria: opcional, perquè els avisos arribin amb l'app tancada.",
                    "L'app no demana accés a la càmera, als contactes, a la ubicació ni als fitxers."
                )
            ),
            ApartadoLegal(
                titulo = "10. Canvis en aquesta política",
                parrafos = listOf(
                    "Si canviem aquest text de manera rellevant, t'ho avisarem a l'app. Última actualització: ${DatosLegales.ULTIMA_ACTUALIZACION}."
                )
            )
        )
    ),
    DocumentoLegal(
        titulo = "Avís legal",
        resumen = "Qui hi ha darrere d'Encaixa i com contactar-hi.",
        apartados = listOf(
            ApartadoLegal(
                titulo = "Titular de l'aplicació",
                puntos = listOf(
                    "Titular: ${DatosLegales.TITULAR}",
                    "Identificació fiscal: ${DatosLegales.DOCUMENTO_IDENTIFICACION}",
                    "Domicili: ${DatosLegales.DOMICILIO}",
                    "Correu de contacte: ${DatosLegales.EMAIL_CONTACTO}"
                )
            ),
            ApartadoLegal(
                titulo = "Propietat intel·lectual",
                parrafos = listOf(
                    "El nom, el logotip, el disseny i el codi d'Encaixa pertanyen al seu titular. No es poden copiar ni reutilitzar sense permís. Les dades que introdueixes continuen sent teves i de la teva família."
                )
            )
        )
    ),
    DocumentoLegal(
        titulo = "Condicions d'ús",
        resumen = "Les regles bàsiques per fer servir Encaixa.",
        apartados = listOf(
            ApartadoLegal(
                titulo = "Ús de l'app",
                puntos = listOf(
                    "Encaixa és per a ús personal i familiar.",
                    "Ets responsable de la informació que introdueixes i de tenir dret a fer-ho, especialment la d'altres persones i la dels nens.",
                    "No facis servir l'app per a fins il·lícits ni per molestar altres persones, per exemple amb anuncis del tauler.",
                    "Cada persona ha de fer servir el seu propi compte i no compartir-ne l'accés."
                )
            ),
            ApartadoLegal(
                titulo = "Avisos i disponibilitat",
                parrafos = listOf(
                    "Els avisos del mòbil són una ajuda, no una garantia: poden endarrerir-se o no arribar per l'estalvi de bateria, la manca de connexió o la configuració del mòbil. No en depenguis únicament per a qüestions importants.",
                    "Procurem que l'app funcioni sempre, però pot haver-hi interrupcions per manteniment o per causes alienes, i podem canviar o retirar funcions."
                )
            ),
            ApartadoLegal(
                titulo = "Responsabilitat",
                parrafos = listOf(
                    "L'app s'ofereix tal qual. No ens fem responsables dels perjudicis derivats d'un ús indegut ni d'un error en un avís, tret del que estableixi la llei.",
                    "Podem suspendre comptes que incompleixin aquestes condicions."
                )
            ),
            ApartadoLegal(
                titulo = "Llei aplicable",
                parrafos = listOf("Aquestes condicions es regeixen per la legislació espanyola.")
            )
        )
    )
)
