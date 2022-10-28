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

import java.io.OutputStream;

import org.apache.logging.log4j.CloseableThreadContext;

import lombok.Getter;
import lombok.Setter;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.TimeoutException;
import nl.nn.adapterframework.parameters.ParameterList;
import nl.nn.adapterframework.parameters.ParameterValueList;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.stream.MessageOutputStream;
import nl.nn.adapterframework.stream.StreamingException;
import nl.nn.adapterframework.util.ClassUtils;

/**
 * Worker class for {@link CollectorPipe} and {@link CollectorSender}.
 *
 * @author Gerrit van Brakel
 *
 * @param <C>
 */
public class CollectionActor<E extends ICollectingElement<C>, C extends ICollector<E>> {

	public static final String CLASSNAME = "nl.nn.adapterframework.collection.CollectionActor";

	public enum Action {
		/** To initiate a new collection */
		OPEN,
		/** add a part to to an existing collection */
		WRITE,
		/** Create a new collection entry, and provide an OutputStream that another pipe can use to write the contents */
		@Deprecated
		STREAM,
		/** Finalize the collection */
		CLOSE;
	}

	/** @ff.default WRITE */
	private @Getter @Setter Action action=Action.WRITE;

	/** Session key used to refer to collection. Must be specified with another value if multiple CollectorPipes are active at the same time in the same session
	  * @ff.default collection
	 */
	private @Getter @Setter String collection="collection";

	private ParameterList parameterList;

	public void configure(ParameterList parameterList, E owner) throws ConfigurationException {
		this.parameterList = parameterList;
	}


	protected C getCollection(PipeLineSession session) throws CollectionException {
		C result = (C)session.get(getCollection());
		if (result==null && getAction()!=Action.OPEN) {
			throw new CollectionException("cannot find collection under key ["+getCollection()+"]");
		}
		return result;
	}

	public Message doAction(Message input, PipeLineSession session, E element) throws CollectionException {
		try (CloseableThreadContext.Instance ctc = CloseableThreadContext.put("action", getAction().name())) {
			C collection = getCollection(session);
			switch(getAction()) {
				case OPEN:
					if (collection!=null) {
						throw new CollectionException("collection in session key ["+getCollection()+"] is already open");
					}
					collection = element.openCollection(input, session, getParameterValueList(input, session));
					session.scheduleCloseOnSessionExit(collection, ClassUtils.nameOf(this));
					session.put(getCollection(), collection);
					return input; // assumes input has not been consumed
				case WRITE:
					try {
						return collection.writeItem(input, session, getParameterValueList(input, session), element);
					} catch (CollectionException | TimeoutException e) {
						throw new CollectionException("cannot write to collection", e);
					}
				case STREAM:
					try {
						OutputStream result = collection.streamItem(input, session, getParameterValueList(input, session), element);
						return Message.asMessage(result);
					} catch (CollectionException e) {
						throw new CollectionException("cannot prepare collection to stream", e);
					}
				case CLOSE:
					try {
						collection.close();
						return input;
					} catch (Exception e) {
						throw new CollectionException("cannot close",e);
					} finally {
						session.remove(getCollection());
					}
				default:
					throw new CollectionException("Unknown action ["+getAction()+"]");
			}
		}
	}

	public boolean canProvideOutputStream() {
		return getAction()==Action.WRITE;
	}

	public MessageOutputStream provideOutputStream(PipeLineSession session, E element) throws StreamingException {
		try {
			C collection = getCollection(session);
			return collection.provideOutputStream(session, getParameterValueList(null, session), element);
		} catch (CollectionException e) {
			throw new StreamingException("cannot provide outputstream to collection", e);
		}
	}

	protected ParameterValueList getParameterValueList(Message input, PipeLineSession session) throws CollectionException {
		try {
			return parameterList!=null ? parameterList.getValues(input, session) : null;
		} catch (ParameterException e) {
			throw new CollectionException("cannot determine parameter values", e);
		}
	}

}
