plugins {
    application
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

application {
    mainClassName = "com.amazon.cloudwatch.otel.metrics.MetricGenerator"
}

dependencies {
    implementation(platform("io.grpc:grpc-bom:1.29.0"))
    implementation("io.grpc:grpc-api")
    implementation("io.grpc:grpc-netty-shaded")
    implementation("io.opentelemetry:opentelemetry-sdk:0.6.0")
    implementation("io.opentelemetry:opentelemetry-exporters-otlp:0.6.0")
    implementation("io.opentelemetry:opentelemetry-extension-trace-propagators:0.6.0")
    implementation("io.opentelemetry:opentelemetry-sdk-extension-aws-v1-support:0.6.0")
    testCompile("junit", "junit", "4.12")
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
}