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
package nl.nn.adapterframework.core;

import nl.nn.adapterframework.stream.Message;

/**
 * An <code>errorMessageFormatter</code> is responsible for returning a string
 * describing the error at hand in a format that the receiver expects. 
 * By implementing this interface, it is possible to customize messages.
 * 
 * @author Johan Verrips
 */
public interface IErrorMessageFormatter {

	public String format(String errorMessage, Throwable t, INamedObject location, Message originalMessage, String messageId, long receivedTime);
}
