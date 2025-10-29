package com.splunk.opentelemetry.instrumentation.jdbc.sqlserver;

import com.google.auto.service.AutoService;
import io.opentelemetry.javaagent.extension.AgentListener;
import io.opentelemetry.sdk.autoconfigure.AutoConfigureUtil;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;

@AutoService(AgentListener.class)
public class ServiceIdentifyingResourceHolder implements AgentListener {

  private static Resource resource = Resource.empty();

  @Override
  public void afterAgent(AutoConfiguredOpenTelemetrySdk sdk) {
    resource = AutoConfigureUtil.getResource(sdk);
  }

  static Resource getResource() {
    return resource;
  }
}
