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
    implementation("com.github.petrukhinandrew:jacodb:d96473ca622e7cfc3b49ca925024ff2c74d2b1a7")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(11)
}
