package com.splunk.opentelemetry.instrumentation.jdbc.sqlserver;


import static io.opentelemetry.javaagent.extension.matcher.AgentElementMatchers.hasClassesNamed;
import static net.bytebuddy.matcher.ElementMatchers.isConstructor;
import static net.bytebuddy.matcher.ElementMatchers.named;

import com.microsoft.sqlserver.jdbc.SQLServerConnection;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

public class SqlServerConnectionInstrumentation implements TypeInstrumentation {

  @Override
  public ElementMatcher<ClassLoader> classLoaderOptimization() {
    return hasClassesNamed("com.microsoft.sqlserver.jdbc.SQLServerConnectionxx");
  }

  @Override
  public ElementMatcher<TypeDescription> typeMatcher() {
    return named("com.microsoft.sqlserver.jdbc.SQLServerConnectionxx");
  }

  @Override
  public void transform(TypeTransformer transformer) {
    transformer.applyAdviceToMethod(
        isConstructor(),
        getClass().getName() + "$SQLServerConnectionAdvice");
  }

  public static class SqlServerConnectionAdvice {
    @Advice.OnMethodExit
    public static void onExit(@Advice.This SQLServerConnection connection) {
      System.exit(1);
      SqlServerResourcePropagator.INSTANCE.propagate(connection);
    }
  }


}
