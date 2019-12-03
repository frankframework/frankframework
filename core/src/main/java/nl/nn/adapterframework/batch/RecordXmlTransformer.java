/*
   Copyright 2013, 2016 Nationale-Nederlanden

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

import nl.nn.adapterframework.doc.IbisDoc;
import org.apache.commons.lang.StringUtils;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.ParameterList;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.TransformerPool;
import nl.nn.adapterframework.util.XmlBuilder;

/**
 * Encapsulates a record in XML, optionally translates it using XSLT or XPath.
 * 
 * 
 * @author  John Dekker / Gerrit van Brakel
 */
public class RecordXmlTransformer extends AbstractRecordHandler {
	private ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

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

	@Override
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
			transformerPool = TransformerPool.configureTransformer(ClassUtils.nameOf(this)+" ["+getName()+"] ", classLoader, getNamespaceDefs(), getXpathExpression(), getStyleSheetName(), getOutputType(), !isOmitXmlDeclaration(), getParameterList());
		}
	}



	@Override
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

	@IbisDoc({"comma separated string with tagnames for the individual input fields (related using there positions). if you leave a tagname empty, the field is not xml-ized", ""})
	public void setOutputFields(String fieldLengths) {
		StringTokenizer st = new StringTokenizer(fieldLengths, ",");
		while (st.hasMoreTokens()) {
			String token = st.nextToken().trim();
			outputFields.add(token);
		}
	}


	@IbisDoc({"roottag for the generated xml document that will be send to the Sender", "record"})
	public void setRootTag(String string) {
		rootTag = string;
	}
	public String getRootTag() {
		return rootTag;
	}

	@IbisDoc({"alternatively: xpath-expression to create stylesheet from", ""})
	public void setXpathExpression(String string) {
		xpathExpression = string;
	}
	public String getXpathExpression() {
		return xpathExpression;
	}

	@IbisDoc({"namespace defintions for xpathexpression. must be in the form of a comma or space separated list of <code>prefix=namespaceuri</code>-definitions. One entry can be without a prefix, that will define the default namespace.", ""})
	public void setNamespaceDefs(String namespaceDefs) {
		this.namespaceDefs = namespaceDefs;
	}
	public String getNamespaceDefs() {
		return namespaceDefs;
	}

	@IbisDoc({"name of stylesheet to transform an individual record", ""})
	public void setStyleSheetName(String string) {
		styleSheetName = string;
	}
	public String getStyleSheetName() {
		return styleSheetName;
	}

	@IbisDoc({"either 'text' or 'xml'. only valid for xpathexpression", "text"})
	public void setOutputType(String string) {
		outputType = string;
	}
	public String getOutputType() {
		return outputType;
	}

	@IbisDoc({"force the transformer generated from the xpath-expression to omit the xml declaration", "true"})
	public void setOmitXmlDeclaration(boolean b) {
		omitXmlDeclaration = b;
	}
	public boolean isOmitXmlDeclaration() {
		return omitXmlDeclaration;
	}

	@IbisDoc({"string which ends the record and must be ignored", ""})
	public void setEndOfRecord(String string) {
		endOfRecord = string;
	}
	public String getEndOfRecord() {
		return endOfRecord;
	}
}
