plugins {
    id("usvm.kotlin-conventions")
}

dependencies {
    implementation(project(":usvm-core"))
    implementation(Libs.rd_core)
    implementation(Libs.rd_framework)
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.9.20")

    testImplementation(kotlin("test"))
    implementation("com.github.petrukhinandrew:jacodb:866e56c0bc155adaaa18faae8a10c6ce92aef431")
}


tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(11)
}
