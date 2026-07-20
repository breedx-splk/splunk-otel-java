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

package com.splunk.opamp.remotecontrol;

import io.opentelemetry.api.logs.Logger;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.logging.Level;
import javax.management.MBeanServer;
import javax.management.ObjectName;

public class BigDumper {

  private static final String DIAGNOSTIC_COMMAND_MBEAN =
      "com.sun.management:type=DiagnosticCommand";

  private static final java.util.logging.Logger logger =
      java.util.logging.Logger.getLogger(BigDumper.class.getName());

  private final ThreadMXBean threadMXBean;
  private final BiConsumer<String, ThreadInfo[]> threadDumpExporter;
  private final Object lock = new Object();
  private final Logger threadDumpLogger;
  private ScheduledExecutorService executorService = null;
  private boolean dumping;

  public BigDumper(BiConsumer<String, ThreadInfo[]> threadDumpExporter, Logger threadDumpLogger) {
    this.threadDumpLogger = threadDumpLogger;
    this.threadMXBean = ManagementFactory.getThreadMXBean();
    this.threadDumpExporter = threadDumpExporter;
  }

  public boolean startPeriodicDumper(String jobId, int count, Duration interval) {
    if (count < 1) {
      logger.warning("Thread dump count must be positive.");
      return false;
    }
    if (interval.isZero() || interval.isNegative()) {
      logger.warning("Thread dump interval must be positive.");
      return false;
    }

    synchronized (lock) {
      if (dumping) {
        logger.info("Periodic thread dumping is already started. Skipping request.");
        return false;
      }
      dumping = true;
      if (count > 1) {
        executorService = Executors.newSingleThreadScheduledExecutor();
      }
    }

    if (count == 1) {
      try {
        dump(jobId);
        return true;
      } finally {
        finishDumping();
      }
    }

    synchronized (lock) {
      logger.info("Starting periodic thread dumps: count = " + count + " interval = " + interval);

      AtomicInteger counter = new AtomicInteger(count);
      executorService.scheduleWithFixedDelay(
          () -> {
            try {
              dump(jobId);
              if (counter.decrementAndGet() == 0) {
                logger.fine("Periodic thread dumping complete.");
                finishDumping();
              }
            } catch (RuntimeException exception) {
              logger.log(Level.WARNING, "Periodic thread dumping failed.", exception);
              finishDumping();
            }
          },
          0,
          interval.toMillis(),
          TimeUnit.MILLISECONDS);
      return true;
    }
  }

  private void finishDumping() {
    synchronized (lock) {
      if (executorService != null) {
        executorService.shutdown();
        executorService = null;
      }
      dumping = false;
    }
  }

  public void dump(String jobId) {
    logger.fine("Taking a thread dump");
    ThreadInfo[] threadInfos = threadMXBean.dumpAllThreads(true, true);
    threadDumpExporter.accept(jobId, threadInfos);

    String fullThreadDump = getFullThreadDump();
    if (fullThreadDump != null) {
      threadDumpLogger
          .logRecordBuilder()
          .setAttribute("splunk.thread.dump", true)
          .setAttribute("profiling.job.id", jobId)
          .setBody(fullThreadDump)
          .emit();
    }
  }

  /** Gets the large jcmd style hotspot wad as a single string. */
  private static String getFullThreadDump() {
    try {
      MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
      ObjectName diagnosticCommand = new ObjectName(DIAGNOSTIC_COMMAND_MBEAN);

      // This is the in-process form of `jcmd <pid> Thread.print`. Using HotSpot's formatter
      // preserves details that ThreadInfo does not expose, such as native thread ids, VM/GC
      // threads, elapsed times, native addresses, and the JNI reference counts.
      String threadDump =
          (String)
              mBeanServer.invoke(
                  diagnosticCommand,
                  "threadPrint",
                  new Object[] {new String[0]},
                  new String[] {String[].class.getName()});
      return currentProcessId() + ":" + System.lineSeparator() + threadDump;
      //      Files.write(DEBUG_THREAD_DUMP_PATH, output.getBytes(StandardCharsets.UTF_8));
    } catch (Exception exception) {
      // The DiagnosticCommand MBean is a HotSpot facility and may not exist on other JVMs.
      logger.log(Level.WARNING, "Unable to obtain a HotSpot diagnostic thread dump", exception);
    }
    return null;
  }

  private static String currentProcessId() {
    String runtimeName = ManagementFactory.getRuntimeMXBean().getName();
    int hostSeparator = runtimeName.indexOf('@');
    return hostSeparator < 0 ? runtimeName : runtimeName.substring(0, hostSeparator);
  }
}
