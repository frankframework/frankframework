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
package org.frankframework.senders;

import jakarta.annotation.Nonnull;

import org.frankframework.core.PipeLineSession;
import org.frankframework.core.SenderException;
import org.frankframework.core.SenderResult;
import org.frankframework.core.TimeoutException;
import org.frankframework.doc.Category;
import org.frankframework.stream.Message;

/**
 * Echos input to output.
 *
 * @author  Gerrit van Brakel
 * @since   4.9
 */
@Category(Category.Type.BASIC)
public class EchoSender extends AbstractSenderWithParameters {

	private boolean synchronous=true;

	@Override
	public @Nonnull SenderResult sendMessage(@Nonnull Message message, @Nonnull PipeLineSession session) throws SenderException, TimeoutException {
		return new SenderResult(message);
	}

	/**
	 * hack to allow to introduce a correlationid
	 * @ff.default true
	 */
	public void setSynchronous(boolean b) {
		synchronous = b;
	}
	@Override
	public boolean isSynchronous() {
		return synchronous;
	}

}
