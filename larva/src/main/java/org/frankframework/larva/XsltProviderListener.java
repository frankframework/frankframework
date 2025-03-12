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
package org.frankframework.larva;

import java.io.IOException;
import java.util.Map;

import javax.xml.transform.TransformerException;

import org.springframework.context.ApplicationContext;
import org.xml.sax.SAXException;

import lombok.Getter;
import lombok.Setter;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.FrankElement;
import org.frankframework.core.IConfigurable;
import org.frankframework.core.ListenerException;
import org.frankframework.core.Resource;
import org.frankframework.stream.Message;
import org.frankframework.util.TransformerPool;

/**
 * XSLT provider listener for the Test Tool.
 *
 * @author Jaco de Groot
 */
public class XsltProviderListener implements IConfigurable, AutoCloseable, FrankElement {
	private final @Getter ClassLoader configurationClassLoader = Thread.currentThread().getContextClassLoader();
	private @Getter @Setter ApplicationContext applicationContext;
	private @Getter @Setter String name;

	private @Setter String filename;
	private @Setter int xsltVersion=0; // set to 0 for auto-detect.
	private @Setter boolean namespaceAware = true;
	private TransformerPool transformerPool = null;
	private Message result;

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

	public void processRequest(Message message, Map<String, Object> parameters) throws ListenerException {
		try {
			result = new Message(transformerPool.transformToString(message, parameters, namespaceAware));
		} catch (IOException e) {
			throw new ListenerException("IOException transforming xml: " + e.getMessage(), e);
		} catch (TransformerException e) {
			throw new ListenerException("TransformerException transforming xml: " + e.getMessage(), e);
		} catch (SAXException e) {
			throw new ListenerException("DomBuilderException transforming xml: " + e.getMessage(), e);
		}
	}

	public Message getResult() {
		Message result = this.result;
		this.result = null;
		return result;
	}

	/**
	 * @deprecated Please remove setting of xslt2, it will be auto-detected. Or use xsltVersion.
	 */
	@Deprecated
	public void setXslt2(boolean b) {
		xsltVersion=b?2:1;
	}

	@Override
	public void close() throws Exception {
		if(getResult() != null) {
			throw new ConfigurationException("Found remaining message on XsltProviderListener ["+getName()+"]");
		}
	}

}
