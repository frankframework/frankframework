/*
Copyright 2016-2017, 2019-2020, 2022 WeAreFrank!

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
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.io.IOUtils;
import org.apache.cxf.jaxrs.ext.multipart.MultipartBody;

import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.PipeLine.ExitState;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.receivers.ServiceDispatcher;
import nl.nn.adapterframework.util.StreamUtil;
import nl.nn.adapterframework.util.XmlUtils;

/**
 * Test Service Listners.
 *
 * @since	7.0-B1
 * @author	Niels Meijer
 */

@Path("/")
public final class TestServiceListener extends Base {

	@GET
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/test-servicelistener")
	@Relation("servicelistener")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getServiceListeners() throws ApiException {

		if (getIbisManager() == null) {
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
	@RolesAllowed({"IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/test-servicelistener")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response postServiceListeners(MultipartBody inputDataMap) throws ApiException {
		Map<String, Object> result = new HashMap<String, Object>();

		String message;
		String serviceName = null;
		String dispatchResult;
		InputStream file = null;

		String fileEncoding = resolveTypeFromMap(inputDataMap, "encoding", String.class, StreamUtil.DEFAULT_INPUT_STREAM_ENCODING);

		try {
			if(inputDataMap.getAttachment("service") != null) {
				serviceName = resolveStringFromMap(inputDataMap, "service");
			}
			if(inputDataMap.getAttachment("file") != null) {
				file = inputDataMap.getAttachment("file").getObject(InputStream.class);

				message = XmlUtils.readXml(IOUtils.toByteArray(file), fileEncoding, false);
			}
			else {
				message = resolveStringWithEncoding(inputDataMap, "message", fileEncoding);
			}

			if(message == null && file == null) {
				throw new ApiException("must provide either a message or file", 400);
			}

			if(!ServiceDispatcher.getInstance().isRegisteredServiceListener(serviceName)) {
				return Response.status(Response.Status.BAD_REQUEST).build();
			}

			try (PipeLineSession session = new PipeLineSession()) {
				dispatchResult = ServiceDispatcher.getInstance().dispatchRequest(serviceName, null, message, session);
			} catch (ListenerException e) {
				String msg = "Exception executing service ["+serviceName+"]";
				log.warn(msg, e);
				return Response.status(Response.Status.BAD_REQUEST).entity(msg).build();
			}

			result.put("state", ExitState.SUCCESS);
			result.put("result", dispatchResult);
		} catch (IOException e) {
			String msg = "Exception executing service ["+serviceName+"]";
			log.warn(msg, e);
			return Response.status(Response.Status.BAD_REQUEST).entity(msg).build();
		}

		return Response.status(Response.Status.CREATED).entity(result).build();
	}
}
