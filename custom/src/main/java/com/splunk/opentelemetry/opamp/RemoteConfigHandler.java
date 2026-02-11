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

import static com.splunk.opentelemetry.SplunkConfiguration.PROFILER_ENABLED_PROPERTY;
import static com.splunk.opentelemetry.SplunkConfiguration.isProfilerEnabled;
import static io.opentelemetry.sdk.autoconfigure.AutoConfigureUtil.getConfig;

import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import java.util.Arrays;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import opamp.proto.AgentConfigFile;
import opamp.proto.AgentRemoteConfig;

public class RemoteConfigHandler {
  private final AutoConfiguredOpenTelemetrySdk sdk;
  private static final Logger logger = Logger.getLogger(RemoteConfigHandler.class.getName());

  public RemoteConfigHandler(AutoConfiguredOpenTelemetrySdk sdk) {
    this.sdk = sdk;
  }

  public void handle(AgentRemoteConfig remoteConfig) {
    if (remoteConfig == null || remoteConfig.config == null) {
      return;
    }
    logger.info("HANDLING REMOTE CONFIG!");
    // TOOD: This is completely junky fiction that I"m just inventing on the fly here...
    AgentConfigFile configFile = remoteConfig.config.config_map.get("config");
    if (configFile == null) {
      return;
    }

    Map<String, String> configMap =
        Arrays.stream(configFile.body.utf8().split("\n"))
            .map(line -> line.split("="))
            .collect(
                Collectors.toMap(pair -> pair[0].toLowerCase().replace('_', '.'), pair -> pair[1]));

    // Hackity hack hack hack....TODO: Fixme
    // TODO: This doesn't see effective changes, so we're hosed unless we propagate here...
    String profilerConfig = configMap.get(PROFILER_ENABLED_PROPERTY);
    ConfigProperties existingConfigProps = getConfig(sdk);
    // Not enabled but needs to be
    if (!isProfilerEnabled(existingConfigProps) && "true".equals(profilerConfig)) {
      // TODO: Probably want to process other possible related config changes first
      // TODO: Activate the profiler if not already activated.
      logger.info("Remote config change: starting the profiler...");
      ProfilerBridge.startProfiler();
    }
    // Active but needs to be turned off
    else if (isProfilerEnabled(existingConfigProps) && "false".equals(profilerConfig)) {
      logger.info("Remote config change: stopping the profiler...");
      ProfilerBridge.stopProfiler();
    }

    // TODO: We also need to keep the remote config around so that we can blend it with our config
  }
}
