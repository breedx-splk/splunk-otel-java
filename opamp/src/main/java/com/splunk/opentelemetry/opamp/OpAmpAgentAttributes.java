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

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.AttributeType;
import io.opentelemetry.sdk.resources.Resource;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

class OpampAgentAttributes {
  private static final List<String> IDENTIFYING_ATTRIBUTES =
      Arrays.asList("service.name", "service.namespace", "service.instance.id");

  private final Resource resource;

  OpampAgentAttributes(Resource resource) {
    this.resource = resource;
  }

  void addIdentifyingAttributes(IdentifyingAttributeConsumer consumer) {
    resource.getAttributes().asMap().entrySet().stream()
        .filter(entry -> IDENTIFYING_ATTRIBUTES.contains(entry.getKey().getKey()))
        .forEach(putIdentifyingAttribute(consumer));
  }

  void addNonIdentifyingAttributes(NonIdentifyingAttributeConsumer consumer) {
    resource.getAttributes().asMap().entrySet().stream()
        .filter(entry -> !IDENTIFYING_ATTRIBUTES.contains(entry.getKey().getKey()))
        .forEach(putNonIdentifyingAttribute(consumer));
  }

  private Consumer<? super Map.Entry<AttributeKey<?>, Object>> putIdentifyingAttribute(
      IdentifyingAttributeConsumer consumer) {
    return entry -> {
      AttributeKey<?> key = entry.getKey();
      Object value = entry.getValue();
      AttributeType type = key.getType();

      // The java type system is truly insufferable.
      switch (type) {
        case STRING:
        case VALUE:
          consumer.putIdentifying(key.getKey(), (String) makeValue(type, value));
          break;
        case LONG:
          consumer.putIdentifying(key.getKey(), (long) makeValue(type, value));
          break;
        case DOUBLE:
          consumer.putIdentifying(key.getKey(), (double) makeValue(type, value));
          break;
        case BOOLEAN:
          consumer.putIdentifying(key.getKey(), (boolean) makeValue(type, value));
          break;
        case STRING_ARRAY:
          consumer.putIdentifying(key.getKey(), (String[]) makeValue(type, value));
          break;
        case LONG_ARRAY:
          {
            consumer.putIdentifying(key.getKey(), (long[]) makeValue(type, value));
            break;
          }
        case DOUBLE_ARRAY:
          {
            consumer.putIdentifying(key.getKey(), (double[]) makeValue(type, value));
            break;
          }
        case BOOLEAN_ARRAY:
          {
            consumer.putIdentifying(key.getKey(), (boolean[]) makeValue(type, value));
            break;
          }
      }
    };
  }

  private Consumer<? super Map.Entry<AttributeKey<?>, Object>> putNonIdentifyingAttribute(
      NonIdentifyingAttributeConsumer consumer) {
    return entry -> {
      AttributeKey<?> key = entry.getKey();
      Object value = entry.getValue();
      AttributeType type = key.getType();

      // The java type system is truly insufferable.
      switch (type) {
        case STRING:
        case VALUE:
          consumer.putNonIdentifying(key.getKey(), (String) makeValue(type, value));
          break;
        case LONG:
          consumer.putNonIdentifying(key.getKey(), (long) makeValue(type, value));
          break;
        case DOUBLE:
          consumer.putNonIdentifying(key.getKey(), (double) makeValue(type, value));
          break;
        case BOOLEAN:
          consumer.putNonIdentifying(key.getKey(), (boolean) makeValue(type, value));
          break;
        case STRING_ARRAY:
          consumer.putNonIdentifying(key.getKey(), (String[]) makeValue(type, value));
          break;
        case LONG_ARRAY:
          {
            consumer.putNonIdentifying(key.getKey(), (long[]) makeValue(type, value));
            break;
          }
        case DOUBLE_ARRAY:
          {
            consumer.putNonIdentifying(key.getKey(), (double[]) makeValue(type, value));
            break;
          }
        case BOOLEAN_ARRAY:
          {
            consumer.putNonIdentifying(key.getKey(), (boolean[]) makeValue(type, value));
            break;
          }
      }
    };
  }

  private Object makeValue(AttributeType attrType, Object value) {
    // More java type insanity
    switch (attrType) {
      case STRING:
      case LONG:
      case DOUBLE:
      case BOOLEAN:
        return value;
      case VALUE:
        return value.toString();
      case STRING_ARRAY:
        {
          List<String> typedValueList = (List<String>) value;
          return typedValueList.toArray(new String[] {});
        }
      case LONG_ARRAY:
        {
          List<Long> typedValueList = (List<Long>) value;
          long[] primitiveArray = new long[typedValueList.size()];
          for (int i = 0; i < typedValueList.size(); i++) {
            primitiveArray[i] = typedValueList.get(i);
          }
          return primitiveArray;
        }
      case DOUBLE_ARRAY:
        {
          List<Double> typedValueList = (List<Double>) value;
          double[] primitiveArray = new double[typedValueList.size()];
          for (int i = 0; i < typedValueList.size(); i++) {
            primitiveArray[i] = typedValueList.get(i);
          }
          return primitiveArray;
        }
      case BOOLEAN_ARRAY:
        {
          List<Boolean> typedValueList = (List<Boolean>) value;
          boolean[] primitiveArray = new boolean[typedValueList.size()];
          for (int i = 0; i < typedValueList.size(); i++) {
            primitiveArray[i] = typedValueList.get(i);
          }
          return primitiveArray;
        }
    }
    return null;
  }

  // Exists for testing
  interface IdentifyingAttributeConsumer {
    void putIdentifying(String key, String value);

    void putIdentifying(String key, long value);

    void putIdentifying(String key, double value);

    void putIdentifying(String key, boolean value);

    void putIdentifying(String key, String[] value);

    void putIdentifying(String key, long[] value);

    void putIdentifying(String key, double[] value);

    void putIdentifying(String key, boolean[] value);
  }

  // Exists for testing
  interface NonIdentifyingAttributeConsumer {
    void putNonIdentifying(String key, String value);

    void putNonIdentifying(String key, long value);

    void putNonIdentifying(String key, double value);

    void putNonIdentifying(String key, boolean value);

    void putNonIdentifying(String key, String[] value);

    void putNonIdentifying(String key, long[] value);

    void putNonIdentifying(String key, double[] value);

    void putNonIdentifying(String key, boolean[] value);
  }

  // Exists for testing
  interface AttributeConsumer
      extends IdentifyingAttributeConsumer, NonIdentifyingAttributeConsumer {}
}
