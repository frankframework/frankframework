/*
   Copyright 2019 Nationale-Nederlanden, 2020 - 2025 WeAreFrank!

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
package org.frankframework.extensions.cmis.server;

import java.io.Serial;
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

import lombok.extern.log4j.Log4j2;

import org.frankframework.extensions.cmis.CmisSessionBuilder;
import org.frankframework.extensions.cmis.CmisSessionException;
import org.frankframework.extensions.cmis.server.impl.IbisDiscoveryService;
import org.frankframework.extensions.cmis.server.impl.IbisNavigationService;
import org.frankframework.extensions.cmis.server.impl.IbisObjectService;
import org.frankframework.extensions.cmis.server.impl.IbisRepositoryService;
import org.frankframework.util.AppConstants;
import org.frankframework.util.ClassUtils;
import org.frankframework.util.StringUtil;

/**
 * After each request the CallContext is removed.
 * The CmisBinding is kept, unless property cmisbridge.closeConnection = true
 */
@Log4j2
public class BridgedCmisService extends FilterCmisService {

	@Serial
	private static final long serialVersionUID = 2L;
	private static final AppConstants APP_CONSTANTS = AppConstants.getInstance();
	public static final boolean CMIS_BRIDGE_CLOSE_CONNECTION = APP_CONSTANTS.getBoolean(RepositoryConnectorFactory.CMIS_BRIDGE_PROPERTY_PREFIX+"closeConnection", false);

	private CmisBinding clientBinding;

	public BridgedCmisService(CallContext context) {
		setCallContext(context);
	}

	public CmisBinding getCmisBinding() {
		if(clientBinding == null) {
			clientBinding = createCmisBinding();
			log.info("initialized {}", this);
		}

		return clientBinding;
	}

	public CmisBinding createCmisBinding() {

		//Make sure cmisbridge properties are defined
		if(APP_CONSTANTS.getProperty(RepositoryConnectorFactory.CMIS_BRIDGE_PROPERTY_PREFIX+"url") == null)
			throw new CmisConnectionException("no bridge properties found");

		CmisSessionBuilder sessionBuilder = new CmisSessionBuilder();
		for(Method method: sessionBuilder.getClass().getMethods()) {
			if(!method.getName().startsWith("set") || method.getParameterTypes().length != 1)
				continue;

			//Remove set from the method name
			String setter = StringUtil.lcFirst(method.getName().substring(3));
			String propertyName = RepositoryConnectorFactory.CMIS_BRIDGE_PROPERTY_PREFIX+setter;
			String value = APP_CONSTANTS.getProperty(propertyName);
			if(value == null)
				continue;

			//Only always grab the first value because we explicitly check method.getParameterTypes().length != 1
			Object castValue = ClassUtils.convertToType(method.getParameterTypes()[0], value);
			log.debug("trying to set property [{}] with value [{}] of type [{}] on [{}]", () -> propertyName, () -> value, () -> castValue.getClass().getCanonicalName(), sessionBuilder::toString);

			try {
				method.invoke(sessionBuilder, castValue);
			} catch (Exception e) {
				throw new CmisConnectionException("error while calling method ["+setter+"] on CmisSessionBuilder ["+sessionBuilder+"]", e);
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
		builder.append(getClass().getSimpleName() + "@").append(Integer.toHexString(hashCode()));
		builder.append(" close [").append(CMIS_BRIDGE_CLOSE_CONNECTION).append("]");
		if(clientBinding != null) {
			builder.append(" session [").append(clientBinding.getSessionId()).append("]");
		}
		return builder.toString();
	}

	@Override
	public void close() {
		super.close();

		if(CMIS_BRIDGE_CLOSE_CONNECTION) {
			clientBinding = null;
			log.info("closed {}", this);
		}
	}
}
