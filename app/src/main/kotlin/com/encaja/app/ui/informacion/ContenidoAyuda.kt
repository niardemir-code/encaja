package com.encaja.app.ui.informacion

/**
 * Contenido de Ajustes > Ayuda. Está separado de la pantalla para poder ampliarlo sin tocar
 * código de interfaz: para añadir preguntas frecuentes u otros temas, basta con añadir una
 * [SeccionAyuda] más (por ejemplo "Preguntas frecuentes") a [SECCIONES_AYUDA].
 */
data class TemaAyuda(
    val titulo: String,
    /** Texto explicativo, un párrafo por elemento. */
    val parrafos: List<String> = emptyList(),
    /** Lista de puntos o pasos, que se muestran debajo de los párrafos. */
    val puntos: List<String> = emptyList(),
    /** Texto final, después de los puntos (p. ej. una advertencia). */
    val nota: String? = null
)

data class SeccionAyuda(val titulo: String, val temas: List<TemaAyuda>)

val SECCIONES_AYUDA: List<SeccionAyuda> = listOf(
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
                    "2. Si empiezas desde cero, elige «Crear una familia nueva» y escribe tu nombre y apellidos: quedarás como la primera persona de la familia, con rol de administrador.",
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
                nota = "El código solo sirve una vez y no caduca por tiempo. Si esa persona ya tiene una cuenta vinculada, no se puede generar otro código."
            ),
            TemaAyuda(
                titulo = "Guía: el día hora a hora",
                parrafos = listOf(
                    "Aquí ves y editas las actividades de un día. Arriba cambias de día con las flechas o con «Ir a una fecha», y «Ahora» te devuelve a hoy y a la hora actual."
                ),
                puntos = listOf(
                    "Línea de tiempo: una fila por niño, de 7:00 a 22:00, con scroll horizontal. Cada bloque es una actividad: verde si está cubierta, rojo si es un hueco.",
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
                titulo = "Familia: disponibilidad",
                parrafos = listOf(
                    "Sirve para apuntar cuándo no puede estar cada persona (trabajo, médico, un viaje...). Es lo que alimenta los avisos ámbar de Semana: si alguien asignado a una actividad tiene a esa hora algo apuntado que le ocupa, se avisa."
                ),
                puntos = listOf(
                    "Hay una fila por persona y por unidad familiar, con una casilla por día. Verde significa libre; con color, tiene algo apuntado (con su icono o su horario, y «+N» si hay varias cosas).",
                    "Toca una casilla para apuntar algo: elige la categoría (Trabajo, Médico, Viaje, Vacaciones, Otro u otra tuya) y su horario o sus fechas.",
                    "Las categorías «por horas» piden hora de inicio y de fin (se permite el turno de noche) y puedes guardar horarios con nombre, como «Mañana». Las «por días» piden fecha de inicio y de fin.",
                    "Con «Repetir en otros días» apuntas lo mismo en varios días a la vez.",
                    "Lo que apuntas en una unidad familiar se aplica a todos sus miembros.",
                    "El botón «Cuidadores» permite mostrar u ocultar personas, solo para la semana que estás viendo."
                ),
                nota = "Con el lápiz de la tarjeta «Categorías de estado de los cuidadores» puedes crear categorías propias (nombre, por horas o por días, icono y color) y decidir si «Ocupa a la persona». Una categoría que no ocupa es solo informativa y no genera avisos."
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
                    "Actividades: ver y editar todas las actividades, buscar y borrar varias a la vez.",
                    "Ocupaciones: repasa y borra lo apuntado en Familia, de hace un mes a un año adelante.",
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
