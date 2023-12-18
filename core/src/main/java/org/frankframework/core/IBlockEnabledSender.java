/*
   Copyright 2020, 2022 WeAreFrank!

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

import org.frankframework.stream.Message;

/**
 * Allows to leverage sending messages in blocks.
 * @param <H> something shared for sending messages in a block, like a connection.
 */
public interface IBlockEnabledSender<H> extends ISenderWithParameters {

	/**
	 * open a resource that can be used multiple times when {@link #sendMessage(Object, Message, PipeLineSession)} is called.
	 */
	H openBlock(PipeLineSession session) throws SenderException, TimeoutException;

	/**
	 * close the resource that is opened by {@link #openBlock(PipeLineSession)}. It is important that this method is always called
	 * after processing with the blockHandle ends. It should effectively be called in a finally clause of a try around the openBlock.
	 */
	void closeBlock(H blockHandle, PipeLineSession session) throws SenderException;
	SenderResult sendMessage(H blockHandle, Message message, PipeLineSession session) throws SenderException, TimeoutException;
}
