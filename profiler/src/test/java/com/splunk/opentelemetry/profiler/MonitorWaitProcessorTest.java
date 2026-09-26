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
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.openjdk.jmc.common.item.Attribute.attr;
import static org.openjdk.jmc.common.unit.UnitLookup.EPOCH_NS;
import static org.openjdk.jmc.common.unit.UnitLookup.FLAG;
import static org.openjdk.jmc.common.unit.UnitLookup.MILLISECOND;
import static org.openjdk.jmc.common.unit.UnitLookup.PLAIN_TEXT;
import static org.openjdk.jmc.common.unit.UnitLookup.STACKTRACE;
import static org.openjdk.jmc.common.unit.UnitLookup.TIMESPAN;

import com.splunk.opentelemetry.profiler.exporter.InMemoryOtelLogger;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.openjdk.jmc.common.IDescribable;
import org.openjdk.jmc.common.IMCStackTrace;
import org.openjdk.jmc.common.item.IAccessorKey;
import org.openjdk.jmc.common.item.IAttribute;
import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IMemberAccessor;
import org.openjdk.jmc.common.item.IType;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.flightrecorder.JfrAttributes;

class MonitorWaitProcessorTest {
  @RegisterExtension final InMemoryOtelLogger logger = new InMemoryOtelLogger();

  @Test
  @SuppressWarnings({"unchecked", "rawtypes"})
  void logsEveryFieldExposedByTheEventType() {
    IAttribute<IQuantity> startTime = JfrAttributes.START_TIME;
    IAttribute<IQuantity> duration = JfrAttributes.DURATION;
    IAttribute<String> monitorClass = attr("testMonitorClass", "Monitor Class", PLAIN_TEXT);
    IAttribute<Boolean> timedOut = attr("testTimedOut", "Timed Out", FLAG);
    IAttribute<IQuantity> timeout = attr("timeout", "Timeout", TIMESPAN);
    IAttribute<String> notifier = attr("testNotifier", "Notifier", PLAIN_TEXT);
    IAttribute<IMCStackTrace> stackTrace = attr("testStackTrace", "Stack Trace", STACKTRACE);

    IType<IItem> eventType = mock(IType.class);
    IItem event = mock(IItem.class);
    when(event.getType()).thenReturn((IType) eventType);

    Map<IAccessorKey<?>, IDescribable> fields = new LinkedHashMap<>();
    fields.put(startTime.getKey(), startTime);
    fields.put(duration.getKey(), duration);
    fields.put(monitorClass.getKey(), monitorClass);
    fields.put(timedOut.getKey(), timedOut);
    fields.put(timeout.getKey(), timeout);
    fields.put(notifier.getKey(), notifier);
    fields.put(stackTrace.getKey(), stackTrace);
    doReturn(fields).when(eventType).getAccessorKeys();

    Instant eventTime = Instant.parse("2026-08-07T12:34:56.789123456Z");
    long eventTimestampNanos =
        TimeUnit.SECONDS.toNanos(eventTime.getEpochSecond()) + eventTime.getNano();
    IQuantity startTimeValue = EPOCH_NS.quantity(eventTimestampNanos);
    IMemberAccessor<IQuantity, IItem> startTimeAccessor = mock(IMemberAccessor.class);
    when(startTimeAccessor.getMember(event)).thenReturn(startTimeValue);
    when(eventType.getAccessor(startTime.getKey())).thenReturn(startTimeAccessor);

    IQuantity durationValue = MILLISECOND.quantity(1250);
    IMemberAccessor<IQuantity, IItem> durationAccessor = mock(IMemberAccessor.class);
    when(durationAccessor.getMember(event)).thenReturn(durationValue);
    when(eventType.getAccessor(duration.getKey())).thenReturn(durationAccessor);

    IMemberAccessor<String, IItem> monitorClassAccessor = mock(IMemberAccessor.class);
    when(monitorClassAccessor.getMember(event)).thenReturn("java.lang.Object");
    when(eventType.getAccessor(monitorClass.getKey())).thenReturn(monitorClassAccessor);

    IMemberAccessor<Boolean, IItem> timedOutAccessor = mock(IMemberAccessor.class);
    when(timedOutAccessor.getMember(event)).thenReturn(true);
    when(eventType.getAccessor(timedOut.getKey())).thenReturn(timedOutAccessor);

    IQuantity timeoutValue = MILLISECOND.quantity(25);
    IMemberAccessor<IQuantity, IItem> timeoutAccessor = mock(IMemberAccessor.class);
    when(timeoutAccessor.getMember(event)).thenReturn(timeoutValue);
    when(eventType.getAccessor(timeout.getKey())).thenReturn(timeoutAccessor);

    IMemberAccessor<String, IItem> notifierAccessor = mock(IMemberAccessor.class);
    when(notifierAccessor.getMember(event)).thenReturn(null);
    when(eventType.getAccessor(notifier.getKey())).thenReturn(notifierAccessor);

    IMCStackTrace stackTraceValue = mock(IMCStackTrace.class);
    when(stackTraceValue.getFrames()).thenReturn(java.util.Collections.emptyList());
    IMemberAccessor<IMCStackTrace, IItem> stackTraceAccessor = mock(IMemberAccessor.class);
    when(stackTraceAccessor.getMember(event)).thenReturn(stackTraceValue);
    when(eventType.getAccessor(stackTrace.getKey())).thenReturn(stackTraceAccessor);

    long beforeProcessing = TimeUnit.MILLISECONDS.toNanos(System.currentTimeMillis());
    new MonitorWaitProcessor(logger).accept(event);
    long afterProcessing = TimeUnit.MILLISECONDS.toNanos(System.currentTimeMillis());

    assertThat(logger.records())
        .singleElement()
        .satisfies(
            record -> {
              assertThat(record.getBodyValue().asString())
                  .isEqualTo(MonitorWaitProcessor.EVENT_NAME);
              assertThat(record.getTimestampEpochNanos()).isEqualTo(eventTimestampNanos);
              assertThat(record.getObservedTimestampEpochNanos())
                  .isBetween(beforeProcessing, afterProcessing);
              assertThat(record.getAttributes().size()).isEqualTo(fields.size());
              assertThat(record.getAttributes().get(longKey("jfr.startTime")))
                  .isEqualTo(eventTime.toEpochMilli());
              assertThat(record.getAttributes().get(doubleKey("jfr.duration"))).isEqualTo(1.25);
              assertThat(record.getAttributes().asMap())
                  .containsEntry(stringKey("jfr.testMonitorClass"), "java.lang.Object");
              assertThat(record.getAttributes().get(stringKey("jfr.testTimedOut")))
                  .isEqualTo("true");
              assertThat(record.getAttributes().get(doubleKey("jfr.timeout"))).isEqualTo(0.025);
              assertThat(record.getAttributes().get(stringKey("jfr.testNotifier")))
                  .isEqualTo("null");
              assertThat(record.getAttributes().get(stringKey("jfr.testStackTrace"))).isEmpty();
            });
  }
}
