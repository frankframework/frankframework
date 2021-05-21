/*
   Copyright 2019-2021 WeAreFrank!

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
package nl.nn.adapterframework.senders;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang3.StringUtils;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import lombok.Getter;
import lombok.Setter;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IForwardTarget;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.stream.JsonEventHandler;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.stream.MessageOutputStream;
import nl.nn.adapterframework.stream.StreamingException;
import nl.nn.adapterframework.stream.xml.JsonXslt3XmlHandler;
import nl.nn.adapterframework.stream.xml.JsonXslt3XmlReader;
import nl.nn.adapterframework.util.XmlJsonWriter;
import nl.nn.adapterframework.xml.IXmlDebugger;

/**
 * Perform an XSLT transformation with a specified stylesheet on a JSON input, yielding JSON, yielding JSON, XML or text.
 * JSON input is transformed into XML map, array, string, integer and boolean elements, in the namespace http://www.w3.org/2013/XSL/json.
 * The XSLT stylesheet or XPathExpression operates on these element.
 * 
 * @see  <a href="https://www.xml.com/articles/2017/02/14/why-you-should-be-using-xslt-30/">https://www.xml.com/articles/2017/02/14/why-you-should-be-using-xslt-30/</a>
 *
 * <tr><th>nested elements</th><th>description</th></tr>
 * <tr><td>{@link nl.nn.adapterframework.parameters.Parameter param}</td><td>any parameters defined on the sender will be applied to the created transformer</td></tr>
 * </table>
 * </p>
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
	public MessageOutputStream provideOutputStream(PipeLineSession session, IForwardTarget next) throws StreamingException {
		MessageOutputStream target = MessageOutputStream.getTargetStream(this, session, next);
		ContentHandler handler = createHandler(null, session, target);
		JsonEventHandler jsonEventHandler = new JsonXslt3XmlHandler(handler);
		return new MessageOutputStream(this, jsonEventHandler, target, threadLifeCycleEventListener, session);
	}

	@Override
	protected ContentHandler createHandler(Message input, PipeLineSession session, MessageOutputStream target) throws StreamingException {
		if (!isJsonResult()) {
			return super.createHandler(input, session, target);
		}
		XmlJsonWriter xjw = new XmlJsonWriter(target.asWriter());
		MessageOutputStream prev = new MessageOutputStream(this,xjw,target,threadLifeCycleEventListener,session);
		ContentHandler handler = super.createHandler(input, session, prev);
		if (getXmlDebugger()!=null) {
			handler = getXmlDebugger().inspectXml(session, "XML to be converted to JSON", handler);
		}
		return handler;
	}


	@Override
	protected XMLReader getXmlReader(PipeLineSession session, ContentHandler handler) throws ParserConfigurationException, SAXException {
		if (getXmlDebugger()!=null) {
			handler = getXmlDebugger().inspectXml(session, "JSON converted to XML", handler);
		}
		return new JsonXslt3XmlReader(handler);
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
