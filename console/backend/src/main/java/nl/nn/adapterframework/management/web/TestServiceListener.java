/*
   Copyright 2016-2023 WeAreFrank!

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
package nl.nn.adapterframework.management.web;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.cxf.jaxrs.ext.multipart.MultipartBody;

import nl.nn.adapterframework.management.bus.BusAction;
import nl.nn.adapterframework.management.bus.BusTopic;
import nl.nn.adapterframework.util.StreamUtil;
import nl.nn.adapterframework.util.XmlEncodingUtils;

/**
 * Test Service Listeners.
 *
 * @since	7.0-B1
 * @author	Niels Meijer
 */

@Path("/")
public class TestServiceListener extends FrankApiBase {

	@GET
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/test-servicelistener")
	@Relation("servicelistener")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getServiceListeners() throws ApiException {
		RequestMessageBuilder builder = RequestMessageBuilder.create(this, BusTopic.SERVICE_LISTENER, BusAction.GET);
		return callSyncGateway(builder);
	}

	@POST
	@RolesAllowed({"IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/test-servicelistener")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response postServiceListeners(MultipartBody inputDataMap) throws ApiException {
		if(inputDataMap == null) {
			throw new ApiException("Missing post parameters");
		}

		String message = null;

		RequestMessageBuilder builder = RequestMessageBuilder.create(this, BusTopic.SERVICE_LISTENER, BusAction.UPLOAD);
		builder.addHeader("service", resolveStringFromMap(inputDataMap, "service"));
		String fileEncoding = resolveTypeFromMap(inputDataMap, "encoding", String.class, StreamUtil.DEFAULT_INPUT_STREAM_ENCODING);
		Attachment filePart = inputDataMap.getAttachment("file");
		if(filePart != null) {
			InputStream file = filePart.getObject(InputStream.class);

			try {
				message = XmlEncodingUtils.readXml(file, fileEncoding);
			} catch (UnsupportedEncodingException e) {
				throw new ApiException("unsupported file encoding ["+fileEncoding+"]");
			} catch (IOException e) {
				throw new ApiException("error reading file", e);
			}
		} else {
			message = resolveStringWithEncoding(inputDataMap, "message", fileEncoding);
		}

		if(StringUtils.isEmpty(message)) {
			throw new ApiException("Neither a file nor a message was supplied", 400);
		}

		builder.setPayload(message);
		return callSyncGateway(builder);
	}
}
