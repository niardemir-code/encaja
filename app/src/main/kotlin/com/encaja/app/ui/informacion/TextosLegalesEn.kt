package com.encaja.app.ui.informacion

/**
 * Legal texts in English. See [documentosLegales].
 * First-draft translation of the Spanish text: before publishing, have all three languages
 * reviewed by someone with legal knowledge. The law that applies is Spanish.
 */
internal val DOCUMENTOS_LEGALES_EN: List<DocumentoLegal> = listOf(
    DocumentoLegal(
        titulo = "Privacy policy",
        resumen = "What data Encaja stores, why, and what your rights are.",
        apartados = listOf(
            ApartadoLegal(
                titulo = "1. Who is responsible",
                parrafos = listOf(
                    "The controller of your data is ${DatosLegales.TITULAR} (${DatosLegales.DOCUMENTO_IDENTIFICACION}), with address at ${DatosLegales.DOMICILIO}.",
                    "You can write to us at ${DatosLegales.EMAIL_CONTACTO} about anything related to your data."
                )
            ),
            ApartadoLegal(
                titulo = "2. What data we process",
                parrafos = listOf("Only what the app needs to work:"),
                puntos = listOf(
                    "Your account data: your email address and, if you sign in with Google, that account's identifier. If you register with a password, it is handled by Google's authentication service and we never see it.",
                    "Data about the people in the family: first and last names of caregivers, and only the first name of children. We don't ask for children's surnames, date of birth, phone number, address, photos or location.",
                    "Data you enter: children's activities (description, date, times, who drops off and who picks up, alerts), caregivers' busy times, menus, the shopping list and notice board posts.",
                    "Technical notification data: a phone identifier (token) so we can send you alerts.",
                    "A local copy of this data and your preferences (theme, tips already seen) is kept on your phone."
                )
            ),
            ApartadoLegal(
                titulo = "3. Why, and on what legal basis",
                parrafos = listOf(
                    "We use your data only to provide the service: organizing your family's logistics and sending you the alerts that concern you. The legal basis is performance of the service you requested when you created your account (Art. 6.1.b GDPR).",
                    "We don't use your data for advertising, we don't sell it, we don't build profiles, and the app includes no analytics or tracking tools."
                )
            ),
            ApartadoLegal(
                titulo = "4. Children's data",
                parrafos = listOf(
                    "The app is meant to be used by the adults in the family. We only store a child's first name and the activities their adults enter. By entering that data, you declare that you have parental authority or the authority to do so.",
                    "Minors must not create an account of their own."
                )
            ),
            ApartadoLegal(
                titulo = "5. Who else accesses the data",
                parrafos = listOf(
                    "Data is stored in Google Firebase services (authentication, database, notifications and cloud functions), which acts as a processor. The database and functions are hosted in the European Union; some Google services, such as authentication and notifications, may involve transfers outside it, carried out with the safeguards Google provides.",
                    "A family's data is only accessible to the accounts linked to that family. It isn't shared with other third parties unless legally required."
                )
            ),
            ApartadoLegal(
                titulo = "6. How long we keep it",
                puntos = listOf(
                    "Activities and busy times older than one month are deleted automatically.",
                    "The rest of the family's data is kept as long as the family uses the app.",
                    "If you delete your account, your access, your link to the family and your phones' identifiers are removed. Your person (first and last name) and their history stay in the family, because they are information shared with the other members and allow another account to take that person over.",
                    "If you want us to remove that information too, or all of a family's data, write to us and we will."
                )
            ),
            ApartadoLegal(
                titulo = "7. Your rights",
                parrafos = listOf(
                    "You can exercise your rights of access, rectification, erasure, restriction of processing, objection and portability by writing to ${DatosLegales.EMAIL_CONTACTO}. You can do many things yourself in the app: correct or delete your data, unlink accounts and delete your account.",
                    "If you believe your data isn't being handled properly, you can complain to the Spanish Data Protection Agency (www.aepd.es)."
                )
            ),
            ApartadoLegal(
                titulo = "8. Security",
                parrafos = listOf(
                    "Communications with the servers are encrypted, and the database access rules prevent one family from seeing another's data. Even so, no system is infallible: let us know if you notice any problem."
                )
            ),
            ApartadoLegal(
                titulo = "9. Phone permissions",
                puntos = listOf(
                    "Notifications: for the notice board and activity alerts.",
                    "Alarms and reminders: so alerts go off at the right time.",
                    "Battery saver: optional, so alerts arrive with the app closed.",
                    "The app doesn't ask for access to the camera, contacts, location or files."
                )
            ),
            ApartadoLegal(
                titulo = "10. Changes to this policy",
                parrafos = listOf(
                    "If we make a significant change to this text, we'll let you know in the app. Last updated: ${DatosLegales.ULTIMA_ACTUALIZACION}."
                )
            )
        )
    ),
    DocumentoLegal(
        titulo = "Legal notice",
        resumen = "Who is behind Encaja and how to get in touch.",
        apartados = listOf(
            ApartadoLegal(
                titulo = "App owner",
                puntos = listOf(
                    "Owner: ${DatosLegales.TITULAR}",
                    "Tax ID: ${DatosLegales.DOCUMENTO_IDENTIFICACION}",
                    "Address: ${DatosLegales.DOMICILIO}",
                    "Contact email: ${DatosLegales.EMAIL_CONTACTO}"
                )
            ),
            ApartadoLegal(
                titulo = "Intellectual property",
                parrafos = listOf(
                    "The name, logo, design and code of Encaja belong to its owner. They may not be copied or reused without permission. The data you enter remains yours and your family's."
                )
            )
        )
    ),
    DocumentoLegal(
        titulo = "Terms of use",
        resumen = "The basic rules for using Encaja.",
        apartados = listOf(
            ApartadoLegal(
                titulo = "Using the app",
                puntos = listOf(
                    "Encaja is for personal and family use.",
                    "You are responsible for the information you enter and for having the right to enter it, especially information about other people and children.",
                    "Don't use the app for unlawful purposes or to bother other people, for example with notice board posts.",
                    "Each person must use their own account and not share their access."
                )
            ),
            ApartadoLegal(
                titulo = "Alerts and availability",
                parrafos = listOf(
                    "Phone alerts are an aid, not a guarantee: they may be delayed or not arrive because of battery saving, lack of connection or phone settings. Don't rely on them alone for important matters.",
                    "We try to keep the app working at all times, but there may be interruptions for maintenance or other reasons beyond our control, and we may change or remove features."
                )
            ),
            ApartadoLegal(
                titulo = "Liability",
                parrafos = listOf(
                    "The app is provided as is. We are not liable for harm resulting from misuse or from a failed alert, except as established by law.",
                    "We may suspend accounts that breach these terms."
                )
            ),
            ApartadoLegal(
                titulo = "Governing law",
                parrafos = listOf("These terms are governed by Spanish law.")
            )
        )
    )
)
