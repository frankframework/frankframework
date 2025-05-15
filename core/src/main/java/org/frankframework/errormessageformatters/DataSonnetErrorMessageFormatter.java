/*
   Copyright 2025 WeAreFrank!

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

import jakarta.annotation.Nonnull;

import com.datasonnet.Mapper;

import lombok.extern.log4j.Log4j2;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.HasName;
import org.frankframework.core.IConfigurable;
import org.frankframework.core.IErrorMessageFormatter;
import org.frankframework.doc.Protected;
import org.frankframework.documentbuilder.DocumentFormat;
import org.frankframework.stream.Message;

/**
 * Create an error message in JSON format.
 * <p>
 *
 * </p>
 */
@Log4j2
public class DataSonnetErrorMessageFormatter extends ErrorMessageFormatter implements IErrorMessageFormatter, IConfigurable {

	private String styleSheetName;
	private Mapper mapper;

	@Override
	public void configure() throws ConfigurationException {
		setMessageFormat(DocumentFormat.JSON);


	}

	@Override
	public Message format(String errorMessage, Throwable t, HasName location, Message originalMessage, String messageId, long receivedTime) {
		Message defaultMessage = super.format(errorMessage, t, location, originalMessage, messageId, receivedTime);
		return null;
	}

	/**
	 * Set a DataSonnet stylesheet to transform the default JSON error message to a custom format.
	 */
	public void setStyleSheetName(String styleSheetName) {
		this.styleSheetName = styleSheetName;
	}

	@Override
	@Protected
	public void setMessageFormat(@Nonnull DocumentFormat messageFormat) {
		super.setMessageFormat(messageFormat);
	}
}
