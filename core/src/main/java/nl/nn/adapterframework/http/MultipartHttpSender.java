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
package nl.nn.adapterframework.http;

import java.io.IOException;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;

import lombok.Getter;
import nl.nn.adapterframework.collection.CollectionActor;
import nl.nn.adapterframework.collection.CollectionActor.Action;
import nl.nn.adapterframework.collection.CollectionException;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.SenderResult;
import nl.nn.adapterframework.core.TimeoutException;
import nl.nn.adapterframework.doc.IbisDocRef;
import nl.nn.adapterframework.http.mime.IMultipartCollectingElement;
import nl.nn.adapterframework.http.mime.MultipartCollector;
import nl.nn.adapterframework.parameters.ParameterValueList;
import nl.nn.adapterframework.stream.Message;

/**
 * Collector that builds up a MultipartEntity, and sends it upon CLOSE.
 *
 * @author Gerrit van Brakel
 *
 */
public class MultipartHttpSender extends HttpSender implements IMultipartCollectingElement {

	private static final String ATTRIBUTE_AND_PARAMETER_PARTNAME="partname";

	private CollectionActor<IMultipartCollectingElement, MultipartCollector> actor = new CollectionActor<>();

	private @Getter String partname;
	private @Getter String filename;
	private @Getter String mimeType;

	{
		setPostType(PostType.FORMDATA);
		setMethodType(HttpMethod.POST);
		setFirstBodyPartName("message");
	}

	@Override
	public void configure() throws ConfigurationException {
		actor.configure(getParameterList(), this);
		if (getParameterList()!=null) {
			getParameterList().configure();
		}
		switch(actor.getAction()) {
			case OPEN:
				configureContent();
				break;
			case WRITE:
				checkStringAttributeOrParameter(ATTRIBUTE_AND_PARAMETER_PARTNAME, getPartname(), ATTRIBUTE_AND_PARAMETER_PARTNAME);
				break;
			case CLOSE:
				super.configure();
				break;
			default:
				throw new ConfigurationException("Unknown action ["+actor.getAction()+"]");
		}
	}

	@Override
	public void open() throws SenderException {
		if (actor.getAction()==Action.CLOSE) {
			super.open();
		}
	}

	@Override
	public void close() throws SenderException {
		if (actor.getAction()==Action.CLOSE) {
			super.close();
		}
	}

	@Override
	public SenderResult sendMessage(Message message, PipeLineSession session) throws SenderException, TimeoutException {
		switch(actor.getAction()) {
			case OPEN:
				try {
					actor.doAction(message, session, this);
					if (StringUtils.isNotEmpty(getFirstBodyPartName())) {
						actor.getCollection(session).getEntityBuilder().addPart(createStringBodypart(message));
						if (log.isDebugEnabled()) log.debug(getLogPrefix()+"appended stringpart ["+getFirstBodyPartName()+"] with value ["+message+"]");
					}
				} catch (CollectionException e) {
					throw new SenderException(e);
				}
			case CLOSE:
				return super.sendMessage(message, session);
			default:
				try {
					return new SenderResult(actor.doAction(message, session, this));
				} catch (CollectionException e) {
					throw new SenderException(e);
				}
		}
	}

	@Override
	public MultipartCollector openCollection(Message input, PipeLineSession session, ParameterValueList pvl) throws CollectionException {
		return MultipartCollector.openCollection(input, session, pvl, this);
	}

	@Override
	protected HttpEntity createMultiPartEntity(Message message, ParameterValueList parameters, PipeLineSession session) throws SenderException, IOException {
		try {
			//actor.getCollection(session).getEntityBuilder().setCharset(Charset.forName(getCharSet())); // TODO: for backward compatibility, should be removed when results prove to be identical
			return actor.getCollection(session).getEntity();
		} catch (CollectionException e) {
			throw new SenderException(e);
		}
	}

	@IbisDocRef({CollectionActor.CLASSNAME})
	public void setAction(Action action) {
		actor.setAction(action);
	}

	@IbisDocRef({CollectionActor.CLASSNAME})
	public void setCollection(String collection) {
		actor.setCollection(collection);
	}

	/** Name of the part in the Multipart. */
	public void setPartname(String partname) {
		this.partname = partname;
	}

	/** MIME type of the part in the Multipart. */
	public void setMimeType(String mimeType) {
		this.mimeType = mimeType;
	}

	/** Filename of the part in the Multipart. */
	public void setFilename(String filename) {
		this.filename = filename;
	}
}
