plugins {
  id("splunk.instrumentation-conventions")
  // TODO: adding muzzle makes glasfish webengine attributes disappear
}

dependencies {
  compileOnly("org.glassfish.main.common:common-util:6.2.2")
}
