/*
   Copyright 2016, 2018 Nationale-Nederlanden

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
package nl.nn.adapterframework.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.List;

import javax.xml.transform.Transformer;

import nl.nn.adapterframework.configuration.Configuration;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IAdapter;
import nl.nn.adapterframework.extensions.graphviz.Format;
import nl.nn.adapterframework.extensions.graphviz.GraphvizEngine;
import nl.nn.adapterframework.extensions.graphviz.GraphvizException;
import nl.nn.adapterframework.extensions.graphviz.Options;

import org.apache.log4j.Logger;

/**
 * Utility class to generate the flow diagram for an adapter or a configuration.
 * 
 * @author Peter Leeuwenburgh
 * @author Niels Meijer
 * @version 2.0
 */

public class FlowDiagram {
	private static Logger log = LogUtil.getLogger(FlowDiagram.class);

	private File adapterFlowDir = new File(AppConstants.getInstance().getResolvedProperty("flow.adapter.dir"));
	private File configFlowDir = new File(AppConstants.getInstance().getResolvedProperty("flow.config.dir"));

	private static final String CONFIG2DOT_XSLT = "/IAF_WebControl/GenerateFlowDiagram/xsl/config2dot.xsl";
	private static final String IBIS2DOT_XSLT = "/IAF_WebControl/GenerateFlowDiagram/xsl/ibis2dot.xsl";

	private GraphvizEngine engine;
	private Options options = Options.create();
	private Format format = Format.SVG;

	public FlowDiagram() {
		this(null);
	}

	public FlowDiagram(String format) {
		this(format, null);
	}

	public FlowDiagram(String format, String version) {
		if (!adapterFlowDir.exists()) {
			if (!adapterFlowDir.mkdirs()) {
				throw new IllegalStateException(adapterFlowDir.getPath() + " does not exist and could not be created");
			}
		}

		if (!configFlowDir.exists()) {
			if (!configFlowDir.mkdirs()) {
				throw new IllegalStateException(configFlowDir.getPath() + " does not exist and could not be created");
			}
		}

		engine = new GraphvizEngine(version);

		if(format != null) {
			try {
				this.format = Format.valueOf(format.toUpperCase());
			}
			catch(IllegalArgumentException e) {
				throw new IllegalArgumentException("unknown format["+format.toUpperCase()+"], must be one of "+Format.values());
			}
		}

		options = options.format(this.format);
	}

	public void generate(IAdapter adapter) throws ConfigurationException, IOException, GraphvizException {
		File destFile = retrieveAdapterFlowFile(adapter);

		if(destFile.exists()) //If the file exists, update it
			destFile.delete();

		String dotInput = adapter.getAdapterConfigurationAsString();
		String dotOutput = null;

		try {
			URL xsltSource = ClassUtils.getResourceURL(this, CONFIG2DOT_XSLT);
			Transformer transformer = XmlUtils.createTransformer(xsltSource);
			dotOutput = XmlUtils.transformXml(transformer, dotInput);
		}
		catch(Exception e) {
			log.warn("failed to create dot file for adapter["+adapter.getName()+"]", e);
		}

		String name = "adapter[" + adapter.getName() + "]";
		generateFlowDiagram(name, dotOutput, destFile);
	}

	public void generate(Configuration configuration) throws ConfigurationException, IOException, GraphvizException {
		File destFile = retrieveConfigurationFlowFile(configuration);

		if(destFile.exists()) //If the file exists, update it
			destFile.delete();

		String dotInput = configuration.getLoadedConfiguration();
		String dotOutput = null;

		try {
			URL xsltSource = ClassUtils.getResourceURL(this, IBIS2DOT_XSLT);
			Transformer transformer = XmlUtils.createTransformer(xsltSource);
			dotOutput = XmlUtils.transformXml(transformer, dotInput);
		}
		catch(Exception e) {
			log.warn("failed to create dot file for configuration["+configuration.getName()+"]", e);
		}

		String name = "configuration[" + configuration.getName() + "]";
		generateFlowDiagram(name, dotOutput, destFile);
	}

	public void generate(List<Configuration> configurations) throws ConfigurationException, IOException, GraphvizException {
		File destFile = retrieveAllConfigurationsFlowFile();
		destFile.delete();

		String dotInput = "<configs>";
		for (Configuration configuration : configurations) {
			dotInput = dotInput
					+ XmlUtils.skipXmlDeclaration(configuration
							.getLoadedConfiguration());
		}
		dotInput = dotInput + "</configs>";
		String dotOutput = null;

		try {
			URL xsltSource = ClassUtils.getResourceURL(this, IBIS2DOT_XSLT);
			Transformer transformer = XmlUtils.createTransformer(xsltSource);
			dotOutput = XmlUtils.transformXml(transformer, dotInput);
		}
		catch(Exception e) {
			log.warn("failed to create dot file for configurations"+configurations.toString()+"", e);
		}

		String name = "configurations[*ALL*]";
		generateFlowDiagram(name, dotOutput, destFile);
	}

	public File retrieveAdapterFlowFile(IAdapter iAdapter) {
		return retrieveFlowFile(adapterFlowDir, iAdapter.getName());
	}

	public File retrieveConfigurationFlowFile(Configuration configuration) {
		return retrieveFlowFile(configFlowDir, configuration.getName());
	}

	public File retrieveAllConfigurationsFlowFile() {
		return retrieveFlowFile(configFlowDir, "_ALL_");
	}

	private File retrieveFlowFile(File parent, String fileName) {
		String name = FileUtils.encodeFileName(fileName) + "." + format.fileExtension;
		log.debug("retrieve flow file for name["+fileName+"] in folder["+parent.getPath()+"]");
		return new File(parent, name);
	}

	private void generateFlowDiagram(String name, String dot, File destFile) throws IOException, GraphvizException {
		log.debug("generating flow diagram for " + name);
		long start = System.currentTimeMillis();

		String flow = engine.execute(dot, options);

		FileOutputStream outputStream = new FileOutputStream(destFile);
		outputStream.write(flow.getBytes());
		outputStream.close();

		log.debug("finished generating flow diagram for "+ name +" in ["+ (System.currentTimeMillis()-start) +"] ms");
	}
}