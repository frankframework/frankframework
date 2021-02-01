/*
   Copyright 2018 Nationale-Nederlanden

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
package nl.nn.ibistesttool;

import java.io.IOException;
import java.io.StringWriter;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.WeakHashMap;

import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;

import nl.nn.adapterframework.configuration.Configuration;
import nl.nn.adapterframework.core.Adapter;
import nl.nn.adapterframework.core.INamedObject;
import nl.nn.adapterframework.core.IPipe;
import nl.nn.adapterframework.core.PipeLine;
import nl.nn.adapterframework.pipes.MessageSendingPipe;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.Misc;

/**
 * Get a description of a specified pipe. The description contains the XML
 * configuration for the pipe and optionally the XSLT files used by the pipe.
 *
 * @author Jaco de Groot (jaco@dynasol.nl)
 */
public class PipeDescriptionProvider {
	private Map<Configuration, Map<String, PipeDescription>> pipeDescriptionCaches = new WeakHashMap<Configuration, Map<String, PipeDescription>>();
	private Map<Configuration, Document> documents = new WeakHashMap<Configuration, Document>();
	private final static String INPUT_VALIDATOR_CHECKPOINT_NAME = "InputValidator";
	private final static String OUTPUT_VALIDATOR_CHECKPOINT_NAME = "OutputValidator";
	private final static String INPUT_WRAPPER_CHECKPOINT_NAME = "InputWrapper";
	private final static String OUTPUT_WRAPPER_CHECKPOINT_NAME = "OutputWrapper";

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
			Adapter adapter = pipeLine.getAdapter();
			Configuration configuration = adapter==null? null : adapter.getConfiguration();
			Map<String, PipeDescription> pipeDescriptionCache = pipeDescriptionCaches.get(configuration);
			if (pipeDescriptionCache == null) {
				pipeDescriptionCache = new HashMap<String, PipeDescription>();
				pipeDescriptionCaches.put(configuration, pipeDescriptionCache);
			}
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
							document = DocumentHelper.parseText(configuration.getLoadedConfiguration());
							documents.put(configuration, document);
						} catch (DocumentException e) {
							pipeDescription = new PipeDescription();
							pipeDescription.setCheckpointName(getCheckpointName(pipe, checkpointName));
							pipeDescription.setDescription("Could not parse configuration: " + e.getMessage());
							pipeDescriptionCache.put(xpathExpression, pipeDescription);
						}
					}
					if (document != null) {
						Node node = document.selectSingleNode(xpathExpression);
						if (node != null) {
							StringWriter stringWriter = new StringWriter();
							OutputFormat outputFormat = OutputFormat.createPrettyPrint();
							XMLWriter xmlWriter = new XMLWriter(stringWriter, outputFormat);
							try {
								xmlWriter.write(node);
								xmlWriter.flush();
								pipeDescription.setDescription(stringWriter.toString());
							} catch(IOException e) {
								pipeDescription.setDescription("IOException: " + e.getMessage());
							}
							addResourceNamesToPipeDescription((Element)node, pipeDescription);
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

	private void addResourceNamesToPipeDescription(Element element, PipeDescription pipeDescription) {
		for (int i = 0, size = element.attributeCount(); i < size; i++) {
			Attribute attribute = element.attribute(i);
			if ("styleSheetName".equals(attribute.getName())
					|| "serviceSelectionStylesheetFilename".equals(attribute.getName())
					|| "schema".equals(attribute.getName())
					|| "wsdl".equals(attribute.getName())
					|| "fileName".equals(attribute.getName())
					|| "schemaLocation".equals(attribute.getName())) {
				if ("schemaLocation".equals(attribute.getName())) {
 					StringTokenizer st = new StringTokenizer(attribute.getValue(),", \t\r\n\f");
 					while (st.hasMoreTokens()) {
 						st.nextToken();
 						String resourceName = st.nextToken();
 						if (!pipeDescription.containsResourceName(resourceName)) {
 							pipeDescription.addResourceName(resourceName);
 						}
 					}
				} else {
					String resourceName = attribute.getValue();
					if (!pipeDescription.containsResourceName(resourceName)) {
						pipeDescription.addResourceName(resourceName);
					}
				}
			}
		}
		for (int i = 0, size = element.nodeCount(); i < size; i++) {
			Node node = element.node(i);
			if (node instanceof Element && "sender".equals(node.getName())) {
				addResourceNamesToPipeDescription((Element)node, pipeDescription);
			}
		}
	}

	/**
	 * Return the content of the specified resource.
	 */
	public String getResource(PipeLine pipeLine, String resourceName) {
		String resource;
		try {
			URL resourceUrl = ClassUtils.getResourceURL(pipeLine, resourceName);
			if(resourceUrl != null)
				resource = Misc.resourceToString(resourceUrl, "\n", false);
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
