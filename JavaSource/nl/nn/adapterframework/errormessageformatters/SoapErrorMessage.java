/*
   Copyright 2013 Nationale-Nederlanden

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

/**
 * ErrorMessageFormatter that returns a soap fault message.
 * 
 * @version $Id$
 * @author  Peter Leeuwenburgh
 */
public class SoapErrorMessage extends ErrorMessageFormatter {

	public String format(String message, Throwable t, INamedObject location, String originalMessage, String messageId, long receivedTime) {

		try {
			return SoapWrapper.getInstance().createSoapFaultMessage(getMessage(message, t));
		} catch (Exception e) {
			log.error("got error getting soapWrapper instance", e);
			return super.format(message, t, location, originalMessage, messageId, receivedTime);
		}
	}
}
