/*
 * $Log: TibcoLogJmsListener.java,v $
 * Revision 1.2  2013-02-08 09:37:08  europe\m168309
 * added contextId to logging record and put environment in sessionKey
 *
 * Revision 1.1  2013/01/31 10:00:05  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Introduction of TibcoLogJmsListener
 *
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

import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.jms.JmsListener;
import nl.nn.adapterframework.soap.SoapWrapper;
import nl.nn.adapterframework.util.DateUtils;
import nl.nn.adapterframework.util.DomBuilderException;

import com.tibco.tibjms.TibjmsMapMessage;

public class TibcoLogJmsListener extends JmsListener {

	private final static int[] LOGLEVELS = { 5, 10, 30, 50, 70, 90 };
	private final static String[] LOGLEVELS_TEXT = { "TRACE", "DEBUG", "INFO",
			"WARN", "ERROR", "FATAL" };

	public String getStringFromRawMessage(Object rawMessage, Map context,
			boolean soap, String soapHeaderSessionKey, SoapWrapper soapWrapper)
			throws JMSException, DomBuilderException, TransformerException,
			IOException {
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
		Enumeration enumeration = tjmMessage.getMapNames();
		List list = Collections.list(enumeration);
		Collections.sort(list);
		Iterator it = list.iterator();
		StringBuffer sb = new StringBuffer();
		long creationTimes = 0;
		int severity = 0;
		String severityStr = null;
		String msg = null;
		String contextId = null;
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
						if (mapName.equalsIgnoreCase("_cl.contextId")) {
							contextId = tjmMessage.getString(mapName);
						} else {
							String mapValue = tjmMessage.getString(mapName);
							if (mapName.equalsIgnoreCase("_cl.physicalCompId.matrix.env")) {
							    context.put("environment", mapValue);
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
		return DateUtils.format(creationTimes) + " " + severityStr + " " + contextId + " " + msg
				+ " " + sb.toString();
	}

	public String getIdFromRawMessage(Object rawMessage, Map threadContext)
			throws ListenerException {
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
