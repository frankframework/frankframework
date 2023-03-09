/*
   Copyright 2013, 2016 Nationale-Nederlanden, 2020-2022 WeAreFrank!

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

import org.apache.commons.lang3.StringUtils;

import lombok.Getter;
import nl.nn.adapterframework.configuration.ConfigurationWarning;
import nl.nn.adapterframework.core.INamedObject;
import nl.nn.adapterframework.core.Resource;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.Misc;
import nl.nn.adapterframework.util.StreamUtil;
import nl.nn.adapterframework.util.StringUtil;
import nl.nn.adapterframework.util.TransformerPool;

/**
 * ErrorMessageFormatter that returns a fixed message with replacements.
 *
 * @author  Peter Leeuwenburgh
 * @since   4.3
 */
public class FixedErrorMessageFormatter extends ErrorMessageFormatter {
	private @Getter String filename = null;
	private @Getter String returnString = null;
	private @Getter String replaceFrom = null;
	private @Getter String replaceTo = null;
	private @Getter String styleSheetName = null;

	@Override
	public Message format(String errorMessage, Throwable t, INamedObject location, Message originalMessage, String messageId, long receivedTime) {

		Message messageToReturn = new Message(getReturnString());
		if (messageToReturn.isEmpty()) {
			messageToReturn=new Message("");
		}
		if (StringUtils.isNotEmpty(getFilename())) {
			try {
				messageToReturn = new Message(messageToReturn.asString() + StreamUtil.resourceToString(ClassUtils.getResourceURL(this, getFilename()), Misc.LINE_SEPARATOR));
			} catch (Throwable e) {
				log.error("got exception loading error message file [{}]", getFilename(), e);
			}
		}
		if (messageToReturn.isEmpty()) {
			messageToReturn = super.format(errorMessage, t, location, originalMessage, messageId, receivedTime);
		}
		if (StringUtils.isNotEmpty(getReplaceFrom())) {
			try {
				messageToReturn = new Message(StringUtil.replace(messageToReturn.asString(), getReplaceFrom(), getReplaceTo()));
			} catch (IOException e) {
				log.error("got error formatting errorMessage", e);
			}
		}

		if (StringUtils.isNotEmpty(getStyleSheetName())) {
			try{
				Resource xsltSource = Resource.getResource(this, getStyleSheetName());
				TransformerPool transformerPool = TransformerPool.getInstance(xsltSource, 0);
				String xsltResult = transformerPool.transform(messageToReturn.asSource());
				messageToReturn = new Message(xsltResult);
			} catch (Exception e) {
				log.error("got error transforming resource [{}] from [{}]", messageToReturn, getStyleSheetName(), e);
			}
		}

		return messageToReturn;
	}

	/** returned message */
	public void setReturnString(String string) {
		returnString = string;
	}

	@Deprecated
	@ConfigurationWarning("attribute 'fileName' is replaced with 'filename'")
	public void setFileName(String fileName) {
		setFilename(fileName);
	}

	/** name of the file containing the resultmessage */
	public void setFilename(String filename) {
		this.filename = filename;
	}

	public void setReplaceFrom (String replaceFrom){
		this.replaceFrom=replaceFrom;
	}

	public void setReplaceTo (String replaceTo){
		this.replaceTo=replaceTo;
	}

	public void setStyleSheetName (String styleSheetName){
		this.styleSheetName=styleSheetName;
	}
}
