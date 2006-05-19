/*
 * $Log: RecordXml2Sender.java,v $
 * Revision 1.4  2006-05-19 09:28:38  europe\m00i745
 * Restore java files from batch package after unwanted deletion.
 *
 * Revision 1.2  2005/10/31 14:38:02  John Dekker <john.dekker@ibissource.org>
 * Add . in javadoc
 *
 * Revision 1.1  2005/10/11 13:00:22  John Dekker <john.dekker@ibissource.org>
 * New ibis file related elements, such as DirectoryListener, MoveFilePie and 
 * BatchFileTransformerPipe
 *
 */
package nl.nn.adapterframework.batch;

import java.util.ArrayList;

import nl.nn.adapterframework.core.ISender;
import nl.nn.adapterframework.core.PipeLineSession;

/**
 * Translate a record using XSL.
 * 
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>classname</td><td>nl.nn.ibis4fundation.transformation.RecordXslTransformer</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setRootTag(String) rootTag}</td><td>Roottag for the generated XML document</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setOutputFields(String) outputfields}</td><td>Comma seperated string with tagnames for the individual input fields (related using there positions). If you leave a tagname empty, the field is not xml-ized</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setSender(ISender) sender}</td><td>Sender that needs to handle the (XML) record</td><td>&nbsp;</td></tr>
 * </table>
 * </p>
 * 
 * @author: John Dekker
 */
public class RecordXml2Sender extends RecordXmlTransformer {
	public static final String version = "$RCSfile: RecordXml2Sender.java,v $  $Revision: 1.4 $ $Date: 2006-05-19 09:28:38 $";

	private ISender sender = null; // answer-sender
	
	public RecordXml2Sender() {
	}

	public Object handleRecord(PipeLineSession session, ArrayList parsedRecord) throws Exception {
		String xml = getXml(parsedRecord);
		return sender.sendMessage(null, xml);
	}
	

	public ISender getSender() {
		return sender;
	}

	public void setSender(ISender sender) {
		this.sender = sender;
	}

}
