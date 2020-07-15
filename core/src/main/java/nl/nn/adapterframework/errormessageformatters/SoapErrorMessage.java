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

import nl.nn.adapterframework.core.INamedObject;
import nl.nn.adapterframework.soap.SoapWrapper;
import nl.nn.adapterframework.stream.Message;

/**
 * ErrorMessageFormatter that returns a soap fault message.
 * 
 * @author  Peter Leeuwenburgh
 */
public class SoapErrorMessage extends ErrorMessageFormatter {

	@Override
	public String format(String errorMessage, Throwable t, INamedObject location, Message originalMessage, String messageId, long receivedTime) {

		try {
			return SoapWrapper.getInstance().createSoapFaultMessage(getErrorMessage(errorMessage, t));
		} catch (Exception e) {
			log.error("got error getting soapWrapper instance", e);
			return super.format(errorMessage, t, location, originalMessage, messageId, receivedTime);
		}
	}
}
