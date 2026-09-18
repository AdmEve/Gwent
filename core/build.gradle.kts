plugins {
    kotlin("jvm")
    application
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

kotlin {
    jvmToolchain(21)
}

application {
    mainClass.set("ir.gwent.core.cli.MainKt")
}

tasks.test {
    useJUnitPlatform()
}
