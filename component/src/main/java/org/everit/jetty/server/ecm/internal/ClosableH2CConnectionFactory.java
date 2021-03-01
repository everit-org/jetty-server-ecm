/*
 * Copyright (C) 2011 Everit Kft. (http://www.everit.org)
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
package org.everit.jetty.server.ecm.internal;

import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.Set;
import java.util.WeakHashMap;

import org.eclipse.jetty.http2.server.HTTP2CServerConnectionFactory;
import org.eclipse.jetty.io.Connection;
import org.eclipse.jetty.io.EndPoint;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConfiguration;

/**
 * A {@link HTTP2CServerConnectionFactory} that provides connections in the way that remembers all
 * referenced {@link EndPoint}s so they can be closed in case of a dynamic update.
 */
public class ClosableH2CConnectionFactory extends HTTP2CServerConnectionFactory implements
    CloseableHttpConfigurationProvider {

  private final WeakHashMap<EndPoint, Boolean> referencedEndpoints =
      new WeakHashMap<>();

  public ClosableH2CConnectionFactory(final HttpConfiguration config) {
    super(config);
  }

  private synchronized Set<EndPoint> cloneReferencedEndPoints() {
    Set<EndPoint> result = null;
    while (result == null) {
      try {
        result = new HashSet<>(this.referencedEndpoints.keySet());
      } catch (ConcurrentModificationException e) {
        // TODO probably some warn logging would be nice
      }
    }
    return result;
  }

  /**
   * Closes all endpoints that are referenced from anywhere.
   */
  @Override
  public void closeReferencedEndpoints() {
    Set<EndPoint> endPoints = cloneReferencedEndPoints();
    for (EndPoint endPoint : endPoints) {
      endPoint.close();
    }
  }

  @Override
  public synchronized Connection newConnection(final Connector connector, final EndPoint endPoint) {
    Connection result = super.newConnection(connector, endPoint);
    this.referencedEndpoints.put(result.getEndPoint(), Boolean.TRUE);
    return result;
  }

}
