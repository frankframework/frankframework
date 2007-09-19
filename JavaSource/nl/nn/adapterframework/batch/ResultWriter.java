/*
 * $Log: ResultWriter.java,v $
 * Revision 1.5  2007-09-19 13:00:54  europe\L190409
 * added openDocument() and closeDocument()
 * added openBlock() and closeBlock()
 *
 * Revision 1.4  2007/09/11 11:51:44  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * updated javadoc
 *
 * Revision 1.3  2007/09/10 11:11:59  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
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
 * <tr><td>{@link #setDefault(boolean) default}</td><td>If true, this resulthandler is the default for all RecordHandlingFlow that do not have a handler specified</td><td>&nbsp;</td></tr>
 * </table>
 * </p>
 * 
 * @author  Gerrit van Brakel
 * @since   4.7
 * @version Id
 */
public abstract class ResultWriter extends AbstractResultHandler {
	public static final String version = "$RCSfile: ResultWriter.java,v $  $Revision: 1.5 $ $Date: 2007-09-19 13:00:54 $";
	
	private String onOpenDocument="<document name=\"#name#\">";
	private String onCloseDocument="</document>";
	private String onOpenBlock="<#name#>";
	private String onCloseBlock="</#name#>";
	private String blockNamePattern="#name#";
	
	private Map openWriters = Collections.synchronizedMap(new HashMap());
	
	protected abstract Writer createWriter(PipeLineSession session, String streamId) throws Exception;

	public void openDocument(PipeLineSession session, String streamId) throws Exception {
		super.openDocument(session,streamId);
		getWriter(session, streamId, true);
		write(session,streamId,replacePattern(getOnOpenDocument(),streamId));
	}

	public void closeDocument(PipeLineSession session, String streamId) {
		Writer w = (Writer)openWriters.remove(streamId);
		if (w != null) {
			try {
				w.close();
			} catch (IOException e) {
				log.error("Exception closing ["+streamId+"]",e);
			}
		}
		super.closeDocument(session,streamId);
	}

	public Object finalizeResult(PipeLineSession session, String streamId, boolean error) throws Exception {
		log.debug("finalizeResult ["+streamId+"]");
		write(session,streamId,replacePattern(getOnCloseDocument(),streamId));
		return null;
	}
	

	
	public void handleResult(PipeLineSession session, String streamId, String recordKey, Object result) throws Exception {
		if (result instanceof String) {
			write(session, streamId, (String)result);
		}
		else if (result instanceof String[]) {
			write(session, streamId, (String[])result);
		}
	}
	
	protected void writeNewLine(Writer w) throws IOException {
		if (w instanceof BufferedWriter) {
			((BufferedWriter)w).newLine();
		} else {
			w.write("\n");
		}
	}
	
	private void write(PipeLineSession session, String streamId, String line) throws Exception {
		if (line!=null) {
			Writer w = getWriter(session, streamId, false);
			w.write(line);
			writeNewLine(w);
		}
	}

	private void write(PipeLineSession session, String streamId, String[] lines) throws Exception {
		Writer w = getWriter(session, streamId, false);
		for (int i = 0; i < lines.length; i++) {
			if (lines[i]!=null) {
				w.write(lines[i]);
				writeNewLine(w);
			}
		}
	}
	
	public void openRecordType(PipeLineSession session, String streamId) throws Exception {
		Writer w = getWriter(session, streamId, false);
		if (w != null && ! StringUtils.isEmpty(getPrefix())) {
			write(session, streamId, getPrefix());
		}
	}

	public void closeRecordType(PipeLineSession session, String streamId) throws Exception {
		Writer w = getWriter(session, streamId, false);
		if (w != null && ! StringUtils.isEmpty(getSuffix())) {
			write(session, streamId, getSuffix());
		}
	}

	protected String replacePattern(String target, String blockName) {
		if (StringUtils.isEmpty(target)) {
			return null;
		}
		if (StringUtils.isEmpty(getBlockNamePattern())) {
			return target;
		}
		String result=target.replaceAll(getBlockNamePattern(),blockName);
		//if (log.isDebugEnabled()) log.debug("target ["+target+"] pattern ["+getBlockNamePattern()+"] value ["+blockName+"] result ["+result+"]");
		return result;   
	}

	public void openBlock(PipeLineSession session, String streamId, String blockName) throws Exception  {
		write(session,streamId, replacePattern(getOnOpenBlock(),blockName));
	}
	public void closeBlock(PipeLineSession session, String streamId, String blockName) throws Exception {
		write(session,streamId, replacePattern(getOnCloseBlock(),blockName));
	}


	protected Writer getWriter(PipeLineSession session, String streamId, boolean create) throws Exception {
		log.debug("getWriter ["+streamId+"], create ["+create+"]");
		Writer writer;
		writer = (Writer)openWriters.get(streamId);
		if (writer != null) {
			return writer;
		}
		
		if (!create) {
			return null;
		}
		writer = createWriter(session,streamId);
		if (writer==null) {
			throw new IOException("cannot get writer for stream ["+streamId+"]");
		}
		openWriters.put(streamId,writer);
		return writer;		
	}

	
	public void setOnOpenDocument(String line) {
		onOpenDocument = line;
	}
	public String getOnOpenDocument() {
		return onOpenDocument;
	}

	public void setOnCloseDocument(String line) {
		onCloseDocument = line;
	}
	public String getOnCloseDocument() {
		return onCloseDocument;
	}

	public void setOnOpenBlock(String line) {
		onOpenBlock = line;
	}
	public String getOnOpenBlock() {
		return onOpenBlock;
	}

	public void setOnCloseBlock(String line) {
		onCloseBlock = line;
	}
	public String getOnCloseBlock() {
		return onCloseBlock;
	}

	public void setBlockNamePattern(String pattern) {
		blockNamePattern = pattern;
	}
	public String getBlockNamePattern() {
		return blockNamePattern;
	}

}
