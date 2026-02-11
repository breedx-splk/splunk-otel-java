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

import java.time.Duration;

public class EffectiveProfilerConfiguration implements ProfilerConfiguration {

  private final ProfilerConfiguration delegate;
  private Boolean enabled;

  public EffectiveProfilerConfiguration(ProfilerConfiguration delegate) {
    this.delegate = delegate;
  }

  @Override
  public boolean isEnabled() {
    if (enabled != null) {
      return enabled;
    }
    return delegate.isEnabled();
  }

  public void setEnabled(Boolean enabled) {
    this.enabled = enabled;
  }

  @Override
  public void log() {
    delegate.log();
  }

  @Override
  public String getIngestUrl() {
    return delegate.getIngestUrl();
  }

  @Override
  public String getOtlpProtocol() {
    return delegate.getOtlpProtocol();
  }

  @Override
  public boolean getMemoryEnabled() {
    return delegate.getMemoryEnabled();
  }

  @Override
  public boolean getMemoryEventRateLimitEnabled() {
    return delegate.getMemoryEventRateLimitEnabled();
  }

  @Override
  public String getMemoryEventRate() {
    return delegate.getMemoryEventRate();
  }

  @Override
  public boolean getUseAllocationSampleEvent() {
    return delegate.getUseAllocationSampleEvent();
  }

  @Override
  public Duration getCallStackInterval() {
    return delegate.getCallStackInterval();
  }

  @Override
  public boolean getIncludeAgentInternalStacks() {
    return delegate.getIncludeAgentInternalStacks();
  }

  @Override
  public boolean getIncludeJvmInternalStacks() {
    return delegate.getIncludeJvmInternalStacks();
  }

  @Override
  public boolean getTracingStacksOnly() {
    return delegate.getTracingStacksOnly();
  }

  @Override
  public int getStackDepth() {
    return delegate.getStackDepth();
  }

  @Override
  public boolean getKeepFiles() {
    return delegate.getKeepFiles();
  }

  @Override
  public String getProfilerDirectory() {
    return delegate.getProfilerDirectory();
  }

  @Override
  public Duration getRecordingDuration() {
    return delegate.getRecordingDuration();
  }

  @Override
  public Object getConfigProperties() {
    return delegate.getConfigProperties();
  }

  public static int getJavaVersion() {
    return ProfilerConfiguration.getJavaVersion();
  }

  public EffectiveProfilerConfiguration replaceDelegate(ProfilerConfiguration delegate) {
    EffectiveProfilerConfiguration result = new EffectiveProfilerConfiguration(delegate);
    result.setEnabled(enabled);
    return result;
  }
}
