/*
   Copyright 2013, 2018 Nationale-Nederlanden, 2021, 2022 WeAreFrank!

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
package nl.nn.adapterframework.batch;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import lombok.Getter;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.ISender;
import nl.nn.adapterframework.core.ISenderWithParameters;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.ClassUtils;

/**
 * ResultHandler that collects a number of records and sends them together to a sender.
 *
 * @ff.parameters any parameters defined on the resultHandler will be handed to the sender, if this is a {@link ISenderWithParameters ISenderWithParameters}
 *
 * @author  Gerrit van Brakel
 * @since   4.7
 * @deprecated Warning: non-maintained functionality.
 */
public class ResultBlock2Sender extends Result2StringWriter {

	private @Getter ISender sender = null;
	private Map<String,Integer> counters = new HashMap<>();
	private Map<String,Integer> levels = new HashMap<>();

	public ResultBlock2Sender() {
		super();
		setOnOpenDocument(null);
		setOnCloseDocument(null);
	}

	@Override
	public void configure() throws ConfigurationException {
		super.configure();

		if (sender==null) {
			throw new ConfigurationException(ClassUtils.nameOf(this)+" has no sender");
		}
		if (StringUtils.isEmpty(sender.getName())) {
			sender.setName("sender of "+getName());
		}
		sender.configure();
	}

	@Override
	public void open() throws SenderException {
		super.open();
		sender.open();
	}

	@Override
	public void close() throws SenderException {
		super.close();
		sender.close();
		counters.clear();
		levels.clear();
	}

	@Override
	public void openDocument(PipeLineSession session, String streamId) throws Exception {
		counters.put(streamId,new Integer(0));
		levels.put(streamId,new Integer(0));
		super.openDocument(session, streamId);
	}

	@Override
	public void closeDocument(PipeLineSession session, String streamId) {
		super.closeDocument(session,streamId);
		counters.remove(streamId);
		levels.remove(streamId);
	}

	protected int getCounter(String streamId) throws SenderException {
		Integer counter = counters.get(streamId);
		if (counter==null) {
			throw new SenderException("no counter found for stream ["+streamId+"]");
		}
		return counter.intValue();
	}

	protected int incCounter(String streamId) throws SenderException {
		Integer counter = counters.get(streamId);
		if (counter==null) {
			throw new SenderException("no counter found for stream ["+streamId+"]");
		}
		int result=counter.intValue()+1;
		counters.put(streamId,new Integer(result));
		return result;
	}

	public int getLevel(String streamId) throws SenderException {
		Integer level = levels.get(streamId);
		if (level==null) {
			throw new SenderException("no level found for stream ["+streamId+"]");
		}
		return level.intValue();
	}

	protected int incLevel(String streamId) throws SenderException {
		Integer level = levels.get(streamId);
		if (level==null) {
			throw new SenderException("no level found for stream ["+streamId+"]");
		}
		int result=level.intValue()+1;
		levels.put(streamId,new Integer(result));
		return result;
	}

	protected int decLevel(String streamId) throws SenderException {
		Integer level = levels.get(streamId);
		if (level==null) {
			throw new SenderException("no level found for stream ["+streamId+"]");
		}
		int result=level.intValue()-1;
		levels.put(streamId,new Integer(result));
		return result;
	}

	@Override
	public void openBlock(PipeLineSession session, String streamId, String blockName, Map<String, Object> blocks) throws Exception {
		super.openBlock(session,streamId,blockName,blocks);
		incLevel(streamId);
	}

	@Override
	public void closeBlock(PipeLineSession session, String streamId, String blockName, Map<String, Object> blocks) throws Exception {
		super.closeBlock(session,streamId,blockName,blocks);
		int level=decLevel(streamId);
		if (level==0) {
			StringWriter writer=(StringWriter)getWriter(session,streamId,false);
			if (writer!=null) {
				Message message=new Message(writer.getBuffer().toString());
				log.debug("sending block ["+message+"] to sender ["+sender.getName()+"]");
				writer.getBuffer().setLength(0);
				getSender().sendMessageOrThrow(message, session).close();
			}
		}
	}

	/** Sender to which each block of results is sent */
	public void setSender(ISender sender) {
		this.sender = sender;
	}
}
