/*
   Copyright 2013, 2016 Nationale-Nederlanden

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
package nl.nn.adapterframework.extensions.esb;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.jms.JmsSender;
import nl.nn.adapterframework.parameters.Parameter;

import org.apache.commons.lang.StringUtils;

/**
 * ESB (Enterprise Service Bus) extension of JmsSender.
 * 
 * @author  Peter Leeuwenburgh
 */
public class EsbJmsSender extends JmsSender {
	private final static String REQUEST_REPLY = "RR";
	private final static String FIRE_AND_FORGET = "FF";

	private String messageProtocol = null;
	private long timeOut = 20000;

	public void configure() throws ConfigurationException {
		if (getMessageProtocol() == null) {
			throw new ConfigurationException(getLogPrefix() + "messageProtocol must be set");
		}
		if (!getMessageProtocol().equalsIgnoreCase(REQUEST_REPLY) && !getMessageProtocol().equalsIgnoreCase(FIRE_AND_FORGET)) {
			throw new ConfigurationException(getLogPrefix() + "illegal value for messageProtocol [" + getMessageProtocol() + "], must be '" + REQUEST_REPLY + "' or '" + FIRE_AND_FORGET + "'");
		}
		if (getMessageProtocol().equalsIgnoreCase(REQUEST_REPLY)) {
			setDeliveryMode(MODE_NON_PERSISTENT);
			setMessageTimeToLive(getTimeOut());
			setReplyTimeout((int) getTimeOut());
			setSynchronous(true);
		} else {
			if (getReplyTo() != null) {
				throw new ConfigurationException(getLogPrefix() + "replyToName [" + getReplyTo() + "] must not be set for messageProtocol [" + getMessageProtocol() + "]");
			}
		}
		if (StringUtils.isEmpty(getSoapAction()) && (paramList==null || paramList.findParameter("SoapAction")==null)) {
			Parameter p = new Parameter();
			p.setName("SoapAction");
			p.setStyleSheetName("/xml/xsl/esb/soapAction.xsl");
			//p.setXslt2(true);
			p.setRemoveNamespaces(true);
			addParameter(p);
		}
		super.configure();
	}
	@IbisDoc({"protocol of ESB service to be called. Possible values \n" +
			" * <ul>\n" +
			" *   <li>\"FF\": Fire & Forget protocol</li>\n" +
			" *   <li>\"RR\": Request-Reply protocol</li>\n" +
			" * </ul>",""})
	public void setMessageProtocol(String string) {
		messageProtocol = string;
	}

	public String getMessageProtocol() {
		return messageProtocol;
	}

	public long getTimeOut() {
		return timeOut;
	}

	@IbisDoc({"receiver timeout, in milliseconds", "20000 (20s)"})
	public void setTimeOut(long l) {
		timeOut = l;
	}
}
