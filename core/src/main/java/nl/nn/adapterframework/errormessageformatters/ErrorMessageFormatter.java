/*
   Copyright 2013 Nationale-Nederlanden, 2020 WeAreFrank!

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
package nl.nn.adapterframework.errormessageformatters;

import java.io.IOException;
import java.util.Date;

import nl.nn.adapterframework.core.IErrorMessageFormatter;
import nl.nn.adapterframework.core.INamedObject;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.XmlBuilder;
import nl.nn.adapterframework.util.XmlUtils;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.logging.log4j.Logger;
/**
 * This <code>ErrorMessageFormatter</code> wraps an error in an XML string.
 *
 * <br/>
 * Sample xml:
 * <br/>
 * <code><pre>
 * &lt;errorMessage&gt;
 *    &lt;message timestamp="Mon Oct 13 12:01:57 CEST 2003" 
 *             originator="NN IOS AdapterFramework(set from 'application.name' and 'application.version')"
 *             message="<i>message describing the error that occurred</i>" &gt;
 *    &lt;location class="nl.nn.adapterframework.pipes.XmlSwitch" name="ServiceSwitch"/&gt
 *    &lt;details&gt<i>detailed information of the error</i>&lt;/details&gt
 *    &lt;originalMessage messageId="..." receivedTime="Mon Oct 27 12:10:18 CET 2003" &gt;
 *        &lt;![CDATA[<i>contents of message for which the error occurred</i>]]&gt;
 *    &lt;/originalMessage&gt;
 * &lt;/errorMessage&gt;
 * </pre></code>
 * 
 * @author  Gerrit van Brakel
 */
public class ErrorMessageFormatter implements IErrorMessageFormatter {
    protected Logger log = LogUtil.getLogger(this);
	private ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

	/**
	 * Format the available parameters into a XML-message.
	 *
	 * Override this method in descender-classes to obtain the required behaviour.
	 */
	@Override
	public String format(String errorMessage, Throwable t, INamedObject location, Message originalMessage, String messageId, long receivedTime) {
	
		String details = null;
		errorMessage = getErrorMessage(errorMessage, t);
		if (t != null) {
			details = ExceptionUtils.getStackTrace(t);
		}
		 
		String originator = AppConstants.getInstance().getProperty("application.name")+" "+ AppConstants.getInstance().getProperty("application.version");
		// Build a Base xml
		XmlBuilder errorXml = new XmlBuilder("errorMessage");
		errorXml.addAttribute("timestamp", new Date().toString());
		errorXml.addAttribute("originator", originator);
		errorXml.addAttribute("message", XmlUtils.replaceNonValidXmlCharacters(errorMessage));

		if (location != null) {
			XmlBuilder locationXml = new XmlBuilder("location");
			locationXml.addAttribute("class", location.getClass().getName());
			locationXml.addAttribute("name", location.getName());
			errorXml.addSubElement(locationXml);
		}

		if (details != null && !details.equals("")) {
			XmlBuilder detailsXml = new XmlBuilder("details");
			// detailsXml.setCdataValue(details);
			detailsXml.setValue(XmlUtils.replaceNonValidXmlCharacters(details), true);
			errorXml.addSubElement(detailsXml);
		}

		XmlBuilder originalMessageXml = new XmlBuilder("originalMessage");
		originalMessageXml.addAttribute("messageId", messageId);
		if (receivedTime != 0) {
			originalMessageXml.addAttribute("receivedTime", new Date(receivedTime).toString());
		}
		// originalMessageXml.setCdataValue(originalMessage);
		try {
			originalMessageXml.setValue(originalMessage.asString(), true);
		} catch (IOException e) {
			log.warn("Could not convert originalMessage for messageId ["+messageId+"]",e);
			originalMessageXml.setValue(originalMessage.toString(), true);
		}
		errorXml.addSubElement(originalMessageXml);

		return errorXml.toXML();
	}

	protected String getErrorMessage(String message, Throwable t) {
		if (t != null) {
			if (message == null || message.equals("")) {
				message = t.getMessage();
			} else {
				message += ": "+t.getMessage();
			}
		}
		return message;
	}

	/**
	 * Get ClassLoader to resolve files
	 * @return runtime ClassLoader of the loaded configuration
	 */
	protected ClassLoader getClassLoader() {
		return this.classLoader;
	}
}
