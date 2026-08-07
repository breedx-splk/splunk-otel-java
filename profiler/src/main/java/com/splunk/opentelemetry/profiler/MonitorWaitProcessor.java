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

import static io.opentelemetry.api.common.AttributeKey.doubleKey;
import static io.opentelemetry.api.common.AttributeKey.longKey;
import static io.opentelemetry.api.common.AttributeKey.stringKey;
import static org.openjdk.jmc.common.unit.UnitLookup.EPOCH_MS;
import static org.openjdk.jmc.common.unit.UnitLookup.EPOCH_NS;
import static org.openjdk.jmc.common.unit.UnitLookup.SECOND;

import io.opentelemetry.api.logs.LogRecordBuilder;
import io.opentelemetry.api.logs.Logger;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmc.common.IMCStackTrace;
import org.openjdk.jmc.common.item.IAccessorKey;
import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IMemberAccessor;
import org.openjdk.jmc.common.item.IType;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.util.FormatToolkit;
import org.openjdk.jmc.flightrecorder.JfrAttributes;

public class MonitorWaitProcessor {
  public static final String EVENT_NAME = "jdk.JavaMonitorWait";
  public static final String INSTRUMENTATION_SCOPE_NAME = "splunk.jdk.monitor.locks";
  private static final String TIMEOUT_FIELD_NAME = "timeout";

  private final Logger logger;

  public MonitorWaitProcessor(Logger monitorWaitLogger) {
    this.logger = monitorWaitLogger;
  }

  public void accept(IItem event) {
    Instant observedTimestamp = Instant.now();
    IType<IItem> eventType = getItemType(event);
    IQuantity startTime = readField(event, eventType, JfrAttributes.START_TIME.getKey());
    LogRecordBuilder logRecord =
        logger
            .logRecordBuilder()
            .setBody(EVENT_NAME)
            .setTimestamp(startTime.clampedLongValueIn(EPOCH_NS), TimeUnit.NANOSECONDS)
            .setObservedTimestamp(observedTimestamp);

    for (IAccessorKey<?> key : eventType.getAccessorKeys().keySet()) {
      String attributeName = "jfr." + key.getIdentifier();
      if (JfrAttributes.START_TIME.getIdentifier().equals(key.getIdentifier())) {
        logRecord.setAttribute(longKey(attributeName), startTime.clampedLongValueIn(EPOCH_MS));
      } else if (isTimespanInSeconds(key)) {
        IQuantity timespan = (IQuantity) readField(event, eventType, key);
        logRecord.setAttribute(doubleKey(attributeName), timespan.doubleValueIn(SECOND));
      } else {
        setFormattedAttribute(logRecord, event, eventType, key, attributeName);
      }
    }

    logRecord.emit();
  }

  private static boolean isTimespanInSeconds(IAccessorKey<?> key) {
    String identifier = key.getIdentifier();
    return JfrAttributes.DURATION.getIdentifier().equals(identifier)
        || TIMEOUT_FIELD_NAME.equals(identifier);
  }

  private static <T> void setFormattedAttribute(
      LogRecordBuilder logRecord,
      IItem event,
      IType<IItem> eventType,
      IAccessorKey<T> key,
      String attributeName) {
    logRecord.setAttribute(
        stringKey(attributeName), formatField(readField(event, eventType, key), key));
  }

  private static <T> T readField(IItem event, IType<IItem> eventType, IAccessorKey<T> key) {
    IMemberAccessor<T, IItem> accessor = eventType.getAccessor(key);
    return accessor.getMember(event);
  }

  private static <T> String formatField(T value, IAccessorKey<T> key) {
    if (value == null) {
      return "null";
    }
    if (value instanceof IMCStackTrace) {
      return FormatToolkit.getHumanReadable((IMCStackTrace) value);
    }
    return key.getContentType().getDefaultFormatter().format(value);
  }

  @SuppressWarnings("unchecked")
  private static IType<IItem> getItemType(IItem item) {
    return (IType<IItem>) item.getType();
  }
}
