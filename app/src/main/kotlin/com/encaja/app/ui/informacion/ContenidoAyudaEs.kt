package com.encaja.app.ui.informacion

/** Contenido de la Ayuda en castellano. Ver [seccionesAyuda]. */
internal val SECCIONES_AYUDA_ES: List<SeccionAyuda> = listOf(
    SeccionAyuda(
        titulo = "Guía de uso",
        temas = listOf(
            TemaAyuda(
                titulo = "Qué es Encaja",
                parrafos = listOf(
                    "Encaja sirve para organizar la logística de una familia con niños: quién lleva y quién recoge a cada niño en sus actividades, cuándo no puede estar cada persona, el menú de la semana y la lista de la compra.",
                    "La app te avisa de lo que falta (actividades sin nadie asignado) y de lo que choca (alguien asignado que tiene otra cosa apuntada a esa hora), y cada persona recibe en su móvil los avisos que le tocan."
                ),
                puntos = listOf(
                    "Barra inferior: las cinco pestañas Semana, Guía, Familia, Menú y Compra.",
                    "Barra superior: el botón «Invitar a alguien», tus iniciales y el botón de Ajustes."
                )
            ),
            TemaAyuda(
                titulo = "Primeros pasos",
                puntos = listOf(
                    "1. Entra con tu correo y contraseña o con tu cuenta de Google.",
                    "2. Si empiezas desde cero, elige «Crear mi familia» y escribe tu nombre y apellidos: quedarás como la primera persona de la familia, con rol de administrador.",
                    "3. Si alguien de tu familia ya usa Encaja, pídele un código de invitación y escríbelo en «Unirme a una familia». Quedarás vinculado a tu persona, con todo su historial.",
                    "4. En Ajustes > Niños, añade a los niños. En Ajustes > Cuidadores, añade a las personas que los cuidan y, si quieres, grupos como «los abuelos».",
                    "5. Si has creado tu propia persona a mano, ve a Ajustes > Cuidadores y pulsa el icono de persona junto a tu nombre para vincular tu cuenta con ella.",
                    "6. Empieza a apuntar actividades en la pestaña Guía."
                )
            ),
            TemaAyuda(
                titulo = "Semana: el semáforo",
                parrafos = listOf(
                    "Es la pantalla de inicio. Muestra la semana con un círculo por día (L M X J V S D). El día de hoy lleva un punto ámbar debajo. Con las flechas cambias de semana, y al tocar el título eliges una fecha en el calendario."
                ),
                puntos = listOf(
                    "Verde: el día tiene actividades y todas están cubiertas.",
                    "Rojo: hay una actividad que necesita acompañamiento y nadie asignado para llevarla o recogerla. Es un hueco.",
                    "Ámbar: todo está asignado, pero la persona asignada tiene apuntada en Familia una ocupación a esa hora. Es un aviso.",
                    "Sin color: el día ya pasó o no tiene actividades."
                ),
                nota = "Toca un día rojo o ámbar para bajar a su tarjeta y resolverlo: se abre la actividad para que asignes a alguien. Si tocas un día verde, verás un resumen con quién lleva y quién recoge."
            ),
            TemaAyuda(
                titulo = "Tablón de la familia",
                parrafos = listOf(
                    "Debajo del semáforo hay un tablón de anuncios para toda la familia. Escribe en «Nuevo anuncio» y pulsa «Publicar». Cada anuncio aparece firmado con tu nombre y la fecha."
                ),
                puntos = listOf(
                    "Todos los miembros con cuenta reciben una notificación cuando alguien publica, menos quien lo escribe.",
                    "Cualquier miembro puede borrar un anuncio con la papelera. No se pide confirmación."
                )
            ),
            TemaAyuda(
                titulo = "Invitar a alguien",
                parrafos = listOf(
                    "Para que otra persona entre en tu familia con su propia cuenta, pulsa «Invitar a alguien» en la barra superior."
                ),
                puntos = listOf(
                    "1. Elige a qué persona de la lista corresponde la invitación (primero tiene que existir en Ajustes > Cuidadores).",
                    "2. Se genera un código de 6 caracteres. Pulsa «Copiar código» y compártelo con esa persona.",
                    "3. La persona instala Encaja, crea su cuenta o entra con ella y escribe el código en «Unirme a una familia».",
                    "4. Desde ese momento su cuenta queda vinculada a esa persona, con todo su historial."
                ),
                nota = "El código solo sirve una vez y caduca a los 14 días. Si esa persona ya tiene una cuenta vinculada, no se puede generar otro código."
            ),
            TemaAyuda(
                titulo = "Guía: el día hora a hora",
                parrafos = listOf(
                    "Aquí ves y editas las actividades de un día. Arriba cambias de día con las flechas o con «Ir a una fecha», y «Ahora» te devuelve a hoy y a la hora actual."
                ),
                puntos = listOf(
                    "Línea de tiempo: una fila por niño, de 7:00 a medianoche, con scroll horizontal. Cada bloque es una actividad: verde si está cubierta, rojo si es un hueco.",
                    "Los círculos con iniciales marcan quién lleva (al principio del bloque) y quién recoge (al final).",
                    "Si es hoy, una línea roja marca la hora actual.",
                    "«Escala del día» ajusta el zoom: menos detalle para ver más horas, o más detalle para separarlas.",
                    "Debajo, «Actividades del día» lista las de cada niño con su hora, quién lleva y quién recoge, y los avisos. La que está en curso se marca como «EN CURSO».",
                    "Toca un bloque o una actividad de la lista para editarla."
                )
            ),
            TemaAyuda(
                titulo = "Crear y editar una actividad",
                parrafos = listOf(
                    "Pulsa el botón «+» de la Guía. Para editar una existente, tócala."
                ),
                puntos = listOf(
                    "Niño/a: elige a quién corresponde.",
                    "Descripción: qué es (por ejemplo «Fútbol» o «Recoger del cole»). Máximo 40 caracteres.",
                    "Icono: opcional, para reconocerla de un vistazo.",
                    "Desde / Hasta: la hora de inicio y de fin. La de fin tiene que ser posterior a la de inicio.",
                    "Requiere acompañamiento: actívalo si alguien tiene que llevarla o recogerla. Desactivado, es solo informativa y nunca genera huecos.",
                    "Quién la lleva y quién la recoge: puede ser una persona, una unidad familiar o nadie (queda como hueco). Puede ser la misma persona en ambos.",
                    "Avisos en el móvil: elige cuánto antes de empezar y de terminar quieres que suene el aviso (de 5 minutos a 2 horas). Funcionan aunque la actividad no requiera acompañamiento.",
                    "Repetir en otros días: marca días sueltos en el calendario o «cada semana en» ciertos días hasta una fecha. Se crea una actividad por día."
                ),
                nota = "Cuando una actividad se repite, al editarla puedes elegir «Guardar solo esta» o «Guardar toda la serie»: la serie copia los cambios (todo menos la fecha) a todas las veces que se repite. «Borrar» elimina solo esa actividad."
            ),
            TemaAyuda(
                titulo = "Familia: cuándo no puede alguien",
                parrafos = listOf(
                    "Aquí apuntas cuándo no puede estar cada persona: trabajo, médico, un viaje... Con eso, la app te avisa en Semana (aviso ámbar) si a alguien le asignas una actividad a una hora en la que está ocupado."
                ),
                puntos = listOf(
                    "Cada persona tiene una fila con un cuadrito por día. Verde: libre. Con color: tiene algo apuntado.",
                    "Toca un cuadrito para apuntar algo. Elige el tipo (Trabajo, Médico, Viaje, Vacaciones, Otro o uno tuyo) y luego el horario, o marca «Todo el día».",
                    "En Trabajo y Médico eliges la hora de inicio y la de fin. Si repites un horario a menudo, pulsa «Nuevo horario», ponle un nombre («Mañana», «Tarde») y lo tendrás a un toque.",
                    "En Viaje y Vacaciones eliges desde qué día hasta qué día.",
                    "Lo que apuntas en un grupo familiar (por ejemplo «los abuelos») se apunta a todos sus miembros.",
                    "El botón «Cuidadores» sirve para ver solo a algunas personas esta semana."
                ),
                nota = "Con el lápiz de la tarjeta «Categorías de estado de los cuidadores» puedes crear tus propios tipos, con nombre, icono y color. Si un tipo no «Ocupa a la persona», es solo informativo y no genera avisos."
            ),
            TemaAyuda(
                titulo = "Familia: repetir en varios días",
                parrafos = listOf(
                    "Para no apuntar lo mismo día por día, usa «Repetir en otros días» dentro de la misma ventana."
                ),
                puntos = listOf(
                    "Activa «Repetir en otros días». Se abre un calendario: toca los días en los que quieres lo mismo. El día que estás editando ya viene marcado.",
                    "Un puntito bajo un día significa que esa persona ya tiene algo de ese tipo ese día. Los días que no marques no cambian.",
                    "Debajo hay una fila con las letras L M X J V S D. Tócala para marcar todos los lunes (o martes...) hasta la fecha de «Repetir hasta».",
                    "Letra con color fuerte: ese día de la semana está apuntado todas las semanas hasta esa fecha. Color suave: solo en algunas. Sin color: en ninguna.",
                    "Si tocas una letra con color fuerte, se quitan esos días y se borra lo que hubiera apuntado.",
                    "«Repetir hasta» empieza en el último día que ya tiene algo apuntado; si no hay ninguno, a los 3 meses. Puedes cambiarlo.",
                    "Antes de guardar cambios que sustituyen o borran varios días, la app te pide confirmar."
                )
            ),
            TemaAyuda(
                titulo = "Familia: borrar varios días a la vez",
                puntos = listOf(
                    "Mantén pulsado un cuadrito del calendario de Familia: se queda marcado con un borde de color.",
                    "Con la selección en marcha, toca otros cuadritos, de la persona o del tipo que sean, para añadirlos o quitarlos.",
                    "Abajo aparece una barra que dice cuántos llevas marcados. Pulsa «Borrar» y confirma: se vacían todos a la vez. «Cancelar» lo deja todo como estaba."
                ),
                nota = "Borrar vacía el cuadrito entero, sea cual sea lo apuntado. No se puede deshacer."
            ),
            TemaAyuda(
                titulo = "Menú",
                parrafos = listOf(
                    "Planifica la comida y la cena de cada día de la semana. Cambia de semana con las flechas."
                ),
                puntos = listOf(
                    "Pulsa el lápiz junto a «Comida» o «Cena», escribe el plato y confirma con el check.",
                    "Si lo dejas vacío, vuelve a «Sin planificar».",
                    "Cualquier miembro de la familia puede editarlo."
                )
            ),
            TemaAyuda(
                titulo = "Compra",
                parrafos = listOf(
                    "La lista de la compra familiar, organizada por comercios."
                ),
                puntos = listOf(
                    "Para añadir un artículo a una tienda nueva, usa el panel de abajo: escribe el artículo y la tienda, y pulsa «+». Si la tienda ya existe, el artículo va a la misma.",
                    "Para añadir más a una tienda que ya tienes, pulsa «Añadir artículo» al final de su lista.",
                    "Marca la casilla de un artículo cuando lo compres: se queda en la lista, marcado, pero no se borra.",
                    "El lápiz cambia el nombre del artículo y la papelera lo elimina.",
                    "Toca una tienda para plegarla o desplegarla.",
                    "El botón «Filtrar comercios» permite ver solo los comercios que quieras en ese momento."
                )
            ),
            TemaAyuda(
                titulo = "Avisos y notificaciones",
                parrafos = listOf(
                    "Cada móvil programa sus propios avisos de actividades, y las notificaciones del tablón llegan por internet."
                ),
                puntos = listOf(
                    "Quien creó la actividad recibe los avisos de llevar y de recoger.",
                    "Quien tiene asignado llevar recibe el aviso de «empieza»; quien tiene asignado recoger recibe el de «termina». Si la persona asignada es una unidad familiar, lo reciben todos sus miembros.",
                    "Las actividades antiguas, creadas antes de que existiera esto, avisan a todos hasta que se vuelvan a guardar.",
                    "Cuando otra persona cambia una actividad que te afecta, tu móvil se actualiza solo."
                ),
                nota = "Para que los avisos suenen a su hora, en Ajustes puedes activar «Alarmas y recordatorios» y quitar las restricciones de batería a la app. Si tu móvil es de Xiaomi, Huawei o Samsung, revisa también «Inicio automático» o «Apps protegidas». Los avisos se reprograman al abrir la app."
            ),
            TemaAyuda(
                titulo = "Ajustes",
                puntos = listOf(
                    "Niños: añade o quita niños. Solo se pide el nombre.",
                    "Cuidadores: añade o quita personas, vincula tu cuenta a tu persona con el icono de persona, y crea unidades familiares (grupos que van juntos, como «los abuelos maternos») para asignarlos a la vez. Los administradores pueden desvincular la cuenta de otra persona.",
                    "Actividades: ver y editar todas las actividades, buscar y borrar varias a la vez (desaparecen al momento).",
                    "Ocupaciones: repasa lo apuntado en Familia, de hace un mes a un año adelante. Puedes marcar varias y borrarlas de una vez: desaparecen al momento.",
                    "Ver las ayudas otra vez: vuelve a mostrar las guías de la primera vez.",
                    "Tema: claro, oscuro o el del sistema.",
                    "Ayuda, Contacto y Privacidad y legal."
                )
            ),
            TemaAyuda(
                titulo = "Cerrar sesión y borrar mi cuenta",
                puntos = listOf(
                    "Cerrar sesión: sale de tu cuenta en este móvil. Tus datos y tu vínculo con la familia se conservan.",
                    "Borrar mi cuenta: elimina tu cuenta de acceso y tu vínculo con la familia, y dejas de recibir avisos. Tu persona y todo su historial se quedan en la familia, y otra cuenta podrá vincularse a ella más adelante con una invitación."
                ),
                nota = "Si eres la última persona con cuenta en la familia, al borrar tu cuenta la app te muestra un código. Apúntalo: con él, quien entre después retoma tu persona con todo su historial."
            ),
            TemaAyuda(
                titulo = "Cosas que conviene saber",
                puntos = listOf(
                    "Las actividades y las ocupaciones de hace más de un mes se borran solas al abrir la app, para no acumular datos que ya no hacen falta.",
                    "Casi todo lo puede hacer cualquier miembro de la familia: editar, borrar, añadir niños o personas. La única acción reservada a los administradores es desvincular la cuenta de otra persona.",
                    "Cada persona debe usar su propia cuenta y vincularse a su propia ficha: así los avisos y las iniciales son los suyos."
                )
            )
        )
    )
)
