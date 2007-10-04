/*
 * $Log: RecordXmlTransformer.java,v $
 * Revision 1.6.2.1  2007-10-04 13:07:13  europe\L190409
 * synchronize with HEAD (4.7.0)
 *
 * Revision 1.7  2007/09/24 14:55:33  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * support for parameters
 *
 * Revision 1.6  2007/07/26 16:10:44  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * now uses XmlBuilder
 *
 * Revision 1.5  2007/05/03 11:36:43  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * encode characters where required
 *
 * Revision 1.4  2006/05/19 09:28:36  Peter Eijgermans <peter.eijgermans@ibissource.org>
 * Restore java files from batch package after unwanted deletion.
 *
 * Revision 1.2  2005/10/31 14:38:02  John Dekker <john.dekker@ibissource.org>
 * Add . in javadoc
 *
 * Revision 1.1  2005/10/11 13:00:20  John Dekker <john.dekker@ibissource.org>
 * New ibis file related elements, such as DirectoryListener, MoveFilePie and 
 * BatchFileTransformerPipe
 *
 */
package nl.nn.adapterframework.batch;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.util.XmlBuilder;
import nl.nn.adapterframework.util.XmlUtils;

import org.apache.commons.lang.StringUtils;

/**
 * Encapsulates a record in XML.
 * 
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>classname</td><td>nl.nn.adapterframework.batch.RecordXmlTransformer</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setName(String) name}</td><td>name of the RecordHandler</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setRootTag(String) rootTag}</td><td>Roottag for the generated XML document</td><td>record</td></tr>
 * <tr><td>{@link #setOutputFields(String) outputfields}</td><td>Comma seperated string with tagnames for the individual input fields (related using there positions). If you leave a tagname empty, the field is not xml-ized</td><td>&nbsp;</td></tr>
 * </table>
 * </p>
 * 
 * @author  John Dekker / Gerrit van Brakel
 * @version Id
 */
public class RecordXmlTransformer extends AbstractRecordHandler {
	public static final String version = "$RCSfile: RecordXmlTransformer.java,v $  $Revision: 1.6.2.1 $ $Date: 2007-10-04 13:07:13 $";

	private String rootTag="record";
	private List outputFields; 

	public RecordXmlTransformer() {
		outputFields = new LinkedList();
	}


	public Object handleRecord(PipeLineSession session, ArrayList parsedRecord, ParameterResolutionContext prc) throws Exception {
		return getXml(parsedRecord);
	}
	
	protected String getXml(ArrayList parsedRecord) {
		XmlBuilder record=new XmlBuilder(getRootTag());
		int ndx = 0;
		for (Iterator it = outputFields.iterator(); it.hasNext();) {
			// get tagname
			String tagName = (String) it.next();
			// get value
			String value = "";
			if (ndx < parsedRecord.size()) {
				//value = (String)parsedRecord.get(ndx++);
				value = XmlUtils.encodeChars((String)parsedRecord.get(ndx++));
			}
			// if tagname is empty, then it is not added to the XML
			if (! StringUtils.isEmpty(tagName)) {
				XmlBuilder field = new XmlBuilder(tagName);
				field.setValue(value,true);
				record.addSubElement(field);
			}
		}
		return record.toXML();
	}

	public void setOutputFields(String fieldLengths) {
		StringTokenizer st = new StringTokenizer(fieldLengths, ",");
		while (st.hasMoreTokens()) {
			String token = st.nextToken().trim();
			outputFields.add(token);
		}
	}

	public void setRootTag(String string) {
		rootTag = string;
	}
	public String getRootTag() {
		return rootTag;
	}

}
