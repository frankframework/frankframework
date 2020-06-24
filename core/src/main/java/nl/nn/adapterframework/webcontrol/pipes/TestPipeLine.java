/*
   Copyright 2016, 2020 Nationale-Nederlanden

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
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.Logger;

import nl.nn.adapterframework.core.IAdapter;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeLineResult;
import nl.nn.adapterframework.core.PipeLineSessionBase;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.http.RestListenerUtils;
import nl.nn.adapterframework.pipes.TimeoutGuardPipe;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.Misc;
import nl.nn.adapterframework.util.XmlBuilder;
import nl.nn.adapterframework.util.XmlUtils;

/**
 * Test a PipeLine.
 * 
 * @author Peter Leeuwenburgh
 */

public class TestPipeLine extends TimeoutGuardPipe {

	protected Logger secLog = LogUtil.getLogger("SEC");
	private boolean secLogMessage = AppConstants.getInstance().getBoolean("sec.log.includeMessage", false);

	@Override
	public PipeRunResult doPipeWithTimeoutGuarded(Message input, IPipeLineSession session) throws PipeRunException {
		String method = (String) session.get("method");
		if (method.equalsIgnoreCase("GET")) {
			return new PipeRunResult(getForward(), doGet(session));
		} else if (method.equalsIgnoreCase("POST")) {
			return new PipeRunResult(getForward(), doPost(session));
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

		String form_adapterName = (String) session.get("adapterName");
		if (StringUtils.isEmpty(form_adapterName)) {
			throw new PipeRunException(this, getLogPrefix(session)
					+ "No adapter selected");
		}
		IAdapter adapter = RestListenerUtils.retrieveIbisManager(session)
				.getRegisteredAdapter(form_adapterName);
		if (adapter == null) {
			throw new PipeRunException(this, getLogPrefix(session)
					+ "Adapter with specified name [" + form_adapterName
					+ "] could not be retrieved");
		}

		boolean writeSecLogMessage = false;
		if (secLogMessage) {
			writeSecLogMessage = (Boolean) session.get("writeSecLogMessage");
		}

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
										inputStream, fileEncoding, adapter,
										writeSecLogMessage);
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
				PipeLineResult plr = processMessage(adapter, form_message,
						writeSecLogMessage);
				session.put("state", plr.getState());
				session.put("result", plr.getResult());
			} catch (Exception e) {
				throw new PipeRunException(this, getLogPrefix(session)
						+ "exception on sending message", e);
			}
		}
		return "<dummy/>";
	}

	private String processZipFile(IPipeLineSession session,
			InputStream inputStream, String fileEncoding, IAdapter adapter,
			boolean writeSecLogMessage) throws IOException {
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
				lastState = processMessage(adapter, message, writeSecLogMessage)
						.getState();
				result += name + ":" + lastState;
			}
			archive.closeEntry();
		}
		archive.close();
		session.put("state", lastState);
		session.put("result", result);
		return "";
	}

	private PipeLineResult processMessage(IAdapter adapter, String message,
			boolean writeSecLogMessage) {
		String messageId = "testmessage" + Misc.createSimpleUUID();
		IPipeLineSession pls = new PipeLineSessionBase();
		Map ibisContexts = XmlUtils.getIbisContext(message);
		String technicalCorrelationId = null;
		if (ibisContexts != null) {
			String contextDump = "ibisContext:";
			for (Iterator it = ibisContexts.keySet().iterator(); it.hasNext();) {
				String key = (String) it.next();
				String value = (String) ibisContexts.get(key);
				if (log.isDebugEnabled()) {
					contextDump = contextDump + "\n " + key + "=[" + value
							+ "]";
				}
				if (key.equals(IPipeLineSession.technicalCorrelationIdKey)) {
					technicalCorrelationId = value;
				} else {
					pls.put(key, value);
				}
			}
			if (log.isDebugEnabled()) {
				log.debug(contextDump);
			}
		}
		Date now = new Date();
		PipeLineSessionBase.setListenerParameters(pls, messageId,
				technicalCorrelationId, now, now);
		if (writeSecLogMessage) {
			secLog.info("message [" + message + "]");
		}

		// Temporarily change threadName so logging for pipeline to test will
		// not be suppressed (see property 'log.thread.rejectRegex')
		String ctName = Thread.currentThread().getName();
		String ntName = StringUtils.replace(ctName, "WebControlTestPipeLine",
				"WCTestPipeLine");
		try {
			Thread.currentThread().setName(ntName);
			return adapter.processMessage(messageId, new Message(message), pls);
		} finally {
			Thread.currentThread().setName(ctName);
		}
	}

	private String retrieveFormInput(IPipeLineSession session) {
		List<String> adapterNames = new ArrayList<String>();
		adapterNames.add("-- select an adapter --");
		adapterNames.addAll(RestListenerUtils.retrieveIbisManager(session)
				.getSortedStartedAdapterNames());
		XmlBuilder adaptersXML = new XmlBuilder("adapters");
		for (int i = 0; i < adapterNames.size(); i++) {
			XmlBuilder adapterXML = new XmlBuilder("adapter");
			adapterXML.setValue(adapterNames.get(i));
			adaptersXML.addSubElement(adapterXML);
		}
		adaptersXML.addAttribute("timeout", getTimeout());
		return adaptersXML.toXML();
	}
}