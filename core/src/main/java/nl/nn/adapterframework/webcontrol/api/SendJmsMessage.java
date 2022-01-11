/*
   Copyright 2016-2021 WeAreFrank!

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

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.jms.JMSFacade.DestinationType;
import nl.nn.adapterframework.jms.JmsSender;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.EnumUtils;
import nl.nn.adapterframework.util.Misc;
import nl.nn.adapterframework.util.StreamUtil;
import nl.nn.adapterframework.util.XmlUtils;

/**
 * Send a message with JMS.
 * 
 * @since	7.0-B1
 * @author	Niels Meijer
 */

@Path("/")
public final class SendJmsMessage extends Base {

	@POST
	@RolesAllowed({"IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("jms/message")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response putJmsMessage(MultipartBody inputDataMap) throws ApiException {

		String message = null, fileName = null;
		InputStream file = null;
		if(inputDataMap == null) {
			throw new ApiException("Missing post parameters");
		}

		String fileEncoding = resolveTypeFromMap(inputDataMap, "encoding", String.class, StreamUtil.DEFAULT_INPUT_STREAM_ENCODING);
		String connectionFactory = resolveStringFromMap(inputDataMap, "connectionFactory");
		String destinationName = resolveStringFromMap(inputDataMap, "destination");
		String destinationType = resolveStringFromMap(inputDataMap, "type");
		String replyTo = resolveTypeFromMap(inputDataMap, "replyTo", String.class, "");
		boolean persistent = resolveTypeFromMap(inputDataMap, "persistent", boolean.class, false);
		boolean synchronous = resolveTypeFromMap(inputDataMap, "synchronous", boolean.class, false);
		boolean lookupDestination = resolveTypeFromMap(inputDataMap, "lookupDestination", boolean.class, false);
		String messageProperty = resolveTypeFromMap(inputDataMap, "property", String.class, "");

		JmsSender qms = jmsBuilder(connectionFactory, destinationName, persistent, destinationType, replyTo, synchronous, lookupDestination);

		if(StringUtils.isNotEmpty(messageProperty)) {
			String[] keypair = messageProperty.split(",");
			Parameter p = new Parameter();
			p.setName(keypair[0]);
			p.setValue(keypair[1]);
			try {
				p.configure();
			} catch (ConfigurationException e) {
				throw new ApiException("Failed to configure message property ["+p.getName()+"]", e);
			}
			qms.addParameter(p);
		}
		
		Attachment filePart = inputDataMap.getAttachment("file");
		if(filePart != null) {
			fileName = filePart.getContentDisposition().getParameter( "filename" );

			if (StringUtils.endsWithIgnoreCase(fileName, ".zip")) {
				try {
					processZipFile(file, qms);

					return Response.status(Response.Status.OK).build();
				} catch (IOException e) {
					throw new ApiException("error processing zip file", e);
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

		if(message != null && message.length() > 0) {
			processMessage(qms, message);
			return Response.status(Response.Status.OK).build();
		}
		else {
			throw new ApiException("must provide either a message or file", 400);
		}
	}

	private JmsSender jmsBuilder(String connectionFactory, String destination, boolean persistent, String type, String replyTo, boolean synchronous, boolean lookupDestination) {
		JmsSender qms = getIbisContext().createBeanAutowireByName(JmsSender.class);
		qms.setName("SendJmsMessageAction");
		if(type.equals("QUEUE")) {
			qms.setQueueConnectionFactoryName(connectionFactory);
		} else {
			qms.setTopicConnectionFactoryName(connectionFactory);
		}
		qms.setDestinationName(destination);
		qms.setPersistent(persistent);
		qms.setDestinationType(EnumUtils.parse(DestinationType.class, type));
		if (StringUtils.isNotEmpty(replyTo)) {
			qms.setReplyToName(replyTo);
		}
		qms.setSynchronous(synchronous);
		qms.setLookupDestination(lookupDestination);
		return qms;
	}

	private void processZipFile(InputStream file, JmsSender qms) throws IOException {
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

				processMessage(qms, currentMessage);
			}
			archive.closeEntry();
		}
		archive.close();
	}

	private void processMessage(JmsSender qms, String message) throws ApiException {
		try {
			qms.open();
			/*
			 * this used to be:
			 *   qms.sendMessage(technicalCorrelationId,new Message(message), null);
			 * Be aware that 'technicalCorrelationId' will not be used by default
			 */
			qms.sendMessage(new Message(message), null);
		} catch (Exception e) {
			throw new ApiException("Error occured sending message", e);
		} 
		try {
			qms.close();
		} catch (Exception e) {
			throw new ApiException("Error occured on closing connection", e);
		}
	}
}