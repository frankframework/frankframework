/*
   Copyright 2013, 2016, 2020 Nationale-Nederlanden, 2020-2022 WeAreFrank!

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

import javax.xml.transform.TransformerException;

import org.apache.commons.lang3.StringUtils;
import org.xml.sax.SAXException;

import lombok.Getter;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IWrapperPipe;
import nl.nn.adapterframework.core.PipeLine;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.core.PipeStartException;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.pipes.FixedForwardPipe;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.CredentialFactory;
import nl.nn.adapterframework.util.TransformerPool;
import nl.nn.adapterframework.util.XmlUtils;

/**
 * Pipe to wrap or unwrap a message from/into a SOAP Envelope.
 * 
 * @ff.parameters Any parameters defined on the pipe will be applied to the created transformer.
 * 
 * @author Peter Leeuwenburgh
 */
public class SoapWrapperPipe extends FixedForwardPipe implements IWrapperPipe {
	protected static final String DEFAULT_SOAP_HEADER_SESSION_KEY = "soapHeader";
	protected static final String DEFAULT_SOAP_NAMESPACE_SESSION_KEY = "soapNamespace";

	protected static final SoapVersion DEFAULT_SOAP_VERSION_FOR_WRAPPING = SoapVersion.SOAP11;

	private @Getter Direction direction = Direction.WRAP;
	private @Getter SoapVersion soapVersion = SoapVersion.AUTO;
	private @Getter String soapNamespace = null;
	private @Getter String soapNamespaceSessionKey = null;
	private @Getter String soapHeaderSessionKey = null;
	private @Getter String encodingStyle = null;
	private @Getter String serviceNamespace = null;
	private @Getter String soapHeaderStyleSheet = null;
	private @Getter String soapBodyStyleSheet = null;
	private @Getter boolean removeOutputNamespaces = false;
	private @Getter boolean removeUnusedOutputNamespaces = true;
	private @Getter String outputNamespace = null;
	private @Getter String root = null;
	private @Getter boolean ignoreSoapFault = false;
	private @Getter boolean allowPlainXml = false;
	
	private @Getter String wssAuthAlias;
	private @Getter String wssUserName;
	private @Getter String wssPassword;
	private @Getter boolean wssPasswordDigest = true;

	private @Getter String onlyIfSessionKey;
	private @Getter String onlyIfValue;
	private @Getter String unlessSessionKey;
	private @Getter String unlessValue;

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
		if (getDirection() == Direction.UNWRAP && PipeLine.INPUT_WRAPPER_NAME.equals(getName())) {
			if (StringUtils.isEmpty(getSoapHeaderSessionKey())) {
				setSoapHeaderSessionKey(DEFAULT_SOAP_HEADER_SESSION_KEY);
			}
			if (StringUtils.isEmpty(getSoapNamespaceSessionKey())) {
				setSoapNamespaceSessionKey(DEFAULT_SOAP_NAMESPACE_SESSION_KEY);
			}
		}
		if (getDirection() == Direction.WRAP && PipeLine.OUTPUT_WRAPPER_NAME.equals(getName())) {
			if (StringUtils.isEmpty(getSoapNamespaceSessionKey())) {
				setSoapNamespaceSessionKey(DEFAULT_SOAP_NAMESPACE_SESSION_KEY);
			}
		}
		if (getSoapVersion()==null) {
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
			if (getDirection() == Direction.WRAP) {
				Message payload = message;
				if (rootTp != null) {
					payload = new Message(rootTp.transform(payload.asSource()));
				}
				if (outputNamespaceTp != null) {
					payload = new Message(outputNamespaceTp.transform(payload.asSource()));
				}
				Map<String,Object> parameterValues = null;
				if (!getParameterList().isEmpty() && (soapHeaderTp != null || soapBodyTp != null)) {
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

	private String determineSoapNamespace(PipeLineSession session) throws IOException {
		String soapNamespace = getSoapNamespace();
		if (StringUtils.isEmpty(soapNamespace)) {
			String savedSoapNamespace = session.getMessage(getSoapNamespaceSessionKey()).asString();
			if (StringUtils.isNotEmpty(savedSoapNamespace)) {
				soapNamespace = savedSoapNamespace;
			} else {
				SoapVersion soapVersion = getSoapVersion();
				if (soapVersion==SoapVersion.AUTO) {
					soapVersion=DEFAULT_SOAP_VERSION_FOR_WRAPPING;
				}
				soapNamespace = soapVersion.namespace;
			}
		}
		return soapNamespace;
	}
	
	protected Message unwrapMessage(Message message, PipeLineSession session) throws SAXException, TransformerException, IOException {
		return soapWrapper.getBody(message, isAllowPlainXml(), session, getSoapNamespaceSessionKey());
	}

	protected Message wrapMessage(Message message, String soapHeader, PipeLineSession session) throws IOException {
		String soapNamespace = determineSoapNamespace(session);
		if (soapNamespace==null) {
			return message;
		}
		return soapWrapper.putInEnvelope(message, getEncodingStyle(), getServiceNamespace(), soapHeader, null, soapNamespace, wssCredentialFactory, isWssPasswordDigest());
	}

	@IbisDoc({"", "wrap"})
	public void setDirection(Direction value) {
		direction = value;
	}

	@IbisDoc({"Soap version to use", "auto"})
	public void setSoapVersion(SoapVersion value) {
		soapVersion = value;
	}

	@IbisDoc({"(only used when direction=<code>wrap</code>) Namespace of the soap envelope", "auto determined from soapVersion"})
	public void setSoapNamespace(String string) {
		soapNamespace = string;
	}

	@IbisDoc({"Key of session variable to store auto detected soapNamespace", "If configured as Pipeline Input Wrapper or PipeLine Output Wrapper: "+ DEFAULT_SOAP_NAMESPACE_SESSION_KEY})
	public void setSoapNamespaceSessionKey(String string) {
		soapNamespaceSessionKey = string;
	}

	@IbisDoc({"Key of session variable to store soap header", "If configured as Pipeline Input Wrapper and direction=<code>unwrap<code>: "+ DEFAULT_SOAP_HEADER_SESSION_KEY})
	public void setSoapHeaderSessionKey(String string) {
		soapHeaderSessionKey = string;
	}

	@IbisDoc({"The encodingStyle to be set in the soap header", ""})
	public void setEncodingStyle(String string) {
		encodingStyle = string;
	}

	@IbisDoc({"The default for the namespace of the message sent. Identifies the service to be called. May be overriden by an actual namespace setting in the message to be sent", ""})
	public void setServiceNamespace(String string) {
		serviceNamespace = string;
	}

	@IbisDoc({"(only used when direction=<code>wrap</code>) Stylesheet to create the content of the soap header. As input for this stylesheet a dummy xml string is used. Note: outputType=<code>xml</code> and xsltVersion=", ""})
	public void setSoapHeaderStyleSheet(String string){
		this.soapHeaderStyleSheet = string;
	}

	@IbisDoc({"(only used when direction=<code>wrap</code>) Stylesheet to apply to the input message. Note: outputType=<code>xml</code> and xsltVersion=2", ""})
	public void setSoapBodyStyleSheet(String string){
		this.soapBodyStyleSheet = string;
	}

	@IbisDoc({"(only used when direction=<code>unwrap</code>) If <code>true</code>, namespaces (and prefixes) in the content of the soap body are removed", "false"})
	public void setRemoveOutputNamespaces(boolean b) {
		removeOutputNamespaces = b;
	}

	@IbisDoc({"(only used when direction=<code>unwrap</code> and <code>removeoutputnamespaces=false</code>) If <code>true</code>, unused namespaces in the content of the soap body are removed", "true"})
	public void setRemoveUnusedOutputNamespaces(boolean b) {
		removeUnusedOutputNamespaces = b;
	}

	@IbisDoc({"(only used when direction=<code>wrap</code>) If not empty, this namespace is added to the root element in the soap body", ""})
	public void setOutputNamespace(String string) {
		outputNamespace = string;
	}

	@IbisDoc({"If not empty, the root element in the soap body is changed to this value", ""})
	public void setRoot(String string) {
		root = string;
	}

	@IbisDoc({"(only used when direction=<code>unwrap</code>) If <code>false</code> and the soap body contains a soap fault, a PipeRunException is thrown", "false"})
	public void setIgnoreSoapFault(boolean b) {
		ignoreSoapFault = b;
	}

	@IbisDoc({"For direction=<code>unwrap</code> only: if true, allow unwrapped xml too", "false"})
	public void setAllowPlainXml(boolean allowPlainXml) {
		this.allowPlainXml = allowPlainXml;
	}

	@IbisDoc({"alias used to obtain credentials for authentication to WebServiceSecurity", ""})
	public void setWssAuthAlias(String string) {
		wssAuthAlias = string;
	}

	@IbisDoc({"Default username for WebServiceSecurity", " "})
	public void setWssUserName(String string) {
		wssUserName = string;
	}

	@IbisDoc({"Default password for WebServiceSecurity", " "})
	public void setWssPassword(String string) {
		wssPassword = string;
	}

	@IbisDoc({"If true, the password is sent digested; Otherwise it is sent in clear text", "true"})
	public void setWssPasswordDigest(boolean b) {
		wssPasswordDigest = b;
	}

	@IbisDoc({"Key of session variable to check if action must be executed. The wrap or unwrap action is only executed if the session variable exists", ""})
	public void setOnlyIfSessionKey(String onlyIfSessionKey) {
		this.onlyIfSessionKey = onlyIfSessionKey;
	}

	@IbisDoc({"Value of session variable 'onlyIfSessionKey' to check if action must be executed. The wrap or unwrap action is only executed if the session variable has the specified value", ""})
	public void setOnlyIfValue(String onlyIfValue) {
		this.onlyIfValue = onlyIfValue;
	}

	@IbisDoc({"Key of session variable to check if action must be executed. The wrap or unwrap action is not executed if the session variable exists", ""})
	public void setUnlessSessionKey(String unlessSessionKey) {
		this.unlessSessionKey = unlessSessionKey;
	}

	@IbisDoc({"Value of session variable 'unlessSessionKey' to check if action must be executed. The wrap or unwrap action is not executed if the session variable has the specified value", ""})
	public void setUnlessValue(String unlessValue) {
		this.unlessValue = unlessValue;
	}

}
