/*
   Copyright 2013, 2016 Nationale-Nederlanden

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
package nl.nn.adapterframework.soap;

import java.io.IOException;
import java.util.Map;

import javax.xml.soap.SOAPException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeLine;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.core.PipeStartException;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.pipes.FixedForwardPipe;
import nl.nn.adapterframework.util.DomBuilderException;
import nl.nn.adapterframework.util.TransformerPool;
import nl.nn.adapterframework.util.XmlUtils;

import org.apache.commons.lang.StringUtils;

/**
 * Pipe to wrap or unwrap a message from/into a SOAP Envelope.
 *
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>{@link #setName(String) name}</td><td>name of the Pipe</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setDirection(String) direction}</td><td>either <code>wrap</code> or <code>unwrap</code></td><td>wrap</td></tr>
 * <tr>
 *   <td>{@link #setSoapHeaderSessionKey(String) soapHeaderSessionKey}</td>
 *   <td>
 *     <table>
 *       <tr><td><code>direction=unwrap</code></td><td>name of the session key to store the content of the SOAP Header from the request in</td></tr>
 *       <tr><td><code>direction=wrap</code></td><td>name of the session key to retrieve the content of the SOAP Header for the response from. If the attribute soapHeaderStyleSheet is not empty, the attribute soapHeaderStyleSheet precedes this attribute</td></tr>
 *     </table>
 *   </td>
 *   <td>soapHeader when inputWrapper of pipeline and direction=unwrap, empty otherwise</td>
 * </tr>
 * <tr><td>{@link #setEncodingStyle(String) encodingStyle}</td><td>the encodingStyle to be set in the SOAP Header</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setServiceNamespace(String) serviceNamespace}</td><td>the namespace of the message sent. Identifies the service to be called. May be overriden by an actual namespace setting in the message to be sent</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setSoapHeaderStyleSheet(String) soapHeaderStyleSheet}</td><td>(only used when <code>direction=wrap</code>) stylesheet to create the content of the SOAP Header. As input for this stylesheet a dummy xml string is used. Note: outputType=<code>xml</code> and xslt2=<code>true</code></td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setSoapBodyStyleSheet(String) soapBodyStyleSheet}</td><td>(only used when <code>direction=wrap</code>) stylesheet to apply to the input message. Note: outputType=<code>xml</code> and xslt2=<code>true</code></td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setRemoveOutputNamespaces(boolean) removeOutputNamespaces}</td><td>(only used when <code>direction=unwrap</code>) when <code>true</code>, namespaces (and prefixes) in the content of the SOAP Body are removed</td><td>false</td></tr>
 * <tr><td>{@link #setRemoveUnusedOutputNamespaces(boolean) removeUnusedOutputNamespaces}</td><td>(only used when <code>direction=unwrap</code> and <code>removeOutputNamespaces=false</code>) when <code>true</code>, unused namespaces in the content of the SOAP Body are removed</td><td>true</td></tr>
 * <tr><td>{@link #setOutputNamespace(String) outputNamespace}</td><td>(only used when <code>direction=wrap</code>) when not empty, this namespace is added to the root element in the SOAP Body</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setSoapNamespace(String) soapNamespace}</td><td>(only used when <code>direction=wrap</code>) namespace of the SOAP Envelope</td><td>http://schemas.xmlsoap.org/soap/envelope/</td></tr>
 * <tr><td>{@link #setRoot(String) root}</td><td>when not empty, the root element in the SOAP Body is changed to this value</td><td>&nbsp;</td></tr>
 * <table>
 * <table border="1">
 * <tr><th>nested elements</th><th>description</th></tr>
 * <tr><td>{@link nl.nn.adapterframework.parameters.Parameter param}</td><td>any parameters defined on the pipe will be applied to the created transformer</td></tr>
 * </table>
 * </p>
 * <p><b>Exits:</b>
 * <table border="1">
 * <tr><th>state</th><th>condition</th></tr>
 * <tr><td>"success"</td><td>default</td></tr>
 * <tr><td><i>{@link #setForwardName(String) forwardName}</i></td><td>if specified</td></tr>
 * </table>
 * </p>
 * @author Peter Leeuwenburgh
 */
public class SoapWrapperPipe extends FixedForwardPipe {
	protected static final String DEFAULT_SOAP_HEADER_SESSION_KEY = "soapHeader";

	private String direction = "wrap";
	private String soapHeaderSessionKey = null;
	private String encodingStyle = null;
	private String serviceNamespace = null;
	private String soapHeaderStyleSheet = null;
	private String soapBodyStyleSheet = null;
	private boolean removeOutputNamespaces = false;
	private boolean removeUnusedOutputNamespaces = true;
	private String outputNamespace = null;
	private String soapNamespace = null;
	private String root = null;

	private SoapWrapper soapWrapper = null;

	private TransformerPool soapHeaderTp = null;
	private TransformerPool soapBodyTp = null;
	private TransformerPool removeOutputNamespacesTp = null;
	private TransformerPool removeUnusedOutputNamespacesTp = null;
	private TransformerPool outputNamespaceTp = null;
	private TransformerPool rootTp = null;

	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		soapWrapper = SoapWrapper.getInstance();
		if ("unwrap".equalsIgnoreCase(getDirection()) && PipeLine.INPUT_WRAPPER_NAME.equals(getName())) {
			if (StringUtils.isEmpty(getSoapHeaderSessionKey())) {
				setSoapHeaderSessionKey(DEFAULT_SOAP_HEADER_SESSION_KEY);
			}
		}
		if (StringUtils.isNotEmpty(getSoapHeaderStyleSheet())) {
			soapHeaderTp = TransformerPool.configureTransformer0(getLogPrefix(null), classLoader, null, null, getSoapHeaderStyleSheet(), "xml", false, getParameterList(), true);
		}
		if (StringUtils.isNotEmpty(getSoapBodyStyleSheet())) {
			soapBodyTp = TransformerPool.configureTransformer0(getLogPrefix(null), classLoader, null, null, getSoapBodyStyleSheet(), "xml", false, getParameterList(), true);
		}
		try {
			if (isRemoveOutputNamespaces()) {
				String removeOutputNamespaces_xslt = XmlUtils.makeRemoveNamespacesXslt(true, false);
				removeOutputNamespacesTp = new TransformerPool(removeOutputNamespaces_xslt);
			}
			if (isRemoveUnusedOutputNamespaces() && !isRemoveOutputNamespaces()) {
				String removeUnusedOutputNamespaces_xslt = XmlUtils.makeRemoveUnusedNamespacesXslt2(true, false);
				removeUnusedOutputNamespacesTp = new TransformerPool(removeUnusedOutputNamespaces_xslt, true);
			}
			if (StringUtils.isNotEmpty(getOutputNamespace())) {
				String outputNamespace_xslt = XmlUtils.makeAddRootNamespaceXslt(getOutputNamespace(), true, false);
				outputNamespaceTp = new TransformerPool(outputNamespace_xslt);
			}
			if (StringUtils.isNotEmpty(getRoot())) {
				String root_xslt = XmlUtils.makeChangeRootXslt(getRoot(), true, false);
				rootTp = new TransformerPool(root_xslt);
			}
		} catch (TransformerConfigurationException e) {
			throw new ConfigurationException(getLogPrefix(null) + "cannot create transformer", e);
		}
	}

    @Override
	public void start() throws PipeStartException {
		super.start();
		if (soapHeaderTp != null) {
			try {
				soapHeaderTp.open();
			} catch (Exception e) {
				throw new PipeStartException(getLogPrefix(null)+"cannot start SOAP Header TransformerPool", e);
			}
		}
		if (soapBodyTp != null) {
			try {
				soapBodyTp.open();
			} catch (Exception e) {
				throw new PipeStartException(getLogPrefix(null)+"cannot start SOAP Body TransformerPool", e);
			}
		}
		if (removeOutputNamespacesTp != null) {
			try {
				removeOutputNamespacesTp.open();
			} catch (Exception e) {
				throw new PipeStartException(getLogPrefix(null)+"cannot start Remove Output Namespaces TransformerPool", e);
			}
		}
		if (removeUnusedOutputNamespacesTp != null) {
			try {
				removeUnusedOutputNamespacesTp.open();
			} catch (Exception e) {
				throw new PipeStartException(getLogPrefix(null)+"cannot start Remove Unused Output Namespaces TransformerPool", e);
			}
		}
		if (outputNamespaceTp != null) {
			try {
				outputNamespaceTp.open();
			} catch (Exception e) {
				throw new PipeStartException(getLogPrefix(null)+"cannot start Output Namespace TransformerPool", e);
			}
		}
		if (rootTp != null) {
			try {
				rootTp.open();
			} catch (Exception e) {
				throw new PipeStartException(getLogPrefix(null)+"cannot start Root TransformerPool", e);
			}
		}
	}

    @Override
	public void stop() {
		super.stop();
		if (soapHeaderTp != null) {
			soapHeaderTp.close();
		}
		if (soapBodyTp != null) {
			soapBodyTp.close();
		}
		if (removeOutputNamespacesTp != null) {
			removeOutputNamespacesTp.close();
		}
		if (removeUnusedOutputNamespacesTp != null) {
			removeUnusedOutputNamespacesTp.close();
		}
		if (outputNamespaceTp != null) {
			outputNamespaceTp.close();
		}
		if (rootTp != null) {
			rootTp.close();
		}
	}

    @Override
	public PipeRunResult doPipe(Object input, IPipeLineSession session) throws PipeRunException {
		String result;
		try {
			if ("wrap".equalsIgnoreCase(getDirection())) {
				String payload = input.toString();
				if (rootTp != null) {
					payload = rootTp.transform(payload, null, true);
				}
				if (outputNamespaceTp != null) {
					payload = outputNamespaceTp.transform(payload, null, true);
				}
				ParameterResolutionContext prc = null;
				Map parameterValues = null;
				if (getParameterList()!=null && (soapHeaderTp != null || soapBodyTp != null)) {
					prc = new ParameterResolutionContext(payload, session);
					parameterValues = prc.getValueMap(getParameterList());
				}
				String soapHeader = null;
				if (soapHeaderTp != null) {
					soapHeader = soapHeaderTp.transform(prc.getInputSource(), parameterValues);
				} else {
					if (StringUtils.isNotEmpty(getSoapHeaderSessionKey())) {
						soapHeader = (String) session.get(getSoapHeaderSessionKey());
					}
				}
				if (soapBodyTp != null) {
					payload = soapBodyTp.transform(prc.getInputSource(), parameterValues);
				}

				result = wrapMessage(payload, soapHeader);
			} else {
				result = unwrapMessage(input.toString());
				if (StringUtils.isEmpty(result)) {
					throw new PipeRunException(this, getLogPrefix(session) + "SOAP Body is empty or message is not a SOAP Message");
				}
				if (soapWrapper.getFaultCount(input.toString()) > 0) {
					throw new PipeRunException(this, getLogPrefix(session) + "SOAP Body contains SOAP Fault");
				}
				if (StringUtils.isNotEmpty(getSoapHeaderSessionKey())) {
					String soapHeader = soapWrapper.getHeader(input.toString());
					session.put(getSoapHeaderSessionKey(), soapHeader);
				}
				if (removeOutputNamespacesTp != null) {
					result = removeOutputNamespacesTp.transform(result, null, true);
				}
				if (removeUnusedOutputNamespacesTp != null) {
					result = removeUnusedOutputNamespacesTp.transform(result, null, true);
				}
				if (rootTp != null) {
					result = rootTp.transform(result, null, true);
				}
			}
		} catch (Exception t) {
			throw new PipeRunException(this, getLogPrefix(session) + " Unexpected exception during (un)wrapping ", t);
		}
		return new PipeRunResult(getForward(), result);
	}

	protected String unwrapMessage(String messageText) throws DomBuilderException, TransformerException, IOException, SOAPException {
		return soapWrapper.getBody(messageText);
	}

	protected String wrapMessage(String message, String soapHeader) throws DomBuilderException, TransformerException, IOException {
		return soapWrapper.putInEnvelope(message, getEncodingStyle(), getServiceNamespace(), soapHeader, null, getSoapNamespace());
	}

	public String getDirection() {
		return direction;
	}
	public void setDirection(String string) {
		direction = string;
	}

	public void setSoapHeaderSessionKey(String string) {
		soapHeaderSessionKey = string;
	}
	public String getSoapHeaderSessionKey() {
		return soapHeaderSessionKey;
	}

	public void setEncodingStyle(String string) {
		encodingStyle = string;
	}
	public String getEncodingStyle() {
		return encodingStyle;
	}

	public void setServiceNamespace(String string) {
		serviceNamespace = string;
	}
	public String getServiceNamespace() {
		return serviceNamespace;
	}

	public void setSoapHeaderStyleSheet(String string){
		this.soapHeaderStyleSheet = string;
	}
	public String getSoapHeaderStyleSheet() {
		return soapHeaderStyleSheet;
	}

	public void setSoapBodyStyleSheet(String string){
		this.soapBodyStyleSheet = string;
	}
	public String getSoapBodyStyleSheet() {
		return soapBodyStyleSheet;
	}

	public void setRemoveOutputNamespaces(boolean b) {
		removeOutputNamespaces = b;
	}
	public boolean isRemoveOutputNamespaces() {
		return removeOutputNamespaces;
	}

	public void setRemoveUnusedOutputNamespaces(boolean b) {
		removeUnusedOutputNamespaces = b;
	}
	public boolean isRemoveUnusedOutputNamespaces() {
		return removeUnusedOutputNamespaces;
	}

	public void setOutputNamespace(String string) {
		outputNamespace = string;
	}
	public String getOutputNamespace() {
		return outputNamespace;
	}

	public void setSoapNamespace(String string) {
		soapNamespace = string;
	}
	public String getSoapNamespace() {
		return soapNamespace;
	}

	public void setRoot(String string) {
		root = string;
	}
	public String getRoot() {
		return root;
	}
}
