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

import org.apache.logging.log4j.CloseableThreadContext;

import lombok.Getter;
import lombok.Setter;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.core.TimeoutException;
import nl.nn.adapterframework.parameters.ParameterValueList;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.stream.MessageOutputStream;
import nl.nn.adapterframework.stream.StreamingException;
import nl.nn.adapterframework.stream.StreamingPipe;
import nl.nn.adapterframework.util.ClassUtils;

/**
 * Base class for pipes that combine a number of items.
 *
 * @author Gerrit van Brakel
 *
 * @param <C>
 */
public abstract class CollectorPipe<C extends ICollector> extends StreamingPipe{

	protected enum Action {
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

	/** @ff.mandatory */
	private @Getter @Setter Action action=null;

	/** Session key used to refer to collection. Must be specified with another value if multiple CollectorPipes are active at the same time in the same session
	  * @ff.default collection
	 */
	private @Getter @Setter String collection="collection";

	protected abstract C openCollection(Message input, PipeLineSession session, ParameterValueList pvl) throws PipeRunException;

	protected C getCollection(PipeLineSession session) throws PipeRunException {
		C result = (C)session.get(getCollection());
		if (result==null && getAction()!=Action.OPEN) {
			throw new PipeRunException(this,"cannot find collection under key ["+getCollection()+"]");
		}
		return result;
	}

	@Override
	public PipeRunResult doPipe(Message input, PipeLineSession session) throws PipeRunException {
		try (CloseableThreadContext.Instance ctc = CloseableThreadContext.put("action", getAction().name())) {
			C collection = getCollection(session);
			switch(getAction()) {
				case OPEN:
					if (collection!=null) {
						throw new PipeRunException(this,"collection in session key ["+getCollection()+"] is already open");
					}
					collection = openCollection(input, session, getParameterValueList(input, session));
					session.scheduleCloseOnSessionExit(collection, ClassUtils.nameOf(this));
					session.put(getCollection(), collection);
					return new PipeRunResult(getSuccessForward(), Message.nullMessage());
				case WRITE:
					try {
						Message result = collection.writeItem(input, session, getParameterValueList(input, session), this);
						return new PipeRunResult(getSuccessForward(), result);
					} catch (CollectionException | TimeoutException e) {
						throw new PipeRunException(this,"cannot write to collection", e);
					}
				case STREAM:
					try {
						Message result = collection.streamItem(input, session, getParameterValueList(input, session), this);
						return new PipeRunResult(getSuccessForward(), result);
					} catch (CollectionException e) {
						throw new PipeRunException(this,"cannot prepare collection to stream", e);
					}
				case CLOSE:
					try {
						collection.close();
						return new PipeRunResult(getSuccessForward(), input);
					} catch (Exception e) {
						throw new PipeRunException(this,"cannot close",e);
					} finally {
						session.remove(getCollection());
					}
				default:
					throw new PipeRunException(this, "Unknown action ["+getAction()+"]");
			}
		}
	}

	@Override
	protected boolean canProvideOutputStream() {
		return getAction()==Action.WRITE && super.canProvideOutputStream();
	}

	@Override
	public MessageOutputStream provideOutputStream(PipeLineSession session) throws StreamingException {
		try {
			C collection = getCollection(session);
			return collection.provideOutputStream(session, getParameterValueList(null, session), this);
		} catch (CollectionException | PipeRunException e) {
			throw new StreamingException("cannot provide outputstream to collection", e);
		}
	}

}
