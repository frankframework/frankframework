/*
 * $Log: RecordXmlTransformer.java,v $
 * Revision 1.1  2005-10-11 13:00:20  europe\m00f531
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

import org.apache.commons.lang.StringUtils;

/**
 * Translate a record using XSL
 * 
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>classname</td><td>nl.nn.ibis4fundation.transformation.RecordXslTransformer</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setRootTag(String) rootTag}</td><td>Roottag for the generated XML document</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setOutputFields(String) outputfields}</td><td>Comma seperated string with tagnames for the individual input fields (related using there positions). If you leave a tagname empty, the field is not xml-ized</td><td>&nbsp;</td></tr>
 * </table>
 * </p>
 * 
 * @author: John Dekker
 */
public class RecordXmlTransformer extends AbstractRecordHandler {
	public static final String version = "$RCSfile: RecordXmlTransformer.java,v $  $Revision: 1.1 $ $Date: 2005-10-11 13:00:20 $";

	private String rootTag;
	private List outputFields; 

	public RecordXmlTransformer() {
		outputFields = new LinkedList();
	}

	public void setOutputFields(String fieldLengths) {
		StringTokenizer st = new StringTokenizer(fieldLengths, ",");
		while (st.hasMoreTokens()) {
			String token = st.nextToken().trim();
			outputFields.add(token);
		}
	}

	public Object handleRecord(PipeLineSession session, ArrayList parsedRecord) throws Exception {
		return getXml(parsedRecord);
	}
	
	protected String getXml(ArrayList parsedRecord) {
		StringBuffer tmpResult = new StringBuffer("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		tmpResult.append("<").append(rootTag).append(">");
		
		int ndx = 0;
		for (Iterator outputFieldIt = outputFields.iterator(); outputFieldIt.hasNext();) {
			// get tagname
			String tagName = (String) outputFieldIt.next();
			
			// get value
			String value = "";
			if (ndx < parsedRecord.size()) {
				value = (String)parsedRecord.get(ndx++);
			}
			// if tagname is empty, then it is not added to the XML
			if (! StringUtils.isEmpty(tagName)) {
				tmpResult.append("<").append(tagName);
				if (StringUtils.isEmpty(value)) {
					tmpResult.append("/>");
				}
				else {
					tmpResult.append(">").append(value).append("</").append(tagName).append(">");
				}
			}
		}
		tmpResult.append("</").append(rootTag).append(">");
		
		return tmpResult.toString();
	}

	public String getRootTag() {
		return rootTag;
	}

	public void setRootTag(String string) {
		rootTag = string;
	}

}
