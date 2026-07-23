plugins {
  id("splunk.instrumentation-conventions")
  // TODO: make this module work with muzzle
}

dependencies {
  compileOnly("org.apache.tomee:openejb-core:9.1.1")
}
