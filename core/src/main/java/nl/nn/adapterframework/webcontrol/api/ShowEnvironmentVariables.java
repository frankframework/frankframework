/*
Copyright 2016 Nationale-Nederlanden

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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.annotation.security.RolesAllowed;
import javax.servlet.ServletConfig;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.Misc;

/**
* Shows the environment variables.
* 
* @author	Niels Meijer
*/

@Path("/")
public final class ShowEnvironmentVariables extends Base {

	@GET
	@RolesAllowed({"ObserverAccess", "IbisTester"})
	@Path("/environmentvariables")
	@Produces(MediaType.APPLICATION_JSON)
	public Response environmentVariables(@Context ServletConfig servletConfig) throws ApiException {
		initBase(servletConfig);
		
		if (ibisManager == null) {
			throw new ApiException("Config not found!");
		}
		
		List<String> propsToHide = new ArrayList<String>();
		String propertiesHideString = AppConstants.getInstance().getString("properties.hide", null);
		if (propertiesHideString!=null) {
			propsToHide.addAll(Arrays.asList(propertiesHideString.split("[,\\s]+")));
		}
		
		Map<String, Object> envVars = new HashMap<String, Object>();

		envVars.put("Application Constants", convertPropertiesToMap(AppConstants.getInstance(), propsToHide));
		envVars.put("System Properties", convertPropertiesToMap(System.getProperties(), propsToHide));
		
		try {
			envVars.put("Environment Variables", convertPropertiesToMap(Misc.getEnvironmentVariables()));
		} catch (Throwable t) {
			log.warn("caught Throwable while getting EnvironmentVariables",t);
		}
		
		return Response.status(Response.Status.CREATED).entity(envVars).build();
	}

	private Map<String, Object> convertPropertiesToMap(Properties props) {
		return convertPropertiesToMap(props, null);
	}

	private Map<String, Object> convertPropertiesToMap(Properties props, List<String> propsToHide) {
		Enumeration<Object> enumeration = props.keys();
		
		Map<String, Object> properties = new HashMap<String, Object>(props.size());
		
		while (enumeration.hasMoreElements()) {
			String propName = (String) enumeration.nextElement();
			String propValue = props.getProperty(propName);
        	if (propsToHide != null && propsToHide.contains(propName)) {
        		propValue = Misc.hide(propValue);
        	}
        	properties.put(propName, propValue);
		}
		return properties;
	}
}
