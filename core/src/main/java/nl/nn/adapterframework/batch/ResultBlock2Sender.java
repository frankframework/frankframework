/*
   Copyright 2013, 2018 Nationale-Nederlanden

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

import org.apache.commons.lang.StringUtils;

import nl.nn.adapterframework.configuration.Configuration;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.ISender;
import nl.nn.adapterframework.core.ISenderWithParameters;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.senders.ConfigurationAware;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.ClassUtils;

/**
 * ResultHandler that collects a number of records and sends them together to a sender.
 * 
 * <table border="1">
 * <tr><th>nested elements</th><th>description</th></tr>
 * <tr><td>{@link ISender sender}</td><td>Sender to which each block of results is sent</td></tr>
 * <tr><td>{@link nl.nn.adapterframework.parameters.Parameter param}</td><td>any parameters defined on the resultHandler will be handed to the sender, if this is a {@link ISenderWithParameters ISenderWithParameters}</td></tr>
 * </table>
 * </p>
 * 
 * @author  Gerrit van Brakel
 * @since   4.7  
 */
public class ResultBlock2Sender extends Result2StringWriter implements ConfigurationAware {

	private ISender sender = null; 
	private Map<String,Integer> counters = new HashMap<String,Integer>();
	private Map<String,Integer> levels = new HashMap<String,Integer>();
	private Configuration configuration;
	
	public ResultBlock2Sender() {
		super();
		setOnOpenDocument(null);
		setOnCloseDocument(null);
	}
	
	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		if (sender==null) {
			throw new ConfigurationException(ClassUtils.nameOf(this)+" ["+getName()+"] has no sender");
		}
		if (StringUtils.isEmpty(sender.getName())) {
			sender.setName("sender of "+getName());
		}
		if (getSender() instanceof ConfigurationAware) {
			((ConfigurationAware)getSender()).setConfiguration(getConfiguration());
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
	public void openDocument(IPipeLineSession session, String streamId, ParameterResolutionContext prc) throws Exception {
		counters.put(streamId,new Integer(0));
		levels.put(streamId,new Integer(0));
		super.openDocument(session, streamId, prc);
	}
	@Override
	public void closeDocument(IPipeLineSession session, String streamId, ParameterResolutionContext prc) {
		super.closeDocument(session,streamId, prc);
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
	public void openBlock(IPipeLineSession session, String streamId, String blockName, ParameterResolutionContext prc) throws Exception {
		super.openBlock(session,streamId,blockName, prc);
		incLevel(streamId);
	}
	@Override
	public void closeBlock(IPipeLineSession session, String streamId, String blockName, ParameterResolutionContext prc) throws Exception {
		super.closeBlock(session,streamId,blockName, prc);
		int level=decLevel(streamId);
		if (level==0) {
			StringWriter writer=(StringWriter)getWriter(session,streamId,false, prc);
			if (writer!=null) {
				Message message=new Message(writer.getBuffer().toString());
				log.debug("sending block ["+message+"] to sender ["+sender.getName()+"]");
				writer.getBuffer().setLength(0);
				/*
				 * This used to be:
				 * getSender().sendMessage(streamId+"-"+incCounter(streamId),message, session); 
				 * Be aware that 'correlationId' no longer reflects streamId and counter
				 */
				getSender().sendMessage(message,session); 
			}
		}
	}


	public void setSender(ISender sender) {
		this.sender = sender;
	}
	public ISender getSender() {
		return sender;
	}

	@Override
	public void setConfiguration(Configuration configuration) {
		this.configuration = configuration;
	}
	@Override
	public Configuration getConfiguration() {
		return configuration;
	}


}
