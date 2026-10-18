package com.encaja.app.ui.informacion

/**
 * Help content in English. See [seccionesAyuda].
 * Button and screen names match what the app shows in English.
 */
internal val SECCIONES_AYUDA_EN: List<SeccionAyuda> = listOf(
    SeccionAyuda(
        titulo = "User guide",
        temas = listOf(
            TemaAyuda(
                titulo = "What Encaja is",
                parrafos = listOf(
                    "Encaja helps organize the logistics of a family with children: who drops off and who picks up each child at their activities, when each person is unavailable, the week's menu and the shopping list.",
                    "The app warns you about what's missing (activities with nobody assigned) and what clashes (someone assigned who has something else booked at that time), and each person receives on their phone the alerts that concern them."
                ),
                puntos = listOf(
                    "Bottom bar: the five tabs Week, Guide, Family, Menu and Shopping.",
                    "Top bar: the \"Invite someone\" button, your initials and the Settings button."
                )
            ),
            TemaAyuda(
                titulo = "First steps",
                puntos = listOf(
                    "1. Sign in with your email and password or with your Google account.",
                    "2. If you're starting from scratch, choose \"Create my family\" and enter your first name and surnames: you'll be the first person in the family, with the administrator role.",
                    "3. If someone in your family already uses Encaja, ask them for an invitation code and enter it under \"Join a family\". You'll be linked to your person, with their whole history.",
                    "4. In Settings > Children, add the children. In Settings > Caregivers, add the people who look after them and, if you like, groups such as \"the grandparents\".",
                    "5. If you created your own person by hand, go to Settings > Caregivers and tap the person icon next to your name to link your account to it.",
                    "6. Start adding activities in the Guide tab."
                )
            ),
            TemaAyuda(
                titulo = "Week: the traffic light",
                parrafos = listOf(
                    "This is the home screen. It shows the week with one circle per day. Today has an amber dot below it. Use the arrows to change week, and tap the title to pick a date on the calendar."
                ),
                puntos = listOf(
                    "Green: the day has activities and all of them are covered.",
                    "Red: an activity needs someone to go with the child and nobody is assigned to drop off or pick up. It's a gap.",
                    "Amber: everything is assigned, but the assigned person has a busy time booked in Family at that hour. It's a warning.",
                    "No colour: the day has passed or has no activities."
                ),
                nota = "Tap a red or amber day to jump to its card and resolve it: the activity opens so you can assign someone. If you tap a green day, you'll see a summary of who drops off and who picks up."
            ),
            TemaAyuda(
                titulo = "Family notice board",
                parrafos = listOf(
                    "Below the traffic light there's a notice board for the whole family. Write in \"New post\" and tap \"Post\". Each post is signed with your name and the date."
                ),
                puntos = listOf(
                    "All members with an account get a notification when someone posts, except the author.",
                    "Any member can delete a post with the bin icon. You're not asked to confirm."
                )
            ),
            TemaAyuda(
                titulo = "Invite someone",
                parrafos = listOf(
                    "For another person to join your family with their own account, tap \"Invite someone\" in the top bar."
                ),
                puntos = listOf(
                    "1. Choose which person in the list the invitation is for (they must exist first in Settings > Caregivers).",
                    "2. A 6-character code is generated. Tap \"Copy code\" and share it with that person.",
                    "3. They install Encaja, create their account or sign in, and enter the code under \"Join a family\".",
                    "4. From then on their account is linked to that person, with their whole history."
                ),
                nota = "The code works only once and expires after 14 days. If that person already has a linked account, another code can't be generated."
            ),
            TemaAyuda(
                titulo = "Guide: the day hour by hour",
                parrafos = listOf(
                    "Here you view and edit a day's activities. At the top you change day with the arrows or with \"Go to a date\", and \"Now\" takes you back to today and the current time."
                ),
                puntos = listOf(
                    "Timeline: one row per child, from 7:00 to midnight, with horizontal scrolling. Each block is an activity: green if covered, red if it's a gap.",
                    "The circles with initials show who drops off (at the start of the block) and who picks up (at the end).",
                    "If it's today, a red line marks the current time.",
                    "\"Day scale\" adjusts the zoom: less detail to see more hours, or more detail to spread them out.",
                    "Below, \"Activities of the day\" lists each child's activities with their time, who drops off, who picks up, and the alerts. The one in progress is marked \"IN PROGRESS\".",
                    "Tap a block or an activity in the list to edit it."
                )
            ),
            TemaAyuda(
                titulo = "Creating and editing an activity",
                parrafos = listOf(
                    "Tap the \"+\" button in the Guide. To edit an existing one, tap it."
                ),
                puntos = listOf(
                    "Child: choose who it's for.",
                    "Description: what it is (for example \"Football\" or \"School pick-up\"). Maximum 40 characters.",
                    "Icon: optional, to recognize it at a glance.",
                    "From / To: the start and end times. The end time must be later than the start time.",
                    "Needs accompaniment: turn it on if someone has to drop off or pick up. Turned off, it's information only and never creates gaps.",
                    "Who drops off and who picks up: can be a person, a family unit or nobody (it stays as a gap). It can be the same person for both.",
                    "Phone alerts: choose how long before it starts and ends you want the alert to sound (from 5 minutes to 2 hours). They work even if the activity doesn't need accompaniment.",
                    "Repeat on other days: mark individual days on the calendar or \"every week on\" certain days until a date. One activity is created per day."
                ),
                nota = "When an activity repeats, when editing it you can choose \"Save only this one\" or \"Save the whole series\": the series copies the changes (everything except the date) to every time it repeats. To change only certain weekdays (e.g. only Mondays): turn on \"Repeat on other days\", mark those days in the M T W T F S S row and tap \"Save the whole series\"; the app will ask whether the change is only for those days or for the whole series. \"Delete\" removes only that activity."
            ),
            TemaAyuda(
                titulo = "Family: when someone can't",
                parrafos = listOf(
                    "Here you record when each person can't be available: work, doctor, a trip... With that, the app warns you in Week (amber warning) if you assign someone an activity at a time when they're busy."
                ),
                puntos = listOf(
                    "Each person has a row with one small cell per day. Green: free. Coloured: something is booked.",
                    "Tap a cell to add something. Choose the type (Work, Doctor, Trip, Holiday, Other or one of your own) and then the schedule, or turn on \"All day\".",
                    "For Work and Doctor you choose the start and end times. If you often repeat a schedule, tap \"New schedule\", give it a name (\"Morning\", \"Afternoon\") and you'll have it one tap away.",
                    "For Trip and Holiday you choose from which day to which day.",
                    "What you record for a family group (for example \"the grandparents\") is recorded for all its members.",
                    "The \"Caregivers\" button lets you see only some people this week."
                ),
                nota = "With the pencil on the \"Caregiver status categories\" card you can create your own types, with a name, icon and colour. If a type doesn't \"Keep the person busy\", it's for information only and creates no warnings."
            ),
            TemaAyuda(
                titulo = "Family: repeat on several days",
                parrafos = listOf(
                    "To avoid entering the same thing day by day, use \"Repeat on other days\" inside the same window."
                ),
                puntos = listOf(
                    "Turn on \"Repeat on other days\". A calendar opens: tap the days you want the same thing. The day you're editing is already marked.",
                    "A small dot under a day means that person already has something of that type on that day. Days you don't mark stay unchanged.",
                    "Below there's a row with the weekday letters. Tap it to mark every Monday (or Tuesday...) until the \"Repeat until\" date.",
                    "Strong-coloured letter: that weekday is booked every week until that date. Light colour: only some weeks. No colour: none.",
                    "If you tap a strong-coloured letter, those days are removed and whatever was booked is deleted.",
                    "\"Repeat until\" starts at the last day that already has something booked; if there's none, 3 months ahead. You can change it.",
                    "Before saving changes that replace or delete several days, the app asks you to confirm."
                )
            ),
            TemaAyuda(
                titulo = "Family: delete several days at once",
                puntos = listOf(
                    "Press and hold a cell in the Family grid: it stays marked with a coloured border.",
                    "With the selection active, tap other cells, of any person or type, to add or remove them.",
                    "A bar appears at the bottom saying how many you have selected. Tap \"Delete\" and confirm: they are all cleared at once. \"Cancel\" leaves everything as it was."
                ),
                nota = "Deleting clears the whole cell, whatever was booked. This can't be undone."
            ),
            TemaAyuda(
                titulo = "Menu",
                parrafos = listOf(
                    "Plan lunch and dinner for each day of the week. Change week with the arrows."
                ),
                puntos = listOf(
                    "Tap the pencil next to \"Lunch\" or \"Dinner\", type the dish and confirm with the check mark.",
                    "If you leave it empty, it goes back to \"Not planned\".",
                    "Any family member can edit it."
                )
            ),
            TemaAyuda(
                titulo = "Shopping",
                parrafos = listOf(
                    "The family shopping list, organized by store."
                ),
                puntos = listOf(
                    "To add an item to a new store, use the panel at the bottom: type the item and the store, and tap \"+\". If the store already exists, the item goes there.",
                    "To add more to a store you already have, tap \"Add item\" at the end of its list.",
                    "Tick an item's box when you buy it: it stays on the list, ticked, but isn't deleted.",
                    "The pencil renames the item and the bin removes it.",
                    "Tap a store to collapse or expand it.",
                    "The \"Filter stores\" button lets you see only the stores you want at that moment."
                )
            ),
            TemaAyuda(
                titulo = "Alerts and notifications",
                parrafos = listOf(
                    "Each phone schedules its own activity alerts, and notice board notifications arrive over the internet."
                ),
                puntos = listOf(
                    "Whoever created the activity receives the drop-off and pick-up alerts.",
                    "Whoever is assigned to drop off receives the \"starts\" alert; whoever is assigned to pick up receives the \"ends\" alert. If the assigned person is a family unit, all its members receive it.",
                    "Old activities, created before this existed, alert everyone until they are saved again.",
                    "When another person changes an activity that affects you, your phone updates by itself."
                ),
                nota = "For alerts to sound on time, in Settings you can turn on \"Alarms and reminders\" and remove battery restrictions for the app. If your phone is a Xiaomi, Huawei or Samsung, also check \"Autostart\" or \"Protected apps\". Alerts are rescheduled when you open the app."
            ),
            TemaAyuda(
                titulo = "Settings",
                puntos = listOf(
                    "Children: add or remove children. Only the first name is asked for.",
                    "Caregivers: add or remove people, link your account to your person with the person icon, and create family units (groups that go together, like \"the maternal grandparents\") to assign them at once. Administrators can unlink another person's account.",
                    "Activities: view and edit all activities, search and delete several at once (they disappear immediately).",
                    "Busy times: review what has been booked in Family, from a month back to a year ahead. You can select several and delete them at once: they disappear immediately.",
                    "See the tips again: shows the first-time guides again.",
                    "Theme: light, dark or the system one.",
                    "Help, Contact and Privacy and legal."
                )
            ),
            TemaAyuda(
                titulo = "Signing out and deleting my account",
                puntos = listOf(
                    "Sign out: leaves your account on this phone. Your data and your link to the family are kept.",
                    "Delete my account: removes your sign-in account and your link to the family, and you stop receiving alerts. Your person and their whole history stay in the family, and another account can link to them later with an invitation."
                ),
                nota = "If you're the last person with an account in the family, when you delete your account the app shows you a code. Write it down: with it, whoever joins next takes over your person with their whole history."
            ),
            TemaAyuda(
                titulo = "Good to know",
                puntos = listOf(
                    "Activities and busy times older than a month are deleted automatically when you open the app, so data that's no longer needed doesn't pile up.",
                    "Almost everything can be done by any family member: editing, deleting, adding children or people. The only action reserved for administrators is unlinking another person's account.",
                    "Each person should use their own account and link to their own profile: that way the alerts and initials are theirs."
                )
            )
        )
    )
)
