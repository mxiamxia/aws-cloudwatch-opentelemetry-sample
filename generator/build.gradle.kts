plugins {
    java
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(platform("io.grpc:grpc-bom:1.29.0"))
    implementation("io.grpc:grpc-api")
    implementation("io.grpc:grpc-netty-shaded")
    implementation("io.opentelemetry:opentelemetry-sdk:0.4.1")
    implementation("io.opentelemetry:opentelemetry-exporters-otlp:0.4.1")
    testCompile("junit", "junit", "4.12")
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
}