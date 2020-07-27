package com.amazon.cloudwatch.otel.metrics;

import com.google.common.base.Strings;
import io.grpc.ManagedChannelBuilder;
import io.opentelemetry.OpenTelemetry;
import io.opentelemetry.common.Labels;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.DefaultContextPropagators;
import io.opentelemetry.exporters.otlp.OtlpGrpcMetricExporter;
import io.opentelemetry.exporters.otlp.OtlpGrpcSpanExporter;
import io.opentelemetry.extensions.trace.propagation.AwsXRayPropagator;
import io.opentelemetry.metrics.LongCounter;
import io.opentelemetry.metrics.LongValueRecorder;
import io.opentelemetry.metrics.Meter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.extensions.trace.aws.AwsXRayIdsGenerator;
import io.opentelemetry.sdk.extensions.trace.aws.resource.AwsResource;
import io.opentelemetry.sdk.metrics.export.IntervalMetricReader;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import io.opentelemetry.sdk.resources.EnvVarResource;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.TracerSdkProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.opentelemetry.trace.Span;
import io.opentelemetry.trace.Status;
import io.opentelemetry.trace.Tracer;

import java.util.Collections;
import java.util.Random;

public class MetricGenerator {

    private static final String OTLP_ENDPOINT = "otlp_endpoint";
    private static final String OTLP_INSTANCE_ID = "otlp_instance_id";
    private static String otlpEndpoint = "127.0.0.1:55680";
    private static String instanceID = "defaultid";

    private static final TracerSdkProvider TRACER_PROVIDER;

    static {
        if (!Strings.isNullOrEmpty(System.getenv(OTLP_ENDPOINT))) {
            otlpEndpoint = System.getenv(OTLP_ENDPOINT);
        }
        if (!Strings.isNullOrEmpty(System.getenv(OTLP_INSTANCE_ID))){
            instanceID = System.getenv(OTLP_INSTANCE_ID);
        }
        Resource resource = EnvVarResource.getResource().merge(AwsResource.create());

        TRACER_PROVIDER = TracerSdkProvider.builder()
                .setIdsGenerator(new AwsXRayIdsGenerator())
                .setResource(resource)
                .build();
        OpenTelemetry.setPropagators(DefaultContextPropagators.builder()
                .addHttpTextFormat(new AwsXRayPropagator())
                .build());
    }

    public static void main(String[] args) {
        try {
            System.out.println("otlp_endpoint:" + otlpEndpoint);
            generateOtlpData();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void generateOtlpData() throws InterruptedException {

        SpanExporter exporter =
                OtlpGrpcSpanExporter.newBuilder()
                .setChannel(ManagedChannelBuilder.forTarget(otlpEndpoint).usePlaintext().build())
                .build();

        // 1. init otlp exporter
        MetricExporter metricExporter =
                OtlpGrpcMetricExporter.newBuilder()
                        .setChannel(ManagedChannelBuilder.forTarget(otlpEndpoint).usePlaintext().build())
                        .build();

        // 2. setup metric collection interval
        IntervalMetricReader intervalMetricReader =
                IntervalMetricReader.builder()
                        .setMetricProducers(
                                Collections.singleton(OpenTelemetrySdk.getMeterProvider().getMetricProducer()))
                        .setExportIntervalMillis(5000)
                        .setMetricExporter(metricExporter)
                        .build();

        // 3. Build the OpenTelemetry `BatchSpanProcessor` with the `NewRelicSpanExporter`
        BatchSpanProcessor spanProcessor = BatchSpanProcessor.newBuilder(exporter).build();

        // 4. Add the span processor to the TracerProvider from the SDK
        OpenTelemetrySdk.getTracerProvider().addSpanProcessor(spanProcessor);

        Meter meter = OpenTelemetry.getMeterProvider().get("cloudwatch-otel", "1.0");
        Tracer tracer = TRACER_PROVIDER.get("cloudwatch-otel", "1.0");

        // 3. Here is an example of a counter
        LongCounter spanCounter =
                meter
                        .longCounterBuilder("spanCounter")
                        .setUnit("one")
                        .setDescription("Counting all the spans")
                        .build();

        // 4. Here's an example of a measure.
        LongValueRecorder spanTimer =
                meter
                        .longValueRecorderBuilder("spanTimer")
                        .setUnit("ms")
                        .setDescription("How long the spans take")
                        .build();

        // bound spanTimer metric instrument
        LongValueRecorder.BoundLongValueRecorder boundTimer = spanTimer.bind(Labels.of("spanName", "testSpan"));

        // 5. use these to instrument some work
        doSomeSimulatedWork(spanCounter, boundTimer, tracer);

        // 6. shutdown metric collector
        intervalMetricReader.shutdown();

    }

    private static void doSomeSimulatedWork(
            LongCounter spanCounter,
            LongValueRecorder.BoundLongValueRecorder boundTimer, Tracer tracer)
            throws InterruptedException {
        Random random = new Random();
        Span spanParent =
                tracer.spanBuilder("testSpan").startSpan();
        spanParent.end();
        for (int i = 0;; i++) {
            Span span =
                    tracer.spanBuilder("testSpan").setSpanKind(Span.Kind.INTERNAL).setParent(spanParent).startSpan();
            try (Scope ignored = tracer.withSpan(span)) {
                boolean markAsError = random.nextBoolean();
                if (markAsError) {
                    span.setStatus(Status.INTERNAL.withDescription("internalError"));
                }
                long startTime = System.currentTimeMillis();
                System.out.println("sending metric data: " + i);
                spanCounter.add(1, Labels.of("spanName", "testSpan", "isItAnError", "" + markAsError, "instanceId", instanceID));
                Thread.sleep(random.nextInt(1000));
                span.end();
                boundTimer.record(System.currentTimeMillis() - startTime);
            }
        }
    }
}
