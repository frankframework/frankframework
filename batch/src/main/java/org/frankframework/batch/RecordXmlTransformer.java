/*
   Copyright 2013, 2016 Nationale-Nederlanden, 2020-2023 WeAreFrank!

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
package org.frankframework.batch;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import lombok.Getter;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.parameters.ParameterList;
import org.frankframework.parameters.ParameterValueList;
import org.frankframework.stream.Message;
import org.frankframework.util.StringUtil;
import org.frankframework.util.TransformerPool;
import org.frankframework.util.TransformerPool.OutputType;
import org.frankframework.util.XmlBuilder;

/**
 * Encapsulates a record in XML, optionally translates it using XSLT or XPath.
 *
 * @author  John Dekker / Gerrit van Brakel
 * @deprecated Warning: non-maintained functionality.
 */
public class RecordXmlTransformer extends AbstractRecordHandler {

	private @Getter String rootTag="record";
	private @Getter String xpathExpression=null;
	private @Getter String namespaceDefs = null;
	private @Getter String styleSheetName;
	private @Getter OutputType outputType=OutputType.TEXT;
	private @Getter boolean omitXmlDeclaration=true;
	private @Getter String endOfRecord;

	private TransformerPool transformerPool;

	private final List<String> outputFields;

	public RecordXmlTransformer() {
		outputFields = new LinkedList<>();
	}

	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		ParameterList params = getParameterList();
		try {
			params.configure();
		} catch (ConfigurationException e) {
			throw new ConfigurationException("while configuring parameters", e);
		}
		if (StringUtils.isNotEmpty(getStyleSheetName())||StringUtils.isNotEmpty(getXpathExpression())) {
			transformerPool = TransformerPool.configureTransformer(this, getNamespaceDefs(), getXpathExpression(), getStyleSheetName(), getOutputType(), !isOmitXmlDeclaration(), getParameterList());
		}
	}


	@Override
	public String handleRecord(PipeLineSession session, List<String> parsedRecord) throws Exception {
		XmlBuilder xml = getXml(parsedRecord);
		if (transformerPool != null) {
			if (log.isDebugEnabled()) {
				log.debug("Transformer [{}] record before XSL transformation [{}]", getName(), xml);
			}
			Message message = xml.asMessage();
			ParameterValueList pvl = paramList.getValues(message, session);
			try (Message transformedMessage = transformerPool.transform(message, pvl)) {
				return transformedMessage.asString();
			}
		}
		return xml.asXmlString();
	}

	protected XmlBuilder getXml(List<String> parsedRecord) {
		XmlBuilder record = new XmlBuilder(getRootTag());
		int ndx = 0;
		for (Iterator<String> it = outputFields.iterator(); it.hasNext(); ) {
			// get tagname
			String tagName = it.next();
			// get value
			String value = "";
			if (ndx < parsedRecord.size()) {
				value = parsedRecord.get(ndx++);
				if (!it.hasNext() && !StringUtils.isEmpty(endOfRecord)) {
					if (value.endsWith(endOfRecord)) {
						int ei = value.length() - endOfRecord.length();
						value = value.substring(0, ei);
					}
				}
			}
			// if tagname is empty, then it is not added to the XML
			if (!StringUtils.isEmpty(tagName)) {
				XmlBuilder field = new XmlBuilder(tagName);
				field.setValue(value,true);
				record.addSubElement(field);
			}
		}
		return record;
	}

	/** comma separated string with tagnames for the individual input fields (related using there positions). if you leave a tagname empty, the field is not xml-ized */
	public void setOutputFields(String fieldLengths) {
		outputFields.addAll(StringUtil.split(fieldLengths));
	}

	/**
	 * Root tag for the generated xml document that will be send to the Sender
	 * @ff.default record
	 */
	public void setRootTag(String string) {
		rootTag = string;
	}

	/** Name of stylesheet to transform an individual record */
	public void setStyleSheetName(String string) {
		styleSheetName = string;
	}

	/** Alternatively: xpath-expression to create stylesheet from */
	public void setXpathExpression(String string) {
		xpathExpression = string;
	}

	/** Namespace defintions for xpathExpression. Must be in the form of a comma or space separated list of <code>prefix=namespaceuri</code>-definitions. One entry can be without a prefix, that will define the default namespace. */
	public void setNamespaceDefs(String namespaceDefs) {
		this.namespaceDefs = namespaceDefs;
	}

	/**
	 * Only valid for <code>xpathExpression</code>
	 * @ff.default text
	 */
	public void setOutputType(OutputType outputType) {
		this.outputType = outputType;
	}

	/**
	 * Force the transformer generated from the xpath-expression to omit the xml declaration
	 * @ff.default true
	 */
	public void setOmitXmlDeclaration(boolean b) {
		omitXmlDeclaration = b;
	}

	/** String which ends the record and must be ignored */
	public void setEndOfRecord(String string) {
		endOfRecord = string;
	}
}
