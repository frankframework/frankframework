/*
   Copyright 2013, 2020 Nationale-Nederlanden

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
package nl.nn.adapterframework.extensions.esb;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

import nl.nn.adapterframework.configuration.Configuration;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.classloaders.DirectoryClassLoader;
import nl.nn.adapterframework.core.Adapter;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeLine;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.http.RestListenerUtils;
import nl.nn.adapterframework.pipes.FixedForwardPipe;
import nl.nn.adapterframework.receivers.GenericReceiver;
import nl.nn.adapterframework.soap.Wsdl;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.DateUtils;
import nl.nn.adapterframework.util.Dir2Xml;
import nl.nn.adapterframework.util.FileUtils;
import nl.nn.adapterframework.util.Misc;
import nl.nn.adapterframework.util.TransformerPool;
import nl.nn.adapterframework.util.XmlUtils;

import org.apache.commons.lang.StringUtils;

public class WsdlGeneratorPipe extends FixedForwardPipe {

	private String sessionKey = "file";
	private String propertiesFileName = "wsdl.properties";
	private static final String WSDL_EXTENSION = ".wsdl";

	@Override
	public PipeRunResult doPipe(Message message, IPipeLineSession session) throws PipeRunException {
		InputStream inputStream = (InputStream) session.get("file");
		if (inputStream == null) {
			throw new PipeRunException(this, getLogPrefix(session) + "got null value from session under key [" + getSessionKey() + "]");
		}

		File tempDir;
		String fileName;
		
		try {
			tempDir = FileUtils.createTempDir(null, "WEB-INF" + File.separator
					+ "classes");
			fileName = (String) session.get("fileName");
			if (FileUtils.extensionEqualsIgnoreCase(fileName, "zip")) {
				FileUtils.unzipStream(inputStream, tempDir);
			} else {
				File file = new File(tempDir, fileName);
				Misc.streamToFile(inputStream, file);
				file.deleteOnExit();
			}
		} catch (IOException e) {
			throw new PipeRunException(this, getLogPrefix(session)
					+ " Exception on uploading and unzipping/writing file", e);
		}

		File propertiesFile = new File(tempDir, getPropertiesFileName());
		PipeLine pipeLine;
		ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
		try {
			// TODO why is this using a DirectoryClassloader?
			// can't we use the current classloader? FixedForwardPipe#classLoader
			// or even the configuration classloader? getAdapter().getConfiguration().getClassLoader()

			DirectoryClassLoader directoryClassLoader = new DirectoryClassLoader(originalClassLoader);
			directoryClassLoader.setDirectory(tempDir.getPath());
			directoryClassLoader.setBasePath(".");
			directoryClassLoader.configure(getAdapter().getConfiguration().getIbisManager().getIbisContext(), "dummy");
			Thread.currentThread().setContextClassLoader(directoryClassLoader);

			if (propertiesFile.exists()) {
				pipeLine = createPipeLineFromPropertiesFile(propertiesFile);
			} else {
				File xsdFile = FileUtils.getFirstFile(tempDir);
				pipeLine = createPipeLineFromXsdFile(xsdFile);
			}
		} catch (Exception e) {
			throw new PipeRunException(this, getLogPrefix(session)
					+ " Exception on generating wsdl", e);
		} finally {
			if (originalClassLoader != null) {
				Thread.currentThread().setContextClassLoader(originalClassLoader);
			}
		}

		Object result = null;
		OutputStream zipOut = null;
		OutputStream fullWsdlOut = null;
		try {
			Adapter adapter = new Adapter();
			Configuration configuration = new Configuration();
			configuration.setClassLoader(getConfigurationClassLoader());
			adapter.setConfiguration(configuration);
			String fileBaseName = FileUtils.getBaseName(fileName).replaceAll(
					" ", "_");
			adapter.setName(fileBaseName);
			GenericReceiver genericReceiver = new GenericReceiver();
			EsbJmsListener esbJmsListener = new EsbJmsListener();
			esbJmsListener.setQueueConnectionFactoryName("jms/qcf_"
					+ fileBaseName);
			esbJmsListener.setDestinationName("jms/dest_" + fileBaseName);
			genericReceiver.setListener(esbJmsListener);
			adapter.registerReceiver(genericReceiver);
			pipeLine.setAdapter(adapter);
			Wsdl wsdl = null;
			String generationInfo = "at " + RestListenerUtils.retrieveRequestURL(session);
			wsdl = new Wsdl(pipeLine, generationInfo);
			wsdl.setIndent(true);
			wsdl.init();
			File wsdlDir = FileUtils.createTempDir(tempDir);
			// zip (with includes)
			File zipOutFile = new File(wsdlDir, wsdl.getFilename() + ".zip");
			zipOutFile.deleteOnExit();
			zipOut = new FileOutputStream(zipOutFile);
			wsdl.setUseIncludes(true);
			wsdl.zip(zipOut, null);
			// full wsdl (without includes)
			File fullWsdlOutFile = new File(wsdlDir, wsdl.getFilename()
					+ ".wsdl");
			fullWsdlOutFile.deleteOnExit();
			fullWsdlOut = new FileOutputStream(fullWsdlOutFile);
			wsdl.setUseIncludes(false);
			wsdl.wsdl(fullWsdlOut, null);
			Dir2Xml dx = new Dir2Xml();
			dx.setPath(wsdlDir.getPath());
			result = dx.getDirList();
		} catch (Exception e) {
			throw new PipeRunException(this, getLogPrefix(session)
					+ " Exception on generating wsdl", e);
		} finally {
			try {
				if (zipOut != null) {
					zipOut.close();
				}
				if (fullWsdlOut != null) {
					fullWsdlOut.close();
				}
			} catch (IOException e1) {
				log.warn("exception closing outputstream", e1);
			}
		}
		return new PipeRunResult(getForward(), result);
	}

	private PipeLine createPipeLineFromPropertiesFile(File propertiesFile)
			throws IOException, ConfigurationException {
		Properties props = new Properties();
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(propertiesFile);
			props.load(fis);
		} finally {
			try {
				if (fis != null) {
					fis.close();
				}
			} catch (IOException e) {
				log.warn("exception closing inputstream", e);
			}
		}

		PipeLine pipeLine = new PipeLine();
		String inputXsd = null;
		if (props.containsKey("input.xsd")) {
			inputXsd = props.getProperty("input.xsd");
			String inputNamespace = props.getProperty("input.namespace");
			String inputRoot = props.getProperty("input.root");
			String inputCmhString = props.getProperty("input.cmh", "1");
			int inputCmh = Integer.parseInt(inputCmhString);
			File inputXsdFile = new File(propertiesFile.getParent(), inputXsd);
			EsbSoapValidator inputValidator = createValidator(inputXsdFile,
					inputNamespace, inputRoot, 1, inputCmh);
			pipeLine.setInputValidator(inputValidator);
		}
		if (props.containsKey("output.xsd")) {
			String outputXsd = props.getProperty("output.xsd");
			String outputNamespace = props.getProperty("output.namespace");
			String outputRoot = props.getProperty("output.root");
			String outputCmhString = props.getProperty("output.cmh", "1");
			int outputCmh = Integer.parseInt(outputCmhString);
			File outputXsdFile = new File(propertiesFile.getParent(), outputXsd);
			int rootPosition;
			if (inputXsd != null && inputXsd.equalsIgnoreCase(outputXsd)) {
				rootPosition = 2;
			} else {
				rootPosition = 1;
			}
			EsbSoapValidator outputValidator = createValidator(outputXsdFile,
					outputNamespace, outputRoot, rootPosition, outputCmh);
			pipeLine.setOutputValidator(outputValidator);
		}
		return pipeLine;
	}

	private PipeLine createPipeLineFromXsdFile(File xsdFile)
			throws ConfigurationException {
		PipeLine pipeLine = new PipeLine();
		EsbSoapValidator inputValidator;
		inputValidator = createValidator(xsdFile, null, null, 1, 1);
		pipeLine.setInputValidator(inputValidator);

		String countRoot = null;
		try {
			String countRootXPath = "count(*/*[local-name()='element'])";
			TransformerPool tp = TransformerPool.getInstance(
					XmlUtils.createXPathEvaluatorSource(countRootXPath, "text"));
			countRoot = tp
					.transform(Misc.fileToString(xsdFile.getPath()), null);
			if (StringUtils.isNotEmpty(countRoot)) {
				log.debug("counted [" + countRoot
						+ "] root elements in xsd file [" + xsdFile.getName()
						+ "]");
				int cr = Integer.parseInt(countRoot);
				if (cr > 1) {
					EsbSoapValidator outputValidator;
					outputValidator = createValidator(xsdFile, null, null, 2, 1);
					pipeLine.setOutputValidator(outputValidator);
				}
			}
		} catch (Exception e) {
			throw new ConfigurationException(e);
		}
		return pipeLine;
	}

	private EsbSoapValidator createValidator(File xsdFile, String namespace,
			String root, int rootPosition, int cmhVersion) throws ConfigurationException {
		if (xsdFile != null) {
			EsbSoapValidator esbSoapValidator = new EsbSoapValidator();
			esbSoapValidator.setWarn(false);
			esbSoapValidator.setCmhVersion(cmhVersion);

			if (StringUtils.isEmpty(namespace)) {
				String xsdTargetNamespace = null;
				try {
					TransformerPool tp = TransformerPool.getInstance(
							XmlUtils.createXPathEvaluatorSource(
									"*/@targetNamespace", "text"));
					xsdTargetNamespace = tp.transform(
							Misc.fileToString(xsdFile.getPath()), null);
					if (StringUtils.isNotEmpty(xsdTargetNamespace)) {
						log.debug("found target namespace ["
								+ xsdTargetNamespace + "] in xsd file ["
								+ xsdFile.getName() + "]");
					} else {
						// default namespace to prevent
						// "(IllegalArgumentException) The schema attribute isn't supported"
						xsdTargetNamespace = "urn:wsdlGenerator";
						log.warn("could not find target namespace in xsd file ["
								+ xsdFile.getName()
								+ "], assuming namespace ["
								+ xsdTargetNamespace + "]");
					}
				} catch (Exception e) {
					throw new ConfigurationException(e);
				}
				if (StringUtils.isEmpty(xsdTargetNamespace)) {
					esbSoapValidator.setSchema(xsdFile.getName());
				} else {
					esbSoapValidator.setSchemaLocation(xsdTargetNamespace
							+ "\t" + xsdFile.getName());
					esbSoapValidator.setAddNamespaceToSchema(true);
				}
			} else {
				esbSoapValidator.setSchemaLocation(namespace + "\t"
						+ xsdFile.getName());
				esbSoapValidator.setAddNamespaceToSchema(true);
			}

			if (StringUtils.isEmpty(root)) {
				String xsdRoot = null;
				try {
					String rootXPath = "*/*[local-name()='element']["
							+ rootPosition + "]/@name";
					TransformerPool tp = TransformerPool.getInstance(
							XmlUtils.createXPathEvaluatorSource(rootXPath,
									"text"));
					xsdRoot = tp.transform(
							Misc.fileToString(xsdFile.getPath()), null);
					if (StringUtils.isNotEmpty(xsdRoot)) {
						log.debug("found root element [" + xsdRoot
								+ "] in xsd file [" + xsdFile.getName() + "]");
						esbSoapValidator.setSoapBody(xsdRoot);
					}
				} catch (Exception e) {
					throw new ConfigurationException(e);
				}
			} else {
				esbSoapValidator.setSoapBody(root);
			}

			esbSoapValidator.setForwardFailureToSuccess(true);
			PipeForward pf = new PipeForward();
			pf.setName("success");
			esbSoapValidator.registerForward(pf);
			esbSoapValidator.configure();
			return esbSoapValidator;
		}
		return null;
	}

	private String getWsdlDocumentation(String filename) {
		return "Generated as "
				+ filename
				+ WSDL_EXTENSION
				+ " by "
				+ AppConstants.getInstance()
						.getProperty("instance.name", "IAF") + " on "
				+ DateUtils.getIsoTimeStamp() + ".";
	}

	public String getSessionKey() {
		return sessionKey;
	}

	public void setSessionKey(String string) {
		sessionKey = string;
	}

	public String getPropertiesFileName() {
		return propertiesFileName;
	}

	public void setPropertiesFileName(String string) {
		propertiesFileName = string;
	}
}