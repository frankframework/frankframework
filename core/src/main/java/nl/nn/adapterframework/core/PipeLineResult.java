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
 * The PipeLineResult is a type to store both the
 * result of the PipeLine processing as well as an exit state.
 * <br/>
 * The exit state is returned to the Adapter that hands it to the <code>Receiver</code>,
 * so that the receiver knows whether or not the request was successfully
 * processed, and might -for instance- not commit a received message.
 * <br/>
 * @author Johan Verrips
 */
public class PipeLineResult {

	private Message result;
	private String state;
	private int exitCode;

	@Override
	public String toString(){
		return "result=["+result+"] state=["+state+"]";
	}

	/**
	 * The result of the pipeline processing
	 */
	public void setResult(Message newResult) {
		result = newResult;
	}
	public Message getResult() {
		return result;
	}

	/**
	 * The exit-state of the pipeline
	 */
	public void setState(String newState) {
		state = newState;
	}
	public String getState() {
		return state;
	}

	public void setExitCode(int code) {
		this.exitCode = code;
	}
	public int getExitCode() {
		return this.exitCode;
	}
}
