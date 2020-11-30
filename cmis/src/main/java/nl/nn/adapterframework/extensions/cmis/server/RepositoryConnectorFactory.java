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

import java.io.File;
import java.util.Map;

import org.apache.chemistry.opencmis.commons.exceptions.CmisRuntimeException;
import org.apache.chemistry.opencmis.commons.impl.server.AbstractServiceFactory;
import org.apache.chemistry.opencmis.commons.server.CallContext;
import org.apache.chemistry.opencmis.commons.server.CmisService;
import org.apache.chemistry.opencmis.server.support.wrapper.CallContextAwareCmisService;
import org.apache.chemistry.opencmis.server.support.wrapper.ConformanceCmisServiceWrapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;

import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.LogUtil;


/**
 * Implementation of a repository factory that proxies all requests to a CmisEventListener
 * When no EventListeners have been defined, all requests will be blocked.
 */
public class RepositoryConnectorFactory extends AbstractServiceFactory {
	public static final String CMIS_BRIDGE_PROPERTY_PREFIX = "cmisbridge.";
	public static final boolean CMIS_BRIDGE_ENABLED = AppConstants.getInstance().getBoolean(CMIS_BRIDGE_PROPERTY_PREFIX+"active", true);

	private static final Logger LOG = LogUtil.getLogger(RepositoryConnectorFactory.class);
	private ThreadLocal<CallContextAwareCmisService> threadLocalService = new ThreadLocal<>();
	private File tempDirectory = null;

	@Override
	public void init(Map<String, String> parameters) {
		LOG.info("initialized proxy repository service");

		String ibisTempDir = AppConstants.getInstance().getResolvedProperty("ibis.tmpdir");
		File baseDir = null;
		if(StringUtils.isNotEmpty(ibisTempDir)) {
			baseDir = new File(ibisTempDir);
		} else {
			baseDir = super.getTempDirectory();
		}
		tempDirectory = new File(baseDir, "cmis");
		if(!tempDirectory.mkdir()) {
			tempDirectory = super.getTempDirectory(); //fall back to default java.io.tmpdir
		}
	}

	@Override
	public void destroy() {
		LOG.info("destroyed proxy repository service");
	}

	@Override
	public CmisService getService(CallContext context) {
		if(!CMIS_BRIDGE_ENABLED) {
			throw new CmisRuntimeException("CMIS bridge not enabled");
		}
		if(!CmisEventDispatcher.getInstance().hasEventListeners()) {
			throw new CmisRuntimeException("no CMIS bridge events registered");
		}

		LOG.debug("retrieve repository service");

		// Make sure that each thread in the HTTP CONN POOL has it's own BridgedCmisService
		CallContextAwareCmisService service = threadLocalService.get();
		if (service == null) {
			service = new ConformanceCmisServiceWrapper(createService(context));
			threadLocalService.set(service);
			LOG.debug("stored repository service in local http-conn-thread");
		}

		service.setCallContext(context); //Update the CallContext

		return service;
	}

	protected FilterCmisService createService(CallContext context) {
		BridgedCmisService service = null;
		try {
			service = new BridgedCmisService(context);
			LOG.info("created repository service ["+service+"]");
		} catch (Exception e) {
			throw new CmisRuntimeException("could not create service instance: " + e, e);
		}

		return service;
	}

	@Override
	public File getTempDirectory() {
		return tempDirectory;
	}
}