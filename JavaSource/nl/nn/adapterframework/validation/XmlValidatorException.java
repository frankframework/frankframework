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
/*
 * $Log: XmlValidatorException.java,v $
 * Revision 1.1  2012-10-26 16:13:38  m00f069
 * Moved *Xmlvalidator*, Schema and SchemasProvider to new validation package
 *
 * Revision 1.1  2012/09/19 09:49:58  Jaco de Groot <jaco.de.groot@ibissource.org>
 * - Set reasonSessionKey to "failureReason" and xmlReasonSessionKey to "xmlFailureReason" by default
 * - Fixed check on unknown namspace in case root attribute or xmlReasonSessionKey is set
 * - Fill reasonSessionKey with a message when an exception is thrown by parser instead of the ErrorHandler being called
 * - Added/fixed check on element of soapBody and soapHeader
 * - Cleaned XML validation code a little (e.g. moved internal XmlErrorHandler class (double code in two classes) to an external class, removed MODE variable and related code)
 *
 */
package nl.nn.adapterframework.validation;

import nl.nn.adapterframework.core.IbisException;

public class XmlValidatorException extends IbisException {
	private static final long serialVersionUID = 1L;

	XmlValidatorException(String cause, Throwable t) {
		super(cause,t);
	}

	XmlValidatorException(String cause) {
		super(cause);
	}
}
