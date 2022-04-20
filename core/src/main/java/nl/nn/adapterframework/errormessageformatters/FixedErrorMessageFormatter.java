/*
   Copyright 2013, 2016 Nationale-Nederlanden, 2020-2021 WeAreFrank!

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
import java.net.URL;

import nl.nn.adapterframework.configuration.ConfigurationWarning;
import nl.nn.adapterframework.core.INamedObject;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.Misc;
import nl.nn.adapterframework.util.XmlUtils;

import javax.xml.transform.Transformer;

import org.apache.commons.lang3.StringUtils;

/**
 * ErrorMessageFormatter that returns a fixed message with replacements.
 * 
 * @author  Peter Leeuwenburgh
 * @since   4.3
 */
public class FixedErrorMessageFormatter extends ErrorMessageFormatter {
	private String filename = null;
	private String returnString = null;
	private String replaceFrom = null;
	private String replaceTo = null;
	private String styleSheetName = null;

	@Override
	public Message format(String errorMessage, Throwable t, INamedObject location, Message originalMessage, String messageId, long receivedTime) {

		Message messageToReturn = new Message(getReturnString());
		if (messageToReturn.isEmpty()) {
			messageToReturn=new Message("");
		}
		if (StringUtils.isNotEmpty(getFileName())) {
			try {
				messageToReturn = new Message(messageToReturn.asString() + Misc.resourceToString(ClassUtils.getResourceURL(this, getFileName()), Misc.LINE_SEPARATOR));
			} catch (Throwable e) {
				log.error("got exception loading error message file [" + getFileName() + "]", e);
			}
		}
		if (messageToReturn.isEmpty()) {
			messageToReturn = super.format(errorMessage, t, location, originalMessage, messageId, receivedTime);
		}

		if (StringUtils.isNotEmpty(getReplaceFrom())) {
			try {
				messageToReturn = new Message(Misc.replace(messageToReturn.asString(), getReplaceFrom(), getReplaceTo()));
			} catch (IOException e) {
				log.error("got error formatting errorMessage", e);
			}
		}

		if (StringUtils.isNotEmpty(styleSheetName)) {
			URL xsltSource = ClassUtils.getResourceURL(this, styleSheetName);
			if (xsltSource!=null) {
				try{
					String xsltResult = null;
					Transformer transformer = XmlUtils.createTransformer(xsltSource);
					xsltResult = XmlUtils.transformXml(transformer, messageToReturn.asSource());
					messageToReturn = new Message(xsltResult);
				} catch (Throwable e) {
					log.error("got error transforming resource [" + xsltSource.toString() + "] from [" + styleSheetName + "]", e);
				}
			}
		}
	
		return messageToReturn;
	}


	@IbisDoc({"returned message", ""})
	public void setReturnString(String string) {
		returnString = string;
	}
	public String getReturnString() {
		return returnString;
	}

	@Deprecated
	@ConfigurationWarning("attribute 'fileName' is replaced with 'filename'")
	public void setFileName(String fileName) {
		setFilename(fileName);
	}

	@IbisDoc({"name of the file containing the resultmessage", ""})
	public void setFilename(String filename) {
		this.filename = filename;
	}
	public String getFileName() {
		return filename;
	}

	public void setReplaceFrom (String replaceFrom){
		this.replaceFrom=replaceFrom;
	}
	public String getReplaceFrom() {
		return replaceFrom;
	}


	public void setReplaceTo (String replaceTo){
		this.replaceTo=replaceTo;
	}
	public String getReplaceTo() {
		return replaceTo;
	}

	public String getStyleSheetName() {
		return styleSheetName;
	}
	public void setStyleSheetName (String styleSheetName){
		this.styleSheetName=styleSheetName;
	}
}