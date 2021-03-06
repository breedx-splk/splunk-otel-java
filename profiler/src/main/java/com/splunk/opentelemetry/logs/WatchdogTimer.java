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

package com.splunk.opentelemetry.logs;

import static com.splunk.opentelemetry.profiler.util.Runnables.logUncaught;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

/**
 * A timer that invokes a callback at an interval, unless the reset() method is called. This class
 * is thread safe.
 */
class WatchdogTimer {

  private final ScheduledExecutorService executorService;
  private final Duration interval;
  private final Runnable callback;
  private final Object lock = new Object();
  private ScheduledFuture<?> future;

  WatchdogTimer(Duration interval, Runnable callback, ScheduledExecutorService executorService) {
    this.interval = interval;
    this.callback = callback;
    this.executorService = executorService;
  }

  void start() {
    synchronized (lock) {
      if (future != null) {
        throw new IllegalStateException("Already started");
      }
      reschedule();
    }
  }

  void reset() {
    synchronized (lock) {
      if (future == null) {
        throw new IllegalStateException("Not started");
      }
      future.cancel(false);
      reschedule();
    }
  }

  void stop() {
    synchronized (lock) {
      if (future == null) {
        throw new IllegalStateException("Not started");
      }
      future.cancel(false);
      future = null;
      executorService.shutdown();
    }
  }

  private void reschedule() {
    future =
        executorService.scheduleAtFixedRate(
            logUncaught(callback), interval.toMillis(), interval.toMillis(), MILLISECONDS);
  }
}
