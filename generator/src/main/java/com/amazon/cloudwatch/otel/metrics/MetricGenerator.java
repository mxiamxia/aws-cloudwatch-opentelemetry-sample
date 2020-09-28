package com.amazon.cloudwatch.otel.metrics;

import com.google.common.base.Strings;
import io.grpc.ManagedChannel;
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
import io.opentelemetry.sdk.metrics.export.IntervalMetricReader;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.TracerSdkProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.opentelemetry.trace.Span;
import io.opentelemetry.trace.Status;
import io.opentelemetry.trace.Tracer;

import java.util.Collections;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class MetricGenerator {

    private static final String OTLP_ENDPOINT = "otlp_endpoint";
    private static final String OTLP_INSTANCE_ID = "otlp_instance_id";
    private static String otlpEndpoint = "127.0.0.1:55680";

    static {
        if (!Strings.isNullOrEmpty(System.getenv(OTLP_ENDPOINT))) {
            otlpEndpoint = System.getenv(OTLP_ENDPOINT);
        }
    }

    public static void main(String[] args) {
        try {
            System.out.println("otlp_endpoint:" + otlpEndpoint);
            sendData();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void sendData() throws InterruptedException {
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

        generateOtlpData();
        intervalMetricReader.shutdown();



    }
    private static void generateOtlpData() throws InterruptedException {
        Meter meter = OpenTelemetry.getMeter("instrumentation-library-name", "ying:1.0.0");
        LongCounter counter = meter
          .longCounterBuilder("processed_jobs")
          .setDescription("Processed jobs")
          .setUnit("1")
          .build();

        // It is recommended that the API user keep a reference to a Bound Counter for the entire time or
        // call unbind when no-longer needed.
        LongCounter.BoundLongCounter someWorkCounter = counter.bind(Labels.of("Key", "SomeWork"));

        for(int i=0; i != 10000; ++i) {
            // Record data
            someWorkCounter.add(123);

            // Alternatively, the user can use the unbounded counter and explicitly
            // specify the labels set at call-time:
            counter.add(123, Labels.of("Key", "SomeWork"));

            TimeUnit.SECONDS.sleep(1);
        }

    }
}
