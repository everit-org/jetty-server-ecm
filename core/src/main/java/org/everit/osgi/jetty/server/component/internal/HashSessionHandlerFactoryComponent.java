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

import java.io.File;
import java.io.IOException;
import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.WeakHashMap;

import javax.servlet.SessionTrackingMode;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionIdListener;
import javax.servlet.http.HttpSessionListener;

import org.eclipse.jetty.server.SessionManager;
import org.eclipse.jetty.server.session.HashSessionIdManager;
import org.eclipse.jetty.server.session.HashSessionManager;
import org.eclipse.jetty.server.session.SessionHandler;
import org.everit.osgi.ecm.annotation.AttributeOrder;
import org.everit.osgi.ecm.annotation.Component;
import org.everit.osgi.ecm.annotation.ConfigurationPolicy;
import org.everit.osgi.ecm.annotation.Service;
import org.everit.osgi.ecm.annotation.ServiceRef;
import org.everit.osgi.ecm.annotation.attribute.BooleanAttribute;
import org.everit.osgi.ecm.annotation.attribute.IntegerAttribute;
import org.everit.osgi.ecm.annotation.attribute.LongAttribute;
import org.everit.osgi.ecm.annotation.attribute.StringAttribute;
import org.everit.osgi.ecm.annotation.attribute.StringAttributes;
import org.everit.osgi.ecm.component.ConfigurationException;
import org.everit.osgi.ecm.extender.ECMExtenderConstants;
import org.everit.osgi.jetty.server.SessionHandlerFactory;
import org.everit.osgi.jetty.server.component.HashSessionHandlerFactoryConstants;
import org.everit.osgi.jetty.server.component.SessionHandlerConstants;
import org.osgi.framework.Constants;

import aQute.bnd.annotation.headers.ProvideCapability;

/**
 * Configurable component that creates a {@link SessionHandler} based on {@link HashSessionManager}
 * implementation.
 */
@Component(componentId = HashSessionHandlerFactoryConstants.FACTORY_PID,
    configurationPolicy = ConfigurationPolicy.FACTORY,
    localizationBase = "OSGI-INF/metatype/sessionHandlerFactory")
@ProvideCapability(ns = ECMExtenderConstants.CAPABILITY_NS_COMPONENT,
    value = ECMExtenderConstants.CAPABILITY_ATTR_CLASS + "=${@class}")
@StringAttributes({
    @StringAttribute(attributeId = Constants.SERVICE_DESCRIPTION, optional = true) })
@AttributeOrder({ SessionHandlerConstants.ATTR_MAX_INACTIVE_INTERVAL,
    SessionHandlerConstants.SERVICE_REF_SESSION_LISTENERS + ".target",
    SessionHandlerConstants.SERVICE_REF_SESSION_ATTRIBUTE_LISTENERS + ".target",
    SessionHandlerConstants.SERVICE_REF_SESSION_ID_LISTENERS + ".target",
    SessionHandlerConstants.ATTR_SECURE_REQUEST_ONLY,
    SessionHandlerConstants.ATTR_USING_COOKIES,
    SessionHandlerConstants.ATTR_COOKIE_NAME,
    SessionHandlerConstants.ATTR_USING_URLS,
    SessionHandlerConstants.SESSION_ID_PARAMETER_NAME,
    SessionHandlerConstants.ATTR_WORKER_NAME,
    SessionHandlerConstants.ATTR_CHECKING_REMOTE_SESSION_ID_ENCODING,
    SessionHandlerConstants.ATTR_SAVE_PERIOD,
    SessionHandlerConstants.ATTR_STORE_DIRECTORY,
    SessionHandlerConstants.ATTR_DELETE_UNRESTORABLE_SESSIONS,
    SessionHandlerConstants.ATTR_HTTP_ONLY,
    SessionHandlerConstants.ATTR_IDLE_SAVE_PERIOD,
    SessionHandlerConstants.ATTR_LAZY_LOAD,
    SessionHandlerConstants.ATTR_NODE_IN_SESSION_ID,
    SessionHandlerConstants.ATTR_REFRESH_COOKIE_AGE,
    SessionHandlerConstants.ATTR_SCAVENGE_PERIOD,
    SessionHandlerConstants.ATTR_RESEED,
    SessionHandlerConstants.SERVICE_REF_RANDOM + ".target",
    Constants.SERVICE_DESCRIPTION, })
@Service
public class HashSessionHandlerFactoryComponent implements SessionHandlerFactory {

  private boolean checkingRemoteSessionIdEncoding;

  private String cookieName;

  private boolean deleteUnrestorableSessions;

  private boolean httpOnly;

  private int idleSavePeriod;

  private boolean lazyLoad;

  private int maxInactiveInterval;

  private boolean nodeIdInSessionId;

  private Random random;

  private final WeakHashMap<HashSessionManager, Boolean> referencedSessionManagers =
      new WeakHashMap<>();

  private int refreshCookieAge;

  private long reseed;

  private int savePeriod;

  private int scavengePeriod;

  private boolean secureRequestOnly;

  private HttpSessionAttributeListener[] sessionAttributeListeners;

  private HttpSessionIdListener[] sessionIdListeners;

  private String sessionIdParameterName;

  private HttpSessionListener[] sessionListeners;

  private String storeDirectory;

  private boolean usingCookies;

  private boolean usingURLs;

  private String workerName;

  private void addListeners(final HashSessionManager sessionManager) {
    if (sessionListeners != null) {
      for (HttpSessionListener sessionListener : sessionListeners) {
        sessionManager.addEventListener(sessionListener);
      }
    }

    if (sessionAttributeListeners != null) {
      for (HttpSessionAttributeListener sessionAttributeListener : sessionAttributeListeners) {
        sessionManager.addEventListener(sessionAttributeListener);
      }
    }

    if (sessionIdListeners != null) {
      for (HttpSessionIdListener sessionIdListener : sessionIdListeners) {
        sessionManager.addEventListener(sessionIdListener);
      }
    }
  }

  private synchronized Set<HashSessionManager> cloneReferencedConnectionFactories() {
    Set<HashSessionManager> result = null;
    while (result == null) {
      try {
        result = new HashSet<>(referencedSessionManagers.keySet());
      } catch (ConcurrentModificationException e) {
        // TODO probably some warn logging would be nice
      }
    }
    return result;
  }

  @Override
  public synchronized SessionHandler createSessionHandler() {
    HashSessionManager sessionManager = new HashSessionManager();

    sessionManager.setMaxInactiveInterval(maxInactiveInterval);
    File storeDirFile = resolveStoreDirectory();
    if (storeDirFile != null) {
      try {
        sessionManager.setStoreDirectory(storeDirFile);
      } catch (IOException e) {
        throw new ConfigurationException("Could not set store directory: " + storeDirFile, e);
      }
    }
    sessionManager.setSavePeriod(savePeriod);
    sessionManager.setCheckingRemoteSessionIdEncoding(checkingRemoteSessionIdEncoding);
    sessionManager.setDeleteUnrestorableSessions(deleteUnrestorableSessions);
    sessionManager.setHttpOnly(httpOnly);
    sessionManager.setIdleSavePeriod(idleSavePeriod);
    sessionManager.setLazyLoad(lazyLoad);
    sessionManager.setNodeIdInSessionId(nodeIdInSessionId);
    sessionManager.setRefreshCookieAge(refreshCookieAge);
    sessionManager.setScavengePeriod(scavengePeriod);
    sessionManager.setSecureRequestOnly(secureRequestOnly);
    sessionManager.setSessionCookie(cookieName);
    sessionManager.setSessionIdPathParameterName(sessionIdParameterName);
    sessionManager.setSessionTrackingModes(resolveSessionTrackingModes());

    HashSessionIdManager hashSessionIdManager = new HashSessionIdManager();

    if (workerName != null) {
      hashSessionIdManager.setWorkerName(workerName);
    }
    hashSessionIdManager.setReseed(reseed);
    if (random != null) {
      hashSessionIdManager.setRandom(random);
    }

    sessionManager.setSessionIdManager(hashSessionIdManager);

    addListeners(sessionManager);

    // TODO add more configuration possibilities (also for the id manager)

    SessionHandler sessionHandler = new SessionHandler(sessionManager);

    return sessionHandler;
  }

  private Set<SessionTrackingMode> resolveSessionTrackingModes() {
    Set<SessionTrackingMode> result = new HashSet<SessionTrackingMode>();
    if (usingCookies) {
      result.add(SessionTrackingMode.COOKIE);
    }
    if (usingURLs) {
      result.add(SessionTrackingMode.URL);
    }
    return result;
  }

  private File resolveStoreDirectory() {
    if (storeDirectory == null) {
      return null;
    }
    return new File(storeDirectory);
  }

  @BooleanAttribute(attributeId = SessionHandlerConstants.ATTR_CHECKING_REMOTE_SESSION_ID_ENCODING,
      defaultValue = SessionHandlerConstants.DEFAULT_CHECKING_REMOTE_SESSION_ID_ENCODING)
  public void setCheckingRemoteSessionIdEncoding(final boolean checkingRemoteSessionIdEncoding) {
    this.checkingRemoteSessionIdEncoding = checkingRemoteSessionIdEncoding;
  }

  @StringAttribute(attributeId = SessionHandlerConstants.ATTR_COOKIE_NAME,
      defaultValue = SessionManager.__DefaultSessionCookie)
  public void setCookieName(final String cookieName) {
    this.cookieName = cookieName;
  }

  @BooleanAttribute(attributeId = SessionHandlerConstants.ATTR_DELETE_UNRESTORABLE_SESSIONS,
      defaultValue = SessionHandlerConstants.DEFAULT_DELETE_UNRESTORABLE_SESSIONS)
  public void setDeleteUnrestorableSessions(final boolean deleteUnrestorableSessions) {
    this.deleteUnrestorableSessions = deleteUnrestorableSessions;
  }

  @BooleanAttribute(attributeId = SessionHandlerConstants.ATTR_HTTP_ONLY,
      defaultValue = SessionHandlerConstants.DEFAULT_HTTP_ONLY)
  public void setHttpOnly(final boolean httpOnly) {
    this.httpOnly = httpOnly;
  }

  @IntegerAttribute(attributeId = SessionHandlerConstants.ATTR_IDLE_SAVE_PERIOD,
      defaultValue = SessionHandlerConstants.DEFAULT_IDLE_SAVE_PERIOD)
  public void setIdleSavePeriod(final int idleSavePeriod) {
    this.idleSavePeriod = idleSavePeriod;
  }

  @BooleanAttribute(attributeId = SessionHandlerConstants.ATTR_LAZY_LOAD,
      defaultValue = SessionHandlerConstants.DEFAULT_LAZY_LOAD)
  public void setLazyLoad(final boolean lazyLoad) {
    this.lazyLoad = lazyLoad;
  }

  /**
   * Sets the session-timeout on the component and on all referenced session managers.
   */
  @IntegerAttribute(attributeId = SessionHandlerConstants.ATTR_MAX_INACTIVE_INTERVAL,
      defaultValue = SessionHandlerConstants.DEFAULT_MAX_INACTIVE_INTERVAL, dynamic = true)
  public synchronized void setMaxInactiveInterval(final int maxInactiveInterval) {
    this.maxInactiveInterval = maxInactiveInterval;
    for (HashSessionManager sessionManager : cloneReferencedConnectionFactories()) {
      sessionManager.setMaxInactiveInterval(maxInactiveInterval);
    }
  }

  @BooleanAttribute(attributeId = SessionHandlerConstants.ATTR_NODE_IN_SESSION_ID,
      defaultValue = SessionHandlerConstants.DEFAULT_NODE_IN_SESSION_ID)
  public void setNodeIdInSessionId(final boolean nodeIdInSessionId) {
    this.nodeIdInSessionId = nodeIdInSessionId;
  }

  @ServiceRef(referenceId = SessionHandlerConstants.SERVICE_REF_RANDOM, optional = true)
  public void setRandom(final Random random) {
    this.random = random;
  }

  @IntegerAttribute(attributeId = SessionHandlerConstants.ATTR_REFRESH_COOKIE_AGE,
      defaultValue = SessionHandlerConstants.DEFAULT_REFRESH_COOKIE_AGE)
  public void setRefreshCookieAge(final int refreshCookieAge) {
    this.refreshCookieAge = refreshCookieAge;
  }

  @LongAttribute(attributeId = SessionHandlerConstants.ATTR_RESEED,
      defaultValue = SessionHandlerConstants.DEFAULT_RESEED)
  public void setReseed(final long reseed) {
    this.reseed = reseed;
  }

  @IntegerAttribute(attributeId = SessionHandlerConstants.ATTR_SAVE_PERIOD,
      defaultValue = SessionHandlerConstants.DEFAULT_SAVE_PERIOD)
  public void setSavePeriod(final int savePeriod) {
    this.savePeriod = savePeriod;
  }

  @IntegerAttribute(attributeId = SessionHandlerConstants.ATTR_SCAVENGE_PERIOD,
      defaultValue = SessionHandlerConstants.DEFAULT_SCAVENGE_PERIOD)
  public void setScavengePeriod(final int scavengePeriod) {
    this.scavengePeriod = scavengePeriod;
  }

  @BooleanAttribute(attributeId = SessionHandlerConstants.ATTR_SECURE_REQUEST_ONLY,
      defaultValue = SessionHandlerConstants.DEFAULT_SECURE_REQUEST_ONLY)
  public void setSecureRequestOnly(final boolean secureRequestOnly) {
    this.secureRequestOnly = secureRequestOnly;
  }

  @ServiceRef(referenceId = SessionHandlerConstants.SERVICE_REF_SESSION_ATTRIBUTE_LISTENERS,
      optional = true)
  public void setSessionAttributeListeners(
      final HttpSessionAttributeListener[] sessionAttributeListeners) {
    this.sessionAttributeListeners = sessionAttributeListeners;
  }

  @ServiceRef(referenceId = SessionHandlerConstants.SERVICE_REF_SESSION_ID_LISTENERS,
      optional = true)
  public void setSessionIdListeners(final HttpSessionIdListener[] sessionIdListeners) {
    this.sessionIdListeners = sessionIdListeners;
  }

  @StringAttribute(attributeId = SessionHandlerConstants.SESSION_ID_PARAMETER_NAME,
      defaultValue = SessionManager.__DefaultSessionIdPathParameterName)
  public void setSessionIdParameterName(final String sessionIdParameterName) {
    this.sessionIdParameterName = sessionIdParameterName;
  }

  @ServiceRef(referenceId = SessionHandlerConstants.SERVICE_REF_SESSION_LISTENERS, optional = true)
  public void setSessionListeners(final HttpSessionListener[] sessionListeners) {
    this.sessionListeners = sessionListeners;
  }

  @StringAttribute(attributeId = SessionHandlerConstants.ATTR_STORE_DIRECTORY, optional = true)
  public void setStoreDirectory(final String storeDirectory) {
    this.storeDirectory = storeDirectory;
  }

  @BooleanAttribute(attributeId = SessionHandlerConstants.ATTR_USING_COOKIES,
      defaultValue = SessionHandlerConstants.DEFAULT_USING_COOKIES)
  public void setUsingCookies(final boolean usingCookies) {
    this.usingCookies = usingCookies;
  }

  @BooleanAttribute(attributeId = SessionHandlerConstants.ATTR_USING_URLS,
      defaultValue = SessionHandlerConstants.DEFAULT_USING_URLS)
  public void setUsingURLs(final boolean usingURLs) {
    this.usingURLs = usingURLs;
  }

  @StringAttribute(attributeId = SessionHandlerConstants.ATTR_WORKER_NAME, optional = true)
  public void setWorkerName(final String workerName) {
    this.workerName = workerName;
  }

}