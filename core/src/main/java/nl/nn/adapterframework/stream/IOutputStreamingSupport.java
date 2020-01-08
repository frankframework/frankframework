/*
   Copyright 2019 Integration Partners

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
package nl.nn.adapterframework.stream;

import nl.nn.adapterframework.core.IPipeLineSession;

public interface IOutputStreamingSupport {

	/**
	 * When this returns <code>true</code> then a call to {{@link #provideOutputStream(String, IPipeLineSession, MessageOutputStream) provideOutputStream()} 
	 * must return a {@link MessageOutputStream} that can be used to write a message to, that then will be processed in a streaming way.
	 */
	public boolean canProvideOutputStream();
	
	/**
	 * When this returns <code>true</code> then {@link #provideOutputStream(String, IPipeLineSession, MessageOutputStream) provideOutputStream()} 
	 * must use {@link MessageOutputStream target} to stream its own output to. 
	 * N.B. A class should only return <code>true</code> from <code>requiresOutputStream</code> if that is the way the output can be produced efficiently
	 * in a streaming way. If the response data is already present in memory, e.g. as a String or byte array, it should send the data as is, requiresOutputStream
	 * should be kept <code>false</false>. Also when the data is available as an InputStream, it should keep requiresOutputStream <code>false</false>.
	 */
	public boolean requiresOutputStream();

	/**
	 * return a {@link MessageOutputStream} that can be used to write a message to, that then will be processed in a streaming way.
	 */
	public MessageOutputStream provideOutputStream(String correlationID, IPipeLineSession session, MessageOutputStream target) throws StreamingException;
	
	
}
