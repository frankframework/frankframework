/*
   Copyright 2013 Nationale-Nederlanden

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package nl.nn.adapterframework.batch;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.ParameterList;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.TransformerPool;
import nl.nn.adapterframework.util.XmlBuilder;

import org.apache.commons.lang.StringUtils;

/**
 * Encapsulates a record in XML, optionally translates it using XSLT or XPath.
 * 
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>classname</td><td>nl.nn.adapterframework.batch.RecordXmlTransformer</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setName(String) name}</td><td>name of the RecordHandler</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setInputFields(String) inputFields}</td><td>Comma separated specification of fieldlengths. If neither this attribute nor <code>inputSeparator</code> is specified then the entire record is parsed</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setInputSeparator(String) inputSeparator}</td><td>Separator that separated the fields in the input record. If neither this attribute nor <code>inputFields</code> is specified then the entire record is parsed</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setTrim(boolean) trim}</td><td>when set <code>true</code>, trailing spaces are removed from each field</td><td>false</td></tr>
 * <tr><td>{@link #setRootTag(String) rootTag}</td><td>Roottag for the generated XML document</td><td>record</td></tr>
 * <tr><td>{@link #setOutputFields(String) outputfields}</td><td>Comma separated string with tagnames for the individual input fields (related using there positions). If you leave a tagname empty, the field is not xml-ized</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setRecordIdentifyingFields(String) recordIdentifyingFields}</td><td>Comma separated list of numbers of those fields that are compared with the previous record to determine if a prefix must be written. If any of these fields is not equal in both records, the record types are assumed to be different</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setStyleSheetName(String) styleSheetName}</td><td>name of stylesheet to transform an individual record</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setXpathExpression(String) xpathExpression}</td><td>alternatively: XPath-expression to create stylesheet from</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setNamespaceDefs(String) namespaceDefs}</td><td>namespace defintions for xpathExpression. Must be in the form of a comma or space separated list of <code>prefix=namespaceuri</code>-definitions</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setOutputType(String) outputType}</td><td>either 'text' or 'xml'. Only valid for xpathExpression</td><td>text</td></tr>
 * <tr><td>{@link #setOmitXmlDeclaration(boolean) omitXmlDeclaration}</td><td>force the transformer generated from the XPath-expression to omit the xml declaration</td><td>true</td></tr>
 * <tr><td>{@link #setEndOfRecord(String) endOfRecord}</td><td>string which ends the record and must be ignored</td><td>&nbsp;</td></tr>
 * </table>
 * </p>
 * 
 * @author  John Dekker / Gerrit van Brakel
 * @version $Id$
 */
public class RecordXmlTransformer extends AbstractRecordHandler {
	public static final String version = "$RCSfile: RecordXmlTransformer.java,v $  $Revision: 1.19 $ $Date: 2012-06-01 10:52:48 $";

	private String rootTag="record";
	private String xpathExpression=null;
	private String namespaceDefs = null; 
	private String styleSheetName;
	private String outputType="text";
	private boolean omitXmlDeclaration=true;
	private String endOfRecord;

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
			transformerPool = TransformerPool.configureTransformer(ClassUtils.nameOf(this)+" ["+getName()+"] ", getNamespaceDefs(), getXpathExpression(), getStyleSheetName(), getOutputType(), !isOmitXmlDeclaration(), getParameterList());
		}
	}



	public Object handleRecord(IPipeLineSession session, List parsedRecord, ParameterResolutionContext prc) throws Exception {
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
				value = (String)parsedRecord.get(ndx++);
				if (!it.hasNext() && !StringUtils.isEmpty(endOfRecord)) {
					if (value.endsWith(endOfRecord)) {
						int ei = value.length() - endOfRecord.length();
						value = value.substring(0, ei);
					}
				}
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

	public void setNamespaceDefs(String namespaceDefs) {
		this.namespaceDefs = namespaceDefs;
	}
	public String getNamespaceDefs() {
		return namespaceDefs;
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

	public void setEndOfRecord(String string) {
		endOfRecord = string;
	}
	public String getEndOfRecord() {
		return endOfRecord;
	}
}
