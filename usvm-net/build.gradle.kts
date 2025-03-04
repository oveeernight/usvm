import com.google.protobuf.gradle.id

plugins {
    id("usvm.kotlin-conventions")
    kotlin("plugin.serialization") version "2.1.10"
    id("com.google.protobuf") version "0.9.4"
}

dependencies {
    implementation(project(":usvm-core"))
    implementation(Libs.rd_core)
    implementation(Libs.rd_framework)
    implementation(Libs.ksmt_runner)
    implementation(Libs.ksmt_yices)
    implementation(Libs.ksmt_cvc5)
    implementation(Libs.ksmt_symfpu)
    implementation("org.jetbrains.kotlin:kotlin-reflect:2.1.0")


    testImplementation(kotlin("test"))
    testImplementation("com.google.protobuf:protobuf-java:4.29.0")
    testImplementation("com.google.protobuf:protobuf-kotlin:4.29.0")
    testImplementation("io.grpc:grpc-kotlin-stub:1.4.0")
//    testImplementation("io.grpc:grpc-stub:1.70.0")
    testImplementation("io.grpc:grpc-protobuf:1.70.0")
    testImplementation("io.grpc:grpc-okhttp:1.70.0")
    testImplementation(Libs.logback)

    implementation("com.github.petrukhinandrew:jacodb:d96473ca622e7cfc3b49ca925024ff2c74d2b1a7")
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:4.29.0"
    }

    plugins {
        id("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:1.70.0"
        }
        id("grpckt") {
            artifact = "io.grpc:protoc-gen-grpc-kotlin:1.4.0:jdk8@jar"
        }
    }

    generateProtoTasks {
        ofSourceSet("test").forEach {
            it.builtins {
                create("kotlin") {
                    outputSubDir = "/home/rnpozharskiy/work/usvm/usvm-net/src/test/kotlin"
                }
            }

            it.plugins {
                id("grpc") {
                    outputSubDir = "/home/rnpozharskiy/work/usvm/usvm-net/src/test/java/grpc"
                }

                id("grpckt") {
                    outputSubDir = "/home/rnpozharskiy/work/usvm/usvm-net/src/test/kotlin/grpc"
                }

            }
        }
    }
}

sourceSets {
    test {
        java {
            srcDirs += File("/home/rnpozharskiy/work/usvm/usvm-net/src/test/java")
            srcDirs += File("/home/rnpozharskiy/work/usvm/usvm-net/src/test/java/grpc")
        }
        kotlin {
            srcDirs += File("/home/rnpozharskiy/work/usvm/usvm-net/src/test/kotlin")
            srcDirs += File("/home/rnpozharskiy/work/usvm/usvm-net/src/test/kotlin/grpc")

        }
    }
}

task<Exec>("dotnet-samples") {
    workingDir(rootProject.rootDir)
    commandLine("dotnet", "publish", "-c", "Release", "usvm-net/src/test/dotnet/samples")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(11)
}
