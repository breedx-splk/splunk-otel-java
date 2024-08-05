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

package com.splunk.opentelemetry.servertiming;

import com.google.auto.service.AutoService;
import com.splunk.opentelemetry.javaagent.bootstrap.WebengineHolder;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.Context;
import io.opentelemetry.javaagent.bootstrap.http.HttpServerResponseCustomizer;
import io.opentelemetry.javaagent.bootstrap.http.HttpServerResponseMutator;
import java.util.logging.Logger;

/**
 * Adds {@code Server-Timing} header (and {@code Access-Control-Expose-Headers}) to the HTTP
 * response. The {@code Server-Timing} header contains the traceId and spanId of the server span.
 */
@AutoService(HttpServerResponseCustomizer.class)
public class ServerTimingHeaderCustomizer implements HttpServerResponseCustomizer {
  static final String SERVER_TIMING = "Server-Timing";
  static final String EXPOSE_HEADERS = "Access-Control-Expose-Headers";
  private static final Logger logger = Logger.getLogger(ServerTimingHeaderCustomizer.class.getName());

  // not using volatile because this field is set only once during agent initialization
  static boolean enabled = false;

  @Override
  public <RESPONSE> void customize(
      Context context, RESPONSE response, HttpServerResponseMutator<RESPONSE> responseMutator) {
    logger.info("SWAT7805 - Header customizer engaged.");
    if (!enabled) {
      logger.info("SWAT7805 - Not adding server timing header due to not enabled");
      return;
    }
    if (!Span.fromContext(context).getSpanContext().isValid()) {
      logger.info("SWAT7805 - Not adding server timing header due to invalid context!");
      return;
    }

    logger.info("SWAT7805 - Adding server header!");
    String headerValue = toHeaderValue(context);
    logger.info("SWAT7805 - Server header value: " + headerValue);
    responseMutator.appendHeader(response, SERVER_TIMING, headerValue);
    responseMutator.appendHeader(response, EXPOSE_HEADERS, SERVER_TIMING);
    logger.info("SWAT7805 - Header customizer exiting");
  }

  private static String toHeaderValue(Context context) {
    TraceParentHolder traceParentHolder = new TraceParentHolder();
    W3CTraceContextPropagator.getInstance()
        .inject(context, traceParentHolder, TraceParentHolder::set);
    return "traceparent;desc=\"" + traceParentHolder.traceParent + "\"";
  }

  private static class TraceParentHolder {
    String traceParent;

    public void set(String key, String value) {
      if ("traceparent".equals(key)) {
        traceParent = value;
      }
    }
  }
}
