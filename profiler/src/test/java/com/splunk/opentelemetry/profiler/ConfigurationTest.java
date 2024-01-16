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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import org.junit.jupiter.api.Test;

class ConfigurationTest {

  String logsEndpoint = "http://logs.example.com";
  String otelEndpoint = "http://otel.example.com";

  @Test
  void getConfigUrl_endpointDefined() {
    ConfigProperties config = mock(ConfigProperties.class);
    when(config.getString(Configuration.CONFIG_KEY_OTEL_OTLP_URL, null)).thenReturn(otelEndpoint);
    when(config.getString(Configuration.CONFIG_KEY_INGEST_URL, otelEndpoint))
        .thenReturn(logsEndpoint);
    String result = Configuration.getConfigUrl(config);
    assertEquals(result, logsEndpoint);
  }

  @Test
  void getConfigUrl_endpointNotDefined() {
    ConfigProperties config = mock(ConfigProperties.class);
    when(config.getString(Configuration.CONFIG_KEY_OTEL_OTLP_URL, null)).thenReturn(otelEndpoint);
    when(config.getString(Configuration.CONFIG_KEY_INGEST_URL, otelEndpoint))
        .thenReturn(otelEndpoint);
    String result = Configuration.getConfigUrl(config);
    assertEquals(result, otelEndpoint);
  }

  @Test
  void getConfigUrlNull() {
    ConfigProperties config = mock(ConfigProperties.class);
    when(config.getString(Configuration.CONFIG_KEY_OTEL_OTLP_URL, null)).thenReturn(null);
    when(config.getString(Configuration.CONFIG_KEY_INGEST_URL, null)).thenReturn(null);
    String result = Configuration.getConfigUrl(config);
    assertNull(result);
  }

  @Test
  void getConfigUrlSplunkRealm() {
    ConfigProperties config = mock(ConfigProperties.class);
    when(config.getString(Configuration.CONFIG_KEY_OTEL_OTLP_URL, null))
        .thenReturn("https://ingest.us0.signalfx.com");
    when(config.getString(Configuration.CONFIG_KEY_INGEST_URL, null)).thenReturn(null);
    String result = Configuration.getConfigUrl(config);
    assertNull(result);
  }
}
