/*
   Copyright 2019 Nationale-Nederlanden, 2020 WeAreFrank!

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

import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.chemistry.opencmis.commons.exceptions.CmisConnectionException;
import org.apache.chemistry.opencmis.commons.server.CallContext;
import org.apache.chemistry.opencmis.commons.spi.AclService;
import org.apache.chemistry.opencmis.commons.spi.CmisBinding;
import org.apache.chemistry.opencmis.commons.spi.DiscoveryService;
import org.apache.chemistry.opencmis.commons.spi.MultiFilingService;
import org.apache.chemistry.opencmis.commons.spi.NavigationService;
import org.apache.chemistry.opencmis.commons.spi.ObjectService;
import org.apache.chemistry.opencmis.commons.spi.PolicyService;
import org.apache.chemistry.opencmis.commons.spi.RelationshipService;
import org.apache.chemistry.opencmis.commons.spi.RepositoryService;
import org.apache.chemistry.opencmis.commons.spi.VersioningService;
import org.apache.logging.log4j.Logger;

import nl.nn.adapterframework.extensions.cmis.CmisSessionBuilder;
import nl.nn.adapterframework.extensions.cmis.CmisSessionException;
import nl.nn.adapterframework.extensions.cmis.server.impl.IbisDiscoveryService;
import nl.nn.adapterframework.extensions.cmis.server.impl.IbisNavigationService;
import nl.nn.adapterframework.extensions.cmis.server.impl.IbisObjectService;
import nl.nn.adapterframework.extensions.cmis.server.impl.IbisRepositoryService;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.LogUtil;

/**
 * After each request the CallContext is removed.
 * The CmisBinding is kept, unless property cmisbridge.closeConnection = true
 */
public class BridgedCmisService extends FilterCmisService {

	private static final long serialVersionUID = 2L;
	private final Logger log = LogUtil.getLogger(this);
	private static final AppConstants APP_CONSTANTS = AppConstants.getInstance();
	public static final boolean CMIS_BRIDGE_CLOSE_CONNECTION = APP_CONSTANTS.getBoolean(RepositoryConnectorFactory.CMIS_BRIDGE_PROPERTY_PREFIX+"closeConnection", false);

	private CmisBinding clientBinding;

	public BridgedCmisService(CallContext context) {
		setCallContext(context);
	}

	public CmisBinding getCmisBinding() {
		if(clientBinding == null) {
			clientBinding = createCmisBinding();
			log.info("initialized "+toString());
		}

		return clientBinding;
	}

	public CmisBinding createCmisBinding() {

		//Make sure cmisbridge properties are defined
		if(APP_CONSTANTS.getResolvedProperty(RepositoryConnectorFactory.CMIS_BRIDGE_PROPERTY_PREFIX+"url") == null)
			throw new CmisConnectionException("no bridge properties found");

		CmisSessionBuilder sessionBuilder = new CmisSessionBuilder();
		for(Method method: sessionBuilder.getClass().getMethods()) {
			if(!method.getName().startsWith("set") || method.getParameterTypes().length != 1)
				continue;

			//Remove set from the method name
			String setter = firstCharToLower(method.getName().substring(3));
			String value = APP_CONSTANTS.getResolvedProperty(RepositoryConnectorFactory.CMIS_BRIDGE_PROPERTY_PREFIX+setter);
			if(value == null)
				continue;

			//Only always grab the first value because we explicitly check method.getParameterTypes().length != 1
			Object castValue = getCastValue(method.getParameterTypes()[0], value);
			log.debug("trying to set property ["+RepositoryConnectorFactory.CMIS_BRIDGE_PROPERTY_PREFIX+setter+"] with value ["+value+"] of type ["+castValue.getClass().getCanonicalName()+"] on ["+sessionBuilder+"]");

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
			log.error(e);
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
		return new IbisObjectService(getCmisBinding().getObjectService(), getCallContext());
	}

	@Override
	public RepositoryService getRepositoryService() {
		return new IbisRepositoryService(getCmisBinding().getRepositoryService(), getCallContext());
	}

	@Override
	public DiscoveryService getDiscoveryService() {
		return new IbisDiscoveryService(getCmisBinding().getDiscoveryService(), getCallContext());
	}

	@Override
	public NavigationService getNavigationService() {
		return new IbisNavigationService(getCmisBinding().getNavigationService(), getCallContext());
	}

	@Override
	public VersioningService getVersioningService() {
		return getCmisBinding().getVersioningService();
	}

	@Override
	public MultiFilingService getMultiFilingService() {
		return getCmisBinding().getMultiFilingService();
	}

	@Override
	public RelationshipService getRelationshipService() {
		return getCmisBinding().getRelationshipService();
	}

	@Override
	public AclService getAclService() {
		return getCmisBinding().getAclService();
	}

	@Override
	public PolicyService getPolicyService() {
		return getCmisBinding().getPolicyService();
	}

	/**
	 * Returns Class SimpleName + hash + attribute info
	 * @return BridgedCmisService@abc12345 close[xxx] session[xxx]
	 */
	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append(getClass().getSimpleName() + "@" + Integer.toHexString(hashCode()));
		builder.append(" close ["+CMIS_BRIDGE_CLOSE_CONNECTION+"]");
		if(clientBinding != null) {
			builder.append(" session ["+clientBinding.getSessionId()+"]");
		}
		return builder.toString();
	}

	@Override
	public void close() {
		super.close();

		if(CMIS_BRIDGE_CLOSE_CONNECTION) {
			clientBinding = null;
			log.info("closed "+toString());
		}
	}
}
