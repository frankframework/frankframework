/*
   Copyright 2013 Nationale-Nederlanden, 2022 WeAreFrank!

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
import nl.nn.adapterframework.util.Misc;

/**
 * Exception thrown when the <code>doPipe()</code> method
 * of a {@link IPipe Pipe} runs in error.
 * @author  Johan Verrips
 */
public class PipeRunException extends IbisException {

	private @Getter IPipe pipeInError = null;

	public PipeRunException(IPipe pipe, String msg) {
		super(Misc.concatStrings(pipe!=null? "Pipe ["+pipe.getName()+"]" : null, " ", msg));
		pipeInError = pipe;
	}

	public PipeRunException(IPipe pipe, String msg, Throwable e) {
		super(Misc.concatStrings(pipe!=null? "Pipe ["+pipe.getName()+"]" : null, " ", msg), e);
		pipeInError = pipe;
	}

}
