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
    implementation("com.github.petrukhinandrew:jacodb:55eca1d90e6e7691562e920d29059b08c5fa02b7")
    testImplementation(kotlin("test"))
}
