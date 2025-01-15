/*
   Copyright 2019-2025 WeAreFrank!

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
package org.frankframework.senders;

import javax.xml.transform.TransformerException;

import org.apache.commons.lang3.StringUtils;
import org.xml.sax.ContentHandler;
import org.xml.sax.XMLReader;

import lombok.Getter;
import lombok.Setter;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.documentbuilder.xml.JsonXslt3XmlReader;
import org.frankframework.stream.Message;
import org.frankframework.stream.MessageBuilder;
import org.frankframework.threading.ThreadConnector;
import org.frankframework.util.TransformerPool;
import org.frankframework.util.XmlJsonWriter;
import org.frankframework.xml.IXmlDebugger;

/**
 * Perform an XSLT transformation with a specified stylesheet on a JSON input, yielding JSON, yielding JSON, XML or text.
 * JSON input is transformed into XML map, array, string, integer and boolean elements, in the namespace http://www.w3.org/2013/XSL/json.
 * The XSLT stylesheet or XPathExpression operates on these element.
 *
 * @see  <a href="https://www.xml.com/articles/2017/02/14/why-you-should-be-using-xslt-30/">https://www.xml.com/articles/2017/02/14/why-you-should-be-using-xslt-30/</a>
 *
 * @author Gerrit van Brakel
 */
public class JsonXsltSender extends XsltSender {

	private boolean jsonResult=true;
	private @Getter @Setter IXmlDebugger xmlDebugger;

	@Override
	public void configure() throws ConfigurationException {
		if (StringUtils.isNotEmpty(getXpathExpression()) && StringUtils.isEmpty(getNamespaceDefs())) {
			setNamespaceDefs("j=http://www.w3.org/2013/XSL/json");
		}
		super.configure();
	}

	@Override
	protected ContentHandler createHandler(Message input, ThreadConnector<?> threadConnector, PipeLineSession session, TransformerPool poolToUse, ContentHandler handler, MessageBuilder messageBuilder) throws TransformerException {
		if (!isJsonResult()) {
			return super.createHandler(input, threadConnector, session, poolToUse, handler, messageBuilder);
		}

		XmlJsonWriter xjw = new XmlJsonWriter(messageBuilder.asWriter());
		handler = super.createHandler(input, threadConnector, session, poolToUse, xjw, messageBuilder);
		if (getXmlDebugger() != null) {
			handler = getXmlDebugger().inspectXml(session, "output XML to be converted to JSON", handler);
		}
		return handler;
	}

	@Override
	protected XMLReader getXmlReader(PipeLineSession session, ContentHandler handler) {
		if (getXmlDebugger()!=null) {
			handler = getXmlDebugger().inspectXml(session, "input JSON converted to XML", handler);
		}
		return new JsonXslt3XmlReader(handler);
	}

	/**
	 * When <code>true</code>, the xml result of the transformation is converted back to json
	 * @ff.default true
	 */
	public void setJsonResult(boolean jsonResult) {
		this.jsonResult = jsonResult;
	}
	public boolean isJsonResult() {
		return jsonResult;
	}

	/**
	 * Namespace definitions for xpathExpression. Must be in the form of a comma or space separated list of <code>prefix=namespaceuri</code> definitions
	 * @ff.default j=http://www.w3.org/2013/XSL/json
	 */
	@Override
	public void setNamespaceDefs(String namespaceDefs) {
		super.setNamespaceDefs(namespaceDefs);
	}
}
