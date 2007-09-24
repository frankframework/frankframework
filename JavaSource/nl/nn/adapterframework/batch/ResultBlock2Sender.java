/*
 * $Log: ResultBlock2Sender.java,v $
 * Revision 1.4  2007-09-24 13:02:38  europe\L190409
 * updated javadoc
 *
 * Revision 1.3  2007/09/19 13:21:21  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * first working version
 *
 * Revision 1.2  2007/09/12 09:15:34  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * updated javadoc
 *
 * Revision 1.1  2007/09/10 11:13:28  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * first version
 *
 */
package nl.nn.adapterframework.batch;

import java.io.StringWriter;
import java.util.HashMap;

import org.apache.commons.lang.StringUtils;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.ISender;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.util.ClassUtils;

/**
 * ResultHandler that collects a number of records and sends them together to a sender.
 * 
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>classname</td><td>nl.nn.adapterframework.batch.ResultBlock2Sender</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setName(String) name}</td><td>name of the RecordHandler</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setPrefix(String) prefix}</td><td>Prefix that has to be written before record, if the record is in another block than the previous record</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setSuffix(String) suffix}</td><td>Suffix that has to be written after the record, if the record is in another block than the next record</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setDefault(boolean) default}</td><td>If true, this resulthandler is the default for all RecordHandlingFlow that do not have a handler specified</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setOnOpenDocument(String) onOpenDocument}</td><td>String that is written before any data of results is written</td><td>&nbsp;</td>&lt;document name=&quot;#name#&quot;&gt;</tr>
 * <tr><td>{@link #setOnCloseDocument(String) onCloseDocument}</td><td>String that is written after all data of results is written</td><td>&nbsp;</td>&lt;/document&gt;</tr>
 * <tr><td>{@link #setOnOpenBlock(String) onOpenBlock}</td><td>String that is written before the start of each logical block, as defined in the flow</td><td>&nbsp;</td>&lt;#name#&gt;</tr>
 * <tr><td>{@link #setOnCloseBlock(String) onCloseBlock}</td><td>String that is written after the end of each logical block, as defined in the flow</td><td>&nbsp;</td>&lt;/#name#&gt;</tr>
 * <tr><td>{@link #setBlockNamePattern(String) blockNamePattern}</td><td>String that is replaced by name of block or name of stream in above strings</td><td>&nbsp;</td>#name#</tr>
 * </table>
 * </p>
 * <table border="1">
 * <tr><th>nested elements</th><th>description</th></tr>
 * <tr><td>{@link nl.nn.adapterframework.core.ISender sender}</td><td>Sender to which each block of results is sent</td></tr>
 * </table>
 * </p>
 * 
 * @author  Gerrit van Brakel
 * @since   4.7  
 * @version Id
 */
public class ResultBlock2Sender extends Result2StringWriter {

	private ISender sender = null; 
	private HashMap counters = new HashMap();
	private HashMap levels = new HashMap();
	
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

	public void openDocument(PipeLineSession session, String streamId) throws Exception {
		counters.put(streamId,new Integer(0));
		levels.put(streamId,new Integer(0));
		super.openDocument(session,streamId);
	}
	public void closeDocument(PipeLineSession session, String streamId) {
		super.closeDocument(session,streamId);
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

	protected int getLevel(String streamId) throws SenderException {
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



	public void openBlock(PipeLineSession session, String streamId, String blockName) throws Exception {
		super.openBlock(session,streamId,blockName);
		incLevel(streamId);
	}
	public void closeBlock(PipeLineSession session, String streamId, String blockName) throws Exception {
		super.closeBlock(session,streamId,blockName);
		int level=decLevel(streamId);
		if (level==0) {
			StringWriter writer=(StringWriter)getWriter(session,streamId,false);
			if (writer!=null) {
				String message=writer.getBuffer().toString();
				log.debug("sending block ["+message+"] to sender ["+sender.getName()+"]");
				writer.getBuffer().setLength(0);
				sender.sendMessage(streamId+"-"+incCounter(streamId),message);
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
