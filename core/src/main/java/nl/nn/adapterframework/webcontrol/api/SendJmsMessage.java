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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.annotation.security.RolesAllowed;
import javax.servlet.ServletConfig;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import nl.nn.adapterframework.jms.JmsSender;
import nl.nn.adapterframework.util.Misc;
import nl.nn.adapterframework.util.XmlUtils;

import org.apache.commons.lang.StringUtils;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;

/**
* Send a message with JMS.
* 
* @author	Niels Meijer
* @author	Dimmen Schox 
*/

@Path("/")
public final class SendJmsMessage extends Base {

	@Context ServletConfig servletConfig;

	@POST
	@RolesAllowed({"IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("jms/message")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response putJmsMessage(MultipartFormDataInput input) throws ApiException {

		initBase(servletConfig);

		String jmsRealm = null, destinationName = null, destinationType = null, replyTo = null, message = null, fileName = null;
		InputStream file = null;
		boolean persistent = false;
		Map<String, List<InputPart>> inputDataMap = input.getFormDataMap();
		if(inputDataMap == null) {
			throw new ApiException("Missing post parameters");
		}

		try {
			if(inputDataMap.get("realm") != null)
				jmsRealm = inputDataMap.get("realm").get(0).getBodyAsString();
			else
				throw new ApiException("JMS realm not defined", 400);
			if(inputDataMap.get("destination") != null)
				destinationName = inputDataMap.get("destination").get(0).getBodyAsString();
			else
				throw new ApiException("Destination name not defined", 400);
			if(inputDataMap.get("type") != null) 
				destinationType = inputDataMap.get("type").get(0).getBodyAsString();
			else
				throw new ApiException("Destination type not defined", 400);
			if(inputDataMap.get("replyTo") != null)
				replyTo = inputDataMap.get("replyTo").get(0).getBodyAsString();
			else
				throw new ApiException("ReplyTo not defined", 400);
			if(inputDataMap.get("message") != null) 
				message = inputDataMap.get("message").get(0).getBodyAsString();
			if(inputDataMap.get("persistent") != null)
				persistent = inputDataMap.get("persistent").get(0).getBody(boolean.class, null);
			if(inputDataMap.get("file") != null)
				file = inputDataMap.get("file").get(0).getBody(InputStream.class, null);
		}
		catch (IOException e) {
			throw new ApiException("Failed to parse one or more parameters!");
		}

		try {
			if (file != null) {
				MultivaluedMap<String, String> headers = inputDataMap.get("file").get(0).getHeaders();
				String[] contentDispositionHeader = headers.getFirst("Content-Disposition").split(";");
				for (String name : contentDispositionHeader) {
					if ((name.trim().startsWith("filename"))) {
						String[] tmp = name.split("=");
						fileName = tmp[1].trim().replaceAll("\"","");
					}
				}

				if (StringUtils.endsWithIgnoreCase(fileName, ".zip")) {
					processZipFile(file, jmsBuilder(jmsRealm, destinationName, persistent, destinationType), replyTo);
					message = null;
				}
				else {
					message = XmlUtils.readXml(Misc.streamToBytes(file), Misc.DEFAULT_INPUT_STREAM_ENCODING, false);
				}
			}
			else {
				message = new String(message.getBytes(), Misc.DEFAULT_INPUT_STREAM_ENCODING);
			}
		}
		catch (Exception e) {
			throw new ApiException("Failed to read message: " + e.getMessage());
		}

		if(message != null && message.length() > 0) {
			JmsSender qms = jmsBuilder(jmsRealm, destinationName, persistent, destinationType);

			if ((replyTo!=null) && (replyTo.length()>0))
				qms.setReplyToName(replyTo);

			processMessage(qms, "testmsg_"+Misc.createUUID(), message);

			return Response.status(Response.Status.OK).build();
		}
		else {
			return Response.status(Response.Status.BAD_REQUEST).build();
		}
	}

	private JmsSender jmsBuilder(String realm, String destination, boolean persistent, String type) {
		JmsSender qms = new JmsSender();
		qms.setName("SendJmsMessageAction");
		qms.setJmsRealm(realm);
		qms.setDestinationName(destination);
		qms.setPersistent(persistent);
		qms.setDestinationType(type);
		return qms;
	}

	private void processZipFile(InputStream file, JmsSender qms, String replyTo) throws IOException {
		ZipInputStream archive = new ZipInputStream(file);
		for (ZipEntry entry=archive.getNextEntry(); entry!=null; entry=archive.getNextEntry()) {
			String name = entry.getName();
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
				String currentMessage = XmlUtils.readXml(b,0,rb,Misc.DEFAULT_INPUT_STREAM_ENCODING,false);
				// initiate MessageSender
				if ((replyTo!=null) && (replyTo.length()>0))
					qms.setReplyToName(replyTo);

				processMessage(qms, name+"_" + Misc.createSimpleUUID(), currentMessage);
			}
			archive.closeEntry();
		}
		archive.close();
	}
	
	private void processMessage(JmsSender qms, String messageId, String message) throws ApiException {
		Map<String, String> ibisContexts = XmlUtils.getIbisContext(message);
		String technicalCorrelationId = messageId;
		if (log.isDebugEnabled()) {
			if (ibisContexts!=null) {
				String contextDump = "ibisContext:";
				for (Iterator<String> it = ibisContexts.keySet().iterator(); it.hasNext();) {
					String key = it.next();
					String value = ibisContexts.get(key);
					if (log.isDebugEnabled()) {
						contextDump = contextDump + "\n " + key + "=[" + value + "]";
					}
					if (key.equals("tcid")) {
						technicalCorrelationId = value;
					}
				}
				log.debug(contextDump);
			}
		}

		try {
			qms.open();
			qms.sendMessage(technicalCorrelationId, message);
		} catch (Exception e) {
			throw new ApiException("Error occured sending message: " + e.getMessage());
		} 
		try {
			qms.close();
		} catch (Exception e) {
			throw new ApiException("Error occured on closing connection: " + e.getMessage());
		}
	}
}