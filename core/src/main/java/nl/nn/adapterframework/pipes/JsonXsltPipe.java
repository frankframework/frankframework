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
import java.io.StringWriter;

import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;

import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.util.DomBuilderException;
import nl.nn.adapterframework.util.JsonXmlReader;
import nl.nn.adapterframework.util.XmlJsonWriter;
import nl.nn.adapterframework.util.XmlUtils;

/**
 * Perform an XSLT transformation with a specified stylesheet on a JSON input, yielding JSON.
 *
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>className</td><td>nl.nn.adapterframework.pipes.XsltPipe</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setName(String) name}</td><td>name of the Pipe</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setMaxThreads(int) maxThreads}</td><td>maximum number of threads that may call {@link #doPipe(java.lang.Object, nl.nn.adapterframework.core.IPipeLineSession)} simultaneously</td><td>0 (unlimited)</td></tr>
 * <tr><td>{@link #setDurationThreshold(long) durationThreshold}</td><td>if durationThreshold >=0 and the duration (in milliseconds) of the message processing exceeded the value specified the message is logged informatory</td><td>-1</td></tr>
 * <tr><td>{@link #setGetInputFromSessionKey(String) getInputFromSessionKey}</td><td>when set, input is taken from this session key, instead of regular input</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setStoreResultInSessionKey(String) storeResultInSessionKey}</td><td>when set, the result is stored under this session key</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setNamespaceAware(boolean) namespaceAware}</td><td>controls namespace-awareness of transformation</td><td>application default</td></tr>
 * <tr><td>{@link #setStyleSheetName(String) styleSheetName}</td><td>stylesheet to apply to the input message</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setXpathExpression(String) xpathExpression}</td><td>XPath-expression to apply to the input message. It's possible to refer to a parameter (which e.g. contains a value from a sessionKey) by using the parameter name prefixed with $</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setNamespaceDefs(String) namespaceDefs}</td><td>namespace defintions for xpathExpression. Must be in the form of a comma or space separated list of <code>prefix=namespaceuri</code>-definitions</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setOutputType(String) outputType}</td><td>either 'text' or 'xml'. Only valid for xpathExpression</td><td>text</td></tr>
 * <tr><td>{@link #setOmitXmlDeclaration(boolean) omitXmlDeclaration}</td><td>force the transformer generated from the XPath-expression to omit the xml declaration</td><td>true</td></tr>
 * <tr><td>{@link #setSessionKey(String) sessionKey}</td><td>If specified, the result is put 
 * in the PipeLineSession under the specified key, and the result of this pipe will be 
 * the same as the input (the xml). If NOT specified, the result of the xpath expression 
 * will be the result of this pipe</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setSkipEmptyTags(boolean) skipEmptyTags}</td><td>when set <code>true</code> empty tags in the output are removed</td><td>false</td></tr>
 * <tr><td>{@link #setIndentXml(boolean) indentXml}</td><td>when set <code>true</code>, result is pretty-printed. (only used when <code>skipEmptyTags="true"</code>)</td><td>true</td></tr>
 * <tr><td>{@link #setRemoveNamespaces(boolean) removeNamespaces}</td><td>when set <code>true</code> namespaces (and prefixes) in the input message are removed</td><td>false</td></tr>
 * <tr><td>{@link #setXslt2(boolean) xslt2}</td><td>when set <code>true</code> XSLT processor 2.0 (net.sf.saxon) will be used, otherwise XSLT processor 1.0 (org.apache.xalan)</td><td>true</td></tr>
 * </table>
 * <table border="1">
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

	{
		setXslt2(true);
	}

	private String jsonToXml(String json) throws TransformerException {
		XMLReader reader=new JsonXmlReader();
		Source source=new SAXSource(reader, new InputSource(new StringReader(json)));
		StringWriter writer = new StringWriter();
        StreamResult result = new StreamResult(writer);
        TransformerFactory tf = XmlUtils.getTransformerFactory(false);
        Transformer transformer = tf.newTransformer();
        transformer.transform(source, result);
        return writer.toString();
	}

	private String xml2Json(String xml) throws TransformerException, DomBuilderException {

		Source source=XmlUtils.stringToSourceForSingleUse(xml);
        SAXResult result = new SAXResult();
		XmlJsonWriter xjw = new XmlJsonWriter();
		result.setHandler(xjw);
        TransformerFactory tf = XmlUtils.getTransformerFactory(false);
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
	protected String transform(Object input, IPipeLineSession session) throws SenderException, TransformerException {
		String xmlResult=super.transform(input, session);
		try {
			return xml2Json(xmlResult);
		} catch (DomBuilderException e) {
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
