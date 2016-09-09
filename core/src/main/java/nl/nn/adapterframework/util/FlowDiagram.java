/*
   Copyright 2016 Nationale-Nederlanden

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
import java.io.IOException;
import java.net.URL;
import java.util.List;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;

import nl.nn.adapterframework.configuration.Configuration;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IAdapter;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeLineSessionBase;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.http.HttpSender;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

/**
 * Utility class to generate the flow diagram for an adapter or a configuration.
 * 
 * @author Peter Leeuwenburgh
 * @version $Id$
 */

public class FlowDiagram {
	private static Logger log = LogUtil.getLogger(FlowDiagram.class);

	private File adapterFlowDir = new File(AppConstants.getInstance()
			.getResolvedProperty("flow.adapter.dir"));
	private File configFlowDir = new File(AppConstants.getInstance()
			.getResolvedProperty("flow.config.dir"));

	private static final String CONFIG2DOT_XSLT = "/IAF_WebControl/GenerateFlowDiagram/xsl/config2dot.xsl";
	private static final String IBIS2DOT_XSLT = "/IAF_WebControl/GenerateFlowDiagram/xsl/ibis2dot.xsl";

	private String url;
	private String format;
	private String truststore;
	private String truststorePassword;

	public FlowDiagram(String url) {
		this(url, null, null, null);
	}

	public FlowDiagram(String url, String format, String truststore,
			String truststorePassword) {
		this.url = url;
		this.format = format;
		this.truststore = truststore;
		this.truststorePassword = truststorePassword;

		if (!adapterFlowDir.exists()) {
			if (!adapterFlowDir.mkdirs()) {
				throw new IllegalStateException(adapterFlowDir.getPath()
						+ " could not be created");
			}
		}

		if (!configFlowDir.exists()) {
			if (!configFlowDir.mkdirs()) {
				throw new IllegalStateException(configFlowDir.getPath()
						+ " could not be created");
			}
		}

		if (StringUtils.isEmpty(this.url)) {
			throw new IllegalStateException("url must be specified");
		}

		if (StringUtils.isEmpty(this.format)) {
			this.format = "svg";
		}
	}

	public void generate(IAdapter iAdapter) throws IOException,
			DomBuilderException, TransformerException, ConfigurationException,
			SenderException, TimeOutException {
		File destFile = retrieveAdapterFlowFile(iAdapter);
		destFile.delete();

		String dotInput = iAdapter.getAdapterConfigurationAsString();

		URL xsltSource = ClassUtils.getResourceURL(this, CONFIG2DOT_XSLT);
		Transformer transformer = XmlUtils.createTransformer(xsltSource);
		String dotOutput = XmlUtils.transformXml(transformer, dotInput);

		String name = "adapter [" + iAdapter.getName() + "]";
		generateFlowDiagram(name, dotOutput, destFile);
	}

	public void generate(Configuration configuration) throws IOException,
			DomBuilderException, TransformerException, ConfigurationException,
			SenderException, TimeOutException {
		File destFile = retrieveConfigurationFlowFile(configuration);
		destFile.delete();

		String dotInput = configuration.getLoadedConfiguration();

		URL xsltSource = ClassUtils.getResourceURL(this, IBIS2DOT_XSLT);
		Transformer transformer = XmlUtils.createTransformer(xsltSource);
		String dotOutput = XmlUtils.transformXml(transformer, dotInput);

		String name = "configuration [" + configuration.getName() + "]";
		generateFlowDiagram(name, dotOutput, destFile);
	}

	public void generate(List<Configuration> configurations)
			throws IOException, DomBuilderException, TransformerException,
			ConfigurationException, SenderException, TimeOutException {
		File destFile = retrieveAllConfigurationsFlowFile();
		destFile.delete();

		String dotInput = "<configs>";
		for (Configuration configuration : configurations) {
			dotInput = dotInput
					+ XmlUtils.skipXmlDeclaration(configuration
							.getLoadedConfiguration());
		}
		dotInput = dotInput + "</configs>";

		URL xsltSource = ClassUtils.getResourceURL(this, IBIS2DOT_XSLT);
		Transformer transformer = XmlUtils.createTransformer(xsltSource);
		String dotOutput = XmlUtils.transformXml(transformer, dotInput);

		String name = "configurations [*ALL*]";
		generateFlowDiagram(name, dotOutput, destFile);
	}

	public File retrieveAdapterFlowFile(IAdapter iAdapter) {
		String directoryName = adapterFlowDir.getPath();
		String fileName = FileUtils.encodeFileName(iAdapter.getName()) + "."
				+ format;
		File file = new File(directoryName, fileName);
		return file;
	}

	public File retrieveConfigurationFlowFile(Configuration configuration) {
		String directoryName = configFlowDir.getPath();
		String fileName = FileUtils.encodeFileName(configuration.getName())
				+ "." + format;
		File file = new File(directoryName, fileName);
		return file;
	}

	public File retrieveAllConfigurationsFlowFile() {
		String directoryName = configFlowDir.getPath();
		String fileName = "_ALL_." + format;
		File file = new File(directoryName, fileName);
		return file;
	}

	private String generateFlowDiagram(String name, String dot, File destFile)
			throws ConfigurationException, SenderException, TimeOutException {
		GenerateFlowDiagramFlowRun generateFlowDiagramFlowRun = new GenerateFlowDiagramFlowRun(
				name, dot, destFile);
		Thread t = new Thread(generateFlowDiagramFlowRun);
		t.start();

		return null;
	}

	private class GenerateFlowDiagramFlowRun implements Runnable {
		String name;
		String dot;
		File destFile;

		GenerateFlowDiagramFlowRun(String name, String dot, File destFile) {
			this.name = name;
			this.dot = dot;
			this.destFile = destFile;
		}

		public void run() {
			log.debug("start generating flow diagram for " + name);
			HttpSender httpSender = null;
			try {
				File tempFile = FileUtils.createTempFile(null, "." + format);
				IPipeLineSession pls = new PipeLineSessionBase();
				pls.put("tempOutputFileName", tempFile.getPath());

				httpSender = new HttpSender();
				httpSender.setUrl(url);
				httpSender.setMethodType("POST");
				if (StringUtils.isNotEmpty(truststore)) {
					httpSender.setTruststore(truststore);
					httpSender.setTruststorePassword(truststorePassword);
				}
				httpSender.setParamsInUrl(false);
				httpSender.setMultipart(true);
				httpSender.setIgnoreRedirects(true);
				httpSender.setVerifyHostname(false);
				httpSender
						.setStreamResultToFileNameSessionKey("tempOutputFileName");

				Parameter p = new Parameter();
				p.setName("outputFormat");
				p.setValue(format);
				httpSender.addParameter(p);

				p = new Parameter();
				p.setName("input");
				p.setValue(dot);
				httpSender.addParameter(p);

				httpSender.configure();
				httpSender.open();
				ParameterResolutionContext prc = new ParameterResolutionContext(
						"dummy", pls);
				String result = httpSender.sendMessage(null, "", prc);

				if (FileUtils.moveFile(tempFile, destFile, true, 0) == null) {
					log.warn("could not rename file [" + tempFile.getPath()
							+ "] to [" + destFile.getPath() + "]");
				} else {
					log.debug("renamed file [" + tempFile.getPath() + "] to ["
							+ destFile.getPath() + "]");
				}
				log.debug("ended generating flow diagram for " + name);
			} catch (Exception e) {
				log.warn("exception on generating flow diagram for " + name, e);
			} finally {
				if (httpSender != null) {
					httpSender.close();
				}
			}
		}
	}
}
