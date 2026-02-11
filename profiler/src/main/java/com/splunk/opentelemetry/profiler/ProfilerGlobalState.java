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

import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Exists just to provide access to a global state for JFR and other profiling related
 * collaborators.
 */
public class ProfilerGlobalState {

  private static AtomicReference<JfrActivator> activator = new AtomicReference<>();
  private static AtomicReference<AutoConfiguredOpenTelemetrySdk> sdk = new AtomicReference<>();

  public static void setSdk(AutoConfiguredOpenTelemetrySdk newSdk) {
    sdk.set(newSdk);
  }

  public AutoConfiguredOpenTelemetrySdk getSdk() {
    return sdk.get();
  }

  public static JfrActivator getActivator() {
    return activator.get();
  }

  public static void setActivator(JfrActivator newActivator) {
    activator.set(newActivator);
  }
}
