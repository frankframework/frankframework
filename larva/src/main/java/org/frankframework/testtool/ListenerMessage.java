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
package org.frankframework.testtool;

import java.util.Map;

import lombok.Getter;
import org.frankframework.core.PipeLineSession;

/**
 * @author Jaco de Groot
 */
public class ListenerMessage {
	private @Getter String message;
	private @Getter Map context;

	public ListenerMessage(String message, Map context) {
		this.message = message;
		this.context = context;
	}

	public String getCorrelationId() {
		return (String)context.get(PipeLineSession.CORRELATION_ID_KEY);
	}

}
