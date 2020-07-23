plugins {
    java
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

tasks.register<Jar>("uberJar") {
    archiveClassifier.set("uber")

    from(sourceSets.main.get().output)

    dependsOn(configurations.runtimeClasspath)
    from({
        configurations.runtimeClasspath.get().filter { it.name.endsWith("jar") }.map { zipTree(it) }
    })
}

dependencies {
    implementation(platform("io.grpc:grpc-bom:1.29.0"))
    implementation("io.grpc:grpc-api")
    implementation("io.grpc:grpc-netty-shaded")
    compile("io.opentelemetry:opentelemetry-sdk:0.4.1")
    compile("io.opentelemetry:opentelemetry-exporters-otlp:0.4.1")
    testCompile("junit", "junit", "4.12")
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
}