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
package org.everit.jetty.server.ecm;

/**
 * Constants that are specifically available for Hash based session handler implementation. Common
 * constants are in {@link SessionHandlerConstants}.
 */
public final class SessionHandlerFactoryConstants {

  public static final String SERVICE_FACTORY_PID =
      "org.everit.jetty.server.ecm.HashSessionHandlerFactory";

  private SessionHandlerFactoryConstants() {
  }
}