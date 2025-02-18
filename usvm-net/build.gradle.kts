plugins {
    id("usvm.kotlin-conventions")
}

dependencies {
    implementation(project(":usvm-core"))
    implementation(Libs.rd_core)
    implementation(Libs.rd_framework)
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.9.20")

    testImplementation(kotlin("test"))
    implementation("com.github.petrukhinandrew:jacodb:36fa9cbb85724446510054ba641bcb1bcf5e50d7")
}


tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(11)
}
