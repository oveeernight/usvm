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
    implementation("com.github.petrukhinandrew:jacodb:9c123740628cdaf15d004c4e6a126a795cb8f281")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(11)
}
