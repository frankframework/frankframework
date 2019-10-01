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

import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.SAXSource;

import org.xml.sax.XMLReader;

import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.stream.MessageOutputStream;
import nl.nn.adapterframework.stream.StreamingException;
import nl.nn.adapterframework.util.DomBuilderException;
import nl.nn.adapterframework.util.JsonXmlReader;
import nl.nn.adapterframework.util.XmlJsonWriter;
import nl.nn.adapterframework.util.XmlUtils;

/**
 * Perform an XSLT transformation with a specified stylesheet on a JSON input, yielding JSON.
 *
 * <tr><th>nested elements</th><th>description</th></tr>
 * <tr><td>{@link nl.nn.adapterframework.parameters.Parameter param}</td><td>any parameters defined on the sender will be applied to the created transformer</td></tr>
 * </table>
 * </p>
 * @author Gerrit van Brakel
 */

public class JsonXsltSender extends XsltSender {

	@Override
	public boolean canProvideOutputStream() {
		return false; // JsonParser requires an InputStream
	}

	@Override
	public boolean canStreamToTarget() {
		// TODO Json<->Xml conversion needs pre and/or post processing, that is not yet implemented for streaming
		return false;
	}

	private String jsonToXml(Message json) throws TransformerException {
		XMLReader reader=new JsonXmlReader();
		Source source=new SAXSource(reader, json.asInputSource());
		return XmlUtils.source2String(source, false);
	}

	private String xml2Json(String xml) throws TransformerException, DomBuilderException {

		Source source=XmlUtils.stringToSourceForSingleUse(xml,true);
        SAXResult result = new SAXResult();
		XmlJsonWriter xjw = new XmlJsonWriter();
		result.setHandler(xjw);
        TransformerFactory tf = XmlUtils.getTransformerFactory(0);
        Transformer transformer = tf.newTransformer();
        transformer.transform(source, result);
		return xjw.toString();

	}


//	@Override
//	protected ContentHandler createHandler(String correlationID, IPipeLineSession session, MessageOutputStream target) throws StreamingException {
//		XmlJsonWriter xjw = new XmlJsonWriter(target.asWriter());
//		MessageOutputStream prev = new MessageOutputStream(xjw,target);
//		return super.createHandler(correlationID, session, prev);
//	}


	@Override
	public MessageOutputStream provideOutputStream(String correlationID, IPipeLineSession session, MessageOutputStream target) throws StreamingException {
		throw new StreamingException("Cannot parse an json-OutputStream");
	}
//
//	@Override
//	protected void parseInputSource(InputSource source, ContentHandler handler) throws IOException, SAXException, ParserConfigurationException {
//		XMLReader reader=new JsonXmlReader();
//		reader.setContentHandler(handler);
//		reader.parse(source);
//	}

	@Override
	public Object sendMessage(String correlationID, Message message, ParameterResolutionContext prc,MessageOutputStream target) throws SenderException {
		try {
			String xml=jsonToXml(message);
			Object result = super.sendMessage(correlationID, new Message(xml), prc, target);
			result = xml2Json(result.toString());
			return result;
		} catch (DomBuilderException|TransformerException e) {
			throw new SenderException(e);
		}
	}
	
}
