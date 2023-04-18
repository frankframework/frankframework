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
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
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
 * Send a message with JMS.
 *
 * @since	7.0-B1
 * @author	Niels Meijer
 */

@Path("/")
public final class SendJmsMessage extends FrankApiBase {

	@POST
	@RolesAllowed({"IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("jms/message")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response putJmsMessage(MultipartBody inputDataMap) {

		String message = null, fileName = null;
		InputStream file = null;
		if(inputDataMap == null) {
			throw new ApiException("Missing post parameters");
		}

		String fileEncoding = resolveTypeFromMap(inputDataMap, "encoding", String.class, DEFAULT_CHARSET);
		String connectionFactory = resolveStringFromMap(inputDataMap, "connectionFactory");
		String destinationName = resolveStringFromMap(inputDataMap, "destination");
		String destinationType = resolveStringFromMap(inputDataMap, "type");
		String replyTo = resolveTypeFromMap(inputDataMap, "replyTo", String.class, "");
		boolean persistent = resolveTypeFromMap(inputDataMap, "persistent", boolean.class, false);
		boolean synchronous = resolveTypeFromMap(inputDataMap, "synchronous", boolean.class, false);
		boolean lookupDestination = resolveTypeFromMap(inputDataMap, "lookupDestination", boolean.class, false);
		String messageProperty = resolveTypeFromMap(inputDataMap, "property", String.class, "");

		RequestMessageBuilder builder = RequestMessageBuilder.create(this, BusTopic.QUEUE, BusAction.UPLOAD);
		builder.addHeader(HEADER_CONNECTION_FACTORY_NAME_KEY, connectionFactory);
		builder.addHeader("destination", destinationName);
		builder.addHeader("type", destinationType);
		builder.addHeader("replyTo", replyTo);
		builder.addHeader("persistent", persistent);
		builder.addHeader("synchronous", synchronous);
		builder.addHeader("lookupDestination", lookupDestination);
		builder.addHeader("messageProperty", messageProperty);

		Attachment filePart = inputDataMap.getAttachment("file");
		if(filePart != null) {
			fileName = filePart.getContentDisposition().getParameter( "filename" );
			file = filePart.getObject(InputStream.class);

			if (StringUtils.endsWithIgnoreCase(fileName, ".zip")) {
				try {
					processZipFile(file, builder);
					return Response.status(Response.Status.OK).build();
				} catch (IOException e) {
					throw new ApiException("error processing zip file", e);
				}
			}
			else {
				try {
					message = XmlEncodingUtils.readXml(StreamUtil.streamToBytes(file), fileEncoding);
				} catch (UnsupportedEncodingException e) {
					throw new ApiException("unsupported file encoding ["+fileEncoding+"]");
				} catch (IOException e) {
					throw new ApiException("error reading file", e);
				}
			}
		} else {
			message = resolveStringWithEncoding(inputDataMap, "message", fileEncoding);
		}

		if(StringUtils.isEmpty(message)) {
			throw new ApiException("Neither a file nor a message was supplied", 400);
		}

		builder.setPayload(message);
		return (synchronous) ? callSyncGateway(builder) : callAsyncGateway(builder);
	}


	private void processZipFile(InputStream file, RequestMessageBuilder builder) throws IOException {
		ZipInputStream archive = new ZipInputStream(file);
		for (ZipEntry entry=archive.getNextEntry(); entry!=null; entry=archive.getNextEntry()) {
			int size = (int)entry.getSize();
			if (size>0) {
				byte[] b=new byte[size];
				int rb=0;
				int chunk=0;
				while ((size - rb) > 0) {
					chunk=archive.read(b,rb,size - rb);
					if (chunk==-1) {
						break;
					}
					rb+=chunk;
				}
				String currentMessage = XmlEncodingUtils.readXml(b, DEFAULT_CHARSET);

				builder.setPayload(currentMessage);
				callAsyncGateway(builder);
			}
			archive.closeEntry();
		}
		archive.close();
	}
}
