/*
   Copyright 2019 Integration Partners

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

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.stream.MessageOutputStream;
import nl.nn.adapterframework.stream.StreamingException;
import nl.nn.adapterframework.util.JsonXmlReader;
import nl.nn.adapterframework.util.XmlJsonWriter;

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

	@Override
	public void configure() throws ConfigurationException {
		if (StringUtils.isNotEmpty(getXpathExpression()) && StringUtils.isEmpty(getNamespaceDefs())) {
			setNamespaceDefs("j=http://www.w3.org/2013/XSL/json");
		}
		super.configure();
	}

	@Override
	public boolean canProvideOutputStream() {
		return false; // JsonParser requires inputSource
	}

	@Override
	protected ContentHandler createHandler(String correlationID, Message input, IPipeLineSession session, MessageOutputStream target) throws StreamingException {
		if (!isJsonResult()) {
			return super.createHandler(correlationID, input, session, target);
		}
		XmlJsonWriter xjw = new XmlJsonWriter(target.asWriter());
		MessageOutputStream prev = new MessageOutputStream(xjw,target,this,threadLifeCycleEventListener,correlationID);
		return super.createHandler(correlationID, input, session, prev);
	}


	@Override
	protected XMLReader getXmlReader(ContentHandler handler) throws ParserConfigurationException, SAXException {
		return new JsonXmlReader(handler);
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
