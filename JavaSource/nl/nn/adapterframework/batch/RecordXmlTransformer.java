/*
 * $Log: RecordXmlTransformer.java,v $
 * Revision 1.10.2.1  2008-04-03 08:09:07  europe\L190409
 * synch from HEAD
 *
 * Revision 1.12  2008/03/27 10:33:11  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * improved logging
 *
 * Revision 1.11  2008/02/28 16:17:07  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * move xslt functionality to base class RecordXmlTransformer
 *
 * Revision 1.10  2008/02/19 09:23:48  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * updated javadoc
 *
 * Revision 1.9  2008/02/15 16:05:10  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * updated javadoc
 *
 * Revision 1.8  2007/10/08 13:28:58  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * changed ArrayList to List where possible
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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.ParameterList;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.TransformerPool;
import nl.nn.adapterframework.util.XmlBuilder;
import nl.nn.adapterframework.util.XmlUtils;

import org.apache.commons.lang.StringUtils;

/**
 * Encapsulates a record in XML, optionally translates it using XSLT or XPath.
 * 
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>classname</td><td>nl.nn.adapterframework.batch.RecordXmlTransformer</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setName(String) name}</td><td>name of the RecordHandler</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setInputFields(String) inputFields}</td><td>Comma separated specification of fieldlengths. Either this attribute or <code>inputSeparator</code> must be specified</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setInputSeparator(String) inputSeparator}</td><td>Separator that separated the fields in the input record. Either this attribute or <code>inputFields</code> must be specified</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setTrim(boolean) trim}</td><td>when set <code>true</code>, trailing spaces are removed from each field</td><td>false</td></tr>
 * <tr><td>{@link #setRootTag(String) rootTag}</td><td>Roottag for the generated XML document</td><td>record</td></tr>
 * <tr><td>{@link #setOutputFields(String) outputfields}</td><td>Comma separated string with tagnames for the individual input fields (related using there positions). If you leave a tagname empty, the field is not xml-ized</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setRecordIdentifyingFields(String) recordIdentifyingFields}</td><td>Comma separated list of numbers of those fields that are compared with the previous record to determine if a prefix must be written. If any of these fields is not equal in both records, the record types are assumed to be different</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setStyleSheetName(String) styleSheetName}</td><td>name of stylesheet to transform an individual record</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setXpathExpression(String) xpathExpression}</td><td>alternatively: XPath-expression to create stylesheet from</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setOutputType(String) outputType}</td><td>either 'text' or 'xml'. Only valid for xpathExpression</td><td>text</td></tr>
 * <tr><td>{@link #setOmitXmlDeclaration(boolean) omitXmlDeclaration}</td><td>force the transformer generated from the XPath-expression to omit the xml declaration</td><td>true</td></tr>
 * </table>
 * </p>
 * 
 * @author  John Dekker / Gerrit van Brakel
 * @version Id
 */
public class RecordXmlTransformer extends AbstractRecordHandler {
	public static final String version = "$RCSfile: RecordXmlTransformer.java,v $  $Revision: 1.10.2.1 $ $Date: 2008-04-03 08:09:07 $";

	private String rootTag="record";
	private String xpathExpression=null;
	private String styleSheetName;
	private String outputType="text";
	private boolean omitXmlDeclaration=true;

	private TransformerPool transformerPool; 
	private ParameterList parameterList = new ParameterList();

	private List outputFields; 

	public RecordXmlTransformer() {
		outputFields = new LinkedList();
	}

	public void configure() throws ConfigurationException {
		super.configure();
		ParameterList params = getParameterList();
		if (params!=null) {
			try {
				params.configure();
			} catch (ConfigurationException e) {
				throw new ConfigurationException("while configuring parameters",e);
			}
		}
		if (StringUtils.isNotEmpty(getStyleSheetName())||StringUtils.isNotEmpty(getXpathExpression())) {
			transformerPool = TransformerPool.configureTransformer(ClassUtils.nameOf(this)+" ["+getName()+"]", getXpathExpression(), getStyleSheetName(), getOutputType(), !isOmitXmlDeclaration(), getParameterList());
		}
	}



	public Object handleRecord(PipeLineSession session, List parsedRecord, ParameterResolutionContext prc) throws Exception {
		String xml = getXml(parsedRecord);
		if (transformerPool!=null) {
			if (log.isDebugEnabled()) {
				log.debug("Transformer ["+getName()+"] record before XSL transformation ["+xml+"]");
			}
			return transformerPool.transform(xml, prc.getValueMap(paramList));
		} else {
			return xml;
		}
	}
	
	protected String getXml(List parsedRecord) {
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

	private ParameterList getParameterList() {
		return parameterList;
	}

	public void addParameter(Parameter param) {
		log.debug("added parameter ["+param.toString()+"]");
		parameterList.add(param);
	}



	public void setRootTag(String string) {
		rootTag = string;
	}
	public String getRootTag() {
		return rootTag;
	}

	public void setXpathExpression(String string) {
		xpathExpression = string;
	}
	public String getXpathExpression() {
		return xpathExpression;
	}

	public void setStyleSheetName(String string) {
		styleSheetName = string;
	}
	public String getStyleSheetName() {
		return styleSheetName;
	}

	public void setOutputType(String string) {
		outputType = string;
	}
	public String getOutputType() {
		return outputType;
	}

	public void setOmitXmlDeclaration(boolean b) {
		omitXmlDeclaration = b;
	}
	public boolean isOmitXmlDeclaration() {
		return omitXmlDeclaration;
	}

}
