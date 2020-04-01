/*
Copyright 2016-2017, 2019 Integration Partners B.V.

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
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;
import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.cxf.jaxrs.ext.multipart.MultipartBody;

import nl.nn.adapterframework.jms.JmsSender;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.Misc;
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
	public Response putJmsMessage(MultipartBody input) throws ApiException {

		String jmsRealm = null, destinationName = null, destinationType = null, replyTo = null, message = null, fileName = null;
		InputStream file = null;
		boolean persistent = false;
		List<Attachment> inputDataMap = input.getAllAttachments();
		if(inputDataMap == null) {
			throw new ApiException("Missing post parameters");
		}

		try {
			jmsRealm = super.getAttributeValue("realm", inputDataMap).orElseThrow(() -> new ApiException("JMS realm not defined", 400)); //inputDataMap.get("message").get(0).getBodyAsString();
			destinationName = super.getAttributeValue("destination", inputDataMap).orElseThrow(() -> new ApiException("Destination name not defined", 400));
			destinationType = super.getAttributeValue("type", inputDataMap).orElseThrow(() -> new ApiException("Destination type not defined", 400));
			replyTo = super.getAttributeValue("replyTo", inputDataMap).orElseThrow(() -> new ApiException("ReplyTo not defined", 400));
			message = super.getAttributeValue("message", inputDataMap).orElse(null);
			persistent = super.getAttributeBooleanValue("persistent", inputDataMap);
			fileName = super.getFileName("file", inputDataMap).orElse(null);
			if(fileName != null)
				file = this.getFile(fileName, inputDataMap).get();
			
		}
		catch (Exception e) {
			throw new ApiException("Failed to parse one or more parameters", e);
		}

		try {
			if (file != null) {
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
			throw new ApiException("Failed to read message", e);
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