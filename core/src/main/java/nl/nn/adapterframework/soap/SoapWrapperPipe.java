/*
   Copyright 2013, 2016, 2020 Nationale-Nederlanden, 2020-2021 WeAreFrank!

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

import org.apache.commons.lang3.StringUtils;
import org.xml.sax.SAXException;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.IWrapperPipe;
import nl.nn.adapterframework.core.PipeLine;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.core.PipeStartException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.doc.IbisDoc;
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
public class SoapWrapperPipe extends FixedForwardPipe implements IWrapperPipe {
	protected static final String DEFAULT_SOAP_HEADER_SESSION_KEY = "soapHeader";
	protected static final String DEFAULT_SOAP_NAMESPACE_SESSION_KEY = "soapNamespace";

	protected static final SoapVersion DEFAULT_SOAP_VERSION_FOR_WRAPPING = SoapVersion.SOAP11;

	private String direction = "wrap";
	private SoapVersion soapVersion = SoapVersion.AUTO;
	private String soapNamespace = null;
	private String soapNamespaceSessionKey = null;
	private String soapHeaderSessionKey = null;
	private String encodingStyle = null;
	private String serviceNamespace = null;
	private String soapHeaderStyleSheet = null;
	private String soapBodyStyleSheet = null;
	private boolean removeOutputNamespaces = false;
	private boolean removeUnusedOutputNamespaces = true;
	private String outputNamespace = null;
	private String root = null;
	private boolean ignoreSoapFault = false;
	private boolean allowPlainXml = false;
	
	private String wssAuthAlias;
	private String wssUserName;
	private String wssPassword;
	private boolean wssPasswordDigest = true;

	private String onlyIfSessionKey;
	private String onlyIfValue;
	private String unlessSessionKey;
	private String unlessValue;

	private CredentialFactory wssCredentialFactory = null;

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
			if (StringUtils.isEmpty(getSoapNamespaceSessionKey())) {
				setSoapNamespaceSessionKey(DEFAULT_SOAP_NAMESPACE_SESSION_KEY);
			}
		}
		if ("wrap".equalsIgnoreCase(getDirection()) && PipeLine.OUTPUT_WRAPPER_NAME.equals(getName())) {
			if (StringUtils.isEmpty(getSoapNamespaceSessionKey())) {
				setSoapNamespaceSessionKey(DEFAULT_SOAP_NAMESPACE_SESSION_KEY);
			}
		}
		if (getSoapVersionEnum()==null) {
			soapVersion=SoapVersion.AUTO;
		}
		if (StringUtils.isNotEmpty(getSoapHeaderStyleSheet())) {
			soapHeaderTp = TransformerPool.configureStyleSheetTransformer(getLogPrefix(null), this, getSoapHeaderStyleSheet(), 0);
		}
		if (StringUtils.isNotEmpty(getSoapBodyStyleSheet())) {
			soapBodyTp = TransformerPool.configureStyleSheetTransformer(getLogPrefix(null), this, getSoapBodyStyleSheet(), 0);
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
			log.debug(getLogPrefix(null) + "created CredentialFactory for username=[" + wssCredentialFactory.getUsername()+"]");
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
	public PipeRunResult doPipe(Message message, PipeLineSession session) throws PipeRunException {
		if (StringUtils.isNotEmpty(getOnlyIfSessionKey())) {
			Object onlyIfActualValue = session.get(getOnlyIfSessionKey());
			if (onlyIfActualValue==null || StringUtils.isNotEmpty(getOnlyIfValue()) && !getOnlyIfValue().equals(onlyIfActualValue)) {
				if (log.isDebugEnabled()) log.debug("onlyIfSessionKey ["+getOnlyIfSessionKey()+"] value ["+onlyIfActualValue+"]: not found or not equal to value ["+getOnlyIfValue()+"]");
				return new PipeRunResult(getSuccessForward(), message);
			}
		}
		if (StringUtils.isNotEmpty(getUnlessSessionKey())) {
			Object unlessActualValue = session.get(getUnlessSessionKey());
			if (unlessActualValue!=null && (StringUtils.isEmpty(getUnlessValue()) || getUnlessValue().equals(unlessActualValue))) {
				if (log.isDebugEnabled()) log.debug("unlessSessionKey ["+getUnlessSessionKey()+"] value ["+unlessActualValue+"]: not found or not equal to value ["+getUnlessValue()+"]");
				return new PipeRunResult(getSuccessForward(), message);
			}
		}
		Message result;
		try {
			if ("wrap".equalsIgnoreCase(getDirection())) {
				Message payload = message;
				if (rootTp != null) {
					payload = new Message(rootTp.transform(payload.asSource()));
				}
				if (outputNamespaceTp != null) {
					payload = new Message(outputNamespaceTp.transform(payload.asSource()));
				}
				Map<String,Object> parameterValues = null;
				if (getParameterList()!=null && (soapHeaderTp != null || soapBodyTp != null)) {
					payload.preserve();
					parameterValues = getParameterList().getValues(payload, session).getValueMap();
				}
				String soapHeader = null;
				if (soapHeaderTp != null) {
					payload.preserve();
					soapHeader = soapHeaderTp.transform(payload, parameterValues);
				} else {
					if (StringUtils.isNotEmpty(getSoapHeaderSessionKey())) {
						soapHeader = session.getMessage(getSoapHeaderSessionKey()).asString();
					}
				}
				if (soapBodyTp != null) {
					payload = new Message(soapBodyTp.transform(payload, parameterValues));
				}

				result = wrapMessage(payload, soapHeader, session);
			} else { // direction==unwrap
				message.preserve();
				result = unwrapMessage(message, session);
				if (Message.isEmpty(result)) {
					throw new PipeRunException(this, getLogPrefix(session) + "SOAP Body is empty or message is not a SOAP Message");
				}
				if (!isIgnoreSoapFault() && soapWrapper.getFaultCount(message) > 0) {
					throw new PipeRunException(this, getLogPrefix(session) + "SOAP Body contains SOAP Fault");
				}
				if (StringUtils.isNotEmpty(getSoapHeaderSessionKey())) {
					String soapHeader = soapWrapper.getHeader(message);
					session.put(getSoapHeaderSessionKey(), soapHeader);
				}
				if (removeOutputNamespacesTp != null) {
					result = new Message(removeOutputNamespacesTp.transform(result.asSource()));
				}
				if (removeUnusedOutputNamespacesTp != null) {
					result = new Message(removeUnusedOutputNamespacesTp.transform(result.asSource()));
				}
				if (rootTp != null) {
					result = new Message(rootTp.transform(result.asSource()));
				}
			}
		} catch (Exception t) {
			throw new PipeRunException(this, getLogPrefix(session) + " Unexpected exception during (un)wrapping ", t);
		}
		return new PipeRunResult(getSuccessForward(), result);
	}

	protected String determineSoapNamespace(PipeLineSession session) throws IOException {
		String soapNamespace = getSoapNamespace();
		if (StringUtils.isEmpty(soapNamespace)) {
			String savedSoapNamespace = session.getMessage(getSoapNamespaceSessionKey()).asString();
			if (StringUtils.isNotEmpty(savedSoapNamespace)) {
				soapNamespace = savedSoapNamespace;
			} else {
				SoapVersion soapVersion = getSoapVersionEnum();
				if (soapVersion==SoapVersion.AUTO) {
					soapVersion=DEFAULT_SOAP_VERSION_FOR_WRAPPING;
				}
				soapNamespace = soapVersion.namespace;
			}
		}
		return soapNamespace;
	}
	
	protected Message unwrapMessage(Message message, PipeLineSession session) throws SAXException, TransformerException, IOException, SOAPException {
		return soapWrapper.getBody(message, isAllowPlainXml(), session, getSoapNamespaceSessionKey());
	}

	protected Message wrapMessage(Message message, String soapHeader, PipeLineSession session) throws DomBuilderException, TransformerException, IOException, SenderException {
		String soapNamespace = determineSoapNamespace(session);
		if (soapNamespace==null) {
			return message;
		}
		return soapWrapper.putInEnvelope(message, getEncodingStyle(), getServiceNamespace(), soapHeader, null, soapNamespace, wssCredentialFactory, isWssPasswordDigest());
	}

	@IbisDoc({"1", "Either <code>wrap</code> or <code>unwrap</code>", "wrap"})
	public void setDirection(String string) {
		direction = string;
	}
	public String getDirection() {
		return direction;
	}

	@IbisDoc({"2", "Soap version to use: one of <code>soap1.1</code>, <code>soap1.2</code>, <code>none</code> or <code>auto</code>. <code>none</code> means that no wrapping or unwrapping will be done, <code>auto</code> means auto-detect", "auto"})
	public void setSoapVersion(String string) {
		soapVersion = SoapVersion.getSoapVersion(string);
	}
	public SoapVersion getSoapVersionEnum() {
		return soapVersion;
	}

	@IbisDoc({"3", "Key of session variable to store auto detected soapNamespace", "soapVersion"})
	public void setSoapNamespaceSessionKey(String string) {
		soapNamespaceSessionKey = string;
	}
	public String getSoapNamespaceSessionKey() {
		return soapNamespaceSessionKey;
	}

	@IbisDoc({"4", "(only used when <code>direction=wrap</code>) Namespace of the soap envelope", "auto determined from soapVersion"})
	public void setSoapNamespace(String string) {
		soapNamespace = string;
	}
	public String getSoapNamespace() {
		return soapNamespace;
	}

	@IbisDoc({"5", "Key of session variable to store soap header", DEFAULT_SOAP_HEADER_SESSION_KEY+", when direction is 'unwrap'"})
	public void setSoapHeaderSessionKey(String string) {
		soapHeaderSessionKey = string;
	}
	public String getSoapHeaderSessionKey() {
		return soapHeaderSessionKey;
	}

	@IbisDoc({"6", "The encodingstyle to be set in the soap header", ""})
	public void setEncodingStyle(String string) {
		encodingStyle = string;
	}
	public String getEncodingStyle() {
		return encodingStyle;
	}

	@IbisDoc({"7", "The default for the namespace of the message sent. Identifies the service to be called. May be overriden by an actual namespace setting in the message to be sent", ""})
	public void setServiceNamespace(String string) {
		serviceNamespace = string;
	}
	public String getServiceNamespace() {
		return serviceNamespace;
	}

	@IbisDoc({"8", "(only used when <code>direction=wrap</code>) Stylesheet to create the content of the soap header. As input for this stylesheet a dummy xml string is used. Note: outputtype=<code>xml</code> and xslt2=<code>true</code>", ""})
	public void setSoapHeaderStyleSheet(String string){
		this.soapHeaderStyleSheet = string;
	}
	public String getSoapHeaderStyleSheet() {
		return soapHeaderStyleSheet;
	}

	@IbisDoc({"9", "(only used when <code>direction=wrap</code>) Stylesheet to apply to the input message. Note: outputtype=<code>xml</code> and xslt2=<code>true</code>", ""})
	public void setSoapBodyStyleSheet(String string){
		this.soapBodyStyleSheet = string;
	}
	public String getSoapBodyStyleSheet() {
		return soapBodyStyleSheet;
	}

	@IbisDoc({"10", "(only used when <code>direction=unwrap</code>) If <code>true</code>, namespaces (and prefixes) in the content of the soap body are removed", "false"})
	public void setRemoveOutputNamespaces(boolean b) {
		removeOutputNamespaces = b;
	}
	public boolean isRemoveOutputNamespaces() {
		return removeOutputNamespaces;
	}

	@IbisDoc({"11", "(only used when <code>direction=unwrap</code> and <code>removeoutputnamespaces=false</code>) If <code>true</code>, unused namespaces in the content of the soap body are removed", "true"})
	public void setRemoveUnusedOutputNamespaces(boolean b) {
		removeUnusedOutputNamespaces = b;
	}
	public boolean isRemoveUnusedOutputNamespaces() {
		return removeUnusedOutputNamespaces;
	}

	@IbisDoc({"12", "(only used when <code>direction=wrap</code>) If not empty, this namespace is added to the root element in the soap body", ""})
	public void setOutputNamespace(String string) {
		outputNamespace = string;
	}
	public String getOutputNamespace() {
		return outputNamespace;
	}

	@IbisDoc({"13", "If not empty, the root element in the soap body is changed to this value", ""})
	public void setRoot(String string) {
		root = string;
	}
	public String getRoot() {
		return root;
	}

	@IbisDoc({"14", "(only used when <code>direction=unwrap</code>) If <code>false</code> and the soap body contains a soap fault, a PipeRunException is thrown", "false"})
	public void setIgnoreSoapFault(boolean b) {
		ignoreSoapFault = b;
	}
	public boolean isIgnoreSoapFault() {
		return ignoreSoapFault;
	}

	@IbisDoc({"15", "For direction=<code>unwrap</code> only: if true, allow unwrapped xml too", "false"})
	public void setAllowPlainXml(boolean allowPlainXml) {
		this.allowPlainXml = allowPlainXml;
	}
	public boolean isAllowPlainXml() {
		return allowPlainXml;
	}

	@IbisDoc({"16", "alias used to obtain credentials for authentication to WebServiceSecurity", ""})
	public void setWssAuthAlias(String string) {
		wssAuthAlias = string;
	}
	public String getWssAuthAlias() {
		return wssAuthAlias;
	}

	@IbisDoc({"17", "Default username for WebServiceSecurity", " "})
	public void setWssUserName(String string) {
		wssUserName = string;
	}
	public String getWssUserName() {
		return wssUserName;
	}

	@IbisDoc({"18", "Default password for WebServiceSecurity", " "})
	public void setWssPassword(String string) {
		wssPassword = string;
	}
	public String getWssPassword() {
		return wssPassword;
	}

	@IbisDoc({"19", "If true, the password is sent digested; Otherwise it is sent in clear text", "true"})
	public void setWssPasswordDigest(boolean b) {
		wssPasswordDigest = b;
	}
	public boolean isWssPasswordDigest() {
		return wssPasswordDigest;
	}

	@IbisDoc({"20", "Key of session variable to check if action must be executed. The wrap or unwrap action is only executed if the session variable exists", ""})
	public void setOnlyIfSessionKey(String onlyIfSessionKey) {
		this.onlyIfSessionKey = onlyIfSessionKey;
	}
	public String getOnlyIfSessionKey() {
		return onlyIfSessionKey;
	}

	@IbisDoc({"21", "Value of session variable 'onlyIfSessionKey' to check if action must be executed. The wrap or unwrap action is only executed if the session variable has the specified value", ""})
	public void setOnlyIfValue(String onlyIfValue) {
		this.onlyIfValue = onlyIfValue;
	}
	public String getOnlyIfValue() {
		return onlyIfValue;
	}

	@IbisDoc({"22", "Key of session variable to check if action must be executed. The wrap or unwrap action is not executed if the session variable exists", ""})
	public void setUnlessSessionKey(String unlessSessionKey) {
		this.unlessSessionKey = unlessSessionKey;
	}
	public String getUnlessSessionKey() {
		return unlessSessionKey;
	}

	@IbisDoc({"23", "Value of session variable 'unlessSessionKey' to check if action must be executed. The wrap or unwrap action is not executed if the session variable has the specified value", ""})
	public void setUnlessValue(String unlessValue) {
		this.unlessValue = unlessValue;
	}
	public String getUnlessValue() {
		return unlessValue;
	}

}
