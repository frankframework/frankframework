/*
   Copyright 2013 Nationale-Nederlanden, 2020, 2022-2026 WeAreFrank!

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

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import lombok.Getter;
import lombok.Setter;

import org.frankframework.core.PipeLine.ExitState;
import org.frankframework.stream.Message;

/**
 * The PipeLineResult is a type to store both the
 * result of the PipeLine processing as well as an exit state.
 * <br/>
 * The exit state is returned to the Adapter that hands it to the <code>Receiver</code>,
 * so that the receiver knows whether or not the request was successfully
 * processed, and might -for instance- not commit a received message.
 * <br/>
 * @author Johan Verrips
 */
@NullMarked
public class PipeLineResult {

	private @Setter @Nullable Message result;
	private @Setter ExitState state;
	private @Getter @Nullable Integer exitCode;

	public PipeLineResult() {
		this.state = ExitState.SUCCESS;
	}

	public PipeLineResult(Message result) {
		this.result = result;
		this.state = ExitState.SUCCESS;
	}

	public PipeLineResult(Message result, ExitState state, @Nullable Integer exitCode) {
		this.result = result;
		this.state = state;
		this.exitCode = exitCode;
	}

	public static PipeLineResult create(PipeLineExit exit, Message result) {
		PipeLineResult pipeLineResult = new PipeLineResult();
		pipeLineResult.setState(exit.getState());
		pipeLineResult.setExitCode(exit.getExitCode());
		if (!exit.isEmptyResult()) {
			pipeLineResult.setResult(result);
		} else {
			pipeLineResult.setResult(Message.nullMessage(result.getContext()));
		}
		return pipeLineResult;
	}

	public boolean isSuccessful() {
		return getState() == ExitState.SUCCESS;
	}

	public Message getResult() {
		if (result == null) {
			return Message.nullMessage();
		}
		return result;
	}

	public ExitState getState() {
		return state;
	}

	public void setExitCode(int exitCode) {
		if (exitCode > 0) {
			this.exitCode = exitCode;
		}
	}

	@Override
	public String toString() {
		return "result=[" + result + "] state=[" + state + "]";
	}

}
