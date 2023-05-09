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
package nl.nn.adapterframework.collection;

import javax.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.CloseableThreadContext;

import lombok.Getter;
import lombok.Setter;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.pipes.FixedForwardPipe;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.ClassUtils;

/**
 * Base class for pipes that can collect items, such as multipart messages and zip archives.
 *
 * @author Niels Meijer
 *
 * @param <C> Collector instance
 * @param <P> parts that are added to the collector
 */
public abstract class CollectorPipeBase<C extends ICollector<P>, P> extends FixedForwardPipe {

	public enum Action {
		/** To initiate a new collection */
		OPEN,
		/** Add an item to to an existing collection */
		WRITE,
		/** Combination of WRITE and CLOSE: Add an item to to an existing collection, then finalize the collection */
		LAST,
		/** Finalize the collection */
		CLOSE;
	}

	/** @ff.default WRITE */
	private @Getter @Setter Action action=Action.WRITE;

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

	@Override
	public PipeRunResult doPipe(Message input, PipeLineSession session) throws PipeRunException {
		try {
			Message result = doAction(getAction(), input, session);
			return new PipeRunResult(getSuccessForward(), result);
		} catch (CollectionException e) {
			throw new PipeRunException(this, "unable to ["+getAction()+"]", e);
		}
	}

	protected @Nullable Collection<C, P> getCollection(PipeLineSession session) throws CollectionException {
		@SuppressWarnings({ "unchecked", "rawtypes" })
		Collection<C, P> collection = (Collection) session.get(getCollectionName());
		if (collection == null && getAction() != Action.OPEN) {
			throw new CollectionException("cannot find collection under key ["+getCollectionName()+"]");
		}
		return collection;
	}

	protected final @Nullable Message doAction(Action action, Message input, PipeLineSession session) throws CollectionException, PipeRunException {
		try (CloseableThreadContext.Instance ctc = CloseableThreadContext.put("action", action.name())) {
			Collection<C, P> collection = getCollection(session);
			switch(action) {
				case OPEN: {
					if(collection != null) {
						throw new CollectionException("collection [" + getCollectionName() + "] is already open, unable to create a new one");
					}
					collection = new Collection<>(createCollector());
					session.scheduleCloseOnSessionExit(collection, ClassUtils.nameOf(this));
					session.put(getCollectionName(), collection);
					break; //should also be able to write?
				}
				case WRITE:
				case LAST:
					collection.add(input, session, getParameterValueList(input, session));
					if (action == Action.LAST) {
						return closeCollector(collection, session);
					}
					break;
				case CLOSE:
					return closeCollector(collection, session);
				default:
					throw new CollectionException("Unknown action ["+action+"]");
			}

			return Message.nullMessage();
		}
	}

	protected abstract C createCollector() throws CollectionException;

	protected Message closeCollector(Collection<C, P> collection, PipeLineSession session) throws CollectionException {
		try {
			return collection.build();
		} catch (Exception e) {
			throw new CollectionException("cannot close collector", e);
		} finally {
			session.remove(getCollectionName());
		}
	}
}
