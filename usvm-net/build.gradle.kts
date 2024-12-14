plugins {
    id("usvm.kotlin-conventions")
}

dependencies {
    implementation(project(":usvm-core"))
    implementation(Libs.rd_core)
    implementation(Libs.rd_framework)
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.9.20")

    testImplementation(kotlin("test"))
    implementation("com.github.petrukhinandrew:jacodb:e84e4996aaf68619df2f3aaa0820e7fa66a2cded")
}


tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(11)
}
