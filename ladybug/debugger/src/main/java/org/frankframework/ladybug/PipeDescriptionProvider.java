/*
   Copyright 2018 Nationale-Nederlanden, 2023-2026 WeAreFrank!

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

import org.springframework.beans.BeansException;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import org.frankframework.components.FrankPlugin;
import org.frankframework.configuration.Configuration;
import org.frankframework.configuration.digester.ConfigurationDigester;
import org.frankframework.core.HasName;
import org.frankframework.core.PipeLine;
import org.frankframework.pipes.MessageSendingPipe;
import org.frankframework.util.ClassLoaderUtils;
import org.frankframework.util.DomBuilderException;
import org.frankframework.util.StreamUtil;
import org.frankframework.util.StringUtil;
import org.frankframework.util.XmlUtils;
import org.frankframework.xml.PrettyPrintFilter;
import org.frankframework.xml.XmlWriter;

/**
 * Get a description of a specified pipe. The description contains the XML
 * configuration for the pipe and optionally the XSLT files used by the pipe.
 *
 * @author Jaco de Groot
 * @author Niels Meijer
 */
public class PipeDescriptionProvider {
	private final Map<Integer, Map<String, PipeDescription>> pipeDescriptionCaches = new WeakHashMap<>();
	private final Map<Integer, Document> loadedConfigurations = new WeakHashMap<>();

	private static final String INPUT_VALIDATOR_CHECKPOINT_NAME = "InputValidator";
	private static final String OUTPUT_VALIDATOR_CHECKPOINT_NAME = "OutputValidator";
	private static final String INPUT_WRAPPER_CHECKPOINT_NAME = "InputWrapper";
	private static final String OUTPUT_WRAPPER_CHECKPOINT_NAME = "OutputWrapper";

	private static record PipeInfo(String checkpointName, String xpathExpression) {

		public String getXPath(PipeLine pipeLine) {
			if (xpathExpression == null) {
				return null;
			}

			if (pipeLine instanceof FrankPlugin) {
				// Single PipelinePart in a FrankPlugin
				return "//*" + xpathExpression;
			} else {
				// Entire Configuration with potentially multiple adapters
				return "//*/adapter[@name=\"" + pipeLine.getAdapter().getName() + "\"]/pipeline" + xpathExpression;
			}
		}
	}

	/**
	 * Get a PipeDescription object for the specified pipe. The returned object
	 * is cached.
	 */
	public PipeDescription getPipeDescription(PipeLine pipeLine, HasName pipe) {
		PipeInfo pipeInfo = getInfo(pipeLine, pipe.getName());
		String checkpointName = pipeInfo.checkpointName;
		String xpathExpression = pipeInfo.getXPath(pipeLine);

		synchronized(pipeDescriptionCaches) {
			// When a configuration is changed (reloaded) a new configuration
			// object will be created. The old configuration object will be
			// removed from pipeDescriptionCaches by the garbage collection as
			// this is a WeakHashMap.

			Map<String, PipeDescription> pipeDescriptionCache = pipeDescriptionCaches.computeIfAbsent(System.identityHashCode(pipeLine), k -> new HashMap<>());

			return pipeDescriptionCache.computeIfAbsent(xpathExpression, xpath -> {
				PipeDescription pipeDescription = new PipeDescription();
				pipeDescription.setCheckpointName(checkpointName);

				if (xpath == null) {
					pipeDescription.setDescription("Could not create xpath to extract pipe from configuration");
				} else {
					try {
						Document document = getLoadedConfiguration(pipeLine);
						Node node = doXPath(document, xpath);

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
					} catch (IllegalStateException e) {
						pipeDescription.setDescription("Could not parse configuration: " + e.getMessage());
					}
				}
				return pipeDescription;
			});
		}
	}

	/**
	 * Get the configuration XML or in case of a plugin, the plugin XML.
	 */
	private Document getLoadedConfiguration(PipeLine pipeline) {
		final Integer uniqueIdentifier;
		if (pipeline instanceof FrankPlugin) {
			uniqueIdentifier = System.identityHashCode(pipeline);
		} else {
			Configuration configuration = pipeline.getAdapter().getConfiguration();
			uniqueIdentifier = System.identityHashCode(configuration);
		}

		return loadedConfigurations.computeIfAbsent(uniqueIdentifier, key -> {
			try {
				String config = pipeline.getBean(ConfigurationDigester.class).getLoadedConfiguration();
				return XmlUtils.buildDomDocument(config);
			} catch (BeansException | DomBuilderException e) {
				throw new IllegalStateException("Could not parse configuration: " + e.getMessage());
			}
		});
	}

	private PipeInfo getInfo(PipeLine pipeLine, String pipeName) {
		if (pipeLine.getPipe(pipeName) == null) {
			if (PipeLine.INPUT_VALIDATOR_NAME.equals(pipeName)) {
				return new PipeInfo(INPUT_VALIDATOR_CHECKPOINT_NAME, "/inputValidator");
			} else if (PipeLine.OUTPUT_VALIDATOR_NAME.equals(pipeName)) {
				return new PipeInfo(OUTPUT_VALIDATOR_CHECKPOINT_NAME, "/outputValidator");
			} else if (PipeLine.INPUT_WRAPPER_NAME.equals(pipeName)) {
				return new PipeInfo(INPUT_WRAPPER_CHECKPOINT_NAME, "/inputWrapper");
			} else if (PipeLine.OUTPUT_WRAPPER_NAME.equals(pipeName)) {
				return new PipeInfo(OUTPUT_WRAPPER_CHECKPOINT_NAME, "/outputWrapper");
			} else if (pipeName.startsWith(MessageSendingPipe.INPUT_VALIDATOR_NAME_PREFIX) && pipeName.endsWith(MessageSendingPipe.INPUT_VALIDATOR_NAME_SUFFIX)) {
				String parentPipeName = getParentPipeName(pipeName, MessageSendingPipe.INPUT_VALIDATOR_NAME_PREFIX, MessageSendingPipe.INPUT_VALIDATOR_NAME_SUFFIX);
				return new PipeInfo(INPUT_VALIDATOR_CHECKPOINT_NAME, "/pipe[@name=\"" + parentPipeName + "\"]/inputValidator");
			} else if (pipeName.startsWith(MessageSendingPipe.OUTPUT_VALIDATOR_NAME_PREFIX) && pipeName.endsWith(MessageSendingPipe.OUTPUT_VALIDATOR_NAME_SUFFIX)) {
				String parentPipeName = getParentPipeName(pipeName, MessageSendingPipe.OUTPUT_VALIDATOR_NAME_PREFIX, MessageSendingPipe.OUTPUT_VALIDATOR_NAME_SUFFIX);
				return new PipeInfo(OUTPUT_VALIDATOR_CHECKPOINT_NAME, "/pipe[@name=\"" + parentPipeName + "\"]/outputValidator");
			} else if (pipeName.startsWith(MessageSendingPipe.INPUT_WRAPPER_NAME_PREFIX) && pipeName.endsWith(MessageSendingPipe.INPUT_WRAPPER_NAME_SUFFIX)) {
				String parentPipeName = getParentPipeName(pipeName, MessageSendingPipe.INPUT_WRAPPER_NAME_PREFIX, MessageSendingPipe.INPUT_WRAPPER_NAME_SUFFIX);
				return new PipeInfo(INPUT_WRAPPER_CHECKPOINT_NAME, "/pipe[@name=\"" + parentPipeName + "\"]/inputWrapper");
			} else if (pipeName.startsWith(MessageSendingPipe.OUTPUT_WRAPPER_NAME_PREFIX) && pipeName.endsWith(MessageSendingPipe.OUTPUT_WRAPPER_NAME_SUFFIX)) {
				String parentPipeName = getParentPipeName(pipeName, MessageSendingPipe.OUTPUT_WRAPPER_NAME_PREFIX, MessageSendingPipe.OUTPUT_WRAPPER_NAME_SUFFIX);
				return new PipeInfo(OUTPUT_WRAPPER_CHECKPOINT_NAME, "/pipe[@name=\"" + parentPipeName + "\"]/outputWrapper");
			} else {
				return new PipeInfo("Pipe " + pipeName, null);
			}
		} else {
			return new PipeInfo("Pipe " + pipeName, "/pipe[@name=\"" + pipeName + "\"]");
		}
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

	// Protected for tests
	protected void addResourceNamesToPipeDescription(Node element, PipeDescription pipeDescription) {
		NamedNodeMap attributes = element.getAttributes();
		for (int i = 0, size = attributes.getLength(); i < size; i++) {
			Attr attribute = (Attr) attributes.item(i);
			String attributeName = attribute.getName();
			if ("styleSheetName".equals(attributeName)
					|| "serviceSelectionStylesheetFilename".equals(attributeName)
					|| "schema".equals(attributeName)
					|| "wsdl".equals(attributeName)
					|| "fileName".equals(attributeName)
					|| "filename".equals(attributeName)
					|| "schemaLocation".equals(attributeName)) {
				if ("schemaLocation".equals(attributeName)) {
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

	private String getParentPipeName(String pipeName, String prefix, String suffix) {
		return pipeName.substring(prefix.length(), pipeName.length() - suffix.length());
	}
}
