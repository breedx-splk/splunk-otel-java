plugins {
  id("splunk.shadow-conventions")
}

dependencies {
  annotationProcessor("com.google.auto.service:auto-service")
  compileOnly("com.google.auto.service:auto-service")

  compileOnly("io.opentelemetry.instrumentation:opentelemetry-instrumentation-api")
  compileOnly("io.opentelemetry.javaagent:opentelemetry-javaagent-extension-api")
  compileOnly("io.micrometer:micrometer-core")
}

tasks {
  compileJava {
    options.release.set(8)
  }
}
