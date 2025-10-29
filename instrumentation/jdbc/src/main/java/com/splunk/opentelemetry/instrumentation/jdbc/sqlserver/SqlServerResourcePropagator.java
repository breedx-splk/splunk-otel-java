package com.splunk.opentelemetry.instrumentation.jdbc.sqlserver;

import com.microsoft.sqlserver.jdbc.SQLServerConnection;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.semconv.ServiceAttributes;
import io.opentelemetry.semconv.incubating.DeploymentIncubatingAttributes;
import io.opentelemetry.semconv.incubating.ServiceIncubatingAttributes;

public class SqlServerResourcePropagator {

  static  SqlServerResourcePropagator INSTANCE = new SqlServerResourcePropagator();

  private SqlServerResourcePropagator(){
  }

  void propagate(SQLServerConnection connection) {
    setSessionVar(connection, ServiceAttributes.SERVICE_NAME);
    setSessionVar(connection, ServiceIncubatingAttributes.SERVICE_NAMESPACE);
    setSessionVar(connection, DeploymentIncubatingAttributes.DEPLOYMENT_ENVIRONMENT_NAME);
  }

  private static void setSessionVar(SQLServerConnection connection, AttributeKey<String> attr) {
      Resource resource = ServiceIdentifyingResourceHolder.getResource();
//      String value = resource.getAttribute(attr);
//      if(value == null){
//        return;
//      }
//      try {
//        PreparedStatement statement = connection.prepareStatement("EXEC sp_set_session_context ?, ?");
//        statement.setBytes(1, attr.getKey().getBytes(UTF_8));
//        statement.setBytes(2, value.getBytes(UTF_8));
//        statement.executeUpdate();
//      } catch (SQLException e) {
//        e.printStackTrace();  //TODO: Remove me
//      }
  }
}



