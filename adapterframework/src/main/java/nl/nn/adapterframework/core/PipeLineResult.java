/*
   Copyright 2013 Nationale-Nederlanden

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


/**
 * The PipeLineResult is a type to store both the
 * result of the PipeLine processing as well as an exit state.
 * <br/>
 * The exit state is returned to the Adapter that hands it to the <code>Receiver</code>,
 * so that the receiver knows whether or not the request was successfully
 * processed, and might -for instance- not commit a received message.
 * <br/>
 * @version $Id$
 * @author Johan Verrips
 */
public class PipeLineResult {

	private String result;
	private String state;

	public String toString(){
		return "result=["+result+"] state=["+state+"]";
	}

	/**
	 * Get the result of the pipeline processing
	 * @return java.lang.String
	 */
	public String getResult() {
		return result;
	}
	/**
	 * set the result of the PipeLine processing to the specified value.
	 */
	public void setResult(String newResult) {
		result = newResult;
	}

	/**
	 * Get the exit-state of the pipeline
	 */
	public String getState() {
		return state;
	}
	/**
	 * set the state of the pipeline. 
	 */
	public void setState(String newState) {
		state = newState;
	}
}
