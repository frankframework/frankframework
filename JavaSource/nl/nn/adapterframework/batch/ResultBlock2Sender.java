/*
 * $Log: ResultBlock2Sender.java,v $
 * Revision 1.2  2007-09-12 09:15:34  europe\L190409
 * updated javadoc
 *
 * Revision 1.1  2007/09/10 11:13:28  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * first version
 *
 */
package nl.nn.adapterframework.batch;

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
 * <tr><td>{@link #setSender(ISender) sender}</td><td>Sender that needs to handle the (XML) record</td><td>&nbsp;</td></tr>
 * </table>
 * </p>
 * 
 * @author  Gerrit van Brakel
 * @since   4.7  
 * @version Id
 */
public class ResultBlock2Sender extends AbstractResultHandler {

	private ISender sender = null; 
	private HashMap buffers = new HashMap();
	private HashMap counters = new HashMap();
	
	public void configure() throws ConfigurationException {
		super.configure();
		if (sender==null) {
			throw new ConfigurationException(ClassUtils.nameOf(this)+" has no sender");
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
		buffers.clear();	
		counters.clear();	
	}

	public void openResult(PipeLineSession session, String streamId) throws Exception {
		StringBuffer buffer = new StringBuffer(1000);
		buffers.put(streamId,buffer);
		counters.put(streamId,new Integer(0));
	}

	protected StringBuffer getBuffer(String streamId) throws SenderException {
		StringBuffer buffer = (StringBuffer)buffers.get(streamId);
		if (buffer==null) {
			throw new SenderException("no buffer found for stream ["+streamId+"]");
		}
		return buffer;
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

	public void handleResult(PipeLineSession session, String streamId, String recordKey, Object result) throws Exception {
		getBuffer(streamId).append(result);
	}

	public Object finalizeResult(PipeLineSession session, String streamId, boolean error) throws Exception {
		buffers.remove(streamId);
		return null;
	}

	public void openRecordType(PipeLineSession session, String streamId) throws Exception {
		StringBuffer buffer = getBuffer(streamId);
		buffer.setLength(0);
		if (StringUtils.isNotEmpty(getPrefix())) {
			buffer.append(getPrefix());
		}
	}

	public void closeRecordType(PipeLineSession session, String streamId) throws Exception {
		StringBuffer buffer = getBuffer(streamId);
		if (StringUtils.isNotEmpty(getSuffix())) {
			buffer.append(getSuffix());
		}
		ISender sender=getSender();
		sender.sendMessage(streamId+"-"+incCounter(streamId),buffer.toString());
		buffer.setLength(0);
	}

	public void setSender(ISender sender) {
		this.sender = sender;
	}
	public ISender getSender() {
		return sender;
	}

}
