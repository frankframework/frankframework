/*
   Copyright 2021 - 2023 WeAreFrank!

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
package nl.nn.adapterframework.testtool;

import java.io.IOException;
import java.util.Map;

import javax.xml.transform.TransformerException;

import org.springframework.context.ApplicationContext;
import org.xml.sax.SAXException;

import lombok.Getter;
import lombok.Setter;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IConfigurable;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.Resource;
import nl.nn.adapterframework.util.TransformerPool;

/**
 * XSLT provider listener for the Test Tool.
 *
 * @author Jaco de Groot
 */
public class XsltProviderListener implements IConfigurable, AutoCloseable {
	private @Getter ClassLoader configurationClassLoader = Thread.currentThread().getContextClassLoader();
	private @Getter @Setter ApplicationContext applicationContext;
	private @Getter @Setter String name;

	private String filename;
	private int xsltVersion=0; // set to 0 for auto detect.
	private boolean namespaceAware = true;
	private TransformerPool transformerPool = null;
	private String result;

	@Override
	public void configure() throws ConfigurationException {
		if(filename == null) {
			throw new ConfigurationException("Could not find filename property for " + getName());
		}
		try {
			Resource stylesheet = Resource.getResource(this, filename);
			if(stylesheet == null) {
				throw new ConfigurationException("Could not find file ["+filename+"]");
			}
			transformerPool = TransformerPool.getInstance(stylesheet, xsltVersion);
		} catch (Exception e) {
			throw new ConfigurationException("Exception creating transformer pool for file '" + filename + "': " + e.getMessage(), e);
		}
	}

	public void processRequest(String message, Map parameters) throws ListenerException {
		try {
			result = transformerPool.transform(message, parameters, namespaceAware);
		} catch (IOException e) {
			throw new ListenerException("IOException transforming xml: " + e.getMessage(), e);
		} catch (TransformerException e) {
			throw new ListenerException("TransformerException transforming xml: " + e.getMessage(), e);
		} catch (SAXException e) {
			throw new ListenerException("DomBuilderException transforming xml: " + e.getMessage(), e);
		}
	}

	public String getResult() {
		String result = this.result;
		this.result = null;
		return result;
	}

	public void setFilename(String filename) {
		this.filename = filename;
	}

	public void setXsltVersion(int xsltVersion) {
		this.xsltVersion=xsltVersion;
	}

	/**
	 * @deprecated Please remove setting of xslt2, it will be auto detected. Or use xsltVersion.
	 */
	@Deprecated
	public void setXslt2(boolean b) {
		xsltVersion=b?2:1;
	}

	/**
	 * Set namespace aware.
	 */
	public void setNamespaceAware(boolean namespaceAware) {
		this.namespaceAware = namespaceAware;
	}

	@Override
	public void close() throws Exception {
		if(getResult() != null) {
			throw new ConfigurationException("Found remaining message on XsltProviderListener ["+getName()+"]");
		}
	}

}
