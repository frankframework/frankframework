/*
Copyright 2016 Integration Partners B.V.

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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.annotation.security.RolesAllowed;
import javax.servlet.ServletConfig;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.io.IOUtils;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;

import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.receivers.ServiceDispatcher;
import nl.nn.adapterframework.util.Misc;
import nl.nn.adapterframework.util.XmlUtils;

/**
* Test Service Listners.
* 
* @author	Niels Meijer
*/

@Path("/")
public final class TestServiceListener extends Base {
	@Context ServletConfig servletConfig;

	@GET
	@RolesAllowed({"ObserverAccess", "IbisTester"})
	@Path("/test-servicelistener")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getServiceListeners() throws ApiException {
		initBase(servletConfig);
		
		if (ibisManager == null) {
			throw new ApiException("Config not found!");
		}
		
		Map<String, Object> returnData = new HashMap<String, Object>();
		
		@SuppressWarnings("rawtypes")
		Iterator it = ServiceDispatcher.getInstance().getRegisteredListenerNames();
		List<String> services = new ArrayList<String>();
		while (it.hasNext()) {
			services.add((String)it.next());
		}
		
		returnData.put("services", services);
		
		return Response.status(Response.Status.CREATED).entity(returnData).build();
	}

	@POST
	@RolesAllowed({"ObserverAccess", "IbisTester"})
	@Path("/test-servicelistener")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response postServiceListeners(MultipartFormDataInput input) throws ApiException {
		Map<String, Object> result = new HashMap<String, Object>();

		String message = null, serviceName = null, dispatchResult = null;
		InputStream file = null;

		Map<String, List<InputPart>> inputDataMap = input.getFormDataMap();
		try {
			if(inputDataMap.get("message") != null)
				message = inputDataMap.get("message").get(0).getBodyAsString();
			if(inputDataMap.get("service") != null) {
				serviceName = inputDataMap.get("service").get(0).getBodyAsString();
			}
			if(inputDataMap.get("file") != null) {
				file = inputDataMap.get("file").get(0).getBody(InputStream.class, null);
				String fileEncoding = Misc.DEFAULT_INPUT_STREAM_ENCODING;
				message = Misc.streamToString(file, "\n", fileEncoding, false);

				message = XmlUtils.readXml(IOUtils.toByteArray(file), fileEncoding,false);
			}
			else {
				message = new String(message.getBytes(), Misc.DEFAULT_INPUT_STREAM_ENCODING);
			}
			
			if(!ServiceDispatcher.getInstance().isRegisteredServiceListener(serviceName)) {
				return Response.status(Response.Status.BAD_REQUEST).build();
			}

			try {
				@SuppressWarnings("rawtypes")
				Map context = new HashMap();
				dispatchResult = ServiceDispatcher.getInstance().dispatchRequest(serviceName, null, message, context);
			} catch (ListenerException e) {
				return Response.status(Response.Status.BAD_REQUEST).build();
			}

			result.put("state", "success");
			result.put("result", dispatchResult);
		} catch (IOException e) {
			return Response.status(Response.Status.BAD_REQUEST).build();
		}

		return Response.status(Response.Status.CREATED).entity(result).build();
	}
}
