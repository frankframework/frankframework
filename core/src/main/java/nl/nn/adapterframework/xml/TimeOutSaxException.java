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
package nl.nn.adapterframework.xml;

/**
 * SaxException thrown to signal that a timeout occurred.
 */
public class TimeOutSaxException extends SaxException {

	public TimeOutSaxException() {
		super();
	}
	public TimeOutSaxException(String message) {
		super(message);
	}
	public TimeOutSaxException(String message, Exception e) {
		super(message, e);
	}
	public TimeOutSaxException(Exception e) {
		super(e);
	}
}
