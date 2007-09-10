/*
 * $Log: ResultWriter.java,v $
 * Revision 1.3  2007-09-10 11:11:59  europe\L190409
 * removed logic processing from writePrefix to calling class
 * renamed writePrefix() and writeSuffix() into open/closeRecordType()
 *
 * Revision 1.2  2007/09/05 13:02:33  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * updated javadoc
 *
 * Revision 1.1  2007/08/03 08:37:51  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * first version
 *
 */
package nl.nn.adapterframework.batch;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import nl.nn.adapterframework.core.PipeLineSession;

import org.apache.commons.lang.StringUtils;


/**
 * Baseclass for resulthandlers that write the transformed record to a writer.
 * 
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>classname</td><td>nl.nn.adapterframework.batch.ResultWriter</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setName(String) name}</td><td>Name of the resulthandler</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setPrefix(String) prefix}</td><td>Prefix that has to be written before record, if the record is in another block than the previous record</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setSuffix(String) suffix}</td><td>Suffix that has to be written after the record, if the record is in another block than the next record</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setDefaultResultHandler(boolean) default}</td><td>If true, this resulthandler is the default for all RecordHandlingFlow that do not have a handler specified</td><td>&nbsp;</td></tr>
 * </table>
 * </p>
 * 
 * @author  Gerrit van Brakel
 * @since   4.7
 * @version Id
 */
public abstract class ResultWriter extends AbstractResultHandler {
	public static final String version = "$RCSfile: ResultWriter.java,v $  $Revision: 1.3 $ $Date: 2007-09-10 11:11:59 $";
	
	private Map openWriters = Collections.synchronizedMap(new HashMap());
	
	protected abstract Writer createWriter(PipeLineSession session, String streamId) throws Exception;

	public void openResult(PipeLineSession session, String streamId) throws Exception {
		getBufferedWriter(session, streamId, true);
	}

	
	public void handleResult(PipeLineSession session, String streamId, String recordKey, Object result) throws Exception {
		if (result instanceof String) {
			write(session, streamId, (String)result);
		}
		else if (result instanceof String[]) {
			write(session, streamId, (String[])result);
		}
	}
	
	private void write(PipeLineSession session, String streamId, String line) throws Exception {
		BufferedWriter bw = getBufferedWriter(session, streamId, false);
		bw.write(line);
		bw.newLine();
	}

	private void write(PipeLineSession session, String streamId, String[] lines) throws Exception {
		BufferedWriter bw = getBufferedWriter(session, streamId, false);
		for (int i = 0; i < lines.length; i++) {
			bw.write(lines[i]);
			bw.newLine();
		}
	}
	
	public Object finalizeResult(PipeLineSession session, String streamId, boolean error) throws IOException {
		BufferedWriter bw = (BufferedWriter)openWriters.remove(streamId);
		if (bw != null) {
			bw.close();
		}
		return null;
	}
	
	public void openRecordType(PipeLineSession session, String streamId) throws Exception {
		BufferedWriter bw = getBufferedWriter(session, streamId, false);
		if (bw != null && ! StringUtils.isEmpty(getPrefix())) {
			write(session, streamId, getPrefix());
		}
	}

	public void closeRecordType(PipeLineSession session, String streamId) throws Exception {
		BufferedWriter bw = getBufferedWriter(session, streamId, false);
		if (bw != null && ! StringUtils.isEmpty(getSuffix())) {
			write(session, streamId, getSuffix());
		}
	}

	private BufferedWriter getBufferedWriter(PipeLineSession session, String streamId, boolean openIfNotOpen) throws Exception {
		BufferedWriter bw;
		bw = (BufferedWriter)openWriters.get(streamId);
		if (bw != null) {
			return bw;
		}
		
		if (!openIfNotOpen) {
			return null;
		}
		Writer writer = createWriter(session,streamId);
		if (writer==null) {
			throw new IOException("cannot get writer for stream ["+streamId+"]");
		}
		if (writer instanceof BufferedWriter) {
			bw=(BufferedWriter)bw;
		} else {
			bw=new BufferedWriter(writer);
		}
		openWriters.put(streamId,bw);
		return bw;		
	}
	
}
