import com.google.protobuf.gradle.id

plugins {
    id("usvm.kotlin-conventions")
    kotlin("plugin.serialization") version "2.1.10"
    id("com.google.protobuf") version "0.9.4"
}

//sourceSets {
//    main {
//        java {
//            srcDirs += File("/home/rnpozharskiy/work/usvm/usvm-net/src/test/kotlin/executor/generated/java")
//        }
//        kotlin {
//            srcDirs += File("/home/rnpozharskiy/work/usvm/usvm-net/src/test/kotlin/executor/generated/kotlin")
//        }
//    }
//}

dependencies {
    implementation(project(":usvm-core"))
    implementation(Libs.rd_core)
    implementation(Libs.rd_framework)
    implementation(Libs.ksmt_runner)
    implementation(Libs.ksmt_yices)
    implementation(Libs.ksmt_cvc5)
    implementation(Libs.ksmt_symfpu)

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.1")

    implementation("org.jetbrains.kotlin:kotlin-reflect:2.1.0")

//    implementation("com.google.protobuf:protobuf-java:4.28.2")
//    implementation("com.google.protobuf:protobuf-kotlin:4.28.2")


    testImplementation(kotlin("test"))
    testImplementation("com.google.protobuf:protobuf-java:4.29.0")
    testImplementation("com.google.protobuf:protobuf-kotlin:4.29.0")
    implementation("com.github.petrukhinandrew:jacodb:75dec37e320da87c31a17ed013bd350d57b9b615")
}

//protobuf {
//    protoc {
//        artifact = "com.google.protobuf:protoc:4.28.2"
//    }
//
//    generateProtoTasks {
//        all().forEach { task ->
//            task.builtins {
//                id("kotlin")
//            }
//        }
//    }
//}

task<Exec>("dotnet-samples") {
    workingDir(rootProject.rootDir)
    commandLine("dotnet", "publish", "usvm-net/src/test/dotnet/samples")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(11)
}
