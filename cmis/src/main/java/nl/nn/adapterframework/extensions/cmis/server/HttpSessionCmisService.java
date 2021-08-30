/*
   Copyright 2019 Nationale-Nederlanden

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package nl.nn.adapterframework.extensions.cmis.server;

import java.lang.reflect.Method;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.servlet.http.HttpSession;

import nl.nn.adapterframework.extensions.cmis.CmisSessionBuilder;
import nl.nn.adapterframework.extensions.cmis.CmisSessionException;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.extensions.cmis.server.impl.IbisDiscoveryService;
import nl.nn.adapterframework.extensions.cmis.server.impl.IbisNavigationService;
import nl.nn.adapterframework.extensions.cmis.server.impl.IbisObjectService;
import nl.nn.adapterframework.extensions.cmis.server.impl.IbisRepositoryService;

import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.chemistry.opencmis.commons.exceptions.CmisConnectionException;
import org.apache.chemistry.opencmis.commons.server.CallContext;
import org.apache.chemistry.opencmis.commons.spi.CmisBinding;
import org.apache.chemistry.opencmis.commons.spi.DiscoveryService;
import org.apache.chemistry.opencmis.commons.spi.NavigationService;
import org.apache.chemistry.opencmis.commons.spi.ObjectService;
import org.apache.chemistry.opencmis.commons.spi.RepositoryService;
import org.apache.log4j.Logger;

/**
 * Uses HTTP sessions to cache {@link CmisBinding} objects.
 */
public class HttpSessionCmisService extends CachedBindingCmisService {

	private static final long serialVersionUID = 1L;
	private final Logger log = LogUtil.getLogger(this);
	public static ThreadLocal<CallContext> callContext = new ThreadLocal<CallContext>();

	/** Key in the HTTP session. **/
	public static final String CMIS_BINDING = "org.apache.chemistry.opencmis.bridge.binding";

	private ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

	public HttpSessionCmisService(CallContext context) {
		setCallContext(context);
		callContext.set(context);
	}

	@Override
	public CmisBinding getCmisBindingFromCache() {
		HttpSession httpSession = getHttpSession(false);
		if (httpSession == null) {
			return null;
		}

		lock.readLock().lock();
		try {
			return (CmisBinding) httpSession.getAttribute(CMIS_BINDING);
		} finally {
			lock.readLock().unlock();
		}
	}

	@Override
	public CmisBinding putCmisBindingIntoCache(CmisBinding binding) {
		HttpSession httpSession = getHttpSession(true);

		lock.writeLock().lock();
		try {
			CmisBinding existingBinding = (CmisBinding) httpSession.getAttribute(CMIS_BINDING);
			if (existingBinding == null) {
				httpSession.setAttribute(CMIS_BINDING, binding);
			} else {
				binding = existingBinding;
			}

			return binding;
		} finally {
			lock.writeLock().unlock();
		}
	}

	/**
	 * Returns the current {@link HttpSession}.
	 * 
	 * @param create
	 *            <code>true</code> to create a new session, <code>false</code>
	 *            to return <code>null</code> if there is no current session
	 */
	public HttpSession getHttpSession(boolean create) {
		return getHttpServletRequest().getSession(create);
	}

	@Override
	public CmisBinding createCmisBinding() {
		AppConstants APP_CONSTANTS = AppConstants.getInstance();
		final String PROPERTY_PREFIX = "cmisbridge.";

		//Make sure cmisbridge properties are defined
		if(APP_CONSTANTS.getResolvedProperty(PROPERTY_PREFIX+"url") == null)
			throw new CmisConnectionException("no bridge properties found");

		CmisSessionBuilder sessionBuilder = new CmisSessionBuilder();
		for(Method method: sessionBuilder.getClass().getMethods()) {
			if(!method.getName().startsWith("set") || method.getParameterTypes().length != 1)
				continue;

			//Remove set from the method name
			String setter = firstCharToLower(method.getName().substring(3));
			String value = APP_CONSTANTS.getResolvedProperty(PROPERTY_PREFIX+setter);
			if(value == null)
				continue;

			//Only always grab the first value because we explicitly check method.getParameterTypes().length != 1
			Object castValue = getCastValue(method.getParameterTypes()[0], value);
			log.debug("trying to set property ["+PROPERTY_PREFIX+setter+"] with value ["+value+"] of type ["+castValue.getClass().getCanonicalName()+"] on ["+sessionBuilder+"]");

			try {
				method.invoke(sessionBuilder, castValue);
			} catch (Exception e) {
				throw new CmisConnectionException("error while calling method ["+setter+"] on CmisSessionBuilder ["+sessionBuilder.toString()+"]", e);
			}
		}

		try {
			Session session = sessionBuilder.build();
			return session.getBinding();
		}
		catch (CmisSessionException e) {
			log.error("unable to create cmis session", e);
			throw new CmisConnectionException(e.getMessage());
		}
	}

	private String firstCharToLower(String input) {
		return input.substring(0, 1).toLowerCase() + input.substring(1);
	}

	private Object getCastValue(Class<?> class1, String value) {
		String className = class1.getName().toLowerCase();
		if("boolean".equals(className))
			return Boolean.parseBoolean(value);
		else if("int".equals(className) || "integer".equals(className))
			return Integer.parseInt(value);
		else
			return value;
	}

	@Override
	public ObjectService getObjectService() {
		return new IbisObjectService(super.getObjectService());
	}

	@Override
	public RepositoryService getRepositoryService() {
		return new IbisRepositoryService(super.getRepositoryService());
	}

	@Override
	public DiscoveryService getDiscoveryService() {
		return new IbisDiscoveryService(super.getDiscoveryService());
	}

	@Override
	public NavigationService getNavigationService() {
		return new IbisNavigationService(super.getNavigationService());
	}
}
