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

import org.apache.commons.lang3.builder.ToStringBuilder;

import nl.nn.adapterframework.stream.Message;

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
 * @see nl.nn.adapterframework.pipes.AbstractPipe#doPipe
 * @see nl.nn.adapterframework.pipes.AbstractPipe#findForward
 */
public class PipeRunResult {

	private PipeForward pipeForward;
	private Message result;

	public PipeRunResult() {
		super();
	}

	public PipeRunResult(PipeForward forward, Object result) {
		this.pipeForward = forward;
		this.result = Message.asMessage(result);
	}

	public void setPipeForward(PipeForward pipeForward) {
		this.pipeForward = pipeForward;
	}
	public PipeForward getPipeForward() {
		return pipeForward;
	}

	public void setResult(Object result) {
		this.result = Message.asMessage(result);
	}
	public Message getResult() {
		return result;
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this);
	}
}
