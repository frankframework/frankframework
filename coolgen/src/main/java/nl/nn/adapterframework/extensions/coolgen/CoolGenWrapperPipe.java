/*
   Copyright 2013, 2016, 2020 Nationale-Nederlanden, 2020, 2022 WeAreFrank!

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
package nl.nn.adapterframework.extensions.coolgen;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyVetoException;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;

import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;

import org.xml.sax.SAXException;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.core.PipeStartException;
import nl.nn.adapterframework.pipes.FixedForwardPipe;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.ClassLoaderUtils;
import nl.nn.adapterframework.util.XmlUtils;
import nl.nn.coolgen.proxy.CoolGenXMLProxy;
import nl.nn.coolgen.proxy.XmlProxyException;

/**
 * Perform the call to a CoolGen proxy with pre- and post transformations.
 *
 * @author Johan Verrips
 */
public class CoolGenWrapperPipe extends FixedForwardPipe {

	private String clientId;
	private String clientPassword;
	private String proxyClassName;
	private String postProcStylesheetName;
	private String preProcStylesheetName;
	private String proxyInputSchema;
	private Transformer preProcTransformer = null;
	private Transformer postProcTransformer = null;
	private Transformer proxyInputFixTransformer = null;


	/**
	 * configure the pipe by creating the required XSLT-transformers using
	 * {@link #createTransformers() }
	 */
	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		createTransformers();
	}

	@Override
	public void start() throws PipeStartException{
		log.debug("creates proxy with class [" + proxyClassName + "]");
		try {
			createProxy(proxyClassName); // just to try if is possible...
		} catch (ConfigurationException e) {
			throw new PipeStartException(e);
		}

	}

	public CoolGenXMLProxy createProxy(String proxyName)
		throws ConfigurationException {
		CoolGenXMLProxy proxy;
		try {
			Class<?> klass = Class.forName(proxyName);
			proxy = (CoolGenXMLProxy) klass.newInstance();
			proxy.setClientId(getClientId());
			proxy.setClientPassword(getClientPassword());
			if (log.isDebugEnabled())
				proxy.setTracing(1);
			else
				proxy.setTracing(0);
		} catch (Exception e) {
			throw new ConfigurationException("could not create proxy ["+proxyName+"]", e);
		}
		return proxy;
	}

	/**
	 * create the required XSLT-transformers.
	 *
	 * <p>Transformers are created using the URIs supplied in:
	 * <ul>
	 * <li>{@link #setPreProcStylesheetName(String) preProcStylesheetName}</li>
	 * <li>{@link #setPostProcStylesheetName(String) postProcStylesheetName}</li>
	 * <li>{@link #setProxyInputSchema(String) proxyInputSchema}</li>
	 * <ul>
	 * For the proxyInputSchema a transformer is created to check the conformance to the schema
	 * the URI supplieds points to.
	 *
	 * Creation of the proxy itself is done in the {@link #start()} method, to be able to recreate it
	 * by restarting the adapter.
	 */
	public void createTransformers() throws ConfigurationException {

		// Create transformers
		if (preProcStylesheetName != null) {
			try {

				URL preprocUrl = ClassLoaderUtils.getResourceURL(this, preProcStylesheetName);

				if (preprocUrl == null)
					throw new ConfigurationException("cannot find resource for preProcTransformer, URL-String [" + preProcStylesheetName + "]");

				log.debug("creating preprocTransformer from URL [" + preprocUrl.toString() + "]");
				preProcTransformer = XmlUtils.createTransformer(preprocUrl);
			} catch (javax.xml.transform.TransformerConfigurationException te) {
				throw new ConfigurationException("got error creating transformer from file [" + preProcStylesheetName + "]", te);
			}
		}
		if (postProcStylesheetName != null) {
			try {

				URL postprocUrl = ClassLoaderUtils.getResourceURL(this, postProcStylesheetName);
				if (postprocUrl == null)
					throw new ConfigurationException("cannot find resource for postProcTransformer, URL-String [" + postProcStylesheetName + "]");

				log.debug("creating postprocTransformer from URL [" + postprocUrl.toString() + "]");
				postProcTransformer = XmlUtils.createTransformer(postprocUrl);
			} catch (javax.xml.transform.TransformerConfigurationException te) {
				throw new ConfigurationException("got error creating transformer from file [" + postProcStylesheetName + "]", te);
			}
		}

		if (proxyInputSchema != null) {
			String stylesheet;
			URL schemaUrl = ClassLoaderUtils.getResourceURL(this, proxyInputSchema);

			if (schemaUrl == null)
				throw new ConfigurationException("cannot find resource for proxyInputSchema, URL-String [" + proxyInputSchema + "]");

			log.debug("creating CoolGenInputViewSchema from URL [" + schemaUrl.toString() + "]");

			// construct a xslt-stylesheet to perform validation to supplied schema
			stylesheet =
					"<xsl:stylesheet version=\"1.0\" xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">";
			stylesheet += "	<xsl:template match=\"/*\">";
			stylesheet += "		<xsl:copy>";
			stylesheet += "			<xsl:attribute name=\"xsi:noNamespaceSchemaLocation\">"
					+ schemaUrl.toString()
					+ "</xsl:attribute>";
			stylesheet += "			<xsl:apply-templates select=\"node()\"/>";
			stylesheet += "		</xsl:copy>";
			stylesheet += "	</xsl:template>";
			stylesheet += "	<xsl:template match=\"@*|node()\">";
			stylesheet += "		<xsl:copy>";
			stylesheet += "			<xsl:apply-templates select=\"@*|node()\"/>";
			stylesheet += "		</xsl:copy>";
			stylesheet += "	</xsl:template>";
			stylesheet += "</xsl:stylesheet>";

			try {
				proxyInputFixTransformer = XmlUtils.createTransformer(stylesheet);
			} catch (javax.xml.transform.TransformerConfigurationException te) {
				throw new ConfigurationException("got error creating transformer from string [" + stylesheet + "]", te);
			}
		}
	}
	/**
	 * Transform the input (optionally), check the conformance to the schema (optionally),
	 * call the required proxy, transform the output (optionally)
	 */
	@Override
	public PipeRunResult doPipe(Message message, PipeLineSession session) throws PipeRunException {

		Writer proxyResult;
		String proxypreProc = null;
		String wrapperResult = "";
		CoolGenXMLProxy proxy;


		ActionListener actionListener = new ActionListener() {
			/**
			 * @see java.awt.event.ActionListener#actionPerformed(ActionEvent)
			 */
			public String errorMessage;
			@Override
			public void actionPerformed(ActionEvent e) {
				errorMessage = e.toString();

			}

			@Override
			public String toString() {
				return errorMessage;
			}
		};

		Source in;

		// TEMPORARY FIX:
		// create proxy before every request, to work around broken connections caused by restarting the comm-bridge
		// should be solved in another way in a more definitive implementation

		try {
			log.info("instantiating proxy ["+proxyClassName+"] as a temporary fix for broken comm-bridge connections");
			proxy = createProxy(proxyClassName);
		} catch (ConfigurationException ce) {
			String msg ="cannot recreate proxy after exception";
			log.error(msg, ce);
			throw new PipeRunException(this, msg, ce);
		}


		proxy.addExceptionListener(actionListener);

		try {
			in = message.asSource();

			if (preProcTransformer != null) {
				proxypreProc = XmlUtils.transformXml(preProcTransformer, in);
				log.debug("preprocessing transformed message into ["+proxypreProc+"]");
			} else
				proxypreProc = message.asString();

			if (proxyInputFixTransformer != null)
				proxypreProc = XmlUtils.transformXml(proxyInputFixTransformer, proxypreProc);

			proxyResult = new StringWriter(10 * 1024);


			// Try to execute the service-preProc as per proxy

			try {
				proxy.clear();
			} catch (PropertyVetoException e) {
				throw new PipeRunException(this, "cannot clear CoolGen proxy", e);
			}
			try {
			proxy.executeXML(new StringReader(proxypreProc), proxyResult);
			proxy.removeExceptionListener(actionListener);

			String err = actionListener.toString();
			if (err != null) {
				// if an error occurs, recreate the proxy and throw an exception
				log.debug("got error, recreating proxy with class ["+ proxyClassName+ "]");
				try {
					proxy = createProxy(proxyClassName);
				} catch (ConfigurationException e) {
					throw new PipeRunException(this,  "cannot recreate proxy ["+proxyClassName+"]", e);
				}
				throw new PipeRunException(this,  "error excuting proxy ["+proxyClassName+"]:"+ err);
			}
			} catch (XmlProxyException xpe) {
				try {
					proxy = createProxy(proxyClassName);
				} catch (ConfigurationException ce) {
					log.error("cannot recreate proxy", ce);
				}
				throw new PipeRunException(this,  "error excecuting proxy", xpe);
			}

			if (postProcTransformer != null) {
				log.debug(" CoolGen proxy returned: [" + proxyResult.toString() + "]");
				wrapperResult = XmlUtils.transformXml(postProcTransformer, proxyResult.toString());
			} else
				wrapperResult = proxyResult.toString();
		} catch (SAXException e) {
			throw new PipeRunException(this, "SAXException excecuting proxy", e);
		} catch (IOException e) {
			throw new PipeRunException(this, "IOException excecuting proxy", e);
		} catch (TransformerException e) {
			throw new PipeRunException(this, "TransformerException excecuting proxy", e);
		}

		return new PipeRunResult(getSuccessForward(),wrapperResult) ;
	}


	/** CICS userId of account perform operation */
	public void setClientId(String newClientId) {
		clientId = newClientId;
	}
	public String getClientId() {
		return clientId;
	}

	/** Password corresponding with userId */
	public void setClientPassword(String newClientPassword) {
		clientPassword = newClientPassword;
	}
	public String getClientPassword() {
		return clientPassword;
	}

	/** Optional URL of XSLT-stylesheet to apply to message before calling proxy */
	public void setPreProcStylesheetName(String newPreProcStylesheetName) {
		preProcStylesheetName = newPreProcStylesheetName;
	}
	public String getPreProcStylesheetName() {
		return preProcStylesheetName;
	}

	/** Optional URL of XSLT-stylesheet to apply to result of proxy */
	public void setPostProcStylesheetName(String newPostProcStylesheetName) {
		postProcStylesheetName = newPostProcStylesheetName;
	}
	public String getPostProcStylesheetName() {
		return postProcStylesheetName;
	}

	public String getProxyClassName() {
		return proxyClassName;
	}
	public String getProxyInputSchema() {
		return proxyInputSchema;
	}

	/** Optional URL of XML-Schema of proxy input message. If specified it is used to validate the input message */
	public void setProxyInputSchema(String newProxyInputSchema) {
		proxyInputSchema = newProxyInputSchema;
	}
	public void setProxyClassName(String newProxyClassName) {
		proxyClassName = newProxyClassName;
	}
}
