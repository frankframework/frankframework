/*
   Copyright 2016, 2018 Nationale-Nederlanden, 2019-2020 WeAreFrank!

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
package nl.nn.adapterframework.util.flow;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.util.List;

import javax.xml.transform.TransformerException;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.BeanInstantiationException;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.xml.sax.SAXException;

import nl.nn.adapterframework.configuration.Configuration;
import nl.nn.adapterframework.core.IAdapter;
import nl.nn.adapterframework.core.Resource;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.FileUtils;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.TransformerPool;
import nl.nn.adapterframework.util.XmlUtils;

/**
 * Utility class to generate the flow diagram for an adapter or a configuration.
 * 
 * @author Niels Meijer
 * @version 2.0
 */

public class FlowDiagramManager implements ApplicationContextAware, InitializingBean, DisposableBean {
	private static Logger log = LogUtil.getLogger(FlowDiagramManager.class);

	private static final AppConstants APP_CONSTANTS = AppConstants.getInstance();
	private File adapterFlowDir = new File(APP_CONSTANTS.getResolvedProperty("flow.adapter.dir"));
	private File configFlowDir = new File(APP_CONSTANTS.getResolvedProperty("flow.config.dir"));
	private ApplicationContext applicationContext;

	private static final String ADAPTER2DOT_XSLT = "/xsl/adapter2dot.xsl";
	private static final String CONFIGURATION2DOT_XSLT = "/xsl/configuration2dot.xsl";

	private TransformerPool transformerPoolAdapter;
	private TransformerPool transformerPoolConfig;
	private Resource noImageAvailable;
	private String fileExtension = null;

	/**
	 * Optional IFlowGenerator. If non present the FlowDiagramManager should still be 
	 * able to generate dot files and return the `noImageAvailable` image.
	 */
	private ThreadLocal<SoftReference<IFlowGenerator>> generators = new ThreadLocal<SoftReference<IFlowGenerator>>();

	@Override
	public void afterPropertiesSet() throws Exception {
		Resource xsltSourceConfig = Resource.getResource(ADAPTER2DOT_XSLT);
		transformerPoolAdapter = TransformerPool.getInstance(xsltSourceConfig, 2);

		Resource xsltSourceIbis = Resource.getResource(CONFIGURATION2DOT_XSLT);
		transformerPoolConfig = TransformerPool.getInstance(xsltSourceIbis, 2);

		IFlowGenerator generator = getFlowGenerator();
		if(generator == null) {
			log.warn("no IFlowGenerator found. Unable to generate flow diagrams");
		} else {
			if(log.isDebugEnabled()) log.debug("using IFlowGenerator ["+generator+"]");
			fileExtension = generator.getFileExtension();
		}

		noImageAvailable = Resource.getResource("/IAF_WebControl/GenerateFlowDiagram/svg/no_image_available.svg");
		if(noImageAvailable == null) {
			throw new IllegalStateException("image [no_image_available.svg] not found");
		}
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	/**
	 * Optional IFlowGenerator. If non present the FlowDiagramManager should still be 
	 * able to generate dot files and return the `noImageAvailable` image.
	 */
	protected IFlowGenerator createFlowGenerator() {
		if(applicationContext == null) {
			throw new IllegalStateException("ApplicationContext has not been autowired, cannot instantiate IFlowDiagram");
		}

		try {
			IFlowGenerator generator = applicationContext.getBean("flowGenerator", IFlowGenerator.class);

			if(log.isTraceEnabled()) log.trace("created new FlowGenerator instance ["+generator+"]");
			return generator;
		} catch (BeanCreationException | BeanInstantiationException | NoSuchBeanDefinitionException e) {
			//Failed to initialize.
			log.warn("failed to initalize IFlowGenerator", e);
		}
		return null;
	}

	/**
	 * The IFlowGenerator is wrapped in a SoftReference, wrapped in a ThreadLocal. 
	 * When the thread is cleaned up, it will remove the instance. Or when the GC is 
	 * running our of heapspace it will remove the IFlowGenerator. This method makes sure,
	 * as long as the IFlowGenerator bean can initialize, always a valid instance is returned.
	 */
	public IFlowGenerator getFlowGenerator() {
		SoftReference<IFlowGenerator> reference = generators.get();
		if(reference == null || reference.get() == null) {
			IFlowGenerator generator = createFlowGenerator();
			if(generator == null) {
				return null;
			}

			reference = new SoftReference<>(generator);
			generators.set(reference);
		}

		return reference.get();
	}

	public InputStream get(IAdapter adapter) throws IOException {
		File destFile = retrieveAdapterFlowFile(adapter);

		if(destFile == null || !destFile.exists()) {
			return noImageAvailable.openStream();
		}

		return new FileInputStream(destFile);
	}

	public InputStream get(Configuration configuration) throws IOException {
		File destFile = retrieveConfigurationFlowFile(configuration);

		if(destFile == null || !destFile.exists()) {
			return noImageAvailable.openStream();
		}

		return new FileInputStream(destFile);
	}

	public InputStream get(List<Configuration> configurations) throws IOException {
		File destFile = retrieveAllConfigurationsFlowFile();

		if(destFile == null || !destFile.exists()) {
			return noImageAvailable.openStream();
		}

		return new FileInputStream(destFile);
	}

	public void generate(IAdapter adapter) throws IOException {
		File destFile = retrieveAdapterFlowFile(adapter);
		if(destFile == null) return;

		if(destFile.exists()) //If the file exists, update it
			destFile.delete();

		String dotOutput = null;
		try {
			dotOutput = generateDot(adapter);
		} catch(Exception e) {
			log.warn("failed to create dot file for adapter["+adapter.getName()+"]", e);
		}

		String name = "adapter[" + adapter.getName() + "]";
		generateFlowDiagram(name, dotOutput, destFile);
	}

	public void generate(Configuration configuration) throws IOException {
		File destFile = retrieveConfigurationFlowFile(configuration);
		if(destFile == null) return;

		if(destFile.exists()) //If the file exists, update it
			destFile.delete();

		String dotOutput = null;
		try {
			dotOutput = generateDot(configuration);
		} catch(Exception e) {
			log.warn("failed to create dot file for configuration["+configuration.getName()+"]", e);
		}

		String name = "configuration[" + configuration.getName() + "]";
		generateFlowDiagram(name, dotOutput, destFile);
	}

	public void generate(List<Configuration> configurations) throws IOException {
		File destFile = retrieveAllConfigurationsFlowFile();
		if(destFile == null) return;

		destFile.delete();

		String dotOutput = null;
		try {
			dotOutput = generateDot(configurations);
		}
		catch(Exception e) {
			log.warn("failed to create dot file for configurations"+configurations.toString()+"", e);
		}

		String name = "configurations[*ALL*]";
		generateFlowDiagram(name, dotOutput, destFile);
	}

	public String generateDot(IAdapter adapter) throws TransformerException, IOException, SAXException {
		return transformerPoolAdapter.transform(adapter.getAdapterConfigurationAsString(), null);
	}

	public String generateDot(Configuration config) throws TransformerException, IOException, SAXException {
		return transformerPoolConfig.transform(config.getLoadedConfiguration(), null);
	}

	public String generateDot(List<Configuration> configurations) throws TransformerException, IOException, SAXException {
		String dotInput = "<configs>";
		for (Configuration configuration : configurations) {
			dotInput = dotInput + XmlUtils.skipXmlDeclaration(configuration.getLoadedConfiguration());
		}
		dotInput = dotInput + "</configs>";

		return transformerPoolConfig.transform(dotInput, null);
	}

	private File retrieveAdapterFlowFile(IAdapter iAdapter) {
		return retrieveFlowFile(adapterFlowDir, iAdapter.getName());
	}

	private File retrieveConfigurationFlowFile(Configuration configuration) {
		return retrieveFlowFile(configFlowDir, configuration.getName());
	}

	private File retrieveAllConfigurationsFlowFile() {
		return retrieveFlowFile(configFlowDir, "_ALL_");
	}

	private File retrieveFlowFile(File parent, String fileName) {
		if(fileExtension == null) {
			return null;
		}

		if (!parent.exists()) {
			if (!parent.mkdirs()) {
				throw new IllegalStateException(parent.getPath() + " does not exist and could not be created");
			}
		}

		String name = FileUtils.encodeFileName(fileName) + "." + fileExtension;
		log.debug("retrieve flow file for name["+fileName+"] in folder["+parent.getPath()+"]");

		return new File(parent, name);
	}

	// Don't call this when no generator is set!
	private void generateFlowDiagram(String name, String dot, File destination) throws IOException {
		log.debug("generating flow diagram for " + name);
		long start = System.currentTimeMillis();

		try (FileOutputStream outputStream = new FileOutputStream(destination)) {
			getFlowGenerator().generateFlow(name, dot, outputStream);
		} catch (IOException e) {
			if(log.isDebugEnabled()) log.debug("error generating flow diagram for ["+name+"]", e);

			throw e;
		}

		log.debug("finished generating flow diagram for ["+ name +"] in ["+ (System.currentTimeMillis()-start) +"] ms");
	}

	@Override
	public void destroy() throws Exception {
		if(transformerPoolAdapter != null)
			transformerPoolAdapter.close();

		if(transformerPoolConfig != null)
			transformerPoolConfig.close();
	}
}