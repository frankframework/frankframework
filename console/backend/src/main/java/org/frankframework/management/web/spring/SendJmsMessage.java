/*
   Copyright 2024 WeAreFrank!

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
package org.frankframework.management.web.spring;

import org.apache.commons.lang3.StringUtils;
import org.frankframework.management.bus.BusAction;
import org.frankframework.management.bus.BusMessageUtils;
import org.frankframework.management.bus.BusTopic;
import org.frankframework.management.web.Description;
import org.frankframework.management.web.Relation;
import org.frankframework.util.RequestUtils;
import org.frankframework.util.StreamUtil;
import org.frankframework.util.XmlEncodingUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.security.RolesAllowed;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@RestController
public class SendJmsMessage extends FrankApiBase {

	// Won't work Spring 5.3 without SpringBoot
	/*@Getter
	@Setter
	public static class JmsMessageMultiPartBody {
		private boolean persistent;
		private boolean synchronous;
		private boolean lookupDestination;
		private String destination;
		private String replyTo;
		private String property;
		private String type;
		private String connectionFactory;
		private String encoding;

		private MultipartFile message;
		private MultipartFile file;
	}*/

	@RolesAllowed("IbisTester")
	@PostMapping(value = "/jms/message", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@Relation("jms")
	@Description("put a JMS message on a queue")
	public ResponseEntity<?> putJmsMessage(
			@RequestPart("persistent") boolean persistentPart,
			@RequestPart("synchronous") boolean synchronousPart,
			@RequestPart("lookupDestination") boolean lookupDestinationPart,
			@RequestPart("destination") String destinationPart,
			@RequestPart("replyTo") String replyToPart,
			@RequestPart("property") String propertyPart,
			@RequestPart("type") String typePart,
			@RequestPart("connectionFactory") String connectionFactoryPart,
			@RequestPart("encoding") String encodingPart,
			@RequestPart("message") MultipartFile messagePart,
			@RequestPart("file") MultipartFile filePart
	) {
		String message = null;
		String fileName = null;
		InputStream file = null;

		String fileEncoding = RequestUtils.resolveRequiredProperty("encoding", encodingPart, StreamUtil.DEFAULT_INPUT_STREAM_ENCODING);
		String connectionFactory = RequestUtils.resolveRequiredProperty("connectionFactory", connectionFactoryPart, null);
		String destinationName = RequestUtils.resolveRequiredProperty("destination", destinationPart, null);
		String destinationType = RequestUtils.resolveRequiredProperty("type", typePart, null);
		String replyTo = RequestUtils.resolveRequiredProperty( "replyTo", replyToPart, "");
		boolean persistent = RequestUtils.resolveRequiredProperty("persistent", persistentPart, false);
		boolean synchronous = RequestUtils.resolveRequiredProperty("synchronous", synchronousPart, false);
		boolean lookupDestination = RequestUtils.resolveRequiredProperty("lookupDestination", lookupDestinationPart, false);
		String messageProperty = RequestUtils.resolveRequiredProperty("property", propertyPart, "");

		RequestMessageBuilder builder = RequestMessageBuilder.create(this, BusTopic.QUEUE, BusAction.UPLOAD);
		builder.addHeader(BusMessageUtils.HEADER_CONNECTION_FACTORY_NAME_KEY, connectionFactory);
		builder.addHeader("destination", destinationName);
		builder.addHeader("type", destinationType);
		builder.addHeader("replyTo", replyTo);
		builder.addHeader("persistent", persistent);
		builder.addHeader("synchronous", synchronous);
		builder.addHeader("lookupDestination", lookupDestination);
		builder.addHeader("messageProperty", messageProperty);

		if(filePart != null) {
			fileName = filePart.getOriginalFilename();
			try {
				file = filePart.getInputStream();
			} catch (IOException e) {
				throw new ApiException("error preparing file content", e);
			}

			if (StringUtils.endsWithIgnoreCase(fileName, ".zip")) {
				try {
					processZipFile(file, builder);
					return ResponseEntity.status(HttpStatus.OK).build();
				} catch (IOException e) {
					throw new ApiException("error processing zip file", e);
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
			message = RequestUtils.resolveStringWithEncoding("message", messagePart, fileEncoding);
		}

		if(StringUtils.isEmpty(message)) {
			throw new ApiException("Neither a file nor a message was supplied", 400);
		}

		builder.setPayload(message);
		return synchronous ? callSyncGateway(builder) : callAsyncGateway(builder);
	}


	private void processZipFile(InputStream file, RequestMessageBuilder builder) throws IOException {
		ZipInputStream archive = new ZipInputStream(file);
		for (ZipEntry entry = archive.getNextEntry(); entry!=null; entry=archive.getNextEntry()) {
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
				String currentMessage = XmlEncodingUtils.readXml(b, null);

				builder.setPayload(currentMessage);
				callAsyncGateway(builder);
			}
			archive.closeEntry();
		}
		archive.close();
	}

}
