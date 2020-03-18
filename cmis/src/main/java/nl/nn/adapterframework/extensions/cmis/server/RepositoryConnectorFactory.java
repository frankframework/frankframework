/*
   Copyright 2018 - 2019 Nationale-Nederlanden

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

import java.util.Map;

import nl.nn.adapterframework.util.LogUtil;

import org.apache.chemistry.opencmis.commons.exceptions.CmisRuntimeException;
import org.apache.chemistry.opencmis.commons.impl.server.AbstractServiceFactory;
import org.apache.chemistry.opencmis.commons.server.CallContext;
import org.apache.chemistry.opencmis.commons.server.CmisService;
import org.apache.chemistry.opencmis.server.support.wrapper.CallContextAwareCmisService;
import org.apache.chemistry.opencmis.server.support.wrapper.ConformanceCmisServiceWrapper;
import org.apache.logging.log4j.Logger;


/**
 * Implementation of a repository factory that proxies all requests to a CMIS Bridge adapter
 */
public class RepositoryConnectorFactory extends AbstractServiceFactory {

	private static final Logger LOG = LogUtil.getLogger(RepositoryConnectorFactory.class);
	private ThreadLocal<CallContextAwareCmisService> threadLocalService = new ThreadLocal<CallContextAwareCmisService>();

	@Override
	public void init(Map<String, String> parameters) {
		LOG.info("Initialized proxy repository service");
	}

	@Override
	public void destroy() {
		LOG.info("Destroyed proxy repository service");
	}

	@Override
	public CmisService getService(CallContext context) {
		LOG.debug("Retrieve proxy repository service");

		CallContextAwareCmisService service = threadLocalService.get();
		if (service == null) {
			service = new ConformanceCmisServiceWrapper(createService(context));
			threadLocalService.set(service);
			LOG.info("Create service wrapper");
		}

		service.setCallContext(context);

		return service;
	}

	protected FilterCmisService createService(CallContext context) {
		HttpSessionCmisService service = null;
		try {
			service = new HttpSessionCmisService(context);
			LOG.info("Created proxy repository service");
		} catch (Exception e) {
			throw new CmisRuntimeException("Could not create service instance: " + e, e);
		}

		return service;
	}
}