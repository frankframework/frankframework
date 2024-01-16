/*
   Copyright 2020 WeAreFrank!

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
package org.frankframework.stream;

import org.frankframework.xml.SaxException;

/**
 * SAXException thrown to signal that the consumer of a stream does not want to receive more of it.
 */
public class SaxAbortException extends SaxException {

	public SaxAbortException() {
		super();
	}
	public SaxAbortException(String message) {
		super(message);
	}
	public SaxAbortException(String message, Exception cause) {
		super(message, cause);
	}
	public SaxAbortException(Exception cause) {
		super(cause);
	}
}
