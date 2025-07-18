/*
   Copyright 2013, 2016 Nationale-Nederlanden, 2020-2025 WeAreFrank!

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
package org.frankframework.errormessageformatters;

import java.io.IOException;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;

import org.frankframework.core.HasName;
import org.frankframework.core.IErrorMessageFormatter;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.Resource;
import org.frankframework.stream.Message;
import org.frankframework.util.ClassLoaderUtils;
import org.frankframework.util.Misc;
import org.frankframework.util.StreamUtil;
import org.frankframework.util.TransformerPool;

/**
 * ErrorMessageFormatter that returns a fixed message with replacements.
 *
 * <p>
 *     The fixed message is loaded from a file or a string configured on the formatter. If neither
 *     is set, then the default {@link ErrorMessageFormatter} is used to create the error message.
 * </p>
 * <p>
 *     Fixed strings in the generated error message can be replaced using {@code replaceFrom} / {@code replaceTo}. As
 *     last step, an optional XSLT stylesheet transformation is applied.
 * </p>
 *
 * @see IErrorMessageFormatter for general information on error message formatters.
 * @author  Peter Leeuwenburgh
 * @since   4.3
 */
@Log4j2
public class FixedErrorMessageFormatter extends ErrorMessageFormatter {
	private @Getter String filename = null;
	private @Getter String returnString = null;
	private @Getter String replaceFrom = null;
	private @Getter String replaceTo = null;
	private @Getter String styleSheetName = null;

	@Override
	public @Nonnull Message format(@Nullable String errorMessage, @Nullable Throwable t, @Nullable HasName location, @Nullable Message originalMessage, @Nonnull PipeLineSession session) {

		Message messageToReturn = new Message(getReturnString());
		if (messageToReturn.isEmpty()) {
			messageToReturn=new Message("");
		}
		if (StringUtils.isNotEmpty(getFilename())) {
			try {
				messageToReturn = new Message(messageToReturn.asString() + StreamUtil.resourceToString(ClassLoaderUtils.getResourceURL(this, getFilename()), Misc.LINE_SEPARATOR));
			} catch (Throwable e) {
				log.error("got exception loading error message file [{}]", getFilename(), e);
			}
		}
		if (messageToReturn.isEmpty()) {
			messageToReturn = super.format(errorMessage, t, location, originalMessage, session);
		}
		if (StringUtils.isNotEmpty(getReplaceFrom())) {
			try {
				String messageAsString = messageToReturn.asString();
				messageToReturn = new Message(messageAsString.replace(getReplaceFrom(), getReplaceTo()));
			} catch (IOException e) {
				log.error("got error formatting errorMessage", e);
			}
		}

		if (StringUtils.isNotEmpty(getStyleSheetName())) {
			try{
				Resource xsltSource = Resource.getResource(this, getStyleSheetName());
				TransformerPool transformerPool = TransformerPool.getInstance(xsltSource, 0);
				messageToReturn = transformerPool.transform(messageToReturn);
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

	/** name of the file containing the result message */
	public void setFilename(String filename) {
		this.filename = filename;
	}

	/**
	 * The string to replace
	 */
	public void setReplaceFrom (String replaceFrom){
		this.replaceFrom=replaceFrom;
	}

	/**
	 * What to replace the {@code replaceFrom} with.
	 */
	public void setReplaceTo (String replaceTo){
		this.replaceTo=replaceTo;
	}

	public void setStyleSheetName (String styleSheetName){
		this.styleSheetName=styleSheetName;
	}
}
