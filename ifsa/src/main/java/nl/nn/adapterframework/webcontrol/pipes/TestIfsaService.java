/*
   Copyright 2016 Nationale-Nederlanden

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
package nl.nn.adapterframework.webcontrol.pipes;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.extensions.ifsa.IfsaRequesterSender;
import nl.nn.adapterframework.pipes.TimeoutGuardPipe;
import nl.nn.adapterframework.util.Misc;
import nl.nn.adapterframework.util.XmlBuilder;
import nl.nn.adapterframework.util.XmlUtils;

import org.apache.commons.lang.StringUtils;

/**
 * Send a test message to an IFSA Service.
 * 
 * @author Peter Leeuwenburgh
 */

public class TestIfsaService extends TimeoutGuardPipe {

	public String doPipeWithTimeoutGuarded(Object input,
			IPipeLineSession session) throws PipeRunException {
		String method = (String) session.get("method");
		if (method.equalsIgnoreCase("GET")) {
			return doGet(session);
		} else if (method.equalsIgnoreCase("POST")) {
			return doPost(session);
		} else {
			throw new PipeRunException(this, getLogPrefix(session)
					+ "illegal value for method [" + method
					+ "], must be 'GET' or 'POST'");
		}
	}

	private String doGet(IPipeLineSession session) throws PipeRunException {
		return retrieveFormInput(session);
	}

	private String doPost(IPipeLineSession session) throws PipeRunException {
		Object form_file = session.get("file");
		String form_message = null;
		form_message = (String) session.get("message");
		if (form_file == null && (StringUtils.isEmpty(form_message))) {
			throw new PipeRunException(this, getLogPrefix(session)
					+ "Nothing to send or test");
		}

		String form_applicationId = (String) session.get("applicationId");
		String form_serviceId = (String) session.get("serviceId");
		String form_messageProtocol = (String) session.get("messageProtocol");

		if (form_file != null) {
			if (form_file instanceof InputStream) {
				InputStream inputStream = (InputStream) form_file;
				String form_fileName = (String) session.get("fileName");
				String form_fileEncoding = (String) session.get("fileEncoding");
				try {
					if (inputStream.available() > 0) {
						String fileEncoding;
						if (StringUtils.isNotEmpty(form_fileEncoding)) {
							fileEncoding = form_fileEncoding;
						} else {
							fileEncoding = Misc.DEFAULT_INPUT_STREAM_ENCODING;
						}
						if (StringUtils.endsWithIgnoreCase(form_fileName,
								".zip")) {
							try {
								form_message = processZipFile(session,
										inputStream, fileEncoding,
										form_applicationId, form_serviceId,
										form_messageProtocol);
							} catch (Exception e) {
								throw new PipeRunException(
										this,
										getLogPrefix(session)
												+ "exception on processing zip file",
										e);
							}
						} else {
							form_message = Misc.streamToString(inputStream,
									"\n", fileEncoding, false);
						}
					}
				} catch (IOException e) {
					throw new PipeRunException(this, getLogPrefix(session)
							+ "exception on converting stream to string", e);
				}
			} else {
				form_message = form_file.toString();
			}
			session.put("message", form_message);
		}
		if (StringUtils.isNotEmpty(form_message)) {
			try {
				String result = processMessage(form_applicationId,
						form_serviceId, form_messageProtocol, form_message);
				session.put("result", result);
			} catch (Exception e) {
				throw new PipeRunException(this, getLogPrefix(session)
						+ "exception on sending message", e);
			}
		}
		return "<dummy/>";
	}

	private String processZipFile(IPipeLineSession session,
			InputStream inputStream, String fileEncoding, String applicationId,
			String serviceId, String messageProtocol) throws IOException {
		String result = "";
		String lastState = null;
		ZipInputStream archive = new ZipInputStream(inputStream);
		for (ZipEntry entry = archive.getNextEntry(); entry != null; entry = archive
				.getNextEntry()) {
			String name = entry.getName();
			int size = (int) entry.getSize();
			if (size > 0) {
				byte[] b = new byte[size];
				int rb = 0;
				int chunk = 0;
				while (((int) size - rb) > 0) {
					chunk = archive.read(b, rb, (int) size - rb);
					if (chunk == -1) {
						break;
					}
					rb += chunk;
				}
				String message = XmlUtils
						.readXml(b, 0, rb, fileEncoding, false);
				if (StringUtils.isNotEmpty(result)) {
					result += "\n";
				}
				try {
					processMessage(applicationId, serviceId, messageProtocol,
							message);
					lastState = "success";
				} catch (Exception e) {
					lastState = "error";
				}
				result += name + ":" + lastState;
			}
			archive.closeEntry();
		}
		archive.close();
		session.put("result", result);
		return "";
	}

	private String processMessage(String applicationId, String serviceId,
			String messageProtocol, String message)
			throws ConfigurationException, SenderException, TimeOutException {
		IfsaRequesterSender sender;
		sender = new IfsaRequesterSender();
		sender.setName("testIfsaServiceAction");
		sender.setApplicationId(applicationId);
		sender.setServiceId(serviceId);
		sender.setMessageProtocol(messageProtocol);
		sender.configure();
		sender.open();
		return sender.sendMessage("testmsg_" + Misc.createUUID(), message);
	}

	private String retrieveFormInput(IPipeLineSession session) {
		List<String> protocols = new ArrayList<String>();
		protocols.add("RR");
		protocols.add("FF");
		XmlBuilder protocolsXML = new XmlBuilder("protocols");
		for (int i = 0; i < protocols.size(); i++) {
			XmlBuilder protocolXML = new XmlBuilder("protocol");
			protocolXML.setValue(protocols.get(i));
			protocolsXML.addSubElement(protocolXML);
		}
		return protocolsXML.toXML();
	}
}