/*
   Copyright 2013 Nationale-Nederlanden

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

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.ISender;
import nl.nn.adapterframework.core.ISenderWithParameters;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.util.ClassUtils;

import org.apache.commons.lang.StringUtils;

/**
 * ResultHandler that collects a number of records and sends them together to a sender.
 * 
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>classname</td><td>nl.nn.adapterframework.batch.ResultBlock2Sender</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setName(String) name}</td><td>name of the RecordHandler</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setPrefix(String) prefix}</td><td><i>Deprecated</i> Prefix that has to be written before record, if the record is in another block than the previous record</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setSuffix(String) suffix}</td><td><i>Deprecated</i> Suffix that has to be written after the record, if the record is in another block than the next record</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setDefault(boolean) default}</td><td>If true, this resulthandler is the default for all RecordHandlingFlow that do not have a handler specified</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setOnOpenDocument(String) onOpenDocument}</td><td>String that is written before any data of results is written</td><td>&lt;document name=&quot;#name#&quot;&gt;</td></tr>
 * <tr><td>{@link #setOnCloseDocument(String) onCloseDocument}</td><td>String that is written after all data of results is written</td><td>&lt;/document&gt;</td></tr>
 * <tr><td>{@link #setOnOpenBlock(String) onOpenBlock}</td><td>String that is written before the start of each logical block, as defined in the flow</td><td>&lt;#name#&gt;</td></tr>
 * <tr><td>{@link #setOnCloseBlock(String) onCloseBlock}</td><td>String that is written after the end of each logical block, as defined in the flow</td><td>&lt;/#name#&gt;</td></tr>
 * <tr><td>{@link #setBlockNamePattern(String) blockNamePattern}</td><td>String that is replaced by name of block or name of stream in above strings</td><td>#name#</td></tr>
 * <tr><td>{@link #setBlockByRecordType(boolean) blockByRecordType}</td><td>when set <code>true</code>(default), every group of records, as indicated by {@link IRecordHandler.isNewRecordType RecordHandler.newRecordType} is handled as a block.</td><td>true</td></tr>
 * </table>
 * </p>
 * <table border="1">
 * <tr><th>nested elements</th><th>description</th></tr>
 * <tr><td>{@link nl.nn.adapterframework.core.ISender sender}</td><td>Sender to which each block of results is sent</td></tr>
 * <tr><td>{@link nl.nn.adapterframework.parameters.Parameter param}</td><td>any parameters defined on the resultHandler will be handed to the sender, if this is a {@link nl.nn.adapterframework.core.ISenderWithParameters ISenderWithParameters}</td></tr>
 * </table>
 * </p>
 * 
 * @author  Gerrit van Brakel
 * @since   4.7  
 * @version $Id$
 */
public class ResultBlock2Sender extends Result2StringWriter {

	private ISender sender = null; 
	private Map counters = new HashMap();
	private Map levels = new HashMap();
	
	public ResultBlock2Sender() {
		super();
		setOnOpenDocument(null);
		setOnCloseDocument(null);
	}
	
	public void configure() throws ConfigurationException {
		super.configure();
		if (sender==null) {
			throw new ConfigurationException(ClassUtils.nameOf(this)+" ["+getName()+"] has no sender");
		}
		if (StringUtils.isEmpty(sender.getName())) {
			sender.setName("sender of "+getName());
		}
		sender.configure();		
	}
	public void open() throws SenderException {
		super.open();
		sender.open();		
	}
	public void close() throws SenderException {
		super.close();
		sender.close();	
		counters.clear();	
		levels.clear();
	}

	public void openDocument(IPipeLineSession session, String streamId, ParameterResolutionContext prc) throws Exception {
		counters.put(streamId,new Integer(0));
		levels.put(streamId,new Integer(0));
		super.openDocument(session, streamId, prc);
	}
	public void closeDocument(IPipeLineSession session, String streamId, ParameterResolutionContext prc) {
		super.closeDocument(session,streamId, prc);
		counters.remove(streamId);
		levels.remove(streamId);
	}


	protected int getCounter(String streamId) throws SenderException {
		Integer counter = (Integer)counters.get(streamId);
		if (counter==null) {
			throw new SenderException("no counter found for stream ["+streamId+"]");
		}
		return counter.intValue();
	}
	protected int incCounter(String streamId) throws SenderException {
		Integer counter = (Integer)counters.get(streamId);
		if (counter==null) {
			throw new SenderException("no counter found for stream ["+streamId+"]");
		}
		int result=counter.intValue()+1;
		counters.put(streamId,new Integer(result));
		return result;
	}

	public int getLevel(String streamId) throws SenderException {
		Integer level = (Integer)levels.get(streamId);
		if (level==null) {
			throw new SenderException("no level found for stream ["+streamId+"]");
		}
		return level.intValue();
	}
	protected int incLevel(String streamId) throws SenderException {
		Integer level = (Integer)levels.get(streamId);
		if (level==null) {
			throw new SenderException("no level found for stream ["+streamId+"]");
		}
		int result=level.intValue()+1;
		levels.put(streamId,new Integer(result));
		return result;
	}
	protected int decLevel(String streamId) throws SenderException {
		Integer level = (Integer)levels.get(streamId);
		if (level==null) {
			throw new SenderException("no level found for stream ["+streamId+"]");
		}
		int result=level.intValue()-1;
		levels.put(streamId,new Integer(result));
		return result;
	}



	public void openBlock(IPipeLineSession session, String streamId, String blockName, ParameterResolutionContext prc) throws Exception {
		super.openBlock(session,streamId,blockName, prc);
		incLevel(streamId);
	}
	public void closeBlock(IPipeLineSession session, String streamId, String blockName, ParameterResolutionContext prc) throws Exception {
		super.closeBlock(session,streamId,blockName, prc);
		int level=decLevel(streamId);
		if (level==0) {
			StringWriter writer=(StringWriter)getWriter(session,streamId,false, prc);
			if (writer!=null) {
				String message=writer.getBuffer().toString();
				log.debug("sending block ["+message+"] to sender ["+sender.getName()+"]");
				writer.getBuffer().setLength(0);
				ISender sender = getSender();
				if (sender instanceof ISenderWithParameters) {
					ISenderWithParameters psender = (ISenderWithParameters)sender;
					psender.sendMessage(streamId+"-"+incCounter(streamId),message,prc); 
				} else {
					sender.sendMessage(streamId+"-"+incCounter(streamId),message); 
				}
			}
		}
	}


	public void setSender(ISender sender) {
		this.sender = sender;
	}
	public ISender getSender() {
		return sender;
	}


}
