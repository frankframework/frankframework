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

//	/**
//	 * When set, the pipe will provide an {@link OutputStream} in this session variable, that the next pipe can use to write it's output to.
//	 */
//	@IbisDoc({"When set, an {@link OutputStream} will be provided in this session variable, that the next pipe can use to write it's output to.", ""})
//	public void setCreateStreamSessionKey(String createStreamSessionKey);
//	public String getCreateStreamSessionKey();

	public boolean canStreamToTarget();  
	/*
	 * When this returns true AND createStreamSessionKey is set 
	 * then doPipe must return an OutputStream in this sessionKey.
	 */
	public boolean canProvideOutputStream();  
	
	public MessageOutputStream provideOutputStream(String correlationID, IPipeLineSession session, MessageOutputStream target) throws StreamingException;
	
}
