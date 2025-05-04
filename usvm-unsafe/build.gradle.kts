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
    implementation("com.github.petrukhinandrew:jacodb:deec7a248a390c60d39d1671539afc4202b3f398")
    testImplementation(kotlin("test"))
}
