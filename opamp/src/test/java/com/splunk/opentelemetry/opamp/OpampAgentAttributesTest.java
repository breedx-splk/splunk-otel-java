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

package com.splunk.opentelemetry.opamp;

import static io.opentelemetry.api.common.AttributeKey.booleanKey;
import static io.opentelemetry.api.common.AttributeKey.doubleKey;
import static io.opentelemetry.api.common.AttributeKey.longKey;
import static io.opentelemetry.api.common.AttributeKey.valueKey;
import static io.opentelemetry.semconv.ServiceAttributes.SERVICE_INSTANCE_ID;
import static io.opentelemetry.semconv.ServiceAttributes.SERVICE_NAME;
import static io.opentelemetry.semconv.ServiceAttributes.SERVICE_NAMESPACE;
import static io.opentelemetry.semconv.ServiceAttributes.SERVICE_VERSION;
import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.Value;
import io.opentelemetry.sdk.resources.Resource;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class OpampAgentAttributesTest {

  @Test
  void addsIdentifyingAndNonIdentifyingAttributes() {
    Attributes attributes =
        Attributes.of(
                SERVICE_NAME,
                "test-service",
                SERVICE_NAMESPACE,
                "test-namespace",
                SERVICE_INSTANCE_ID,
                "test-instance",
                SERVICE_VERSION,
                "test-version")
            .toBuilder()
            .put(longKey("long"), 12L)
            .put(doubleKey("double"), 99.0)
            .put(booleanKey("bool"), true)
            .put(valueKey("val"), Value.of("vvv"))
            .put("longarr", new long[] {2L, 3L, 5L})
            .put(AttributeKey.longArrayKey("longobjarr"), Arrays.asList(2L, 3L, 5L))
            .put("doublearr", new double[] {2.0, 3.0})
            .put(AttributeKey.doubleArrayKey("doubleobjarr"), Arrays.asList(5.0, 6.0))
            .put("stringarr", new String[] {"foo", "flimflam"})
            .put(AttributeKey.stringArrayKey("stringobjarr"), Arrays.asList("flim", "jibberjo"))
            .put("boolarr", new boolean[] {true, false})
            .put(AttributeKey.booleanArrayKey("boolobjarr"), Arrays.asList(true, true, false, true))
            .build();

    FakeAttributeConsumer consumer = new FakeAttributeConsumer();
    OpampAgentAttributes agentAttributes = new OpampAgentAttributes(Resource.create(attributes));

    agentAttributes.addIdentifyingAttributes(consumer);
    agentAttributes.addNonIdentifyingAttributes(consumer);

    assertThat(consumer.identifying).hasSize(3);
    assertThat(consumer.identifying)
        .containsEntry(SERVICE_NAME.getKey(), "test-service")
        .containsEntry(SERVICE_NAMESPACE.getKey(), "test-namespace")
        .containsEntry(SERVICE_INSTANCE_ID.getKey(), "test-instance");

    assertThat(consumer.nonIdentifying)
        .containsEntry(SERVICE_VERSION.getKey(), "test-version")
        .containsEntry("long", 12L)
        .containsEntry("double", 99.0)
        .containsEntry("bool", true)
        .containsEntry("val", "vvv");
    assertThat((long[]) consumer.nonIdentifying.get("longarr")).containsExactly(2L, 3L, 5L);
    assertThat((long[]) consumer.nonIdentifying.get("longobjarr")).containsExactly(2L, 3L, 5L);
    assertThat((double[]) consumer.nonIdentifying.get("doublearr")).containsExactly(2.0, 3.0);
    assertThat((double[]) consumer.nonIdentifying.get("doubleobjarr")).containsExactly(5.0, 6.0);
    assertThat((String[]) consumer.nonIdentifying.get("stringarr"))
        .containsExactly("foo", "flimflam");
    assertThat((String[]) consumer.nonIdentifying.get("stringobjarr"))
        .containsExactly("flim", "jibberjo");
    assertThat((boolean[]) consumer.nonIdentifying.get("boolarr")).containsExactly(true, false);
    assertThat((boolean[]) consumer.nonIdentifying.get("boolobjarr"))
        .containsExactly(true, true, false, true);
  }

  static class FakeAttributeConsumer implements OpampAgentAttributes.AttributeConsumer {
    final Map<String, Object> identifying = new LinkedHashMap<>();
    final Map<String, Object> nonIdentifying = new LinkedHashMap<>();

    @Override
    public void putIdentifying(String key, String value) {
      identifying.put(key, value);
    }

    @Override
    public void putIdentifying(String key, long value) {
      identifying.put(key, value);
    }

    @Override
    public void putIdentifying(String key, double value) {
      identifying.put(key, value);
    }

    @Override
    public void putIdentifying(String key, boolean value) {
      identifying.put(key, value);
    }

    @Override
    public void putIdentifying(String key, String[] value) {
      identifying.put(key, value);
    }

    @Override
    public void putIdentifying(String key, long[] value) {
      identifying.put(key, value);
    }

    @Override
    public void putIdentifying(String key, double[] value) {
      identifying.put(key, value);
    }

    @Override
    public void putIdentifying(String key, boolean[] value) {
      identifying.put(key, value);
    }

    @Override
    public void putNonIdentifying(String key, String value) {
      nonIdentifying.put(key, value);
    }

    @Override
    public void putNonIdentifying(String key, long value) {
      nonIdentifying.put(key, value);
    }

    @Override
    public void putNonIdentifying(String key, double value) {
      nonIdentifying.put(key, value);
    }

    @Override
    public void putNonIdentifying(String key, boolean value) {
      nonIdentifying.put(key, value);
    }

    @Override
    public void putNonIdentifying(String key, String[] value) {
      nonIdentifying.put(key, value);
    }

    @Override
    public void putNonIdentifying(String key, long[] value) {
      nonIdentifying.put(key, value);
    }

    @Override
    public void putNonIdentifying(String key, double[] value) {
      nonIdentifying.put(key, value);
    }

    @Override
    public void putNonIdentifying(String key, boolean[] value) {
      nonIdentifying.put(key, value);
    }
  }
}
