/*
   Copyright 2018 Nationale-Nederlanden, 2023 WeAreFrank!

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
package org.frankframework.ladybug;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;

import org.frankframework.configuration.Configuration;
import org.frankframework.core.INamedObject;
import org.frankframework.core.IPipe;
import org.frankframework.core.PipeLine;
import org.frankframework.pipes.MessageSendingPipe;
import org.frankframework.util.ClassLoaderUtils;
import org.frankframework.util.DomBuilderException;
import org.frankframework.util.StreamUtil;
import org.frankframework.util.StringUtil;
import org.frankframework.util.XmlUtils;
import org.frankframework.xml.PrettyPrintFilter;
import org.frankframework.xml.XmlWriter;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

/**
 * Get a description of a specified pipe. The description contains the XML
 * configuration for the pipe and optionally the XSLT files used by the pipe.
 *
 * @author Jaco de Groot (jaco@dynasol.nl)
 */
public class PipeDescriptionProvider {
	private final Map<Configuration, Map<String, PipeDescription>> pipeDescriptionCaches = new WeakHashMap<>();
	private final Map<Configuration, Document> documents = new WeakHashMap<>();
	private static final String INPUT_VALIDATOR_CHECKPOINT_NAME = "InputValidator";
	private static final String OUTPUT_VALIDATOR_CHECKPOINT_NAME = "OutputValidator";
	private static final String INPUT_WRAPPER_CHECKPOINT_NAME = "InputWrapper";
	private static final String OUTPUT_WRAPPER_CHECKPOINT_NAME = "OutputWrapper";

	/**
	 * Get a PipeDescription object for the specified pipe. The returned object
	 * is cached.
	 */
	public PipeDescription getPipeDescription(PipeLine pipeLine, IPipe pipe) {
		PipeDescription pipeDescription;
		INamedObject pipeLineOwner = pipeLine.getOwner();
		String adapterName = pipeLineOwner==null? "?": pipeLineOwner.getName();
		String pipeName = pipe.getName();
		String checkpointName = null;
		String xpathExpression = null;
		if (pipeLine.getPipe(pipeName) == null) {
			if (PipeLine.INPUT_VALIDATOR_NAME.equals(pipeName)) {
				checkpointName = INPUT_VALIDATOR_CHECKPOINT_NAME;
				xpathExpression = "//*/adapter[@name=\"" + adapterName + "\"]/pipeline/inputValidator";
			} else if (PipeLine.OUTPUT_VALIDATOR_NAME.equals(pipeName)) {
				checkpointName = OUTPUT_VALIDATOR_CHECKPOINT_NAME;
				xpathExpression = "//*/adapter[@name=\"" + adapterName + "\"]/pipeline/outputValidator";
			} else if (PipeLine.INPUT_WRAPPER_NAME.equals(pipeName)) {
				checkpointName = INPUT_WRAPPER_CHECKPOINT_NAME;
				xpathExpression = "//*/adapter[@name=\"" + adapterName + "\"]/pipeline/inputWrapper";
			} else if (PipeLine.OUTPUT_WRAPPER_NAME.equals(pipeName)) {
				checkpointName = OUTPUT_WRAPPER_CHECKPOINT_NAME;
				xpathExpression = "//*/adapter[@name=\"" + adapterName + "\"]/pipeline/outputWrapper";
			} else if (pipeName.startsWith(MessageSendingPipe.INPUT_VALIDATOR_NAME_PREFIX)
					&& pipeName.endsWith(MessageSendingPipe.INPUT_VALIDATOR_NAME_SUFFIX)) {
				checkpointName = INPUT_VALIDATOR_CHECKPOINT_NAME;
				String parentPipeName = getParentPipeName(pipeName,
						MessageSendingPipe.INPUT_VALIDATOR_NAME_PREFIX,
						MessageSendingPipe.INPUT_VALIDATOR_NAME_SUFFIX);
				xpathExpression = "//*/adapter[@name=\"" + adapterName + "\"]/pipeline/pipe[@name=\"" + parentPipeName + "\"]/inputValidator";
			} else if (pipeName.startsWith(MessageSendingPipe.OUTPUT_VALIDATOR_NAME_PREFIX)
					&& pipeName.endsWith(MessageSendingPipe.OUTPUT_VALIDATOR_NAME_SUFFIX)) {
				checkpointName = OUTPUT_VALIDATOR_CHECKPOINT_NAME;
				String parentPipeName = getParentPipeName(pipeName,
						MessageSendingPipe.OUTPUT_VALIDATOR_NAME_PREFIX,
						MessageSendingPipe.OUTPUT_VALIDATOR_NAME_SUFFIX);
				xpathExpression = "//*/adapter[@name=\"" + adapterName + "\"]/pipeline/pipe[@name=\"" + parentPipeName + "\"]/outputValidator";
			} else if (pipeName.startsWith(MessageSendingPipe.INPUT_WRAPPER_NAME_PREFIX)
					&& pipeName.endsWith(MessageSendingPipe.INPUT_WRAPPER_NAME_SUFFIX)) {
				checkpointName = INPUT_WRAPPER_CHECKPOINT_NAME;
				String parentPipeName = getParentPipeName(pipeName,
						MessageSendingPipe.INPUT_WRAPPER_NAME_PREFIX,
						MessageSendingPipe.INPUT_WRAPPER_NAME_SUFFIX);
				xpathExpression = "//*/adapter[@name=\"" + adapterName + "\"]/pipeline/pipe[@name=\"" + parentPipeName + "\"]/inputWrapper";
			} else if (pipeName.startsWith(MessageSendingPipe.OUTPUT_WRAPPER_NAME_PREFIX)
					&& pipeName.endsWith(MessageSendingPipe.OUTPUT_WRAPPER_NAME_SUFFIX)) {
				checkpointName = OUTPUT_WRAPPER_CHECKPOINT_NAME;
				String parentPipeName = getParentPipeName(pipeName,
						MessageSendingPipe.OUTPUT_WRAPPER_NAME_PREFIX,
						MessageSendingPipe.OUTPUT_WRAPPER_NAME_SUFFIX);
				xpathExpression = "//*/adapter[@name=\"" + adapterName + "\"]/pipeline/pipe[@name=\"" + parentPipeName + "\"]/outputWrapper";
			}
		} else {
			xpathExpression = "//*/adapter[@name=\"" + adapterName + "\"]/pipeline/pipe[@name=\"" + pipeName + "\"]";
		}
		synchronized(pipeDescriptionCaches) {
			// When a configuration is changed (reloaded) a new configuration
			// object will be created. The old configuration object will be
			// removed from pipeDescriptionCaches by the garbage collection as
			// this is a WeakHashMap.
			Configuration configuration = pipeLine.getConfiguration();

			Map<String, PipeDescription> pipeDescriptionCache = pipeDescriptionCaches.computeIfAbsent(configuration, k -> new HashMap<>());
			pipeDescription = pipeDescriptionCache.get(xpathExpression);
			if (pipeDescription == null) {
				pipeDescription = new PipeDescription();
				pipeDescription.setCheckpointName(getCheckpointName(pipe, checkpointName));
				if (xpathExpression == null) {
					pipeDescription.setDescription("Could not create xpath to extract pipe from configuration");
					pipeDescriptionCache.put(xpathExpression, pipeDescription);
				} else {
					Document document = documents.get(configuration);
					if (document == null) {
						try {
							String config = configuration.getLoadedConfiguration();
							document = XmlUtils.buildDomDocument(config);
							documents.put(configuration, document);
						} catch (DomBuilderException e) {
							pipeDescription = new PipeDescription();
							pipeDescription.setCheckpointName(getCheckpointName(pipe, checkpointName));
							pipeDescription.setDescription("Could not parse configuration: " + e.getMessage());
							pipeDescriptionCache.put(xpathExpression, pipeDescription);
						}
					}
					if (document != null) {
						Node node = doXPath(document, xpathExpression);
						if (node != null) {
							XmlWriter xmlWriter = new XmlWriter();
							ContentHandler handler = new PrettyPrintFilter(xmlWriter);
							try {
								String input = XmlUtils.nodeToString(node);
								XmlUtils.parseXml(input, handler);
								pipeDescription.setDescription(xmlWriter.toString());
							} catch (IOException | TransformerException | SAXException e) {
								pipeDescription.setDescription("Exception: " + e.getMessage());
							}
							addResourceNamesToPipeDescription(node, pipeDescription);
						} else {
							pipeDescription.setDescription("Pipe not found in configuration.");
						}
					}
				}
				pipeDescriptionCache.put(xpathExpression, pipeDescription);
			}
		}
		return pipeDescription;
	}

	private Node doXPath(Document document, String xpathExpression) {
		XPath xPath = XmlUtils.getXPathFactory().newXPath();
		try {
			XPathExpression xPathExpression = xPath.compile(xpathExpression);
			return (Node)xPathExpression.evaluate(document, XPathConstants.NODE);
		} catch (XPathExpressionException e) {
			return null;
		}
	}

	//Protected for tests
	protected void addResourceNamesToPipeDescription(Node element, PipeDescription pipeDescription) {
		NamedNodeMap attributes = element.getAttributes();
		for (int i = 0, size = attributes.getLength(); i < size; i++) {
			Attr attribute = (Attr) attributes.item(i);
			if ("styleSheetName".equals(attribute.getName())
					|| "serviceSelectionStylesheetFilename".equals(attribute.getName())
					|| "schema".equals(attribute.getName())
					|| "wsdl".equals(attribute.getName())
					|| "fileName".equals(attribute.getName())
					|| "filename".equals(attribute.getName())
					|| "schemaLocation".equals(attribute.getName())) {
				if ("schemaLocation".equals(attribute.getName())) {
					int index = 0;
					for(String resourceName : StringUtil.split(attribute.getValue(), ", \t\r\n\f")) {
						if(index++ % 2 == 1 && pipeDescription.doesNotContainResourceName(resourceName)) {
							pipeDescription.addResourceName(resourceName);
						}
					}
				} else {
					String resourceName = attribute.getValue();
					if (pipeDescription.doesNotContainResourceName(resourceName)) {
						pipeDescription.addResourceName(resourceName);
					}
				}
			}
		}
		NodeList childNodes = element.getChildNodes();
		for (int i = 0, size = childNodes.getLength(); i < size; i++) {
			Node node = childNodes.item(i);
			if (node instanceof Element && "sender".equals(node.getNodeName())) {
				addResourceNamesToPipeDescription(node, pipeDescription);
			}
		}
	}

	/**
	 * Return the content of the specified resource.
	 */
	public String getResource(PipeLine pipeLine, String resourceName) {
		String resource;
		try {
			URL resourceUrl = ClassLoaderUtils.getResourceURL(pipeLine, resourceName);
			if(resourceUrl != null)
				resource = StreamUtil.resourceToString(resourceUrl, "\n", false);
			else
				resource = "File not found: " + resourceName;
		} catch(IOException e) {
			resource = "IOException: " + e.getMessage();
		}
		return resource;
	}

	private String getCheckpointName(IPipe pipe, String checkpointName) {
		if (checkpointName == null) {
			checkpointName = "Pipe " + pipe.getName();
		}
		return checkpointName;
	}

	private String getParentPipeName(String pipeName, String prefix, String suffix) {
		return pipeName.substring(prefix.length(), pipeName.length() - suffix.length());
	}
}
