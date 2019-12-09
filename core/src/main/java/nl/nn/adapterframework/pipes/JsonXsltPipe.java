/*
   Copyright 2013, 2017 Nationale-Nederlanden

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
package nl.nn.adapterframework.pipes;

import java.io.StringReader;

import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.SAXSource;

import org.apache.commons.lang3.StringUtils;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.stream.MessageOutputStream;
import nl.nn.adapterframework.util.JsonXmlReader;
import nl.nn.adapterframework.util.XmlJsonWriter;
import nl.nn.adapterframework.util.XmlUtils;

/**
 * Perform an XSLT transformation with a specified stylesheet on a JSON input, yielding JSON, XML or text.
 * JSON input is transformed into map, array, string, integer and boolean elements, in the namespace http://www.w3.org/2013/XSL/json.
 *
 * <tr><th>nested elements</th><th>description</th></tr>
 * <tr><td>{@link nl.nn.adapterframework.parameters.Parameter param}</td><td>any parameters defined on the pipe will be applied to the created transformer</td></tr>
 * </table>
 * </p>
 * @author Gerrit van Brakel
 */

public class JsonXsltPipe extends XsltPipe {
	
	private boolean jsonResult=true;

	@Override
	public void configure() throws ConfigurationException {
		if (StringUtils.isNotEmpty(getXpathExpression()) && StringUtils.isEmpty(getNamespaceDefs())) {
			setNamespaceDefs("j=http://www.w3.org/2013/XSL/json");
		}
		super.configure();
	}


	@Override
	public boolean canProvideOutputStream() {
		// TODO Json<->Xml conversion needs pre and/or post processing, that is not yet implemented for streaming
		return false;
	}

	@Override
	public boolean canStreamToTarget() {
		// TODO Json<->Xml conversion needs pre and/or post processing, that is not yet implemented for streaming
		return false;
	}

	private String jsonToXml(String json) throws TransformerException {
		XMLReader reader=new JsonXmlReader();
		Source source=new SAXSource(reader, new InputSource(new StringReader(json)));
		return XmlUtils.source2String(source, false);
	}

	private String xml2Json(String xml) throws TransformerException, SAXException {
		Source source = XmlUtils.stringToSourceForSingleUse(xml, true);
		SAXResult result = new SAXResult();
		XmlJsonWriter xjw = new XmlJsonWriter();
		result.setHandler(xjw);
		TransformerFactory tf = XmlUtils.getTransformerFactory(0);
		Transformer transformer = tf.newTransformer();
		transformer.transform(source, result);
		return xjw.toString();
	}


	@Override
	protected String getInputXml(Object input, IPipeLineSession session) throws TransformerException {
		//TODO: GvB: use SAXSource for primary transformation, instead of first converting to XML String. However, there appears to be a problem with that currently.
		String json=super.getInputXml(input, session);
		//if (log.isDebugEnabled()) log.debug("json ["+json+"]");
		String xml=jsonToXml(json);
		//if (log.isDebugEnabled()) log.debug("xml ["+xml+"]");
		return xml;
	}
	
	@Override
	protected String transform(Object input, IPipeLineSession session, MessageOutputStream target) throws SenderException, TransformerException, TimeOutException {
		String xmlResult=super.transform(input, session, target);
		if (!isJsonResult()) {
			return xmlResult;
		}
		try {
			//if (log.isDebugEnabled()) log.debug("xml result ["+xmlResult+"]");
			return xml2Json(xmlResult);
		} catch (SAXException e) {
			throw new TransformerException(e);
		}
	}

	@IbisDoc({"1", "When <code>true</code>, the xml result of the transformation is converted back to json", "true"})
	public void setJsonResult(boolean jsonResult) {
		this.jsonResult = jsonResult;
	}
	public boolean isJsonResult() {
		return jsonResult;
	}

	@Override
	@IbisDoc({"2", "Namespace defintions for xpathExpression. Must be in the form of a comma or space separated list of <code>prefix=namespaceuri</code>-definitions", "j=http://www.w3.org/2013/XSL/json"})
	public void setNamespaceDefs(String namespaceDefs) {
		super.setNamespaceDefs(namespaceDefs);
	}

}
