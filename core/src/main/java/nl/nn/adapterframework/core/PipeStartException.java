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
 * Exception that indicates that the starting of a {@link IPipe Pipe} did not
 * succeed.<br/>
 * 
 * @author Johan Verrips IOS
 * @see nl.nn.adapterframework.pipes.AbstractPipe#start()
 */
public class PipeStartException extends IbisException {

	private String pipeNameInError = null;

	/**
	 * PipeStartException constructor comment.
	 */
	public PipeStartException() {
		super();
	}

	/**
	 * PipeStartException constructor comment.
	 */
	public PipeStartException(String msg) {
		super(msg);
	}

	public PipeStartException(String msg, Throwable e) {
		super(msg, e);
	}

	public PipeStartException(Throwable e) {
		super(e);
	}

	/**
	 * Get the name of the pipe in error.
	 */
	public java.lang.String getPipeNameInError() {
		return pipeNameInError;
	}

	/**
	 * Set the name of the pipe in error.
	 */
	public void setPipeNameInError(java.lang.String newPipeNameInError) {
		pipeNameInError = newPipeNameInError;
	}
}
