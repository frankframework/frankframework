/*
   Copyright 2020 WeAreFrank!

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
 * Allows to leverage sending messages in blocks. 
 * @param <H> something shared for sending messages in a block, like a connection.
 */
public interface IBlockEnabledSender<H> extends ISenderWithParameters {
	
	public H openBlock(IPipeLineSession session) throws SenderException, TimeOutException;
	public void closeBlock(H blockHandle, IPipeLineSession session) throws SenderException;
	public Message sendMessage(H blockHandle, Message message, IPipeLineSession session) throws SenderException, TimeOutException;
}
