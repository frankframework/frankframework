/*
   Copyright 2019-2022 WeAreFrank!

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

import java.util.function.BiConsumer;

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
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.stream.JsonEventHandler;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.stream.MessageOutputStream;
import nl.nn.adapterframework.stream.StreamingException;
import nl.nn.adapterframework.stream.ThreadConnector;
import nl.nn.adapterframework.stream.xml.JsonXslt3XmlHandler;
import nl.nn.adapterframework.stream.xml.JsonXslt3XmlReader;
import nl.nn.adapterframework.util.TransformerPool;
import nl.nn.adapterframework.util.XmlJsonWriter;
import nl.nn.adapterframework.xml.IXmlDebugger;

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
	public MessageOutputStream provideOutputStream(PipeLineSession session, IForwardTarget next) throws StreamingException {
		if (!canProvideOutputStream()) {
			log.debug("sender [{}] cannot provide outputstream", () -> getName());
			return null;
		}
		ThreadConnector threadConnector = getStreamingXslt() ? new ThreadConnector(this, "provideOutputStream", threadLifeCycleEventListener, txManager, session) : null;
		MessageOutputStream target = MessageOutputStream.getTargetStream(this, session, next);
		try {
			TransformerPool poolToUse = getTransformerPoolToUse(session);
			ContentHandler handler = createHandler(null, threadConnector, session, poolToUse, target);
			JsonEventHandler jsonEventHandler = new JsonXslt3XmlHandler(handler);
			return new MessageOutputStream(this, jsonEventHandler, target, threadLifeCycleEventListener, txManager, session, threadConnector);
		} catch (SenderException | ConfigurationException e) {
			throw new StreamingException(e);
		}
	}

	@Override
	protected ContentHandler createHandler(Message input, ThreadConnector threadConnector, PipeLineSession session, TransformerPool poolToUse, MessageOutputStream target) throws StreamingException {
		if (!isJsonResult()) {
			return super.createHandler(input, threadConnector, session, poolToUse, target);
		}
		XmlJsonWriter xjw = new XmlJsonWriter(target.asWriter());
		MessageOutputStream prev = new MessageOutputStream(this,xjw,target,threadLifeCycleEventListener, txManager, session, null);
		ContentHandler handler = super.createHandler(input, threadConnector, session, poolToUse, prev);
		if (getXmlDebugger()!=null) {
			handler = getXmlDebugger().inspectXml(session, "output XML to be converted to JSON", handler, (resource,label)->target.closeOnClose(resource));
		}
		return handler;
	}


	@Override
	protected XMLReader getXmlReader(PipeLineSession session, ContentHandler handler, BiConsumer<AutoCloseable,String> closeOnCloseRegister) throws ParserConfigurationException, SAXException {
		if (getXmlDebugger()!=null) {
			handler = getXmlDebugger().inspectXml(session, "input JSON converted to XML", handler, closeOnCloseRegister);
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

	@Override
	/**
	 * Namespace defintions for xpathExpression. Must be in the form of a comma or space separated list of <code>prefix=namespaceuri</code>-definitions
	 * @ff.default j=http://www.w3.org/2013/XSL/json
	 */
	public void setNamespaceDefs(String namespaceDefs) {
		super.setNamespaceDefs(namespaceDefs);
	}


}
