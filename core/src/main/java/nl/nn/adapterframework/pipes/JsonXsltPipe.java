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

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.stream.MessageOutputStream;
import nl.nn.adapterframework.util.JsonXmlReader;
import nl.nn.adapterframework.util.XmlJsonWriter;
import nl.nn.adapterframework.util.XmlUtils;

/**
 * Perform an XSLT transformation with a specified stylesheet on a JSON input, yielding JSON.
 *
 * <tr><th>nested elements</th><th>description</th></tr>
 * <tr><td>{@link nl.nn.adapterframework.parameters.Parameter param}</td><td>any parameters defined on the pipe will be applied to the created transformer</td></tr>
 * </table>
 * </p>
 * <p><b>Exits:</b>
 * <table border="1">
 * <tr><th>state</th><th>condition</th></tr>
 * <tr><td>"success"</td><td>default</td></tr>
 * <tr><td><i>{@link #setForwardName(String) forwardName}</i></td><td>if specified</td></tr>
 * </table>
 * </p>
 * @author Gerrit van Brakel
 */

public class JsonXsltPipe extends XsltPipe {

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

		Source source=XmlUtils.stringToSourceForSingleUse(xml,true);
        SAXResult result = new SAXResult();
		XmlJsonWriter xjw = new XmlJsonWriter();
		result.setHandler(xjw);
        TransformerFactory tf = XmlUtils.getTransformerFactory(0);
        Transformer transformer = tf.newTransformer();
        transformer.transform(source, result);
		return xjw.toString();

	}

//	private Node jsonToDom(String json) throws TransformerException, DomBuilderException {
//		XMLReader reader=new JsonXmlReader();
//		Source source=new SAXSource(reader, new InputSource(new StringReader(json)));
//        DOMResult result = new DOMResult();
//        TransformerFactory tf = XmlUtils.getTransformerFactory(true);
//        Transformer transformer = tf.newTransformer();
//        transformer.transform(source, result);
//        return result.getNode();
//	}

	@Override
	protected String getInputXml(Object input, IPipeLineSession session) throws TransformerException {
		//TODO: GvB: use SAXSource for primary transformation, instead of first converting to XML String. However, there appears to be a problem with that currently.
		return jsonToXml(super.getInputXml(input, session));
//		return super.getInput(xml, session);

//		Node node = jsonToDom(input);
////		System.out.println("node: "+ToStringBuilder.reflectionToString(node));
//		Source source=new DOMSource(node);
//		return new ParameterResolutionContext(source, session, isNamespaceAware(), isXslt2());
		
//		XMLReader reader=new JsonXmlReader();
//		Source source=new SAXSource(reader, new InputSource(new StringReader(input)));
//		return new ParameterResolutionContext(source, session, isNamespaceAware(), isXslt2());
	}
	
	@Override
	protected String transform(Object input, IPipeLineSession session, MessageOutputStream target) throws SenderException, TransformerException, TimeOutException {
		String xmlResult=super.transform(input, session, target);
		try {
			return xml2Json(xmlResult);
		} catch (SAXException e) {
			throw new TransformerException(e);
		}
	}
//	protected String transform(TransformerPool tp, Source source, Map parametervalues) throws TransformerException, IOException {
//		SAXResult result = new SAXResult();
//		XmlJsonWriter xjw = new XmlJsonWriter();
//		result.setHandler(xjw);
//		tp.transform(source, result, parametervalues);
//		return xjw.toString();
//	}

	
}
