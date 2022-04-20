/*
   Copyright 2021 WeAreFrank!

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
package nl.nn.adapterframework.testtool;

import java.util.Map;

/**
 * @author Jaco de Groot
 */
public class ListenerMessage {
	private String correlationId;
	private String message;
	private Map context;

	public ListenerMessage(String correlationId, String message, Map context) {
		this.correlationId = correlationId;
		this.message = message;
		this.context = context;
	}

	public String getCorrelationId() {
		return correlationId;
	}

	public String getMessage() {
		return message;
	}

	public Map getContext() {
		return context;
	}
}
