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
 * Exception thrown when the <code>doPipe()</code> method
 * of a {@link IPipe Pipe} runs in error.
 * @version $Id$
 * @author  Johan Verrips
 */
public class PipeRunException extends IbisException {
	
	IPipe pipeInError=null;
public PipeRunException(IPipe pipe, String msg) {
	super(msg);
	setPipeInError(pipe);
}
public PipeRunException(IPipe pipe, String msg, Throwable e) {
	super(msg, e);
	setPipeInError(pipe);
}
/**
 * The pipe in error.
 * @return java.lang.String Name of the pipe in error
 */
public IPipe getPipeInError() {
	return pipeInError;
}
/**
 * The pipe in error. 
 * @param newPipeInError the pipe in error
 */
protected void setPipeInError(IPipe newPipeInError) {
	pipeInError = newPipeInError;
}
}
