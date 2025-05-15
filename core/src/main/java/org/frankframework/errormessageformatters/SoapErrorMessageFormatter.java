/*
   Copyright 2013 Nationale-Nederlanden, 2020-2025 WeAreFrank!

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

import lombok.extern.log4j.Log4j2;

import org.frankframework.core.HasName;
import org.frankframework.core.PipeLineSession;
import org.frankframework.doc.Protected;
import org.frankframework.documentbuilder.DocumentFormat;
import org.frankframework.soap.SoapWrapper;
import org.frankframework.stream.Message;

/**
 * ErrorMessageFormatter that returns a soap fault message.
 *
 * @author  Peter Leeuwenburgh
 */
@Log4j2
public class SoapErrorMessageFormatter extends ErrorMessageFormatter {

	@Override
	public Message format(String errorMessage, Throwable t, HasName location, Message originalMessage, PipeLineSession session) {

		try {
			return SoapWrapper.getInstance().createSoapFaultMessage(getErrorMessage(errorMessage, t));
		} catch (Exception e) {
			log.error("got error getting soapWrapper instance", e);
			return super.format(errorMessage, t, location, originalMessage, session);
		}
	}

	@Override
	@Protected
	public void setMessageFormat(@Nonnull DocumentFormat messageFormat) {
		// Add no logic, override to add @Protected annotation which will make sure this method cannot be used from configuration for this class
		super.setMessageFormat(messageFormat);
	}
}
