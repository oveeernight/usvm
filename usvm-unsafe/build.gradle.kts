plugins {
    kotlin("jvm")
    id("usvm.kotlin-conventions")
}

group = "org.usvm"
version = "unspecified"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":usvm-core"))
    implementation(Libs.ksmt_runner)
    implementation("com.github.petrukhinandrew:jacodb:dbc1537aeeb29ffad38c0159251829fe7c98e603")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(11)
}
