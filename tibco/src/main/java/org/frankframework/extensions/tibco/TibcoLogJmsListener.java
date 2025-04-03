/*
   Copyright 2013 Nationale-Nederlanden, 2023 WeAreFrank!

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
package org.frankframework.extensions.tibco;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.transform.TransformerException;

import jakarta.jms.JMSException;

import org.apache.commons.lang3.StringUtils;
import org.xml.sax.SAXException;

import com.tibco.tibjms.TibjmsMapMessage;

import org.frankframework.jms.JmsListener;
import org.frankframework.soap.SoapWrapper;
import org.frankframework.stream.Message;
import org.frankframework.util.DateFormatUtils;

/**
 * {@inheritDoc}
 */
public class TibcoLogJmsListener extends JmsListener {

	private static final int[] LOGLEVELS = { 5, 10, 30, 50, 70, 90 };
	private static final String[] LOGLEVELS_TEXT = { "TRACE", "DEBUG", "INFO", "WARN", "ERROR", "FATAL" };

	@Override
	public Message extractMessage(jakarta.jms.Message rawMessage, Map<String,Object> context, boolean soap, String soapHeaderSessionKey, SoapWrapper soapWrapper) throws JMSException, SAXException, TransformerException, IOException {
		TibjmsMapMessage tjmMessage;
		try {
			tjmMessage = (TibjmsMapMessage) rawMessage;
		} catch (ClassCastException e) {
			log.error("message received by listener on [{}] was not of type TibjmsMapMessage, but [{}]", getDestinationName(), rawMessage.getClass()
					.getName(), e);
			return null;
		}
		Enumeration enumeration = tjmMessage.getMapNames();
		List list = Collections.list(enumeration);
		Collections.sort(list);
		Iterator it = list.iterator();
		StringBuilder sb = new StringBuilder();
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
			if ("_cl.creationTimes".equalsIgnoreCase(mapName)) {
				creationTimes = tjmMessage.getLong(mapName);
			} else {
				if ("_cl.severity".equalsIgnoreCase(mapName)) {
					severity = tjmMessage.getInt(mapName);
					severityStr = logLevelToText(severity);
					if (severityStr == null) {
						severityStr = "[" + severity + "]";
					}
					severityStr = StringUtils.rightPad(severityStr, 5);
				} else {
					if ("_cl.msg".equalsIgnoreCase(mapName)) {
						msg = tjmMessage.getString(mapName);
					} else {
						if ("_cl.engineName".equalsIgnoreCase(mapName)) {
							engineName = tjmMessage.getString(mapName);
						} else {
							if ("_cl.jobId".equalsIgnoreCase(mapName)) {
								jobId = tjmMessage.getString(mapName);
							} else {
								String mapValue = tjmMessage.getString(mapName);
								if ("_cl.physicalCompId.matrix.env".equalsIgnoreCase(mapName)) {
									environment = mapValue;
									context.put("environment", environment);
								}
								if ("_cl.physicalCompId.matrix.node".equalsIgnoreCase(mapName)) {
									node = mapValue;
								}
								if (first) {
									first = false;
								} else {
									sb.append(",");
								}
								sb.append("[").append(mapName).append("]=[").append(mapValue).append("]");
							}
						}
					}
				}
			}
		}
		return new Message(DateFormatUtils.format(creationTimes) + " " + severityStr + " [" + (engineName != null ? engineName : (environment + "-" + node)) + "] [" + (jobId != null ? jobId : "") + "] " + msg + " " + sb);
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
