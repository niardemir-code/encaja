package com.encaja.app.ui.informacion

/**
 * Contingut de l'Ajuda en català. Ver [seccionesAyuda].
 * Els noms de botons i pantalles són els mateixos que es veuen a l'app en català.
 */
internal val SECCIONES_AYUDA_CA: List<SeccionAyuda> = listOf(
    SeccionAyuda(
        titulo = "Guia d'ús",
        temas = listOf(
            TemaAyuda(
                titulo = "Què és Encaixa",
                parrafos = listOf(
                    "Encaixa serveix per organitzar la logística d'una família amb nens: qui porta i qui recull cada nen a les seves activitats, quan no pot estar cada persona, el menú de la setmana i la llista de la compra.",
                    "L'app t'avisa del que falta (activitats sense ningú assignat) i del que xoca (algú assignat que té una altra cosa apuntada a aquella hora), i cada persona rep al mòbil els avisos que li toquen."
                ),
                puntos = listOf(
                    "Barra inferior: les cinc pestanyes Setmana, Guia, Família, Menú i Compra.",
                    "Barra superior: el botó «Convida algú», les teves inicials i el botó de Configuració."
                )
            ),
            TemaAyuda(
                titulo = "Primers passos",
                puntos = listOf(
                    "1. Entra amb el teu correu i contrasenya o amb el teu compte de Google.",
                    "2. Si comences de zero, tria «Crea la meva família» i escriu el teu nom i cognoms: seràs la primera persona de la família, amb rol d'administrador.",
                    "3. Si algú de la teva família ja fa servir Encaixa, demana-li un codi d'invitació i escriu-lo a «Uneix-me a una família». Quedaràs vinculat a la teva persona, amb tot el seu historial.",
                    "4. A Configuració > Nens, afegeix els nens. A Configuració > Cuidadors, afegeix les persones que els cuiden i, si vols, grups com «els avis».",
                    "5. Si has creat la teva pròpia persona a mà, ves a Configuració > Cuidadors i prem la icona de persona al costat del teu nom per vincular-hi el teu compte.",
                    "6. Comença a apuntar activitats a la pestanya Guia."
                )
            ),
            TemaAyuda(
                titulo = "Setmana: el semàfor",
                parrafos = listOf(
                    "És la pantalla d'inici. Mostra la setmana amb un cercle per dia. El dia d'avui porta un punt ambre a sota. Amb les fletxes canvies de setmana, i en tocar el títol tries una data al calendari."
                ),
                puntos = listOf(
                    "Verd: el dia té activitats i totes estan cobertes.",
                    "Vermell: hi ha una activitat que necessita acompanyament i ningú assignat per portar-la o recollir-la. És un buit.",
                    "Ambre: tot està assignat, però la persona assignada té apuntada a Família una ocupació a aquella hora. És un avís.",
                    "Sense color: el dia ja ha passat o no té activitats."
                ),
                nota = "Toca un dia vermell o ambre per baixar a la seva targeta i resoldre'l: s'obre l'activitat perquè hi assignis algú. Si toques un dia verd, veuràs un resum amb qui porta i qui recull."
            ),
            TemaAyuda(
                titulo = "Tauler de la família",
                parrafos = listOf(
                    "Sota el semàfor hi ha un tauler d'anuncis per a tota la família. Escriu a «Nou anunci» i prem «Publica». Cada anunci apareix signat amb el teu nom i la data."
                ),
                puntos = listOf(
                    "Tots els membres amb compte reben una notificació quan algú publica, excepte qui l'escriu.",
                    "Qualsevol membre pot esborrar un anunci amb la paperera. No es demana confirmació."
                )
            ),
            TemaAyuda(
                titulo = "Convidar algú",
                parrafos = listOf(
                    "Perquè una altra persona entri a la teva família amb el seu propi compte, prem «Convida algú» a la barra superior."
                ),
                puntos = listOf(
                    "1. Tria a quina persona de la llista correspon la invitació (primer ha d'existir a Configuració > Cuidadors).",
                    "2. Es genera un codi de 6 caràcters. Prem «Copia el codi» i comparteix-lo amb aquesta persona.",
                    "3. La persona instal·la Encaixa, crea el seu compte o hi entra i escriu el codi a «Uneix-me a una família».",
                    "4. Des d'aquell moment el seu compte queda vinculat a aquesta persona, amb tot el seu historial."
                ),
                nota = "El codi només serveix una vegada i caduca als 14 dies. Si aquesta persona ja té un compte vinculat, no es pot generar un altre codi."
            ),
            TemaAyuda(
                titulo = "Guia: el dia hora a hora",
                parrafos = listOf(
                    "Aquí veus i edites les activitats d'un dia. A dalt canvies de dia amb les fletxes o amb «Vés a una data», i «Ara» et torna a avui i a l'hora actual."
                ),
                puntos = listOf(
                    "Línia de temps: una fila per nen, de les 7:00 a mitjanit, amb scroll horitzontal. Cada bloc és una activitat: verd si està coberta, vermell si és un buit.",
                    "Els cercles amb inicials marquen qui porta (al principi del bloc) i qui recull (al final).",
                    "Si és avui, una línia vermella marca l'hora actual.",
                    "«Escala del dia» ajusta el zoom: menys detall per veure més hores, o més detall per separar-les.",
                    "A sota, «Activitats del dia» llista les de cada nen amb la seva hora, qui porta i qui recull, i els avisos. La que està en curs es marca com a «EN CURS».",
                    "Toca un bloc o una activitat de la llista per editar-la."
                )
            ),
            TemaAyuda(
                titulo = "Crear i editar una activitat",
                parrafos = listOf(
                    "Prem el botó «+» de la Guia. Per editar-ne una d'existent, toca-la."
                ),
                puntos = listOf(
                    "Nen/a: tria a qui correspon.",
                    "Descripció: què és (per exemple «Futbol» o «Recollir de l'escola»). Màxim 40 caràcters.",
                    "Icona: opcional, per reconèixer-la d'un cop d'ull.",
                    "Des de / Fins a: l'hora d'inici i de fi. La de fi ha de ser posterior a la d'inici.",
                    "Requereix acompanyament: activa'l si algú l'ha de portar o recollir. Desactivat, és només informativa i mai genera buits.",
                    "Qui la porta i qui la recull: pot ser una persona, una unitat familiar o ningú (queda com a buit). Pot ser la mateixa persona en tots dos.",
                    "Avisos al mòbil: tria quant abans de començar i d'acabar vols que soni l'avís (de 5 minuts a 2 hores). Funcionen encara que l'activitat no requereixi acompanyament.",
                    "Repeteix en altres dies: marca dies solts al calendari o «cada setmana en» certs dies fins a una data. Es crea una activitat per dia."
                ),
                nota = "Quan una activitat es repeteix, en editar-la pots triar «Desa només aquesta» o «Desa tota la sèrie»: la sèrie copia els canvis (tot menys la data) a totes les vegades que es repeteix. Per canviar només certs dies de la setmana (p. ex. només els dilluns): activa «Repeteix en altres dies», marca aquests dies a la fila L M X J V S D i prem «Desa tota la sèrie»; l'app preguntarà si el canvi és només per a aquests dies o per a tota la sèrie. «Esborra» elimina només aquella activitat."
            ),
            TemaAyuda(
                titulo = "Família: quan algú no pot",
                parrafos = listOf(
                    "Aquí apuntes quan no pot estar cada persona: feina, metge, un viatge... Amb això, l'app t'avisa a Setmana (avís ambre) si assignes una activitat a algú a una hora en què està ocupat."
                ),
                puntos = listOf(
                    "Cada persona té una fila amb una caseta per dia. Verd: lliure. Amb color: té alguna cosa apuntada.",
                    "Toca una caseta per apuntar alguna cosa. Tria el tipus (Feina, Metge, Viatge, Vacances, Altre o un de teu) i després l'horari, o marca «Tot el dia».",
                    "A Feina i Metge tries l'hora d'inici i la de fi. Si repeteixes sovint un horari, prem «Nou horari», posa-li un nom («Matí», «Tarda») i el tindràs a un toc.",
                    "A Viatge i Vacances tries des de quin dia fins a quin dia.",
                    "El que apuntes en un grup familiar (per exemple «els avis») s'apunta a tots els seus membres.",
                    "El botó «Cuidadors» serveix per veure només algunes persones aquesta setmana."
                ),
                nota = "Amb el llapis de la targeta «Categories d'estat dels cuidadors» pots crear els teus propis tipus, amb nom, icona i color. Si un tipus no «Ocupa la persona», és només informatiu i no genera avisos."
            ),
            TemaAyuda(
                titulo = "Família: repetir en diversos dies",
                parrafos = listOf(
                    "Per no apuntar el mateix dia per dia, fes servir «Repeteix en altres dies» dins de la mateixa finestra."
                ),
                puntos = listOf(
                    "Activa «Repeteix en altres dies». S'obre un calendari: toca els dies en què vols el mateix. El dia que estàs editant ja ve marcat.",
                    "Un puntet sota un dia vol dir que aquella persona ja té alguna cosa d'aquest tipus aquell dia. Els dies que no marquis no canvien.",
                    "A sota hi ha una fila amb les lletres dels dies de la setmana. Toca-la per marcar tots els dilluns (o dimarts...) fins a la data de «Repeteix fins a».",
                    "Lletra amb color fort: aquell dia de la setmana està apuntat totes les setmanes fins a aquella data. Color suau: només en algunes. Sense color: en cap.",
                    "Si toques una lletra amb color fort, es treuen aquests dies i s'esborra el que hi hagués apuntat.",
                    "«Repeteix fins a» comença a l'últim dia que ja té alguna cosa apuntada; si no n'hi ha cap, als 3 mesos. Ho pots canviar.",
                    "Abans de desar canvis que substitueixen o esborren diversos dies, l'app et demana confirmar."
                )
            ),
            TemaAyuda(
                titulo = "Família: esborrar diversos dies alhora",
                puntos = listOf(
                    "Mantén premuda una caseta del calendari de Família: es queda marcada amb una vora de color.",
                    "Amb la selecció en marxa, toca altres casetes, de la persona o del tipus que siguin, per afegir-les o treure-les.",
                    "A baix apareix una barra que diu quantes en portes marcades. Prem «Esborra» i confirma: es buiden totes alhora. «Cancel·la» ho deixa tot com estava."
                ),
                nota = "Esborrar buida la caseta sencera, sigui el que sigui el que hi ha apuntat. No es pot desfer."
            ),
            TemaAyuda(
                titulo = "Menú",
                parrafos = listOf(
                    "Planifica el dinar i el sopar de cada dia de la setmana. Canvia de setmana amb les fletxes."
                ),
                puntos = listOf(
                    "Prem el llapis al costat de «Dinar» o «Sopar», escriu el plat i confirma amb el check.",
                    "Si ho deixes buit, torna a «Sense planificar».",
                    "Qualsevol membre de la família ho pot editar."
                )
            ),
            TemaAyuda(
                titulo = "Compra",
                parrafos = listOf(
                    "La llista de la compra familiar, organitzada per comerços."
                ),
                puntos = listOf(
                    "Per afegir un article a una botiga nova, fes servir el panell de baix: escriu l'article i la botiga, i prem «+». Si la botiga ja existeix, l'article hi va.",
                    "Per afegir-ne més a una botiga que ja tens, prem «Afegeix un article» al final de la seva llista.",
                    "Marca la casella d'un article quan el compris: es queda a la llista, marcat, però no s'esborra.",
                    "El llapis canvia el nom de l'article i la paperera l'elimina.",
                    "Toca una botiga per plegar-la o desplegar-la.",
                    "El botó «Filtra els comerços» permet veure només els comerços que vulguis en aquell moment."
                )
            ),
            TemaAyuda(
                titulo = "Avisos i notificacions",
                parrafos = listOf(
                    "Cada mòbil programa els seus propis avisos d'activitats, i les notificacions del tauler arriben per internet."
                ),
                puntos = listOf(
                    "Qui va crear l'activitat rep els avisos de portar i de recollir.",
                    "Qui té assignat portar rep l'avís de «comença»; qui té assignat recollir rep el d'«acaba». Si la persona assignada és una unitat familiar, ho reben tots els seus membres.",
                    "Les activitats antigues, creades abans que existís això, avisen tothom fins que es tornin a desar.",
                    "Quan una altra persona canvia una activitat que t'afecta, el teu mòbil s'actualitza sol."
                ),
                nota = "Perquè els avisos sonin a la seva hora, a Configuració pots activar «Alarmes i recordatoris» i treure les restriccions de bateria a l'app. Si el teu mòbil és de Xiaomi, Huawei o Samsung, revisa també «Inici automàtic» o «Apps protegides». Els avisos es reprogramen en obrir l'app."
            ),
            TemaAyuda(
                titulo = "Configuració",
                puntos = listOf(
                    "Nens: afegeix o treu nens. Només es demana el nom.",
                    "Cuidadors: afegeix o treu persones, vincula el teu compte a la teva persona amb la icona de persona, i crea unitats familiars (grups que van junts, com «els avis materns») per assignar-los alhora. Els administradors poden desvincular el compte d'una altra persona.",
                    "Activitats: mira i edita totes les activitats, cerca i esborra'n diverses alhora (desapareixen a l'instant).",
                    "Ocupacions: repassa el que s'ha apuntat a Família, des d'un mes enrere fins a un any endavant. Pots marcar-ne diverses i esborrar-les alhora: desapareixen a l'instant.",
                    "Torna a veure les ajudes: torna a mostrar les guies de la primera vegada.",
                    "Tema: clar, fosc o el del sistema.",
                    "Ajuda, Contacte i Privacitat i legal."
                )
            ),
            TemaAyuda(
                titulo = "Tancar la sessió i esborrar el meu compte",
                puntos = listOf(
                    "Tanca la sessió: surt del teu compte en aquest mòbil. Les teves dades i el teu vincle amb la família es conserven.",
                    "Esborra el meu compte: elimina el teu compte d'accés i el teu vincle amb la família, i deixes de rebre avisos. La teva persona i tot el seu historial es queden a la família, i un altre compte s'hi podrà vincular més endavant amb una invitació."
                ),
                nota = "Si ets l'última persona amb compte a la família, en esborrar el teu compte l'app et mostra un codi. Apunta'l: amb ell, qui entri després reprèn la teva persona amb tot el seu historial."
            ),
            TemaAyuda(
                titulo = "Coses que convé saber",
                puntos = listOf(
                    "Les activitats i les ocupacions de fa més d'un mes s'esborren soles en obrir l'app, per no acumular dades que ja no calen.",
                    "Gairebé tot ho pot fer qualsevol membre de la família: editar, esborrar, afegir nens o persones. L'única acció reservada als administradors és desvincular el compte d'una altra persona.",
                    "Cada persona ha de fer servir el seu propi compte i vincular-se a la seva pròpia fitxa: així els avisos i les inicials són els seus."
                )
            )
        )
    )
)
