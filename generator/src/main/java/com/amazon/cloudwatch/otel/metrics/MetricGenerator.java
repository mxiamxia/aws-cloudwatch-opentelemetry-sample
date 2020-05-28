package com.amazon.cloudwatch.otel.metrics;

import io.grpc.ManagedChannelBuilder;
import io.opentelemetry.OpenTelemetry;
import io.opentelemetry.exporters.otlp.OtlpGrpcMetricExporter;
import io.opentelemetry.metrics.LongCounter;
import io.opentelemetry.metrics.LongMeasure;
import io.opentelemetry.metrics.Meter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.metrics.export.IntervalMetricReader;
import io.opentelemetry.sdk.metrics.export.MetricExporter;

import java.util.Collections;
import java.util.Random;

public class MetricGenerator {

    private static final String OTLP_ENDPOINT = "otlp.endpoint";
    private static String otlpEndpoint = "127.0.0.1:55680";

    public static void main(String[] args) {
        try {
            generateMetrics();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static void generateMetrics() throws InterruptedException {

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

        Meter meter = OpenTelemetry.getMeterProvider().get("cloudwatch-otel", "1.0");

        // 3. Here is an example of a counter
        LongCounter spanCounter =
                meter
                        .longCounterBuilder("spanCounter")
                        .setUnit("one")
                        .setDescription("Counting all the spans")
                        .setMonotonic(true)
                        .build();

        // 4. Here's an example of a measure.
        LongMeasure spanTimer =
                meter
                        .longMeasureBuilder("spanTimer")
                        .setUnit("ms")
                        .setDescription("How long the spans take")
                        .setAbsolute(true)
                        .build();

        // bound spanTimer metric instrument
        LongMeasure.BoundLongMeasure boundTimer = spanTimer.bind("spanName", "testSpan");

        // 5. use these to instrument some work
        doSomeSimulatedWork(spanCounter, boundTimer);

        // 6. shutdown metric collector
        intervalMetricReader.shutdown();

    }

    private static void doSomeSimulatedWork(
            LongCounter spanCounter,
            LongMeasure.BoundLongMeasure boundTimer)
            throws InterruptedException {
        Random random = new Random();
        for (int i = 0;; i++) {
            long startTime = System.currentTimeMillis();
            boolean markAsError = random.nextBoolean();
            System.out.println("sending metric data: " + i);
            spanCounter.add(1, "spanName", "testSpan", "isItAnError", "" + markAsError);
            // do some work
            Thread.sleep(random.nextInt(1000));
            boundTimer.record(System.currentTimeMillis() - startTime);
        }
    }
}
