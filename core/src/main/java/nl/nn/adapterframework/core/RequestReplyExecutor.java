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
import lombok.Setter;
import nl.nn.adapterframework.stream.Message;

/**
 * Runnable object for calling a request reply service. When a
 * <code>Throwable</code> has been thrown during execution it should be returned
 * by getThrowable() otherwise the reply should be returned by getReply().
 *
 * @author Jaco de Groot
 */
public abstract class RequestReplyExecutor implements Runnable {
	protected @Getter @Setter Message request;
	protected @Getter @Setter SenderResult reply;
	protected @Getter @Setter Throwable throwable;
}