/*
 * $Log: RecordXml2Sender.java,v $
 * Revision 1.7  2007-09-24 13:02:38  europe\L190409
 * updated javadoc
 *
 * Revision 1.6  2007/07/26 16:10:10  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * cosmetic changes
 *
 * Revision 1.5  2007/05/03 11:39:43  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * implement methods configure(), open() and close()
 *
 * Revision 1.4  2006/05/19 09:28:38  Peter Eijgermans <peter.eijgermans@ibissource.org>
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

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.ISender;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.util.ClassUtils;

/**
 * Translate a record into XML, then send it using a sender.
 * 
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>classname</td><td>nl.nn.adapterframework.batch.RecordXml2Sender</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setName(String) name}</td><td>name of the RecordHandler</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setRootTag(String) rootTag}</td><td>Roottag for the generated XML document</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setOutputFields(String) outputfields}</td><td>Comma seperated string with tagnames for the individual input fields (related using there positions). If you leave a tagname empty, the field is not xml-ized</td><td>&nbsp;</td></tr>
 * </table>
 * </p>
 * <table border="1">
 * <tr><th>nested elements</th><th>description</th></tr>
 * <tr><td>{@link nl.nn.adapterframework.core.ISender sender}</td><td>Sender that needs to handle the (XML) record</td></tr>
 * </table>
 * </p>
 * 
 * @author  John Dekker
 * @version Id
 */
public class RecordXml2Sender extends RecordXmlTransformer {
	public static final String version = "$RCSfile: RecordXml2Sender.java,v $  $Revision: 1.7 $ $Date: 2007-09-24 13:02:38 $";

	private ISender sender = null; 
	
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
	}

	public Object handleRecord(PipeLineSession session, ArrayList parsedRecord) throws Exception {
		String xml = getXml(parsedRecord);
		return sender.sendMessage(null, xml);
	}
	


	public void setSender(ISender sender) {
		this.sender = sender;
	}
	public ISender getSender() {
		return sender;
	}

}
