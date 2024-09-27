/*
   Copyright 2013 Nationale-Nederlanden, 2020, 2022-2023 WeAreFrank!

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

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.builder.ToStringBuilder;

import org.frankframework.pipes.AbstractPipe;
import org.frankframework.stream.Message;

/**
 * The PipeRunResult is a type to store both the result of the processing of a message
 * in {@link IPipe#doPipe(Message, PipeLineSession) doPipe()} as well as the exitState.
 * <br/>
 * <b>Responsibility:</b><br/>
 * <ul><li>keeper of the result of the execution of a <code>Pipe</code></li>
 *     <li>keeper of the forward to be returned to the <code>PipeLine</code></li>
 * </ul><br/>
 * <code>Pipe</code>s return a <code>PipeRunResult</code> with the information
 * as above.
 *
 * @author Johan Verrips
 * @see PipeForward
 * @see AbstractPipe#doPipe
 * @see AbstractPipe#findForward
 */
public class PipeRunResult implements AutoCloseable {

	private @Getter @Setter PipeForward pipeForward;
	private Message result;

	public PipeRunResult() {
		super();
	}

	public PipeRunResult(PipeForward forward, Message result) {
		this.pipeForward = forward;
		this.result = result;
	}

	public PipeRunResult(PipeForward forward, Object result) {
		this.pipeForward = forward;
		setResult(result);
	}

	public void setResult(Object result) {
		if (result instanceof Message message) {
			this.result = message;
		} else {
			this.result = Message.asMessage(result);
		}
	}

	public void setResult(Message result) {
		this.result = result;
	}

	public Message getResult() {
		if (result == null) {
			return Message.nullMessage();
		}
		return result;
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this);
	}

	public boolean isSuccessful() {
		return PipeForward.SUCCESS_FORWARD_NAME.equalsIgnoreCase(getPipeForward().getName());
	}

	@Override
	public void close() throws Exception {
		if (result != null) {
			result.close();
		}
	}
}
