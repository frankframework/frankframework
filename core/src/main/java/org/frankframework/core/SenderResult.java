/*
   Copyright 2022-2023 WeAreFrank!

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
package org.frankframework.core;

import jakarta.annotation.Nonnull;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.frankframework.stream.Message;

/**
 * The SenderResult is a type to store both the result of the processing of a message by a Sender,
 * as well as the exitState.
 */
public class SenderResult implements AutoCloseable {

	private @Getter @Setter boolean success;
	private @Getter @Setter Message result;
	private @Getter @Setter String errorMessage;
	private @Getter @Setter String forwardName;

	public SenderResult(String result) {
		this(new Message(result));
	}

	public SenderResult(@Nonnull Message result) {
		this(true, result, null, null);
	}

	public SenderResult(@Nonnull Message result, String errorMessage) {
		this(StringUtils.isEmpty(errorMessage), result, errorMessage, null);
	}

	public SenderResult(boolean success, @Nonnull Message result, String errorMessage, String forwardName) {
		this.success = success;
		this.forwardName = forwardName;
		this.result = result;
		this.errorMessage = errorMessage;
	}

	@Override
	public void close() {
		if (result != null) {
			result.close();
		}
	}
}
