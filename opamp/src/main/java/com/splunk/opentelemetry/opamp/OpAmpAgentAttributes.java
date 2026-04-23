package com.splunk.opentelemetry.opamp;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.AttributeType;
import io.opentelemetry.opamp.client.OpampClientBuilder;
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

  void addIdentifyingAttributes(OpampClientBuilder builder) {
    resource.getAttributes().asMap().entrySet().stream()
        .filter(entry -> IDENTIFYING_ATTRIBUTES.contains(entry.getKey().getKey()))
        .forEach(putAttribute(builder, true));
  }

  void addNonIdentifyingAttributes(OpampClientBuilder builder) {
    resource.getAttributes().asMap().entrySet().stream()
        .filter(entry -> !IDENTIFYING_ATTRIBUTES.contains(entry.getKey().getKey()))
        .forEach(putAttribute(builder, false));
  }

  private Consumer<? super Map.Entry<AttributeKey<?>, Object>> putAttribute(
      OpampClientBuilder builder, boolean identifying) {
    return entry -> {
      AttributeKey<?> key = entry.getKey();
      Object value = entry.getValue();
      AttributeType type = key.getType();

      // The java type system is truly insufferable.
      switch (type) {
        case STRING:
        case VALUE:
          if (identifying) {
            builder.putIdentifyingAttribute(key.getKey(), (String) makeValue(type, value));
          } else {
            builder.putNonIdentifyingAttribute(key.getKey(), (String) makeValue(type, value));
          }
          break;
        case LONG:
          if (identifying) {
            builder.putIdentifyingAttribute(key.getKey(), (long) makeValue(type, value));
          } else {
            builder.putNonIdentifyingAttribute(key.getKey(), (long) makeValue(type, value));
          }
          break;
        case DOUBLE:
          if (identifying) {
            builder.putIdentifyingAttribute(key.getKey(), (double) makeValue(type, value));
          } else {
            builder.putNonIdentifyingAttribute(key.getKey(), (double) makeValue(type, value));
          }
          break;
        case BOOLEAN:
          if (identifying) {
            builder.putIdentifyingAttribute(key.getKey(), (boolean) makeValue(type, value));
          } else {
            builder.putNonIdentifyingAttribute(key.getKey(), (boolean) makeValue(type, value));
          }
          break;
        case STRING_ARRAY:
          if (identifying) {
            builder.putIdentifyingAttribute(key.getKey(), (String[]) makeValue(type, value));
          } else {
            builder.putNonIdentifyingAttribute(key.getKey(), (String[]) makeValue(type, value));
          }
          break;
        case LONG_ARRAY: {
          if (identifying) {
            builder.putIdentifyingAttribute(key.getKey(), (long[]) makeValue(type, value));
          } else {
            builder.putNonIdentifyingAttribute(key.getKey(), (long[]) makeValue(type, value));
          }
          break;
        }
        case DOUBLE_ARRAY: {
          if (identifying) {
            builder.putIdentifyingAttribute(key.getKey(), (double[]) makeValue(type, value));
          } else {
            builder.putNonIdentifyingAttribute(key.getKey(), (double[]) makeValue(type, value));
          }
          break;
        }
        case BOOLEAN_ARRAY: {
          if (identifying) {
            builder.putIdentifyingAttribute(key.getKey(), (boolean[]) makeValue(type, value));
          } else {
            builder.putNonIdentifyingAttribute(key.getKey(), (boolean[]) makeValue(type, value));
          }
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
      case STRING_ARRAY: {
        List<String> typedValueList = (List<String>) value;
        return typedValueList.toArray(new String[] {});
      }
      case LONG_ARRAY: {
        List<Long> typedValueList = (List<Long>) value;
        long[] primitiveArray = new long[typedValueList.size()];
        for (int i = 0; i < typedValueList.size(); i++) {
          primitiveArray[i] = typedValueList.get(i);
        }
        return primitiveArray;
      }
      case DOUBLE_ARRAY: {
        List<Double> typedValueList = (List<Double>) value;
        double[] primitiveArray = new double[typedValueList.size()];
        for (int i = 0; i < typedValueList.size(); i++) {
          primitiveArray[i] = typedValueList.get(i);
        }
        return primitiveArray;
      }
      case BOOLEAN_ARRAY: {
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
}
