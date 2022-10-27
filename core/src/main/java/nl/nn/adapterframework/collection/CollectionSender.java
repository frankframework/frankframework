/*
   Copyright 2022 WeAreFrank!

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
package nl.nn.adapterframework.collection;

import lombok.Getter;
import lombok.Setter;
import nl.nn.adapterframework.core.IForwardTarget;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeoutException;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.stream.MessageOutputStream;
import nl.nn.adapterframework.stream.StreamingException;
import nl.nn.adapterframework.stream.StreamingSenderBase;

/**
 * Sender that writes an item to a collection, created by {@link CollectorPipe} with action=<code>OPEN</code.
 *
 * @ff.parameters all parameters are handled by the collection.
 *
 * @author Gerrit van Brakel
 */
public class CollectionSender extends StreamingSenderBase {

	/** Session key used to refer to collection. Must be specified with another value if multiple CollectorPipes are active at the same time in the same session
	  * @ff.default collection
	 */
	private @Getter @Setter String collection="collection";


	@Override
	public PipeRunResult sendMessage(Message message, PipeLineSession session, IForwardTarget next) throws SenderException, TimeoutException {
		try {
			ICollector collection = getCollection(session);
			Message result = collection.writeItem(message, session, getParameterValueList(message, session), this);
			return new PipeRunResult(null, result);
		} catch (CollectionException e) {
			throw new SenderException(e);
		}
	}

	@Override
	public MessageOutputStream provideOutputStream(PipeLineSession session, IForwardTarget next) throws StreamingException {
		try {
			ICollector collection = getCollection(session);
			return collection.provideOutputStream(session, getParameterValueList(null, session), this);
		} catch (CollectionException | SenderException e) {
			throw new StreamingException(e);
		}
	}

	protected ICollector getCollection(PipeLineSession session) throws CollectionException {
		ICollector result = (ICollector)session.get(getCollection());
		if (result==null) {
			throw new CollectionException("cannot find collection data under key ["+getCollection()+"]");
		}
		return result;
	}


}
