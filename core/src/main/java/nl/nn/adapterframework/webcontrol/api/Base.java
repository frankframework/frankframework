/*
Copyright 2016-2020 WeAreFrank!

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
package nl.nn.adapterframework.webcontrol.api;

import java.util.List;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.ws.rs.core.Context;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.springframework.beans.BeanInstantiationException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;

import nl.nn.adapterframework.configuration.IbisContext;
import nl.nn.adapterframework.configuration.IbisManager;
import nl.nn.adapterframework.lifecycle.IbisApplicationServlet;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.flow.FlowDiagramManager;

/**
 * Baseclass to fetch ibisContext + ibisManager
 * 
 * @since	7.0-B1
 * @author	Niels Meijer
 */

public abstract class Base {
	@Context ServletConfig servletConfig;

	protected Logger log = LogUtil.getLogger(this);
	private IbisContext ibisContext = null;
	protected static String HATEOASImplementation = AppConstants.getInstance().getString("ibis-api.hateoasImplementation", "default");

	/**
	 * Retrieves the IbisContext from <code>servletConfig</code>.
	 */
	private void retrieveIbisContextFromServlet() {
		if(servletConfig == null) {
			throw new ApiException(new IllegalStateException("no ServletConfig found to retrieve IbisContext from"));
		}

		ibisContext = IbisApplicationServlet.getIbisContext(servletConfig.getServletContext());
	}

	public IbisContext getIbisContext() {
		if(ibisContext == null) {
			retrieveIbisContextFromServlet();
		}

		if(ibisContext.getBootState().getException() != null) {
			throw new ApiException(ibisContext.getBootState().getException());
		}

		return ibisContext;
	}

	/**
	 * Retrieves the IbisManager from the IbisContext
	 */
	public IbisManager getIbisManager() {
		IbisManager ibisManager = getIbisContext().getIbisManager();

		if (ibisManager==null) {
			throw new ApiException(new IllegalStateException("Could not retrieve ibisManager from context"));
		}

		return ibisManager;
	}

	public ClassLoader getClassLoader() {
		return this.getClass().getClassLoader();
	}

	protected FlowDiagramManager getFlowDiagramManager() {
		try {
			return getIbisContext().getBean("flowDiagramManager", FlowDiagramManager.class);
		} catch (BeanCreationException | BeanInstantiationException | NoSuchBeanDefinitionException e) {
			throw new ApiException("failed to initalize FlowDiagramManager", e);
		}
	}

	protected String resolveStringFromMap(Map<String, List<InputPart>> inputDataMap, String key) throws ApiException {
		return resolveStringFromMap(inputDataMap, key, null);
	}

	protected String resolveStringFromMap(Map<String, List<InputPart>> inputDataMap, String key, String defaultValue) throws ApiException {
		String result = resolveTypeFromMap(inputDataMap, key, String.class, null);
		if(StringUtils.isEmpty(result)) {
			if(defaultValue != null) {
				return defaultValue;
			}
			throw new ApiException("Key ["+key+"] may not be empty");
		}
		return result;
	}

	protected <T> T resolveTypeFromMap(Map<String, List<InputPart>> inputDataMap, String key, Class<T> clazz, T defaultValue) throws ApiException {
		try {
			if(inputDataMap.get(key) != null) {
				return inputDataMap.get(key).get(0).getBody(clazz, null);
			}
		} catch (Exception e) {
			log.debug("Failed to parse parameter ["+key+"]", e);
		}
		if(defaultValue != null) {
			return defaultValue;
		}
		throw new ApiException("Key ["+key+"] not defined", 400);
	}
}
