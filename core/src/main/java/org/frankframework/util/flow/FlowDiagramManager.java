/*
   Copyright 2016, 2018 Nationale-Nederlanden, 2019-2021 WeAreFrank!

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
package org.frankframework.util.flow;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.List;

import org.apache.commons.lang3.CharUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanInstantiationException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.http.MediaType;
import org.springframework.util.MimeType;

import lombok.Setter;
import lombok.extern.log4j.Log4j2;

import org.frankframework.configuration.Configuration;
import org.frankframework.core.Adapter;
import org.frankframework.util.AppConstants;
import org.frankframework.util.ClassUtils;
import org.frankframework.util.SpringUtils;
import org.frankframework.util.XmlUtils;

/**
 * Utility class to generate the flow diagram for an adapter or a configuration.
 *
 * @author Niels Meijer
 * @version 2.0
 */
@Log4j2
public class FlowDiagramManager implements ApplicationContextAware, InitializingBean, DisposableBean {

	private static final AppConstants APP_CONSTANTS = AppConstants.getInstance();
	private final File adapterFlowDir = new File(APP_CONSTANTS.getProperty("flow.adapter.dir"));
	private final File configFlowDir = new File(APP_CONSTANTS.getProperty("flow.config.dir"));
	private @Setter ApplicationContext applicationContext;

	private IFlowGenerator flowGenerator;

	@Override
	public void afterPropertiesSet() throws Exception {
		if(applicationContext == null) {
			throw new IllegalStateException("ApplicationContext has not been autowired, cannot instantiate IFlowDiagram");
		}

		String generatorBeanClass = AppConstants.getInstance().getProperty("flow.generator");
		if(StringUtils.isNotEmpty(generatorBeanClass)) {
			flowGenerator = createFlowGenerator(generatorBeanClass);
		} else {
			log.info("no FlowGenerator configured. No flow diagrams will be generated");
		}
	}

	public MimeType getMediaType() {
		return flowGenerator != null ? flowGenerator.getMediaType() : MediaType.TEXT_PLAIN;
	}

	/**
	 * Optional IFlowGenerator. If not present, the FlowDiagramManager should still be
	 * able to generate dot files and return the `noImageAvailable` image.
	 */
	protected IFlowGenerator createFlowGenerator(String generatorBeanClass) {
		log.debug("trying to initialize FlowGenerator [{}]", generatorBeanClass);
		try {
			Class<?> clazz = ClassUtils.loadClass(generatorBeanClass);
			if(clazz.isAssignableFrom(IFlowGenerator.class)) {
				throw new IllegalStateException("provided generator does not implement IFlowGenerator interface");
			}
			return (IFlowGenerator) SpringUtils.createBean(applicationContext, clazz);
		} catch (ClassNotFoundException e) {
			log.warn("FlowGenerator class [{}] not found", generatorBeanClass, e);
		} catch (BeanCreationException | BeanInstantiationException | NoSuchBeanDefinitionException e) {
			log.warn("failed to initalize FlowGenerator", e);
		}

		return null;
	}

	public InputStream get(Adapter adapter) throws IOException {
		File destFile = retrieveAdapterFlowFile(adapter);

		if(destFile == null || !destFile.exists()) {
			return null;
		}

		return new FileInputStream(destFile);
	}

	public InputStream get(Configuration configuration) throws IOException {
		File destFile = retrieveConfigurationFlowFile(configuration);

		if(destFile == null || !destFile.exists()) {
			return null;
		}

		return new FileInputStream(destFile);
	}

	public InputStream get(List<Configuration> configurations) throws IOException {
		File destFile = retrieveAllConfigurationsFlowFile();

		if(destFile == null || !destFile.exists()) {
			return null;
		}

		return new FileInputStream(destFile);
	}

	public void generate(Adapter adapter) throws IOException {
		File destFile = retrieveAdapterFlowFile(adapter);
		if(destFile == null) return;

		if(destFile.exists()) { //If the file exists, update it
			Files.delete(destFile.toPath());
		}

		String adapterXml = getAdapterConfigurationAsString(adapter);
		String name = "adapter[" + adapter.getName() + "]";

		generateFlowDiagram(name, adapterXml, destFile);
	}

	public void generate(Configuration configuration) throws IOException {
		File destFile = retrieveConfigurationFlowFile(configuration);
		if(destFile == null) return;

		if(destFile.exists()) { //If the file exists, update it
			Files.delete(destFile.toPath());
		}

		String configurationXml = configuration.getLoadedConfiguration();
		String name = "configuration [" + configuration.getName() + "]";

		generateFlowDiagram(name, configurationXml, destFile);
	}

	public void generate(List<Configuration> configurations) throws IOException {
		File destFile = retrieveAllConfigurationsFlowFile();
		if(destFile == null) return;

		if(destFile.exists()) { //If the file exists, update it
			Files.delete(destFile.toPath());
		}

		String configurationsXml = getConfigurationXml(configurations);
		String name = "configurations[ALL]";

		generateFlowDiagram(name, configurationsXml, destFile);
	}

	public String getAdapterConfigurationAsString(Adapter adapter) {
		String loadedConfig = adapter.getConfiguration().getLoadedConfiguration();
		String encodedName = StringUtils.replace(adapter.getName(), "'", "''");
		String xpath = "//adapter[@name='" + encodedName + "']";

		return XmlUtils.copyOfSelect(loadedConfig, xpath);
	}

	public String getConfigurationXml(List<Configuration> configurations) {
		StringBuilder dotInput = new StringBuilder("<Configurations>");
		for (Configuration configuration : configurations) {
			dotInput.append(XmlUtils.skipXmlDeclaration(configuration.getLoadedConfiguration()));
		}
		dotInput.append("</Configurations>");

		return dotInput.toString();
	}

	private File retrieveAdapterFlowFile(Adapter adapter) {
		return retrieveFlowFile(adapterFlowDir, adapter.getName());
	}

	private File retrieveConfigurationFlowFile(Configuration configuration) {
		String filename = configuration.getName();
		if(StringUtils.isEmpty(filename)) {
			log.warn("cannot generate FlowFile, configuration name is null");
			return null;
		}
		return retrieveFlowFile(configFlowDir, filename);
	}

	private File retrieveAllConfigurationsFlowFile() {
		return retrieveFlowFile(configFlowDir, "_ALL_");
	}

	private File retrieveFlowFile(File parent, String fileName) {
		if(flowGenerator == null) { //fail fast check to see if an IFlowGenerator is available.
			log.debug("cannot retrieve Flow file, no generator found");
			return null;
		}

		if (!parent.exists()&& !parent.mkdirs()) {
			throw new IllegalStateException(parent.getPath() + " does not exist and could not be created");
		}

		String name = encodeFileName(fileName) + "." + flowGenerator.getFileExtension();
		log.trace("retrieve flow file for name[{}] in folder[{}]", ()->fileName, parent::getPath);

		return new File(parent, name);
	}

	// Don't call this when no generator is set!
	private void generateFlowDiagram(String name, String xml, File destination) throws IOException {
		if(flowGenerator == null || StringUtils.isEmpty(xml)) { //fail fast check to see if an IFlowGenerator is available.
			log.debug("cannot generate flow diagram for [{}]", name);
			return;
		}

		log.debug("generating flow diagram for [{}]", name);
		long start = System.currentTimeMillis();

		try (FileOutputStream outputStream = new FileOutputStream(destination)) {
			flowGenerator.generateFlow(xml, outputStream);
		} catch (FlowGenerationException e) {
			log.debug("error generating flow diagram for [{}]", name, e);

			if(destination.exists()) {
				Files.delete(destination.toPath());
			}

			throw new IOException("error generating flow diagram for ["+name+"]", e);
		}

		log.debug("finished generating flow diagram for [{}] in [{}] ms", () -> name, () -> System.currentTimeMillis() - start);
	}

	static String encodeFileName(String fileName) {
		StringBuilder encodedFileName = new StringBuilder();
		for (int i = 0; i < fileName.length(); i++) {
			char c = fileName.charAt(i);
			if (CharUtils.isAsciiAlphanumeric(c)) {
				encodedFileName.append(c);
			} else if ("-_.+=".indexOf(c) >= 0) {
				encodedFileName.append(c);
			} else {
				encodedFileName.append('_');
			}
		}
		return encodedFileName.toString();
	}

	@Override
	public void destroy() throws Exception {
		if(flowGenerator != null) {
			flowGenerator.destroy();
		}
	}
}
