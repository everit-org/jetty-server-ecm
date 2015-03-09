/*
 * Copyright (C) 2015 Everit Kft. (http://www.everit.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.everit.osgi.jetty.server.component.internal;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;

import org.eclipse.jetty.server.NetworkConnector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.everit.osgi.ecm.annotation.Activate;
import org.everit.osgi.ecm.annotation.Component;
import org.everit.osgi.ecm.annotation.ConfigurationPolicy;
import org.everit.osgi.ecm.annotation.Deactivate;
import org.everit.osgi.ecm.annotation.ReferenceConfigurationType;
import org.everit.osgi.ecm.annotation.ServiceRef;
import org.everit.osgi.ecm.component.ComponentContext;
import org.everit.osgi.ecm.component.ConfigurationException;
import org.everit.osgi.ecm.component.ServiceHolder;
import org.everit.osgi.ecm.extender.ECMExtenderConstants;
import org.everit.osgi.jetty.server.component.JettyServerConstants;
import org.everit.osgi.jetty.server.component.NetworkConnectorFactory;
import org.everit.osgi.jetty.server.component.ServletContextHandlerFactory;
import org.osgi.framework.ServiceRegistration;

import aQute.bnd.annotation.headers.ProvideCapability;

@Component(componentId = "org.everit.osgi.jetty.server.component.JettyServer",
    configurationPolicy = ConfigurationPolicy.FACTORY)
@ProvideCapability(ns = ECMExtenderConstants.CAPABILITY_NS_COMPONENT,
    value = ECMExtenderConstants.CAPABILITY_ATTR_CLASS + "=${@class}")
public class JettyServerComponent {

  @ServiceRef(setter = "setNetworkConnectorFactories",
      configurationType = ReferenceConfigurationType.CLAUSE, optional = false, dynamic = true)
  private ServiceHolder<NetworkConnectorFactory>[] networkConnectorFactories;

  private Server server;

  private ServiceRegistration<Server> serviceRegistration;

  @ServiceRef(setter = "setServletContextHandlerFactories", optional = true)
  private ServiceHolder<ServletContextHandlerFactory>[] servletContextHandlerFactories;

  @Activate
  public void activate(final ComponentContext<JettyServerComponent> componentContext) {

    server = new Server();
    ContextHandlerCollection contextHandlerCollection = new ContextHandlerCollection();

    server.setHandler(contextHandlerCollection);

    for (ServiceHolder<NetworkConnectorFactory> serviceHolder : networkConnectorFactories) {
      NetworkConnectorFactory networkConnectorFactory = serviceHolder.getService();
      Map<String, Object> attributes = serviceHolder.getAttributes();

      String host = resolveHostFromAttributes(attributes);
      int port = resolvePortFromAttributes(serviceHolder.getReferenceId(), attributes);

      NetworkConnector connector = networkConnectorFactory.createNetworkConnector(server, host,
          port);

      server.addConnector(connector);
    }

    addServletContextsToServer(contextHandlerCollection);

    Dictionary<String, Object> serviceProps = new Hashtable<String, Object>(
        componentContext.getProperties());

    try {
      server.start();
    } catch (Exception e) {
      try {
        server.stop();
        server.destroy();
      } catch (Exception stopE) {
        e.addSuppressed(stopE);
      }
      // TODO
      throw new RuntimeException(e);
    }
    serviceRegistration = componentContext.registerService(Server.class, server, serviceProps);
  }

  private void addServletContextsToServer(final ContextHandlerCollection contextHandlerCollection) {

    for (ServiceHolder<ServletContextHandlerFactory> holder : servletContextHandlerFactories) {
      Map<String, Object> attributes = holder.getAttributes();
      Object contextPath = attributes.get(JettyServerConstants.ATTR_CONTEXTPATH);

      if (contextPath == null) {
        throw new ConfigurationException("'" + JettyServerConstants.ATTR_CONTEXTPATH
            + "' attribute must be provided in clause");
      }

      contextHandlerCollection.addHandler(holder.getService().createHandler(
          contextHandlerCollection));
    }
  }

  @Deactivate
  public void deactivate() {
    if (serviceRegistration != null) {
      serviceRegistration.unregister();
    }
  }

  private String resolveHostFromAttributes(final Map<String, Object> attributes) {
    Object hostValue = attributes.get(JettyServerConstants.ATTR_HOST);
    if (hostValue == null) {
      return null;
    }
    return String.valueOf(hostValue);
  }

  private int resolvePortFromAttributes(final String referenceId,
      final Map<String, Object> attributes) {

    Object portValue = attributes.get(JettyServerConstants.ATTR_PORT);
    if (portValue == null) {
      return 0;
    }

    try {
      return Integer.parseInt(String.valueOf(portValue));
    } catch (NumberFormatException e) {
      throw new ConfigurationException("Invalid value for connector port of reference: "
          + referenceId, e);
    }
  }

  public void setNetworkConnectorFactories(
      final ServiceHolder<NetworkConnectorFactory>[] networkConnectorFactories) {
    this.networkConnectorFactories = networkConnectorFactories;
  }

  public void setServletContextHandlerFactories(
      final ServiceHolder<ServletContextHandlerFactory>[] servletContextHandlerFactories) {
    this.servletContextHandlerFactories = servletContextHandlerFactories;
  }
}
