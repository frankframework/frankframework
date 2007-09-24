/*
 * $Log: Result2StringWriter.java,v $
 * Revision 1.3  2007-09-24 14:55:33  europe\L190409
 * support for parameters
 *
 * Revision 1.2  2007/09/24 13:02:38  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * updated javadoc
 *
 * Revision 1.1  2007/09/19 13:01:12  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * first version
 *
 */
package nl.nn.adapterframework.batch;

import java.io.StringWriter;
import java.io.Writer;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;


/**
 * Resulthandler that writes the transformed record to a file.
 * 
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>classname</td><td>nl.nn.adapterframework.batch.Result2Filewriter</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setName(String) name}</td><td>Name of the resulthandler</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setPrefix(String) prefix}</td><td><i>Deprecated</i> Prefix that has to be written before record, if the record is in another block than the previous record</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setSuffix(String) suffix}</td><td><i>Deprecated</i> Suffix that has to be written after the record, if the record is in another block than the next record. <br/>N.B. If a suffix is set without a prefix, it is only used at the end of processing (i.e. at the end of the file) as a final close</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setDefault(boolean) default}</td><td>If true, this resulthandler is the default for all RecordHandlingFlow that do not have a handler specified</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setOnOpenDocument(String) onOpenDocument}</td><td>String that is written before any data of results is written</td><td>&lt;document name=&quot;#name#&quot;&gt;</td></tr>
 * <tr><td>{@link #setOnCloseDocument(String) onCloseDocument}</td><td>String that is written after all data of results is written</td><td>&lt;/document&gt;</td></tr>
 * <tr><td>{@link #setOnOpenBlock(String) onOpenBlock}</td><td>String that is written before the start of each logical block, as defined in the flow</td><td>&lt;#name#&gt;</td></tr>
 * <tr><td>{@link #setOnCloseBlock(String) onCloseBlock}</td><td>String that is written after the end of each logical block, as defined in the flow</td><td>&lt;/#name#&gt;</td></tr>
 * <tr><td>{@link #setBlockNamePattern(String) blockNamePattern}</td><td>String that is replaced by name of block or name of stream in above strings</td><td>#name#</td></tr>
 * <tr><td>{@link #setOutputDirectory(String) outputDirectory}</td><td>Directory in which the resultfile must be stored</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setFilenamePattern(String) filenamePattern}</td><td>Name of the file is created using the MessageFormat. Params: 1=inputfilename, 2=extension of file, 3=current date</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setMove2dirAfterFinalize(String) move2dirAfterFinalize}</td><td>Directory to which the created file must be moved after finalization (is optional)</td><td>&nbsp;</td></tr>
 * </table>
 * </p>
 * 
 * @author: Gerrit van Brakel
 * @since   4.7
 * @version Id
 */
public class Result2StringWriter extends ResultWriter {
	public static final String version = "$RCSfile: Result2StringWriter.java,v $  $Revision: 1.3 $ $Date: 2007-09-24 14:55:33 $";
	
	private Map openWriters = Collections.synchronizedMap(new HashMap());
	
	protected Writer createWriter(PipeLineSession session, String streamId, ParameterResolutionContext prc) throws Exception {
		Writer writer=new StringWriter();
		openWriters.put(streamId,writer);
		return writer;
	}
	
	public Object finalizeResult(PipeLineSession session, String streamId, boolean error, ParameterResolutionContext prc) throws Exception {
		super.finalizeResult(session,streamId, error, prc);
		StringWriter writer = (StringWriter)getWriter(session,streamId,false, prc);
		String result=null;
		if (writer!=null) {
			result = (writer).getBuffer().toString();
		} 
		return result;		
	}

}
