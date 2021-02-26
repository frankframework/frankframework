/*
   Copyright 2013 Nationale-Nederlanden
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
package nl.nn.adapterframework.extensions.tibco;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.jms.JMSException;
import javax.xml.transform.TransformerException;

import org.apache.commons.lang.StringUtils;
import org.xml.sax.SAXException;

import com.tibco.tibjms.TibjmsMapMessage;

import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.jms.JmsListener;
import nl.nn.adapterframework.soap.SoapWrapper;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.DateUtils;

public class TibcoLogJmsListener extends JmsListener {

	private final static int[] LOGLEVELS = { 5, 10, 30, 50, 70, 90 };
	private final static String[] LOGLEVELS_TEXT = { "TRACE", "DEBUG", "INFO", "WARN", "ERROR", "FATAL" };

	@Override
	public Message extractMessage(Object rawMessage, Map<String,Object> context, boolean soap, String soapHeaderSessionKey, SoapWrapper soapWrapper) throws JMSException, SAXException, TransformerException, IOException {
		TibjmsMapMessage tjmMessage;
		try {
			tjmMessage = (TibjmsMapMessage) rawMessage;
		} catch (ClassCastException e) {
			log.error("message received by listener on [" + getDestinationName() + "] was not of type TibjmsMapMessage, but [" + rawMessage.getClass().getName() + "]", e);
			return null;
		}
		Enumeration enumeration = tjmMessage.getMapNames();
		List list = Collections.list(enumeration);
		Collections.sort(list);
		Iterator it = list.iterator();
		StringBuffer sb = new StringBuffer();
		long creationTimes = 0;
		int severity = 0;
		String severityStr = null;
		String msg = null;
		String engineName = null;
		String jobId = null;
		String environment = null;
		String node = null;
		boolean first = true;
		while (it.hasNext()) {
			String mapName = (String) it.next();
			if (mapName.equalsIgnoreCase("_cl.creationTimes")) {
				creationTimes = tjmMessage.getLong(mapName);
			} else {
				if (mapName.equalsIgnoreCase("_cl.severity")) {
					severity = tjmMessage.getInt(mapName);
					severityStr = logLevelToText(severity);
					if (severityStr == null) {
						severityStr = "[" + severity + "]";
					}
					severityStr = StringUtils.rightPad(severityStr, 5);
				} else {
					if (mapName.equalsIgnoreCase("_cl.msg")) {
						msg = tjmMessage.getString(mapName);
					} else {
						if (mapName.equalsIgnoreCase("_cl.engineName")) {
							engineName = tjmMessage.getString(mapName);
						} else {
							if (mapName.equalsIgnoreCase("_cl.jobId")) {
								jobId = tjmMessage.getString(mapName);
							} else {
								String mapValue = tjmMessage.getString(mapName);
								if (mapName.equalsIgnoreCase("_cl.physicalCompId.matrix.env")) {
									environment = mapValue;
									context.put("environment", environment);
								}
								if (mapName.equalsIgnoreCase("_cl.physicalCompId.matrix.node")) {
									node = mapValue;
								}
								if (first) {
									first = false;
								} else {
									sb.append(",");
								}
								sb.append("[" + mapName + "]=[" + mapValue + "]");
							}
						}
					}
				}
			}
		}
		return new Message(DateUtils.format(creationTimes) + " " + severityStr + " [" + (engineName != null ? engineName : (environment + "-" + node)) + "] [" + (jobId != null ? jobId : "") + "] " + msg + " " + sb.toString());
	}

	@Override
	public String getIdFromRawMessage(javax.jms.Message rawMessage, Map<String, Object> threadContext) throws ListenerException {
		TibjmsMapMessage tjmMessage;
		try {
			tjmMessage = (TibjmsMapMessage) rawMessage;
		} catch (ClassCastException e) {
			log.error("message received by listener on ["
					+ getDestinationName()
					+ "] was not of type TibjmsMapMessage, but ["
					+ rawMessage.getClass().getName() + "]", e);
			return null;
		}
		return retrieveIdFromMessage(tjmMessage, threadContext);
	}

	private String logLevelToText(int logLevel) {
		for (int i = 0; i < LOGLEVELS.length; i++) {
			if (logLevel == LOGLEVELS[i]) {
				return LOGLEVELS_TEXT[i];
			}
		}
		return null;
	}
}