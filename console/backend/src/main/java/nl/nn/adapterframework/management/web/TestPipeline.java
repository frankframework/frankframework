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
import java.util.HashMap;
import java.util.Map;
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
import org.springframework.messaging.Message;

import nl.nn.adapterframework.management.bus.BusAction;
import nl.nn.adapterframework.management.bus.BusMessageUtils;
import nl.nn.adapterframework.management.bus.BusTopic;
import nl.nn.adapterframework.management.bus.ResponseMessageBase;
import nl.nn.adapterframework.util.RequestUtils;
import nl.nn.adapterframework.util.StreamUtil;
import nl.nn.adapterframework.util.XmlEncodingUtils;

/**
 * Test a PipeLine.
 *
 * @since	7.0-B1
 * @author	Niels Meijer
 */

@Path("/")
public class TestPipeline extends FrankApiBase {

	@POST
	@RolesAllowed("IbisTester")
	@Path("/test-pipeline")
	@Relation("pipeline")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response postTestPipeLine(MultipartBody inputDataMap) throws ApiException {
		RequestMessageBuilder builder = RequestMessageBuilder.create(this, BusTopic.TEST_PIPELINE, BusAction.UPLOAD);
		String configuration = RequestUtils.resolveStringFromMap(inputDataMap, "configuration");
		builder.addHeader("configuration", configuration);
		String adapterName = RequestUtils.resolveStringFromMap(inputDataMap, "adapter");
		builder.addHeader("adapter", adapterName);

		// resolve session keys
		String sessionKeys = RequestUtils.resolveTypeFromMap(inputDataMap, "sessionKeys", String.class, "");
		if(StringUtils.isNotEmpty(sessionKeys)) { //format: [{"index":1,"key":"test","value":"123"}]
			builder.addHeader("sessionKeys", sessionKeys);
		}

		String fileEncoding = RequestUtils.resolveTypeFromMap(inputDataMap, "encoding", String.class, StreamUtil.DEFAULT_INPUT_STREAM_ENCODING);

		String message;
		Attachment filePart = inputDataMap.getAttachment("file");
		if(filePart != null) {
			String fileName = filePart.getContentDisposition().getParameter("filename");
			InputStream file = filePart.getObject(InputStream.class);

			if (StringUtils.endsWithIgnoreCase(fileName, ".zip")) {
				try {
					String zipResults = processZipFile(file, builder);
					return testPipelineResponse(zipResults);
				} catch (Exception e) {
					throw new ApiException("An exception occurred while processing zip file", e);
				}
			}
			else {
				try {
					message = XmlEncodingUtils.readXml(file, fileEncoding);
				} catch (UnsupportedEncodingException e) {
					throw new ApiException("unsupported file encoding ["+fileEncoding+"]");
				} catch (IOException e) {
					throw new ApiException("error reading file", e);
				}
			}
		} else {
			message = RequestUtils.resolveStringWithEncoding(inputDataMap, "message", fileEncoding);
		}

		if(StringUtils.isEmpty(message)) {
			throw new ApiException("Neither a file nor a message was supplied", 400);
		}

		builder.setPayload(message);
		Message<?> response = sendSyncMessage(builder);
		String state = BusMessageUtils.getHeader(response, ResponseMessageBase.STATE_KEY);
		return testPipelineResponse(getPayload(response), state, message);
	}

	private String getPayload(Message<?> response) {
		Object payload = response.getPayload();
		if(payload instanceof String) {
			return (String) payload;
		} else if(payload instanceof byte[]) {
			return new String((byte[]) payload);
		} else if(payload instanceof InputStream) {
			try {
				return StreamUtil.streamToString((InputStream) payload,  "\n", false);
			} catch (IOException e) {
				throw new ApiException("unable to read response payload", e);
			}
		}
		throw new ApiException("unexpected response payload type ["+payload.getClass().getCanonicalName()+"]");
	}

	private Response testPipelineResponse(String payload) {
		return testPipelineResponse(payload, "SUCCESS", null);
	}
	private Response testPipelineResponse(String payload, String state, String message) {
		Map<String, Object> result = new HashMap<>();
		result.put("state", state);
		result.put("result", payload);
		if(message != null) {
			result.put("message", message);
		}
		return Response.status(200).entity(result).build();
	}

	// cannot call callAsyncGateway, backend calls are not synchronous
	private String processZipFile(InputStream file, RequestMessageBuilder builder) throws IOException {
		StringBuilder result = new StringBuilder();

		ZipInputStream archive = new ZipInputStream(file);
		for (ZipEntry entry=archive.getNextEntry(); entry!=null; entry=archive.getNextEntry()) {
			String name = entry.getName();
			int size = (int)entry.getSize();
			if (size>0) {
				byte[] b=new byte[size];
				int rb=0;
				int chunk;
				while ((size - rb) > 0) {
					chunk=archive.read(b,rb,size - rb);
					if (chunk==-1) {
						break;
					}
					rb+=chunk;
				}
				String currentMessage = XmlEncodingUtils.readXml(b, null);

				builder.setPayload(currentMessage);
				Message<?> response = sendSyncMessage(builder);
				result.append(name);
				result.append(": ");
				result.append(BusMessageUtils.getHeader(response, ResponseMessageBase.STATE_KEY));
				result.append("\n");
			}
			archive.closeEntry();
		}
		archive.close();

		return result.toString();
	}
}
