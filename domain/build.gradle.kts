// Módulo :domain — Kotlin puro, sin dependencia de Android ni de Firebase.
// Así se testea con JUnit normal (sin emulador) y es la parte reutilizable
// si algún día se hace una versión Kotlin Multiplatform.
plugins {
    kotlin("jvm")
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
}

tasks.test {
    useJUnitPlatform()
}
