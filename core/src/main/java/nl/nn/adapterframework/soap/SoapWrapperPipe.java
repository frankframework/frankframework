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
import javax.xml.transform.TransformerException;

import org.apache.commons.lang.StringUtils;
import org.xml.sax.SAXException;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeLine;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.core.PipeStartException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.pipes.FixedForwardPipe;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.CredentialFactory;
import nl.nn.adapterframework.util.DomBuilderException;
import nl.nn.adapterframework.util.TransformerPool;
import nl.nn.adapterframework.util.XmlUtils;

/**
 * Pipe to wrap or unwrap a message from/into a SOAP Envelope.
 *
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
 * <tr><td>{@link #setIgnoreSoapFault(boolean) ignoreSoapFault}</td><td>(only used when <code>direction=unwrap</code>) when <code>false</code> and the SOAP Body contains a SOAP Fault, a PipeRunException is thrown</td><td>false</td></tr>
 * <tr><td>{@link #setWssAuthAlias(String) wssAuthAlias}</td><td>alias used to obtain credentials for authentication to Web Services Security</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setWssUserName(String) wssUserName}</td><td>&nbsp;</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setWssPassword(String) wssPassword}</td><td>&nbsp;</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setWssPasswordDigest(boolean) wssPasswordDigest}</td><td>when true, the password is sent digested. Otherwise it is sent in clear text</td><td>true</td></tr>
 * <table>
 * <table border="1">
 * <tr><th>nested elements</th><th>description</th></tr>
 * <tr><td>{@link nl.nn.adapterframework.parameters.Parameter param}</td><td>any parameters defined on the pipe will be applied to the created transformer</td></tr>
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
	private boolean ignoreSoapFault = false;

	private CredentialFactory wssCredentialFactory = null;
	private String wssAuthAlias;
	private String wssUserName;
	private String wssPassword;
	private boolean wssPasswordDigest = true;

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
			soapHeaderTp = TransformerPool.configureStyleSheetTransformer(getLogPrefix(null), getConfigurationClassLoader(), getSoapHeaderStyleSheet(), 0);
		}
		if (StringUtils.isNotEmpty(getSoapBodyStyleSheet())) {
			soapBodyTp = TransformerPool.configureStyleSheetTransformer(getLogPrefix(null), getConfigurationClassLoader(), getSoapBodyStyleSheet(), 0);
		}
		if (isRemoveOutputNamespaces()) {
			removeOutputNamespacesTp = XmlUtils.getRemoveNamespacesTransformerPool(true, false);
		}
		if (isRemoveUnusedOutputNamespaces() && !isRemoveOutputNamespaces()) {
			removeUnusedOutputNamespacesTp = XmlUtils.getRemoveUnusedNamespacesXslt2TransformerPool(true, false);
		}
		if (StringUtils.isNotEmpty(getOutputNamespace())) {
			outputNamespaceTp = XmlUtils.getAddRootNamespaceTransformerPool(getOutputNamespace(), true, false);
		}
		if (StringUtils.isNotEmpty(getRoot())) {
			rootTp = XmlUtils.getChangeRootTransformerPool(getRoot(), true, false);
		}
		if (StringUtils.isNotEmpty(getWssAuthAlias()) || StringUtils.isNotEmpty(getWssUserName())) {
			wssCredentialFactory = new CredentialFactory(getWssAuthAlias(), getWssUserName(), getWssPassword());
			log.debug(getLogPrefix(null) + "created CredentialFactory for username=["
					+ wssCredentialFactory.getUsername()+"]");
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
	public PipeRunResult doPipe(Message message, IPipeLineSession session) throws PipeRunException {
		String result;
		try {
			if ("wrap".equalsIgnoreCase(getDirection())) {
				String payload = message.asString();
				if (rootTp != null) {
					payload = rootTp.transform(payload, null, true);
				}
				if (outputNamespaceTp != null) {
					payload = outputNamespaceTp.transform(payload, null, true);
				}
				Map<String,Object> parameterValues = null;
				if (getParameterList()!=null && (soapHeaderTp != null || soapBodyTp != null)) {
					parameterValues = getParameterList().getValues(new Message(payload), session).getValueMap();
				}
				String soapHeader = null;
				if (soapHeaderTp != null) {
					soapHeader = soapHeaderTp.transform(payload, parameterValues);
				} else {
					if (StringUtils.isNotEmpty(getSoapHeaderSessionKey())) {
						soapHeader = (String) session.get(getSoapHeaderSessionKey());
					}
				}
				if (soapBodyTp != null) {
					payload = soapBodyTp.transform(payload, parameterValues);
				}

				result = wrapMessage(payload, soapHeader);
			} else {
				result = unwrapMessage(message.asString());
				if (StringUtils.isEmpty(result)) {
					throw new PipeRunException(this, getLogPrefix(session) + "SOAP Body is empty or message is not a SOAP Message");
				}
				if (!isIgnoreSoapFault() && soapWrapper.getFaultCount(message.asString()) > 0) {
					throw new PipeRunException(this, getLogPrefix(session) + "SOAP Body contains SOAP Fault");
				}
				if (StringUtils.isNotEmpty(getSoapHeaderSessionKey())) {
					String soapHeader = soapWrapper.getHeader(message.asString());
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

	protected String unwrapMessage(String messageText) throws SAXException, TransformerException, IOException, SOAPException {
		return soapWrapper.getBody(messageText);
	}

	protected String wrapMessage(String message, String soapHeader) throws DomBuilderException, TransformerException, IOException, SenderException {
		return soapWrapper.putInEnvelope(message, getEncodingStyle(), getServiceNamespace(), soapHeader, null, getSoapNamespace(), wssCredentialFactory, isWssPasswordDigest());
	}

	public String getDirection() {
		return direction;
	}

	@IbisDoc({"either <code>wrap</code> or <code>unwrap</code>", "wrap"})
	public void setDirection(String string) {
		direction = string;
	}

	public void setSoapHeaderSessionKey(String string) {
		soapHeaderSessionKey = string;
	}
	public String getSoapHeaderSessionKey() {
		return soapHeaderSessionKey;
	}

	@IbisDoc({"the encodingstyle to be set in the soap header", ""})
	public void setEncodingStyle(String string) {
		encodingStyle = string;
	}
	public String getEncodingStyle() {
		return encodingStyle;
	}

	@IbisDoc({"the namespace of the message sent. identifies the service to be called. may be overriden by an actual namespace setting in the message to be sent", ""})
	public void setServiceNamespace(String string) {
		serviceNamespace = string;
	}
	public String getServiceNamespace() {
		return serviceNamespace;
	}

	@IbisDoc({"(only used when <code>direction=wrap</code>) stylesheet to create the content of the soap header. as input for this stylesheet a dummy xml string is used. note: outputtype=<code>xml</code> and xslt2=<code>true</code>", ""})
	public void setSoapHeaderStyleSheet(String string){
		this.soapHeaderStyleSheet = string;
	}
	public String getSoapHeaderStyleSheet() {
		return soapHeaderStyleSheet;
	}

	@IbisDoc({"(only used when <code>direction=wrap</code>) stylesheet to apply to the input message. note: outputtype=<code>xml</code> and xslt2=<code>true</code>", ""})
	public void setSoapBodyStyleSheet(String string){
		this.soapBodyStyleSheet = string;
	}
	public String getSoapBodyStyleSheet() {
		return soapBodyStyleSheet;
	}

	@IbisDoc({"(only used when <code>direction=unwrap</code>) when <code>true</code>, namespaces (and prefixes) in the content of the soap body are removed", "false"})
	public void setRemoveOutputNamespaces(boolean b) {
		removeOutputNamespaces = b;
	}
	public boolean isRemoveOutputNamespaces() {
		return removeOutputNamespaces;
	}

	@IbisDoc({"(only used when <code>direction=unwrap</code> and <code>removeoutputnamespaces=false</code>) when <code>true</code>, unused namespaces in the content of the soap body are removed", "true"})
	public void setRemoveUnusedOutputNamespaces(boolean b) {
		removeUnusedOutputNamespaces = b;
	}
	public boolean isRemoveUnusedOutputNamespaces() {
		return removeUnusedOutputNamespaces;
	}

	@IbisDoc({"(only used when <code>direction=wrap</code>) when not empty, this namespace is added to the root element in the soap body", ""})
	public void setOutputNamespace(String string) {
		outputNamespace = string;
	}
	public String getOutputNamespace() {
		return outputNamespace;
	}

	@IbisDoc({"(only used when <code>direction=wrap</code>) namespace of the soap envelope", "http://schemas.xmlsoap.org/soap/envelope/"})
	public void setSoapNamespace(String string) {
		soapNamespace = string;
	}
	public String getSoapNamespace() {
		return soapNamespace;
	}

	@IbisDoc({"when not empty, the root element in the soap body is changed to this value", ""})
	public void setRoot(String string) {
		root = string;
	}
	public String getRoot() {
		return root;
	}

	@IbisDoc({"(only used when <code>direction=unwrap</code>) when <code>false</code> and the soap body contains a soap fault, a piperunexception is thrown", "false"})
	public void setIgnoreSoapFault(boolean b) {
		ignoreSoapFault = b;
	}
	public boolean isIgnoreSoapFault() {
		return ignoreSoapFault;
	}

	@IbisDoc({"", " "})
	public void setWssUserName(String string) {
		wssUserName = string;
	}
	public String getWssUserName() {
		return wssUserName;
	}

	@IbisDoc({"", " "})
	public void setWssPassword(String string) {
		wssPassword = string;
	}
	public String getWssPassword() {
		return wssPassword;
	}

	@IbisDoc({"alias used to obtain credentials for authentication to web services security", ""})
	public void setWssAuthAlias(String string) {
		wssAuthAlias = string;
	}
	public String getWssAuthAlias() {
		return wssAuthAlias;
	}

	@IbisDoc({"when true, the password is sent digested. otherwise it is sent in clear text", "true"})
	public void setWssPasswordDigest(boolean b) {
		wssPasswordDigest = b;
	}
	public boolean isWssPasswordDigest() {
		return wssPasswordDigest;
	}
}
