dependencies {
  // slf4j is included in the otel javaagent, no need to add it here too
  compileOnly("org.slf4j:slf4j-api")
  // add micrometer to the bootstrap classloader so that it's available in instrumentations
  implementation("io.micrometer:micrometer-core")
}

tasks {
  compileJava {
    options.release.set(8)
  }
}
