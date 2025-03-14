/*
 * Copyright Splunk Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.splunk.opentelemetry.profiler;

import static com.splunk.opentelemetry.profiler.LogExporterBuilder.EXTRA_CONTENT_TYPE;
import static com.splunk.opentelemetry.profiler.LogExporterBuilder.STACKTRACES_HEADER_VALUE;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.opentelemetry.exporter.otlp.http.logs.OtlpHttpLogRecordExporter;
import io.opentelemetry.exporter.otlp.http.logs.OtlpHttpLogRecordExporterBuilder;
import io.opentelemetry.exporter.otlp.logs.OtlpGrpcLogRecordExporter;
import io.opentelemetry.exporter.otlp.logs.OtlpGrpcLogRecordExporterBuilder;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.logs.export.LogRecordExporter;
import org.junit.jupiter.api.Test;

class LogExporterBuilderTest {

  @Test
  void testBuildSimpleGrpc() {
    ConfigProperties config = mock(ConfigProperties.class);
    OtlpGrpcLogRecordExporterBuilder builder = mock(OtlpGrpcLogRecordExporterBuilder.class);
    OtlpGrpcLogRecordExporter expected = mock(OtlpGrpcLogRecordExporter.class);

    when(builder.addHeader(EXTRA_CONTENT_TYPE, STACKTRACES_HEADER_VALUE)).thenReturn(builder);
    when(builder.build()).thenReturn(expected);

    LogRecordExporter exporter = LogExporterBuilder.buildGrpcExporter(config, () -> builder);
    assertSame(expected, exporter);
    verify(builder, never()).setEndpoint(anyString());
  }

  @Test
  void testBuildSimpleHttp() {
    ConfigProperties config = mock(ConfigProperties.class);
    OtlpHttpLogRecordExporterBuilder builder = mock(OtlpHttpLogRecordExporterBuilder.class);
    OtlpHttpLogRecordExporter expected = mock(OtlpHttpLogRecordExporter.class);

    when(builder.addHeader(EXTRA_CONTENT_TYPE, STACKTRACES_HEADER_VALUE)).thenReturn(builder);
    when(builder.build()).thenReturn(expected);

    LogRecordExporter exporter = LogExporterBuilder.buildHttpExporter(config, () -> builder);
    assertSame(expected, exporter);
    verify(builder, never()).setEndpoint(anyString());
  }

  @Test
  void testCustomEndpointGrpc() {
    String endpoint = "http://example.com:9122/";

    ConfigProperties config = mock(ConfigProperties.class);
    OtlpGrpcLogRecordExporterBuilder builder = mock(OtlpGrpcLogRecordExporterBuilder.class);
    OtlpGrpcLogRecordExporter expected = mock(OtlpGrpcLogRecordExporter.class);

    when(builder.addHeader(EXTRA_CONTENT_TYPE, STACKTRACES_HEADER_VALUE)).thenReturn(builder);
    when(builder.build()).thenReturn(expected);

    when(config.getString(Configuration.CONFIG_KEY_PROFILER_OTLP_PROTOCOL, null))
        .thenReturn("grpc");
    when(config.getString(Configuration.CONFIG_KEY_OTEL_OTLP_URL, null))
        .thenReturn("http://shadowed.example.com:9122/");
    when(config.getString(Configuration.CONFIG_KEY_INGEST_URL, "http://shadowed.example.com:9122/"))
        .thenReturn(endpoint);
    LogRecordExporter exporter = LogExporterBuilder.buildGrpcExporter(config, () -> builder);

    assertNotNull(exporter);
    verify(builder).setEndpoint(endpoint);
  }

  @Test
  void testCustomEndpointHttp() {
    String endpoint = "http://example.com:9122/";

    ConfigProperties config = mock(ConfigProperties.class);
    OtlpHttpLogRecordExporterBuilder builder = mock(OtlpHttpLogRecordExporterBuilder.class);
    OtlpHttpLogRecordExporter expected = mock(OtlpHttpLogRecordExporter.class);

    when(builder.addHeader(EXTRA_CONTENT_TYPE, STACKTRACES_HEADER_VALUE)).thenReturn(builder);
    when(builder.build()).thenReturn(expected);

    when(config.getString(Configuration.CONFIG_KEY_PROFILER_OTLP_PROTOCOL, null))
        .thenReturn("http/protobuf");
    when(config.getString(Configuration.CONFIG_KEY_OTEL_OTLP_URL, null))
        .thenReturn("http://shadowed.example.com:9122/");
    when(config.getString(
            Configuration.CONFIG_KEY_INGEST_URL, "http://shadowed.example.com:9122/v1/logs"))
        .thenReturn(endpoint);
    LogRecordExporter exporter = LogExporterBuilder.buildHttpExporter(config, () -> builder);

    assertNotNull(exporter);
    verify(builder).setEndpoint(endpoint);
  }
}
