plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":domain"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
