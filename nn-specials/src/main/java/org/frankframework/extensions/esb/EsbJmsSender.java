/*
   Copyright 2013, 2016 Nationale-Nederlanden, 2021 WeAreFrank!

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
package org.frankframework.extensions.esb;

import org.apache.commons.lang3.StringUtils;

import lombok.Getter;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.configuration.ConfigurationWarning;
import org.frankframework.doc.Category;
import org.frankframework.jms.JmsSender;
import org.frankframework.parameters.Parameter;
import org.frankframework.util.SpringUtils;

/**
 * ESB (Enterprise Service Bus) extension of @{codeJmsSender}.
 *
 * @{inheritDoc}
 *
 * @author  Peter Leeuwenburgh
 */
@Category(Category.Type.NN_SPECIAL)
public class EsbJmsSender extends JmsSender {

	public enum MessageProtocol {
		/** Fire & Forget protocol */
		FF,
		/** Request-Reply protocol */
		RR
	}

	private @Getter MessageProtocol messageProtocol = null;
	private @Getter long timeout = 20_000;

	@Override
	public void configure() throws ConfigurationException {
		if (getMessageProtocol() == null) {
			throw new ConfigurationException("messageProtocol must be set");
		}
		if (getMessageProtocol() == MessageProtocol.RR) {
			setDeliveryMode(DeliveryMode.NON_PERSISTENT);
			setMessageTimeToLive(getTimeout());
			setReplyTimeout((int) getTimeout());
			setSynchronous(true);
		} else {
			if (getReplyToName() != null) {
				throw new ConfigurationException("replyToName [" + getReplyToName() + "] must not be set for messageProtocol [" + getMessageProtocol() + "]");
			}
		}
		if (StringUtils.isEmpty(getSoapAction()) && !paramList.hasParameter("SoapAction")) {
			Parameter p = SpringUtils.createBean(getApplicationContext(), Parameter.class);
			p.setName("SoapAction");
			p.setStyleSheetName("/xml/xsl/esb/soapAction.xsl");
			p.setXsltVersion(2);
			p.setRemoveNamespaces(true);
			addParameter(p);
		}
		super.configure();
	}

	/** protocol of ESB service to be called */
	public void setMessageProtocol(MessageProtocol protocol) {
		messageProtocol = protocol;
	}

	/**
	 * receiver timeout, in milliseconds
	 * @ff.default 20000 (20s)
	 */
	public void setTimeout(long l) {
		timeout = l;
	}

	/**
	 * receiver timeout, in milliseconds.
	 * @deprecated use {@link #setTimeout(long)} instead.
	 * @ff.default 20000 (20s)
	 */
	@Deprecated(since = "8.1")
	@ConfigurationWarning("Use attribute timeout instead")
	public void setTimeOut(long l) {
		timeout = l;
	}

	/** if messageProtocol=<code>RR</code> then <code>deliveryMode</code> defaults to <code>NON_PERSISTENT</code> */
	@Override
	public void setDeliveryMode(DeliveryMode deliveryMode) {
		super.setDeliveryMode(deliveryMode);
	}

	/** if messageProtocol=<code>RR</code> then <code>replyTimeout</code> defaults to <code>timeout</code> */
	@Override
	public void setReplyTimeout(int replyTimeout) {
		super.setReplyTimeout(replyTimeout);
	}
	/** if messageProtocol=<code>RR</code> then <code>synchronous</code> defaults to <code>true</code> */
	@Override
	public void setSynchronous(boolean synchronous) {
		super.setSynchronous(synchronous);
	}
	/** if messageProtocol=<code>RR</code> then if <code>soapAction</code> is empty then it is derived from the element MessageHeader/To/Location in the SOAP header of the input message (if $messagingLayer='P2P' then '$applicationFunction' else '$operationName_$operationVersion) */
	@Override
	public void setSoapAction(String soapAction) {
		super.setSoapAction(soapAction);
	}

}
