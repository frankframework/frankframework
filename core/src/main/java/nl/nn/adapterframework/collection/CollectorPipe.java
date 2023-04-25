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

import nl.nn.adapterframework.collection.CollectionActor.Action;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.doc.ReferTo;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.stream.MessageOutputStream;
import nl.nn.adapterframework.stream.StreamingException;
import nl.nn.adapterframework.stream.StreamingPipe;

/**
 * Base class for pipes that combine a number of items.
 *
 * @author Gerrit van Brakel
 *
 * @param <C>
 */
public abstract class CollectorPipe<E extends ICollectingElement<C>, C extends ICollector<E>> extends StreamingPipe implements ICollectingElement<C> {

	private CollectionActor<E, C> actor = new CollectionActor<>();

	@SuppressWarnings("unchecked")
	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		actor.configure(getParameterList(), (E)this);
	}

	@SuppressWarnings("unchecked")
	@Override
	public PipeRunResult doPipe(Message input, PipeLineSession session) throws PipeRunException {
		try {
			return new PipeRunResult(getSuccessForward(), actor.doAction(input, session, (E)this));
		} catch (CollectionException e) {
			throw new PipeRunException(this,"", e);
		}
	}

	@Override
	protected boolean canProvideOutputStream() {
		return actor.canProvideOutputStream() && super.canProvideOutputStream();
	}

	@SuppressWarnings("unchecked")
	@Override
	public MessageOutputStream provideOutputStream(PipeLineSession session) throws StreamingException {
		return actor.provideOutputStream(session, (E)this);
	}

	@ReferTo(CollectionActor.class)
	public void setAction(Action action) {
		actor.setAction(action);
	}
	public Action getAction() {
		return actor.getAction();
	}

	@ReferTo(CollectionActor.class)
	public void setCollection(String collection) {
		actor.setCollection(collection);
	}
	public String getCollection() {
		return actor.getCollection();
	}
}
