plugins {
    id("usvm.kotlin-conventions")
    kotlin("plugin.serialization") version "1.9.20"
}

dependencies {
    implementation(project(":usvm-core"))
    implementation(Libs.rd_core)
    implementation(Libs.rd_framework)
    implementation(Libs.ksmt_runner)
    implementation(Libs.ksmt_yices)
    implementation(Libs.ksmt_cvc5)
    implementation(Libs.ksmt_symfpu)

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.1")

    implementation("org.jetbrains.kotlin:kotlin-reflect:1.9.20")

    testImplementation(kotlin("test"))
    implementation("com.github.petrukhinandrew:jacodb:e84e4996aaf68619df2f3aaa0820e7fa66a2cded")
}

task<Exec>("dotnet-samples") {
    commandLine("dotnet publish /home/rnpozharskiy/work/usvm/usvm-net/src/test/dotnet/samples")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(11)
}
