/*
   Copyright 2013-2020 Nationale-Nederlanden, 2020-2025 WeAreFrank!

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
package org.frankframework.extensions.esb;

import org.apache.commons.lang3.StringUtils;

import lombok.Getter;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.configuration.ConfigurationWarnings;
import org.frankframework.configuration.util.ConfigurationUtils;
import org.frankframework.core.Adapter;
import org.frankframework.core.DestinationValidator;
import org.frankframework.core.IListener;
import org.frankframework.core.ISender;
import org.frankframework.core.PipeLine;
import org.frankframework.doc.Category;
import org.frankframework.jms.JmsException;
import org.frankframework.parameters.IParameter;
import org.frankframework.parameters.Parameter;
import org.frankframework.parameters.ParameterList;
import org.frankframework.receivers.Receiver;
import org.frankframework.soap.SoapWrapperPipe;
import org.frankframework.util.AppConstants;
import org.frankframework.util.SpringUtils;
import org.frankframework.util.StringUtil;

/**
 * Extension to SoapWrapperPipe for separate modes.
 *
 * <p><b>Configuration </b><i>(where deviating from SoapWrapperPipe)</i><b>:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>{@link #setMode(Mode) mode}</td><td>either <code>i2t</code> (ifsa2tibco), <code>reg</code> (regular) or <code>bis</code> (Business Integration Services)</td><td>reg</td></tr>
 * <tr><td>{@link #setCmhVersion(int) cmhVersion}</td><td>(only used when <code>mode=reg</code>) Common Message Header version (1 or 2)</td><td>1 when <code>mode=reg</code>, 0 otherwise</td></tr>
 * <tr><td>{@link #setSoapHeaderSessionKey(String) soapHeaderSessionKey}</td><td>if direction=<code>unwrap</code>: </td><td>soapHeader</td></tr>
 * <tr><td>{@link #setSoapHeaderStyleSheet(String) soapHeaderStyleSheet}</td><td>if direction=<code>wrap</code> and mode=<code>i2t</code>:</td><td>/xml/xsl/esb/soapHeader.xsl</td></tr>
 * <tr><td></td><td>if direction=<code>wrap</code> and mode=<code>reg</code>:</td><td>TODO (for now identical to the "<code>i2t</code>" SOAP Header)</td></tr>
 * <tr><td></td><td>if direction=<code>wrap</code> and mode=<code>bis</code>:</td><td>/xml/xsl/esb/bisSoapHeader.xsl</td></tr>
 * <tr><td>{@link #setSoapBodyStyleSheet(String) soapBodyStyleSheet}</td><td>if direction=<code>wrap</code> and mode=<code>reg</code>:</td><td>/xml/xsl/esb/soapBody.xsl</td></tr>
 * <tr><td></td><td>if direction=<code>wrap</code> and mode=<code>bis</code>:</td><td>/xml/xsl/esb/bisSoapBody.xsl</td></tr>
 * <tr><td>{@link #setAddOutputNamespace(boolean) addOutputNamespace}</td><td>(only used when <code>direction=wrap</code>) when <code>true</code>, <code>outputNamespace</code> is automatically set using the parameters (if $messagingLayer='P2P' then 'http://nn.nl/XSD/$businessDomain/$applicationName/$applicationFunction' else is serviceContext is not empty 'http://nn.nl/XSD/$businessDomain/$serviceName/$serviceContext/$serviceContextVersion/$operationName/$operationVersion' else 'http://nn.nl/XSD/$businessDomain/$serviceName/$serviceVersion/$operationName/$operationVersion')</td><td><code>false</code></td></tr>
 * <tr><td>{@link #setRetrievePhysicalDestination(boolean) retrievePhysicalDestination}</td><td>(only used when <code>direction=wrap</code>) when <code>true</code>, the physical destination is retrieved from the queue instead of using the parameter <code>destination</code></td><td><code>true</code></td></tr>
 * <tr><td>{@link #setUseFixedValues(boolean) useFixedValues}</td><td>If <code>true</code>, the fields CorrelationId, MessageId and Timestamp will have a fixed value (for testing purposes only)</td><td><code>false</code></td></tr>
 * <tr><td>{@link #setFixResultNamespace(boolean) fixResultNamespace}</td><td>(only used when <code>direction=wrap</code>) when <code>true</code> and the Result tag already exists, the namespace is changed</td><td><code>false</code></td></tr>
 * <tr><td>{@link #setP2pAlias(String) p2pAlias}</td><td>When the messagingLayer part of the destination has this value interpret it as P2P</td><td><code></code></td></tr>
 * <tr><td>{@link #setEsbAlias(String) esbAlias}</td><td>When the messagingLayer part of the destination has this value interpret it as ESB</td><td><code></code></td></tr>
 * </table></p>
 * <p>
 * <b>/xml/xsl/esb/soapHeader.xsl:</b>
 * <table border="1">
 * <tr><th>element</th><th>level</th><th>value</th></tr>
 * <tr><td>MessageHeader</td><td>0</td><td>&nbsp;</td></tr>
 * <tr><td>&nbsp;</td><td>&nbsp;</td><td>xmlns=$namespace</td></tr>
 * <tr><td>From</td><td>1</td><td>&nbsp;</td></tr>
 * <tr><td>Id</td><td>2</td><td>$fromId</td></tr>
 * <tr><td>To</td><td>1</td><td>&nbsp;</td></tr>
 * <tr><td>Location</td><td>2</td><td>if $messagingLayer='P2P' then<br/>&nbsp;$messagingLayer.$businessDomain.$applicationName.$applicationFunction.$paradigm<br/>else if $serviceContext is not empty then<br/>&nbsp;$messagingLayer.$businessDomain.$serviceLayer.$serviceName.$serviceContext.$serviceContextVersion.$operationName.$operationVersion.$paradigm<br/>else<br/>&nbsp;$messagingLayer.$businessDomain.$serviceLayer.$serviceName.$serviceVersion.$operationName.$operationVersion.$paradigm</td></tr>
 * <tr><td>HeaderFields</td><td>1</td><td>&nbsp;</td></tr>
 * <tr><td>CPAId</td><td>2</td><td>$cpaId</td></tr>
 * <tr><td>ConversationId</td><td>2</td><td>$conversationId</td></tr>
 * <tr><td>CorrelationId</td><td>2</td><td>$correlationId (if empty then skip this element)</td></tr>
 * <tr><td>MessageId</td><td>2</td><td>$messageId</td></tr>
 * <tr><td>ExternalRefToMessageId</td><td>2</td><td>$externalRefToMessageId (if empty then skip this element)</td></tr>
 * <tr><td>Timestamp</td><td>2</td><td>$timestamp</td></tr>
 * <tr><td>TransactionId</td><td>2</td><td>$transactionId (only used when $mode=reg and $cmhVersion=2; if empty then skip this element)</td></tr>
 * <tr><td>Service</td><td>1</td><td>&nbsp;</td></tr>
 * <tr><td>Name</td><td>2</td><td>$serviceName</td></tr>
 * <tr><td>Context</td><td>2</td><td>$serviceContext</td></tr>
 * <tr><td>Action</td><td>2</td><td>&nbsp;</td></tr>
 * <tr><td>Paradigm</td><td>3</td><td>$paradigm</td></tr>
 * <tr><td>Name</td><td>3</td><td>$operationName</td></tr>
 * <tr><td>Version</td><td>3</td><td>$operationVersion</td></tr>
 * </table>
 * <b>Parameters:</b>
 * <table border="1">
 * <tr><th>name</th><th>default</th></tr>
 * <tr><td>mode</td><td>copied from <code>mode</code></td></tr>
 * <tr><td>cmhVersion</td><td>copied from <code>cmhVersion</code></td></tr>
 * <tr><td>namespace</td><td>"http://nn.nl/XSD/Generic/MessageHeader/2" (only when $mode=reg and $cmhVersion=2)<br/>"http://nn.nl/XSD/Generic/MessageHeader/1" (otherwise)</td></tr>
 * <tr><td>businessDomain</td><td>&nbsp;</td></tr>
 * <tr><td>serviceName</td><td>&nbsp;</td></tr>
 * <tr><td>serviceContext</td><td>&nbsp;</td></tr>
 * <tr><td>service(Context)Version</td><td>1</td></tr>
 * <tr><td>operationName</td><td>&nbsp;</td></tr>
 * <tr><td>operationVersion</td><td>1</td></tr>
 * <tr><td>paradigm</td><td>&nbsp;</td></tr>
 * <tr><td>applicationName</td><td>&nbsp;</td></tr>
 * <tr><td>applicationFunction</td><td>&nbsp;</td></tr>
 * <tr><td>messagingLayer</td><td>ESB</td></tr>
 * <tr><td>serviceLayer</td><td>&nbsp;</td></tr>
 * <tr><td>destination</td><td>if not empty this parameter contains the preceding parameters as described in 'Location' in the table above</td></tr>
 * <tr><td>fromId</td><td>property 'instance.name'</td></tr>
 * <tr><td>cpaId</td><td>if applicable, copied from the original (received) SOAP Header, else 'n/a'</td></tr>
 * <tr><td>conversationId</td><td>if applicable, copied from the original (received) SOAP Header, else parameter pattern '{hostname}_{uid}'</td></tr>
 * <tr><td>messageId</td><td>parameter pattern '{hostname}_{uid}'</td></tr>
 * <tr><td>correlationId</td><td>if $paradigm equals 'Response' then copied from MessageId in the original (received) SOAP Header</td></tr>
 * <tr><td>externalRefToMessageId</td><td>if applicable, copied from the original (received) SOAP Header</td></tr>
 * <tr><td>timestamp</td><td>parameter pattern '{now,date,yyyy-MM-dd'T'HH:mm:ss}'</td></tr>
 * <tr><td>transactionId</td><td>if applicable, copied from the original (received) SOAP Header</td></tr>
 * </table>
 * </p>
 * <p>
 * <b>/xml/xsl/esb/bisSoapHeader.xsl:</b>
 * <table border="1">
 * <tr><th>element</th><th>level</th><th>value</th></tr>
 * <tr><td>MessageHeader</td><td>0</td><td>&nbsp;</td></tr>
 * <tr><td>&nbsp;</td><td>&nbsp;</td><td>xmlns=$namespace</td></tr>
 * <tr><td>From</td><td>1</td><td>&nbsp;</td></tr>
 * <tr><td>Id</td><td>2</td><td>$fromId</td></tr>
 * <tr><td>HeaderFields</td><td>1</td><td>&nbsp;</td></tr>
 * <tr><td>ConversationId</td><td>2</td><td>$conversationId</td></tr>
 * <tr><td>MessageId</td><td>2</td><td>$messageId</td></tr>
 * <tr><td>ExternalRefToMessageId</td><td>2</td><td>$externalRefToMessageId (if empty then skip this element)</td></tr>
 * <tr><td>Timestamp</td><td>2</td><td>$timestamp</td></tr>
 * </table>
 * <b>Parameters:</b>
 * <table border="1">
 * <tr><th>name</th><th>default</th></tr>
 * <tr><td>namespace</td><td>"http://www.ing.com/CSP/XSD/General/Message_2"</td></tr>
 * <tr><td>fromId</td><td>property 'instance.name'</td></tr>
 * <tr><td>conversationId</td><td>if applicable, copied from the original (received) SOAP Header, else parameter pattern '{hostname}_{uid}'</td></tr>
 * <tr><td>messageId</td><td>parameter pattern '{hostname}_{uid}'</td></tr>
 * <tr><td>externalRefToMessageId</td><td>if applicable, copied from MessageId in the original (received) SOAP Header</td></tr>
 * <tr><td>timestamp</td><td>parameter pattern '{now,date,yyyy-MM-dd'T'HH:mm:ss}'</td></tr>
 * </table>
 * </p>
 * <p>
 * <b>/xml/xsl/esb/soapBody.xsl:</b>
 * <table border="1">
 * <tr><th>element</th><th>level</th><th>value</th></tr>
 * <tr><td>[Payload]</td><td>0</td><td>if $errorCode is empty then the complete payload will be copied and if not already existing a Result tag will be added<br/>else only the root tag will be copied</td></tr>
 * <tr><td>Result</td><td>1</td><td>this element will be the last child in the copied root tag (only applicable for $paradigm 'Response'); if $errorCode is empty and a Result tag already exists then skip this element including its child elements</td></tr>
 * <tr><td>&nbsp;</td><td>&nbsp;</td><td>xmlns=$namespace</td></tr>
 * <tr><td>Status</td><td>2</td><td>if $errorCode is empty then 'OK'<br/>else 'ERROR'</td></tr>
 * <tr><td>ErrorList</td><td>2</td><td>if $errorCode is empty then skip this element including its child elements</td></tr>
 * <tr><td>Error</td><td>3</td><td>&nbsp;</td></tr>
 * <tr><td>Code</td><td>4</td><td>$errorCode</td></tr>
 * <tr><td>Reason</td><td>4</td><td>if $errorReason is not empty then $errorReason<br/>else it will be derived from $errorCode:
 *   <table border="1">
 *   <tr><th>errorCode</th><th>errorText</th></tr>
 *   <tr><td>ERR6002</td><td>Service Interface Request Time Out</td></tr>
 *   <tr><td>ERR6003</td><td>Invalid Request Message</td></tr>
 *   <tr><td>ERR6004</td><td>Invalid Backend system response</td></tr>
 *   <tr><td>ERR6005</td><td>Backend system failure response</td></tr>
 *   <tr><td>ERR6999</td><td>Unspecified Errors</td></tr>
 *  </table>
 * </td></tr>
 * <tr><td>Service</td><td>4</td><td>&nbsp;</td></tr>
 * <tr><td>Name</td><td>5</td><td>$serviceName</td></tr>
 * <tr><td>Context</td><td>5</td><td>$serviceContext</td></tr>
 * <tr><td>Action</td><td>5</td><td>&nbsp;</td></tr>
 * <tr><td>Paradigm</td><td>6</td><td>$paradigm</td></tr>
 * <tr><td>Name</td><td>6</td><td>$operationName</td></tr>
 * <tr><td>Version</td><td>6</td><td>$operationVersion</td></tr>
 * <tr><td>DetailList</td><td>4</td><td>if $errorDetailCode is empty then skip this element including its child elements</td></tr>
 * <tr><td>Detail</td><td>5</td><td>&nbsp;</td></tr>
 * <tr><td>Code</td><td>6</td><td>$errorDetailCode</td></tr>
 * <tr><td>Text</td><td>6</td><td>$errorDetailText (if empty then skip this element)</td></tr>
 * </table>
 * <b>Parameters:</b>
 * <table border="1">
 * <tr><th>name</th><th>default</th></tr>
 * <tr><td>mode</td><td>copied from <code>mode</code></td></tr>
 * <tr><td>cmhVersion</td><td>copied from <code>cmhVersion</code></td></tr>
 * <tr><td>namespace</td><td>"http://nn.nl/XSD/Generic/MessageHeader/2" (only when $mode=reg and $cmhVersion=2)<br/>"http://nn.nl/XSD/Generic/MessageHeader/1" (otherwise)</td></tr>
 * <tr><td>errorCode</td><td>&nbsp;</td></tr>
 * <tr><td>errorReason</td><td>&nbsp;</td></tr>
 * <tr><td>errorDetailCode</td><td>&nbsp;</td></tr>
 * <tr><td>errorDetailText</td><td>&nbsp;</td></tr>
 * <tr><td>serviceName</td><td>&nbsp;</td></tr>
 * <tr><td>serviceContext</td><td>&nbsp;</td></tr>
 * <tr><td>operationName</td><td>&nbsp;</td></tr>
 * <tr><td>operationVersion</td><td>1</td></tr>
 * <tr><td>paradigm</td><td>&nbsp;</td></tr>
 * <tr><td>fixResultNamespace</td><td>false</td></tr>
 * </table>
 * </p>
 * <p>
 * <b>/xml/xsl/esb/bisSoapBody.xsl:</b>
 * <table border="1">
 * <tr><th>element</th><th>level</th><th>value</th></tr>
 * <tr><td>[Payload]</td><td>0</td><td>if $errorCode is empty then the complete payload will be copied and if not already existing a Result tag will be added<br/>else only the root tag will be copied</td></tr>
 * <tr><td>Result</td><td>1</td><td>this element will be the last child in the copied root tag (only applicable for $paradigm 'Response' and 'Reply'); if $errorCode is empty and a Result tag already exists then skip this element including its child elements</td></tr>
 * <tr><td>&nbsp;</td><td>&nbsp;</td><td>xmlns=$namespace</td></tr>
 * <tr><td>Status</td><td>2</td><td>if $errorCode is empty then 'OK'<br/>else 'ERROR'</td></tr>
 * <tr><td>ErrorList</td><td>2</td><td>if $errorCode is empty then skip this element including its child elements</td></tr>
 * <tr><td>Error</td><td>3</td><td>&nbsp;</td></tr>
 * <tr><td>Code</td><td>4</td><td>$errorCode</td></tr>
 * <tr><td>Reason</td><td>4</td><td>if $errorReason is not empty then $errorReason<br/>else it will be derived from $errorCode:
 *   <table border="1">
 *   <tr><th>errorCode</th><th>errorText</th></tr>
 *   <tr><td>ERR6002</td><td>Service Interface Request Time Out</td></tr>
 *   <tr><td>ERR6003</td><td>Invalid Request Message</td></tr>
 *   <tr><td>ERR6004</td><td>Invalid Backend system response</td></tr>
 *   <tr><td>ERR6005</td><td>Backend system failure response</td></tr>
 *   <tr><td>ERR6999</td><td>Unspecified Errors</td></tr>
 *  </table>
 * </td></tr>
 * <tr><td>Service</td><td>4</td><td>&nbsp;</td></tr>
 * <tr><td>Name</td><td>5</td><td>$serviceName</td></tr>
 * <tr><td>Context</td><td>5</td><td>$serviceContext</td></tr>
 * <tr><td>Action</td><td>5</td><td>&nbsp;</td></tr>
 * <tr><td>Name</td><td>6</td><td>$operationName</td></tr>
 * <tr><td>Version</td><td>6</td><td>$operationVersion</td></tr>
 * <tr><td>DetailList</td><td>4</td><td>if $errorDetailCode is empty then skip this element including its child elements</td></tr>
 * <tr><td>Detail</td><td>5</td><td>&nbsp;</td></tr>
 * <tr><td>Code</td><td>6</td><td>$errorDetailCode</td></tr>
 * <tr><td>Text</td><td>6</td><td>$errorDetailText (if empty then skip this element)</td></tr>
 * </table>
 * <b>Parameters:</b>
 * <table border="1">
 * <tr><th>name</th><th>default</th></tr>
 * <tr><td>namespace</td><td>"http://www.ing.com/CSP/XSD/General/Message_2"</td></tr>
 * <tr><td>errorCode</td><td>&nbsp;</td></tr>
 * <tr><td>errorReason</td><td>&nbsp;</td></tr>
 * <tr><td>errorDetailCode</td><td>&nbsp;</td></tr>
 * <tr><td>errorDetailText</td><td>&nbsp;</td></tr>
 * <tr><td>serviceName</td><td>&nbsp;</td></tr>
 * <tr><td>serviceContext</td><td>&nbsp;</td></tr>
 * <tr><td>operationName</td><td>&nbsp;</td></tr>
 * <tr><td>operationVersion</td><td>1</td></tr>
 * <tr><td>paradigm</td><td>&nbsp;</td></tr>
 * </table>
 * </p>
 * @author Peter Leeuwenburgh
 */
@Category(Category.Type.NN_SPECIAL)
public class EsbSoapWrapperPipe extends SoapWrapperPipe implements DestinationValidator {
	protected static final String OUTPUTNAMESPACEBASEURI = "http://nn.nl/XSD";
	protected static final String BUSINESSDOMAIN_PARAMETER_NAME = "businessDomain";
	protected static final String SERVICENAME_PARAMETER_NAME = "serviceName";
	protected static final String SERVICECONTEXT_PARAMETER_NAME = "serviceContext";
	protected static final String SERVICECONTEXTVERSION_PARAMETER_NAME = "serviceContextVersion";
	protected static final String OPERATIONNAME_PARAMETER_NAME = "operationName";
	protected static final String OPERATIONVERSION_PARAMETER_NAME = "operationVersion";
	protected static final String PARADIGM_PARAMETER_NAME = "paradigm";
	protected static final String APPLICATIONNAME_PARAMETER_NAME = "applicationName";
	protected static final String APPLICATIONFUNCTION_PARAMETER_NAME = "applicationFunction";
	protected static final String MESSAGINGLAYER_PARAMETER_NAME = "messagingLayer";
	protected static final String SERVICELAYER_PARAMETER_NAME = "serviceLayer";
	protected static final String DESTINATION_PARAMETER_NAME = "destination";
	protected static final String PHYSICALDESTINATION_PARAMETER_NAME = "physicalDestination";
	protected static final String FROMID_PARAMETER_NAME = "fromId";
	protected static final String CPAID_PARAMETER_NAME = "cpaId";
	protected static final String CONVERSATIONID_PARAMETER_NAME = "conversationId";
	protected static final String MESSAGEID_PARAMETER_NAME = "messageId";
	protected static final String CORRELATIONID_PARAMETER_NAME = "correlationId";
	protected static final String EXTERNALREFTOMESSAGEID_PARAMETER_NAME = "externalRefToMessageId";
	protected static final String TIMESTAMP_PARAMETER_NAME = "timestamp";
	protected static final String FIXRESULTNAMESPACE_PARAMETER_NAME = "fixResultNamespace";
	protected static final String TRANSACTIONID_PARAMETER_NAME = "transactionId";
	protected static final String MODE_PARAMETER_NAME = "mode";
	protected static final String CMHVERSION_PARAMETER_NAME = "cmhVersion";

	private @Getter boolean useFixedValues = false;
	private @Getter boolean fixResultNamespace = false;
	private String p2pAlias;
	private String esbAlias;

	private @Getter Mode mode = Mode.REG;
	private @Getter int cmhVersion = 0;
	private @Getter boolean addOutputNamespace = false;
	private @Getter boolean retrievePhysicalDestination = true;

	public enum Mode {
		/** ifsa2tibco */
		I2T,
		/** Regular */
		REG,
		/** Business Integration Services */
		BIS
	}

	@Override
	public void configure() throws ConfigurationException {
		if (getMode() == Mode.REG) {
			if (cmhVersion == 0) {
				cmhVersion = 1;
			} else if (cmhVersion < 0 || cmhVersion > 2) {
				ConfigurationWarnings.add(this, log, "cmhVersion [" + cmhVersion + "] for mode [" + getMode() + "] should be set to '1' or '2', assuming '1'");
				cmhVersion = 1;
			}
		} else {
			if (cmhVersion != 0) {
				ConfigurationWarnings.add(this, log, "cmhVersion ["+cmhVersion+"] for mode ["+getMode().toString()+"] should not be set, assuming '0'");
				cmhVersion = 0;
			}
		}
		if (getDirection()==Direction.WRAP) {
			if (StringUtils.isEmpty(getSoapHeaderSessionKey())) {
				setSoapHeaderSessionKey(DEFAULT_SOAP_HEADER_SESSION_KEY);
			}
			if (StringUtils.isEmpty(getSoapHeaderStyleSheet())) {
				if (getMode() == Mode.BIS) {
					setSoapHeaderStyleSheet("/xml/xsl/esb/bisSoapHeader.xsl");
				} else {
					setSoapHeaderStyleSheet("/xml/xsl/esb/soapHeader.xsl");
				}
			}
			if (StringUtils.isEmpty(getSoapBodyStyleSheet())) {
				if (getMode() == Mode.REG) {
					setSoapBodyStyleSheet("/xml/xsl/esb/soapBody.xsl");
				} else if (getMode() == Mode.BIS) {
					setSoapBodyStyleSheet("/xml/xsl/esb/bisSoapBody.xsl");
				}
			}
			stripDestination();
			if (isAddOutputNamespace()) {
				String ons = getOutputNamespaceBaseUri();
				if ("P2P".equals(getMessagingLayer()) || (StringUtils.isNotEmpty(p2pAlias) && getMessagingLayer().equalsIgnoreCase(p2pAlias))) {
					ons = ons +
					"/" + getBusinessDomain() +
					"/" + getApplicationName() +
					"/" + getApplicationFunction();
				} else {
					ons = ons +
					"/" + getBusinessDomain() +
					"/" + getServiceName() +
					(StringUtils.isEmpty(getServiceContext()) ? "" : "/" + getServiceContext()) +
					"/" + getServiceContextVersion() +
					"/" + getOperationName() +
					"/" + getOperationVersion();
				}
				setOutputNamespace(ons);
			}
			addParameters();
		}
		super.configure();
		if (isUseFixedValues() && (!ConfigurationUtils.isConfigurationStubbed(getConfigurationClassLoader()))) {
				throw new ConfigurationException("returnFixedDate only allowed in stub mode");
		}
	}

	private String getParameterValue(String key) {
		IParameter p = getParameterList().findParameter(key);
		return p != null ? p.getValue() : "";
	}

	public static String getOutputNamespaceBaseUri() {
		return OUTPUTNAMESPACEBASEURI;
	}

	public String getBusinessDomain() {
		return getParameterValue(BUSINESSDOMAIN_PARAMETER_NAME);
	}

	public String getServiceName() {
		return getParameterValue(SERVICENAME_PARAMETER_NAME);
	}

	public String getServiceContext() {
		return getParameterValue(SERVICECONTEXT_PARAMETER_NAME);
	}

	public String getServiceContextVersion() {
		return getParameterValue(SERVICECONTEXTVERSION_PARAMETER_NAME);
	}

	public String getOperationName() {
		return getParameterValue(OPERATIONNAME_PARAMETER_NAME);
	}

	public String getOperationVersion() {
		return getParameterValue(OPERATIONVERSION_PARAMETER_NAME);
	}

	public String getDestination() {
		IParameter p = getParameterList().findParameter(DESTINATION_PARAMETER_NAME);
		return p == null ? null : p.getValue();
	}

	public String getMessagingLayer() {
		return getParameterValue(MESSAGINGLAYER_PARAMETER_NAME);
	}

	public String getServiceLayer() {
		return getParameterValue(SERVICELAYER_PARAMETER_NAME);
	}

	public String getParadigm() {
		return getParameterValue(PARADIGM_PARAMETER_NAME);
	}

	public String getApplicationName() {
		return getParameterValue(APPLICATIONNAME_PARAMETER_NAME);
	}

	public String getApplicationFunction() {
		return getParameterValue(APPLICATIONFUNCTION_PARAMETER_NAME);
	}

	private void stripDestination() {
		ParameterList parameterList = getParameterList();
		IParameter pd = parameterList.findParameter(DESTINATION_PARAMETER_NAME);
		IParameter ppd = parameterList.findParameter(PHYSICALDESTINATION_PARAMETER_NAME);
		String destination = null;
		if (isRetrievePhysicalDestination()) {
			if (ppd!=null) {
				destination = ppd.getValue();
			} else if (pd!=null) {
				destination = pd.getValue();
			}
		} else {
			if (pd!=null) {
				destination = pd.getValue();
			}
		}
		Parameter p;
		if (StringUtils.isNotEmpty(destination)) {
			if(destination.startsWith("ESB.") || destination.startsWith("P2P.")
					|| (StringUtils.isNotEmpty(esbAlias) && destination.startsWith(esbAlias + "."))
					|| (StringUtils.isNotEmpty(p2pAlias) && destination.startsWith(p2pAlias + "."))
					) {
				//In case the messaging layer is ESB, the destination syntax is:
				// Destination = [MessagingLayer].[BusinessDomain].[ServiceLayer].[ServiceName].[ServiceContext].[ServiceContextVersion].[OperationName].[OperationVersion].[Paradigm]
				//In case the messaging layer is P2P, the destination syntax is:
				// Destination = [MessagingLayer].[BusinessDomain].[ApplicationName].[ApplicationFunction].[Paradigm]
				boolean p2p = false;
				boolean esbDestinationWithoutServiceContext = false;
				int count = 0;
				for (final String str : StringUtil.split(destination, ".")) {
					count++;
					p = SpringUtils.createBean(getApplicationContext());
					switch (count) {
						case 1:
							if ("P2P".equals(str)
									|| (StringUtils.isNotEmpty(p2pAlias) && str.equalsIgnoreCase(p2pAlias))
							) {
								p2p = true;
							} else {
								esbDestinationWithoutServiceContext = isEsbDestinationWithoutServiceContext(destination);
							}
							p.setName(MESSAGINGLAYER_PARAMETER_NAME);
							break;
						case 2:
							p.setName(BUSINESSDOMAIN_PARAMETER_NAME);
							break;
						case 3:
							if (p2p) {
								p.setName(APPLICATIONNAME_PARAMETER_NAME);
							} else {
								p.setName(SERVICELAYER_PARAMETER_NAME);
							}
							break;
						case 4:
							if (p2p) {
								p.setName(APPLICATIONFUNCTION_PARAMETER_NAME);
							} else {
								p.setName(SERVICENAME_PARAMETER_NAME);
							}
							break;
						case 5:
							if (p2p) {
								p.setName(PARADIGM_PARAMETER_NAME);
							} else {
								if (esbDestinationWithoutServiceContext) {
									p.setName(SERVICECONTEXTVERSION_PARAMETER_NAME);
								} else {
									p.setName(SERVICECONTEXT_PARAMETER_NAME);
								}
							}
							break;
						case 6:
							if (esbDestinationWithoutServiceContext) {
								p.setName(OPERATIONNAME_PARAMETER_NAME);
							} else {
								p.setName(SERVICECONTEXTVERSION_PARAMETER_NAME);
							}
							break;
						case 7:
							if (esbDestinationWithoutServiceContext) {
								p.setName(OPERATIONVERSION_PARAMETER_NAME);
							} else {
								p.setName(OPERATIONNAME_PARAMETER_NAME);
							}
							break;
						case 8:
							if (esbDestinationWithoutServiceContext) {
								p.setName(PARADIGM_PARAMETER_NAME);
							} else {
								p.setName(OPERATIONVERSION_PARAMETER_NAME);
							}
							break;
						case 9:
							if (!esbDestinationWithoutServiceContext) {
								p.setName(PARADIGM_PARAMETER_NAME);
							}

							break;
						default:
					}
					p.setValue(str);
					addParameter(p);
				}
			} else {
				p = SpringUtils.createBean(getApplicationContext());
				p.setName(MESSAGINGLAYER_PARAMETER_NAME);
				p.setValue("P2P");
				addParameter(p);
				//
				p = SpringUtils.createBean(getApplicationContext());
				p.setName(BUSINESSDOMAIN_PARAMETER_NAME);
				p.setValue("?");
				addParameter(p);
				//
				p = SpringUtils.createBean(getApplicationContext());
				p.setName(APPLICATIONNAME_PARAMETER_NAME);
				p.setValue("?");
				addParameter(p);
				//
				p = SpringUtils.createBean(getApplicationContext());
				p.setName(APPLICATIONFUNCTION_PARAMETER_NAME);
				p.setValue(destination.replaceAll("\\W", "_"));
				addParameter(p);
				//
				p = SpringUtils.createBean(getApplicationContext());
				p.setName(PARADIGM_PARAMETER_NAME);
				p.setValue("?");
				addParameter(p);
			}
		}
	}

	private void addParameters() {
		ParameterList parameterList = getParameterList();
		Parameter p;
		if (!parameterList.hasParameter(FROMID_PARAMETER_NAME)) {
			p = SpringUtils.createBean(getApplicationContext());
			p.setName(FROMID_PARAMETER_NAME);
			p.setValue(AppConstants.getInstance().getProperty("instance.name", ""));
			addParameter(p);
		}
		if (getMode() != Mode.BIS && !parameterList.hasParameter(CPAID_PARAMETER_NAME)) {
			p = SpringUtils.createBean(getApplicationContext());
			p.setName(CPAID_PARAMETER_NAME);
			p.setSessionKey(getSoapHeaderSessionKey());
			p.setXpathExpression("MessageHeader/HeaderFields/CPAId");
			p.setRemoveNamespaces(true);
			p.setDefaultValue("n/a");
			addParameter(p);
		}
		if (!parameterList.hasParameter(CONVERSATIONID_PARAMETER_NAME)) {
			p = SpringUtils.createBean(getApplicationContext());
			p.setName(CONVERSATIONID_PARAMETER_NAME);
			p.setSessionKey(getSoapHeaderSessionKey());
			p.setXpathExpression("MessageHeader/HeaderFields/ConversationId");
			p.setRemoveNamespaces(true);
			if (isUseFixedValues()) {
				p.setPattern("{fixedhostname}_{fixeduid}");
			} else {
				p.setPattern("{hostname}_{uid}");
			}
			p.setDefaultValueMethods("pattern");
			addParameter(p);
		}
		if (!parameterList.hasParameter(MESSAGEID_PARAMETER_NAME)) {
			p = SpringUtils.createBean(getApplicationContext());
			p.setName(MESSAGEID_PARAMETER_NAME);
			if (isUseFixedValues()) {
				p.setPattern("{fixedhostname}_{fixeduid}");
			} else {
				p.setPattern("{hostname}_{uid}");
			}
			addParameter(p);
		}
		if (!parameterList.hasParameter(EXTERNALREFTOMESSAGEID_PARAMETER_NAME)) {
			p = SpringUtils.createBean(getApplicationContext());
			p.setName(EXTERNALREFTOMESSAGEID_PARAMETER_NAME);
			p.setSessionKey(getSoapHeaderSessionKey());
			if (getMode() == Mode.BIS) {
				p.setXpathExpression("MessageHeader/HeaderFields/MessageId");
			} else {
				p.setXpathExpression("MessageHeader/HeaderFields/ExternalRefToMessageId");
			}
			p.setRemoveNamespaces(true);
			addParameter(p);
		}
		if (getMode() != Mode.BIS && !parameterList.hasParameter(CORRELATIONID_PARAMETER_NAME)) {
			String paradigm;
			IParameter ppn = parameterList.findParameter(PARADIGM_PARAMETER_NAME);
			if (ppn!=null) {
				paradigm = ppn.getValue();
				if ("Response".equals(paradigm)) {
					p = SpringUtils.createBean(getApplicationContext());
					p.setName(CORRELATIONID_PARAMETER_NAME);
					p.setSessionKey(getSoapHeaderSessionKey());
					p.setXpathExpression("MessageHeader/HeaderFields/MessageId");
					p.setRemoveNamespaces(true);
					addParameter(p);
				}
			}
		}
		if (!parameterList.hasParameter(TIMESTAMP_PARAMETER_NAME)) {
			p = SpringUtils.createBean(getApplicationContext());
			p.setName(TIMESTAMP_PARAMETER_NAME);
			if (isUseFixedValues()) {
				p.setPattern("{fixeddate,date,yyyy-MM-dd'T'HH:mm:ss}");
			} else {
				p.setPattern("{now,date,yyyy-MM-dd'T'HH:mm:ss}");
			}
			addParameter(p);
		}
		if (!parameterList.hasParameter(FIXRESULTNAMESPACE_PARAMETER_NAME)) {
			p = SpringUtils.createBean(getApplicationContext());
			p.setName(FIXRESULTNAMESPACE_PARAMETER_NAME);
			p.setValue(String.valueOf(isFixResultNamespace()));
			addParameter(p);
		}
		if (!parameterList.hasParameter(TRANSACTIONID_PARAMETER_NAME)) {
			p = SpringUtils.createBean(getApplicationContext());
			p.setName(TRANSACTIONID_PARAMETER_NAME);
			p.setSessionKey(getSoapHeaderSessionKey());
			p.setXpathExpression("MessageHeader/HeaderFields/TransactionId");
			p.setRemoveNamespaces(true);
			addParameter(p);
		}
		p = SpringUtils.createBean(getApplicationContext());
		p.setName(MODE_PARAMETER_NAME);
		p.setValue(getMode().toString());
		addParameter(p);

		p = SpringUtils.createBean(getApplicationContext());
		p.setName(CMHVERSION_PARAMETER_NAME);
		p.setValue(String.valueOf(getCmhVersion()));
		addParameter(p);
	}

	public static boolean isValidNamespace(String namespace) {
		if (namespace != null
				&& namespace.startsWith(getOutputNamespaceBaseUri())) {
			String[] split = namespace.split("/");
			if (split.length == 9 || split.length == 10) {
				for (int i = 0; i < split.length; i++) {
					if (i == 1) {
						if (!split[i].isEmpty()) {
							return false;
						}
					} else if (split[i].isEmpty()) {
						return false;
					}
				}
				return true;
			}
		}
		return false;
	}

	public boolean retrievePhysicalDestinationFromSender (ISender sender) {
		if (sender instanceof EsbJmsSender ejSender) {
			String physicalDestination = ejSender.getPhysicalDestinationShortName();
			if (physicalDestination==null) {
				physicalDestination="?";
			}
			Parameter p = SpringUtils.createBean(getApplicationContext());
			p.setName(PHYSICALDESTINATION_PARAMETER_NAME);
			p.setValue(physicalDestination);
			addParameter(p);
			return true;
		}
		return false;
	}

	public boolean retrievePhysicalDestinationFromListener (IListener<?> listener) throws JmsException {
		if (listener instanceof EsbJmsListener ejListener) {
			if (!ejListener.isSynchronous()) {
				return false;
			}
			String physicalDestination = ejListener.getPhysicalDestinationShortName(true);
			// force an exception when physical destination cannot be retrieved
			// so adapter will not start and destination parameter is not set
			// incorrectly
			if (physicalDestination==null) {
				physicalDestination="?";
			} else {
				int pos = physicalDestination.lastIndexOf(".");
				pos++;
				if (pos > 0 && pos < physicalDestination.length()) {
					String pds = physicalDestination.substring(0, pos);
					String paradigm = physicalDestination.substring(pos);
					if ("Request".equals(paradigm) || "Solicit".equals(paradigm)) {
						physicalDestination = pds + "Response";
					} else {
						physicalDestination = pds + "?";
					}
				} else {
					physicalDestination = physicalDestination + ".?";
				}
			}
			Parameter p = SpringUtils.createBean(getApplicationContext());
			p.setName(PHYSICALDESTINATION_PARAMETER_NAME);
			p.setValue(physicalDestination);
			addParameter(p);
			return true;
		}
		return false;
	}

	public static boolean isEsbDestinationWithoutServiceContext(String destination) {
		int dotCount = StringUtils.countMatches(destination, ".");
		return dotCount < 8;
	}

	public static boolean isEsbNamespaceWithoutServiceContext(String namespace) {
		int slashCount = StringUtils.countMatches(namespace, "/");
		return slashCount < 9;
	}
	/**
	 * @ff.default REG
	 */
	public void setMode(Mode mode) {
		this.mode = mode;
	}

	/**
	 * <b>Only used when <code>mode=reg</code>!</b> Sets the Common Message Header version. 1 or 2
	 * @ff.default 1
	 */
	public void setCmhVersion(int i) {
		cmhVersion = i;
	}

	/**
	 * (only used when <code>direction=wrap</code>) when <code>true</code>, <code>outputNamespace</code> is automatically set using the parameters (if $messagingLayer='P2P' then 'http://nn.nl/XSD/$businessDomain/$applicationName/$applicationFunction' else is serviceContext is not empty 'http://nn.nl/XSD/$businessDomain/$serviceName/$serviceContext/$serviceContextVersion/$operationName/$operationVersion' else 'http://nn.nl/XSD/$businessDomain/$serviceName/$serviceVersion/$operationName/$operationVersion')
	 * @ff.default false
	 */
	public void setAddOutputNamespace(boolean b) {
		addOutputNamespace = b;
	}

	/**
	 * (only used when <code>direction=wrap</code>) when <code>true</code>, the physical destination is retrieved from the queue instead of using the parameter <code>destination</code>
	 * @ff.default true
	 */
	public void setRetrievePhysicalDestination(boolean b) {
		retrievePhysicalDestination = b;
	}

	/**
	 * If <code>true</code>, the fields CorrelationId, MessageId and Timestamp will have a fixed value (for testing purposes only)
	 * @ff.default false
	 */
	public void setUseFixedValues(boolean b) {
		useFixedValues = b;
	}

	/**
	 * (only used when <code>direction=wrap</code>) when <code>true</code> and the Result tag already exists, the namespace is changed
	 * @ff.default false
	 */
	public void setFixResultNamespace(boolean b) {
		fixResultNamespace = b;
	}

	/** When the messagingLayer part of the destination has this value interpret it as P2P */
	public void setP2pAlias(String p2pAlias) {
		this.p2pAlias = p2pAlias;
	}

	/** When the messagingLayer part of the destination has this value interpret it as ESB */
	public void setEsbAlias(String esbAlias) {
		this.esbAlias = esbAlias;
	}

	@Override
	public void validateListenerDestinations(PipeLine pipeLine) throws ConfigurationException {
		Adapter owningAdapter = pipeLine.getAdapter();
		if (owningAdapter != null) {
			for (Receiver<?> receiver : owningAdapter.getReceivers()) {
				IListener<?> listener = receiver.getListener();
				try {
					if (retrievePhysicalDestinationFromListener(listener)) {
						break;
					}
				} catch (JmsException e) {
					throw new ConfigurationException(e);
				}
			}
		}
	}

	@Override
	public void validateSenderDestination(ISender sender) throws ConfigurationException {
		retrievePhysicalDestinationFromSender(sender);
	}
}
