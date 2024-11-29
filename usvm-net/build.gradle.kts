plugins {
    kotlin("jvm")
}

group = "org.usvm"
version = "unspecified"

repositories {
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    implementation(project(":usvm-core"))
    testImplementation(kotlin("test"))
    implementation("com.github.oveeernight:jacodb:5ea471456fb01b2056871f3c80e0b8b74b0c965a")
}


tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(11)
}
