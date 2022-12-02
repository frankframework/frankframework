/*
Copyright 2016-2017, 2020-2022 WeAreFrank!

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
import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Data;
import nl.nn.adapterframework.management.bus.BusAction;
import nl.nn.adapterframework.management.bus.BusTopic;
import nl.nn.adapterframework.management.bus.RequestMessageBuilder;
import nl.nn.adapterframework.util.Misc;
import nl.nn.adapterframework.util.StreamUtil;
import nl.nn.adapterframework.util.XmlUtils;

/**
 * Test a PipeLine.
 *
 * @since	7.0-B1
 * @author	Niels Meijer
 */

@Path("/")
public class TestPipeline extends FrankApiBase {

	@Data
	public static class PostedSessionKey {
		int index;
		String key;
		String value;
	}

	@POST
	@RolesAllowed("IbisTester")
	@Path("/test-pipeline")
	@Relation("pipeline")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response postTestPipeLine(MultipartBody inputDataMap) throws ApiException {
		String message = null;

		RequestMessageBuilder builder = RequestMessageBuilder.create(this, BusTopic.TEST_PIPELINE, BusAction.UPLOAD);
		String configuration = resolveStringFromMap(inputDataMap, "configuration");
		builder.addHeader("configuration", configuration);
		String adapterName = resolveStringFromMap(inputDataMap, "adapter");
		builder.addHeader("adapter", adapterName);

		// resolve session keys
		String sessionKeys = resolveTypeFromMap(inputDataMap, "sessionKeys", String.class, "");
		Map<String, String> sessionKeyMap = null;
		if(StringUtils.isNotEmpty(sessionKeys)) {
			try {
				sessionKeyMap = Stream.of(new ObjectMapper().readValue(sessionKeys, PostedSessionKey[].class)).collect(Collectors.toMap(item -> item.key, item-> item.value));
			} catch (Exception e) {
				throw new ApiException("An exception occurred while parsing session keys", e);
			}
		}

		String fileEncoding = resolveTypeFromMap(inputDataMap, "encoding", String.class, StreamUtil.DEFAULT_INPUT_STREAM_ENCODING);

		boolean synchronous = resolveTypeFromMap(inputDataMap, "synchronous", boolean.class, true);
		builder.addHeader("synchronous", synchronous);

		Attachment filePart = inputDataMap.getAttachment("file");
		if(filePart != null) {
			String fileName = filePart.getContentDisposition().getParameter("filename");
			InputStream file = filePart.getObject(InputStream.class);

			if (StringUtils.endsWithIgnoreCase(fileName, ".zip")) {
				try {
					processZipFile(file, builder);
					return Response.status(Response.Status.OK).build();
				} catch (Exception e) {
					throw new ApiException("An exception occurred while processing zip file", e);
				}
			}
			else {
				try {
					message = XmlUtils.readXml(Misc.streamToBytes(file), fileEncoding, false);
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
				String currentMessage = XmlUtils.readXml(b,0,rb,StreamUtil.DEFAULT_INPUT_STREAM_ENCODING,false);

				builder.setPayload(currentMessage);
				callAsyncGateway(builder);
			}
			archive.closeEntry();
		}
		archive.close();
	}
}
