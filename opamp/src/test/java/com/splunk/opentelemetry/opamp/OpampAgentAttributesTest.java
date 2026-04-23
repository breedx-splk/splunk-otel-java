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
import static io.opentelemetry.opamp.client.internal.request.service.HttpRequestService.DEFAULT_DELAY_BETWEEN_RETRIES;
import static io.opentelemetry.semconv.ServiceAttributes.SERVICE_INSTANCE_ID;
import static io.opentelemetry.semconv.ServiceAttributes.SERVICE_NAME;
import static io.opentelemetry.semconv.ServiceAttributes.SERVICE_NAMESPACE;
import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.Value;
import io.opentelemetry.instrumentation.testing.internal.AutoCleanupExtension;
import io.opentelemetry.opamp.client.OpampClient;
import io.opentelemetry.opamp.client.OpampClientBuilder;
import io.opentelemetry.opamp.client.internal.connectivity.http.OkHttpSender;
import io.opentelemetry.opamp.client.internal.request.delay.PeriodicDelay;
import io.opentelemetry.opamp.client.internal.request.service.HttpRequestService;
import io.opentelemetry.opamp.client.internal.response.MessageData;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.testing.internal.armeria.common.HttpResponse;
import io.opentelemetry.testing.internal.armeria.common.HttpStatus;
import io.opentelemetry.testing.internal.armeria.common.MediaType;
import io.opentelemetry.testing.internal.armeria.testing.junit5.server.mock.MockWebServerExtension;
import io.opentelemetry.testing.internal.armeria.testing.junit5.server.mock.RecordedRequest;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import opamp.proto.AgentToServer;
import opamp.proto.AnyValue;
import opamp.proto.ArrayValue;
import opamp.proto.KeyValue;
import opamp.proto.ServerErrorResponse;
import opamp.proto.ServerToAgent;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class OpampAgentAttributesTest {
  private static final MockWebServerExtension server = new MockWebServerExtension();

  @RegisterExtension static final AutoCleanupExtension cleanup = AutoCleanupExtension.create();

  @BeforeAll
  static void setUp() {
    server.start();
  }

  @BeforeEach
  void reset() {
    server.beforeTestExecution(null);
  }

  @AfterAll
  static void cleanUp() {
    server.stop();
  }

  @Test
  void addsIdentifyingAndNonIdentifyingAttributesWithExpectedTypes() throws Exception {
    Attributes attributes =
        Attributes.of(
                SERVICE_NAME,
                "test-service",
                SERVICE_NAMESPACE,
                "test-namespace",
                SERVICE_INSTANCE_ID,
                "test-instance",
                io.opentelemetry.semconv.ServiceAttributes.SERVICE_VERSION,
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

    enqueueEmptyResponse();

    OpampClientBuilder builder = OpampClient.builder();
    builder.setRequestService(
        HttpRequestService.create(
            OkHttpSender.create(server.httpUri().toString()),
            PeriodicDelay.ofFixedDuration(java.time.Duration.ofMillis(500)),
            DEFAULT_DELAY_BETWEEN_RETRIES));
    OpampAgentAttributes agentAttributes = new OpampAgentAttributes(Resource.create(attributes));
    agentAttributes.addIdentifyingAttributes(builder);
    agentAttributes.addNonIdentifyingAttributes(builder);

    OpampClient client = builder.build(new NoopCallbacks());
    cleanup.deferCleanup(client);

    RecordedRequest recordedRequest = server.takeRequest();
    AgentToServer agentToServer =
        AgentToServer.ADAPTER.decode(recordedRequest.request().content().array());

    List<KeyValue> identifyingAttributes = agentToServer.agent_description.identifying_attributes;
    assertThat(identifyingAttributes).hasSize(3);
    assertThat(identifyingAttributes)
        .anyMatch(
            kv ->
                kv.key.equals(SERVICE_NAME.getKey())
                    && kv.value.string_value.equals("test-service"));
    assertThat(identifyingAttributes)
        .anyMatch(
            kv ->
                kv.key.equals(SERVICE_NAMESPACE.getKey())
                    && kv.value.string_value.equals("test-namespace"));
    assertThat(identifyingAttributes)
        .anyMatch(
            kv ->
                kv.key.equals(SERVICE_INSTANCE_ID.getKey())
                    && kv.value.string_value.equals("test-instance"));
    List<KeyValue> nonIdentifyingAttributes =
        agentToServer.agent_description.non_identifying_attributes;
    assertThat(nonIdentifyingAttributes)
        .anyMatch(
            kv ->
                kv.key.equals(io.opentelemetry.semconv.ServiceAttributes.SERVICE_VERSION.getKey())
                    && kv.value.string_value.equals("test-version"));
    assertThat(nonIdentifyingAttributes)
        .anyMatch(kv -> kv.key.equals("long") && kv.value.int_value.equals(12L));
    assertThat(nonIdentifyingAttributes)
        .anyMatch(kv -> kv.key.equals("double") && kv.value.double_value.equals(99.0));
    assertThat(nonIdentifyingAttributes)
        .anyMatch(kv -> kv.key.equals("bool") && kv.value.bool_value.equals(true));
    assertThat(nonIdentifyingAttributes)
        .anyMatch(kv -> kv.key.equals("val") && kv.value.string_value.equals("vvv"));

    AnyValue longsArray =
        createArrayAttribute(
            new AnyValue.Builder().int_value(2L).build(),
            new AnyValue.Builder().int_value(3L).build(),
            new AnyValue.Builder().int_value(5L).build());
    assertThat(nonIdentifyingAttributes)
        .anyMatch(kv -> kv.key.equals("longarr") && kv.value.equals(longsArray));
    assertThat(nonIdentifyingAttributes)
        .anyMatch(kv -> kv.key.equals("longobjarr") && kv.value.equals(longsArray));
    assertThat(nonIdentifyingAttributes)
        .anyMatch(
            matching(
                "doublearr",
                new AnyValue.Builder().double_value(2.0).build(),
                new AnyValue.Builder().double_value(3.0).build()));
    assertThat(nonIdentifyingAttributes)
        .anyMatch(
            matching(
                "doubleobjarr",
                new AnyValue.Builder().double_value(5.0).build(),
                new AnyValue.Builder().double_value(6.0).build()));
    assertThat(nonIdentifyingAttributes)
        .anyMatch(
            matching(
                "stringarr",
                new AnyValue.Builder().string_value("foo").build(),
                new AnyValue.Builder().string_value("flimflam").build()));
    assertThat(nonIdentifyingAttributes)
        .anyMatch(
            matching(
                "stringobjarr",
                new AnyValue.Builder().string_value("flim").build(),
                new AnyValue.Builder().string_value("jibberjo").build()));
    assertThat(nonIdentifyingAttributes)
        .anyMatch(
            matching(
                "boolarr",
                new AnyValue.Builder().bool_value(true).build(),
                new AnyValue.Builder().bool_value(false).build()));
    assertThat(nonIdentifyingAttributes)
        .anyMatch(
            matching(
                "boolobjarr",
                new AnyValue.Builder().bool_value(true).build(),
                new AnyValue.Builder().bool_value(true).build(),
                new AnyValue.Builder().bool_value(false).build(),
                new AnyValue.Builder().bool_value(true).build()));
  }

  private static void enqueueEmptyResponse() {
    server.enqueue(
        HttpResponse.of(
            HttpStatus.OK, MediaType.X_PROTOBUF, new ServerToAgent.Builder().build().encode()));
  }

  private static Predicate<? super KeyValue> matching(String key, AnyValue... values) {
    return kv -> kv.key.equals(key) && kv.value.equals(createArrayAttribute(values));
  }

  private static AnyValue createArrayAttribute(AnyValue... values) {
    return new AnyValue.Builder()
        .array_value(new ArrayValue.Builder().values(Arrays.asList(values)).build())
        .build();
  }

  private static class NoopCallbacks implements OpampClient.Callbacks {
    @Override
    public void onConnect(OpampClient opampClient) {}

    @Override
    public void onConnectFailed(OpampClient opampClient, @Nullable Throwable throwable) {}

    @Override
    public void onErrorResponse(
        OpampClient opampClient, ServerErrorResponse serverErrorResponse) {}

    @Override
    public void onMessage(OpampClient opampClient, MessageData messageData) {}
  }
}
