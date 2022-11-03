/*
   Copyright 2022 WeAreFrank!

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

import lombok.Getter;
import lombok.Setter;
import nl.nn.adapterframework.stream.Message;

/**
 * The SenderResult is a type to store both the result of the processing of a message by a Sender,
 * as well as the exitState.
 */
public class SenderResult {

	private @Getter @Setter String forwardName;
	private @Getter boolean success;
	private @Getter @Setter Message result;

	public SenderResult() {
		super();
	}

	public SenderResult(String result) {
		this(null, true, new Message(result));
	}

	public SenderResult(Message result) {
		this(null, true, result);
	}

	public SenderResult(boolean success, Message result) {
		this(null, success, result);
	}

	public SenderResult(String forwardName, boolean success, Message result) {
		this.forwardName = forwardName;
		this.success = success;
		this.result = result;
	}

}
