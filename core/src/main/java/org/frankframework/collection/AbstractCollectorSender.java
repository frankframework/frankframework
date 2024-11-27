/*
   Copyright 2022-2023 WeAreFrank!

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
package org.frankframework.collection;

import jakarta.annotation.Nonnull;

import org.apache.commons.lang3.StringUtils;

import lombok.Getter;
import lombok.Setter;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.SenderException;
import org.frankframework.core.SenderResult;
import org.frankframework.core.TimeoutException;
import org.frankframework.senders.AbstractSenderWithParameters;
import org.frankframework.stream.Message;

/**
 * Sender that writes an item to a collection, created by {@link AbstractCollectorPipe} with <code>action=OPEN</code>.
 *
 * @ff.parameters all parameters are handled by the collection.
 *
 * @author Niels Meijer
 */
public abstract class AbstractCollectorSender<C extends ICollector<P>, P> extends AbstractSenderWithParameters {

	/** Session key used to refer to collection. Must be specified with another value if multiple CollectorPipes are active at the same time in the same session
	  * @ff.default collection
	 */
	private @Getter @Setter String collectionName = "collection";

	@Override
	public void configure() throws ConfigurationException {
		super.configure();

		if(StringUtils.isBlank(collectionName)) {
			throw new ConfigurationException("collectionName may not be blank");
		}
	}

	protected @Nonnull Collection<C, P> getCollection(PipeLineSession session) throws CollectionException {
		@SuppressWarnings({ "unchecked", "rawtypes" })
		Collection<C, P> collection = (Collection) session.get(getCollectionName());
		if (collection == null) {
			throw new CollectionException("cannot find collection under key ["+getCollectionName()+"]");
		}
		return collection;
	}

	@Override
	public @Nonnull SenderResult sendMessage(@Nonnull Message message, @Nonnull PipeLineSession session) throws SenderException, TimeoutException {
		try {
			Collection<C, P> collection = getCollection(session);
			collection.add(message, session, getParameterValueList(message, session));
		} catch (CollectionException e) {
			throw new SenderException("unable to write to collection", e);
		}
		return new SenderResult(Message.nullMessage());
	}
}
