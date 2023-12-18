/*
   Copyright 2019, 2020 WeAreFrank!

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
package org.frankframework.stream;

import org.frankframework.core.IForwardTarget;
import org.frankframework.core.PipeLineSession;

public interface IOutputStreamingSupport {

	/**
	 * Implementations should return <code>true</code> when they do not require an OutputStream, but can
	 * provide one to the preceding pipe if they are themselves provided with one from the next pipe.
	 */
	public boolean supportsOutputStreamPassThrough();

	/**
	 * return a {@link MessageOutputStream} that can be used to write a message to, that then will be processed in a streaming way.
	 * If a target MessageOutputStream is required to stream output to, this can be obtained from <code>next</code>, if specified.
	 * If the implementor of this method is a pipe, and it is the last one in the chain of streaming pipes, it must provide the appropriate
	 * forward in the provide MessageOutputStream.
	 * If the class cannot provide an outputstream, it must return null.
	 * If the provider of an outputstream is a pipe itself, it must provide a proper pipeforward in the provided outputstream
	 */
	public MessageOutputStream provideOutputStream(PipeLineSession session, IForwardTarget next) throws StreamingException;

}
