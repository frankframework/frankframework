/*
 * $Log: ExtendedSoapWrapperPipe.java,v $
 * Revision 1.1  2011-11-30 13:52:00  europe\m168309
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/26 12:47:18  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * first version
 *
 *
 */
package nl.nn.adapterframework.soap;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.soap.SoapWrapperPipe;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.DateUtils;
import nl.nn.adapterframework.util.DomBuilderException;
import nl.nn.adapterframework.util.Misc;
import nl.nn.adapterframework.util.TransformerPool;
import nl.nn.adapterframework.util.XmlBuilder;
import nl.nn.adapterframework.util.XmlUtils;

/**
 * Extra attributes for extra functionality.
 * <p>
 * In the SOAP header the element MessageHeader is included:<br/><code><pre>
 *	&lt;MessageHeader xmlns="http://www.nn.nl/"&gt;
 *		&lt;From&gt;
 *			&lt;Id&gt;PolicyConversion_01_ServiceAgents_01&lt;/Id&gt;
 *		&lt;/From&gt;
 *		&lt;HeaderFields&gt;
 *			&lt;ConversationId&gt;1790257_10000050_04&lt;/ConversationId&gt;
 *			&lt;MessageId&gt;1790257&lt;/MessageId&gt;
 *			&lt;ExternalRefToMessageId&gt;NPELI153452&lt;/ExternalRefToMessageId&gt;
 *			&lt;Timestamp&gt;2011-03-02T10:26:31.464+01:00&lt;/Timestamp&gt;
 *		&lt;/HeaderFields&gt;
 *	&lt;/MessageHeader&gt;
 * </pre></code></p><p>
 * In the SOAP body the element Result is included (as last child in root tag), when best case scenario:<code><pre>
 *	&lt;Result xmlns="http://www.nn.nl/"&gt;
 *		&lt;Status&gt;OK&lt;/Status&gt;
 *	&lt;/Result&gt;
 * </pre></code>
 * in case of an error:<br/><code><pre>
 *	&lt;Result xmlns="http://www.nn.nl/"&gt;
 *		&lt;Status&gt;ERROR&lt;/Status&gt;
 *		&lt;ErrorList&gt;
 *			&lt;Error&gt;
 *				&lt;Code&gt;ERR6003&lt;/Code&gt;
 *				&lt;Reason&gt;Invalid Request Message&lt;/Reason&gt;
 *				&lt;Service&gt;
 *					&lt;Name&gt;migrationauditdata_01&lt;/Name&gt;
 *					&lt;Context&gt;1&lt;/Context&gt;
 *					&lt;Action&gt;
 *						&lt;Name&gt;SetPolicyDetails_Action&lt;/Name&gt;
 *						&lt;Version&gt;1&lt;/Version&gt;
 *					&lt;/Action&gt;
 *				&lt;/Service&gt;
 *				&lt;DetailList&gt;
 *					&lt;Detail&gt;
 *						&lt;Code/&gt;
 *						&lt;Text&gt;Pipe [Validate tibco request] msgId [Test Tool correlation id] got invalid xml according to schema [....&lt;/Text&gt;
 *					&lt;/Detail&gt;
 *				&lt;/DetailList&gt;
 *			&lt;/Error&gt;
 *		&lt;/ErrorList&gt;
 *	&lt;/Result&gt;
 * </pre></code>
 * </p><p>
 * Existence of the elements MessageHeader and Result:
 * <table border="1">
 * <tr><th>direction</th><th>MessageHeader</th><th>Result</th></tr>
 * <tr><td>unwrap</td><td>optional</td><td>optional</td></tr>
 * <tr><td>wrap</td><td>mandatory</td><td>mandatory</td></tr>
 *  </table>
 * </p><p>
 * If direction=unwrap and one of the following conditions is true a PipeRunException is thrown:
 * <ul><li>Result/Status in the response SOAP body equals 'ERROR'</li>
 * <li>faultcode in the response SOAP fault is not empty</li></ul>
 * </p>
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>{@link #setName(String) name}</td><td>name of the Pipe</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setDirection(String) direction}</td><td>either <code>wrap</code> or <code>unwrap</code></td><td>wrap</td></tr>
 * <tr><td>{@link #setInputXPath(String) inputXPath}</td><td>(only used when direction=unwrap) xpath expression to extract the message which is returned. The initial message is the content of the soap body. If empty, the content of the soap body is passed (without the root body)</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setInputNamespaceDefs(String) inputNamespaceDefs}</td><td>(only used when direction=unwrap) namespace defintions for xpathExpression. Must be in the form of a comma or space separated list of <code>prefix=namespaceuri</code>-definitions</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setMessageHeaderInSoapBody(boolean) messageHeaderInSoapBody}</td><td>when <code>true</code>, the MessageHeader is put in the SOAP body instead of in the SOAP header (first one is the old standard)</td><td><code>false</code></td></tr>
 * <tr><td>{@link #setMessageHeaderSessionKey(String) messageHeaderSessionKey}</td><td>
 * <table> 
 * <tr><td><code>direction=unwrap</code></td><td>name of the session key to store the input MessageHeader in</td></tr>
 * <tr><td><code>direction=wrap</code></td><td>name of the session key the original input MessageHeader is stored in; used to create the output MessageHeader</td></tr>
 * </table> 
 * </td><td>messageHeader</td></tr>
 * <tr><td>{@link #setMessageHeaderNamespace(String) messageHeaderNamespace}</td><td>namespace used in MessageHeader</td><td>http://www.nn.nl/</td></tr>
 * <tr><td>{@link #setMessageHeaderConversationIdSessionKey(String) messageHeaderConversationIdSessionKey}</td><td>(only used when direction=wrap and the original input MessageHeader doesn't exist) key of session variable to retrieve the ConversationId for the output MessageHeader from</td><td>messageHeaderConversationId</td></tr>
 * <tr><td>{@link #setMessageHeaderExternalRefToMessageIdSessionKey(String) messageHeaderExternalRefToMessageIdSessionKey}</td><td>(only used when direction=wrap and the original input MessageHeader doesn't exist) key of session variable to retrieve the ExternalRefToMessageId for the output MessageHeader from</td><td>messageHeaderExternalRefToMessageId</td></tr>
 * <tr><td>{@link #setResultInPayload(boolean) resultInPayload}</td><td>when <code>true</code>, the Result is put in the payload (as last child in root tag) instead of in the SOAP body as sibling of the payload (last one is the old standard)</td><td><code>true</code></td></tr>
 * <tr><td>{@link #setResultNamespace(String) resultNamespace}</td><td>namespace used in Result</td><td>http://www.nn.nl/</td></tr>
 * <tr><td>{@link #setResultErrorCodeSessionKey(String) resultErrorCodeSessionKey}</td><td>(only used when direction=wrap) key of session variable to store the Result error code in (if an error occurs)</td><td>resultErrorCode</td></tr>
 * <tr><td>{@link #setResultErrorTextSessionKey(String) resultErrorTextSessionKey}</td><td>(only used when direction=wrap) key of session variable to store the Result error text in (if an error occurs). If not specified or no value retrieved, the following error text is derived from the error code: 
 *   <table border="1">
 *   <tr><th>errorCode</th><th>errorText</th></tr>
 *   <tr><td>ERR6002</td><td>Service Interface Request Time Out</td></tr>
 *   <tr><td>ERR6003</td><td>Invalid Request Message</td></tr>
 *   <tr><td>ERR6004</td><td>Invalid Backend system response</td></tr>
 *   <tr><td>ERR6005</td><td>Backend system failure response</td></tr>
 *   <tr><td>ERR6999</td><td>Unspecified Errors</td></tr>
 *  </table></td><td>resultErrorText</td></tr>
 * <tr><td>{@link #setResultErrorReasonSessionKey(String) resultErrorReasonSessionKey}</td><td>(only used when direction=wrap and an error occurs) key of session variable to store the Result error reason in</td><td>resultErrorReason</td></tr>
 * <tr><td>{@link #setResultServiceName(String) resultServiceName}</td><td>(only used when direction=wrap) name of the service; used in the Result error response</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setResultActionName(String) resultActionName}</td><td>(only used when direction=wrap) name of the operation; used in the Result error response</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setOutputRootOnError(String) outputRootOnError}</td><td>(only used when direction=wrap and an error occurs) name of output root element in the SOAP body. If empty, the input message is used in the response</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setOutputNamespaceOnError(String) outputNamespaceOnError}</td><td>(only used when direction=wrap, outputRoot is not empty and an error occurs) namespace of the output root element in the SOAP body</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setRemoveOutputNamespaces(boolean) removeOutputNamespaces}</td><td>(only used when direction=unwrap) when <code>true</code>, namespaces (and prefixes) in the output are removed</td><td>false</td></tr>
 * <tr><td>{@link #setOmitResult(boolean) omitResult}</td><td>(only used when direction=wrap) when <code>true</code>, the Result is omitted and instead of Result/Status=ERROR a PipeRunException is thrown</td><td><code>false</code></td></tr>
 * <tr><td>{@link #setOutputNamespace(String) outputNamespace}</td><td>(only used when direction=wrap) when not empty, namespace of the output root element in the SOAP body</td><td>&nbsp;</td></tr>
 * </table></p>
 * <p><b>Exits:</b>
 * <table border="1">
 * <tr><th>state</th><th>condition</th></tr>
 * <tr><td>"success"</td><td>default</td></tr>
 * <tr><td><i>{@link #setForwardName(String) forwardName}</i></td><td>if specified</td></tr>
 * </table>
 * </p>
 * @version Id
 * @author Peter Leeuwenburgh
 */
public class ExtendedSoapWrapperPipe extends SoapWrapperPipe {
	private final static String soapNamespaceDefs = "soapenv=http://schemas.xmlsoap.org/soap/envelope/";
	private final static String soapHeaderXPath = "soapenv:Envelope/soapenv:Header";
	private final static String soapBodyXPath = "soapenv:Envelope/soapenv:Body";
	private final static String soapErrorXPath = "soapenv:Fault/faultcode";
	private final static String messageHeaderXPath = "mh:MessageHeader";
	private final static String messageHeaderConversationIdXPath = "mh:MessageHeader/mh:HeaderFields/mh:ConversationId";
	private final static String messageHeaderExternalRefToMessageIdXPath = "mh:MessageHeader/mh:HeaderFields/mh:MessageId";
	private final static String resultErrorXPath = "re:Result/re:Status='ERROR'";

	private final static String[][] ERRORS = { { "ERR6002", "Service Interface Request Time Out" }, {
			"ERR6003", "Invalid Request Message" }, {
			"ERR6004", "Invalid Backend system response" }, {
			"ERR6005", "Backend system failure response" }, {
			"ERR6999", "Unspecified Errors" }
	};

	private String inputXPath = null;
	private String inputNamespaceDefs = null;
	private boolean messageHeaderInSoapBody = false;
	private String messageHeaderSessionKey = "messageHeader";
	private String messageHeaderNamespace = "http://www.nn.nl/";
	private String messageHeaderConversationIdSessionKey = "messageHeaderConversationId";
	private String messageHeaderExternalRefToMessageIdSessionKey = "messageHeaderExternalRefToMessageId";
	private boolean resultInPayload = true;
	private String resultNamespace = "http://www.nn.nl/";
	private String resultErrorCodeSessionKey = "resultErrorCode";
	private String resultErrorTextSessionKey = "resultErrorText";
	private String resultErrorReasonSessionKey = "resultErrorReason";
	private String resultServiceName = null;
	private String resultActionName = null;
	private String outputRootOnError = null;
	private String outputNamespaceOnError = null;
	private boolean removeOutputNamespaces = false;
	private boolean omitResult = false;
	private String outputNamespace = null;

	private TransformerPool bodyMessageTp;
	private TransformerPool messageHeaderTp;
	private TransformerPool messageHeaderConversationIdTp;
	private TransformerPool messageHeaderExternalRefToMessageIdTp;
	private TransformerPool resultErrorTp;
	private String resultErrorXe;
	private TransformerPool removeOutputNamespacesTp;
	private TransformerPool outputNamespaceTp;

	public void configure() throws ConfigurationException {
		super.configure();
		if (StringUtils.isNotEmpty(getSoapHeaderSessionKey())) {
			throw new ConfigurationException(getLogPrefix(null) + "soapHeaderSessionKey is not allowed");
		}
		if (StringUtils.isEmpty(getMessageHeaderSessionKey())) {
			throw new ConfigurationException(getLogPrefix(null) + "messageHeaderSessionKey must be set");
		}
		if (StringUtils.isEmpty(getOutputRootOnError()) && StringUtils.isNotEmpty(getOutputNamespaceOnError())) {
			throw new ConfigurationException(getLogPrefix(null) + "outputRootOnError must be set when outputNamespaceOnError is set");
		}
		try {
			if (StringUtils.isNotEmpty(getInputXPath())) {
				String bodyMessageNd = StringUtils.isNotEmpty(getInputNamespaceDefs()) ? soapNamespaceDefs + "\n" + getInputNamespaceDefs() : soapNamespaceDefs;
				String bodyMessageXe = StringUtils.isNotEmpty(getInputXPath()) ? soapBodyXPath + "/" + getInputXPath() : soapBodyXPath + "/*";
				bodyMessageTp = new TransformerPool(XmlUtils.createXPathEvaluatorSource(bodyMessageNd, bodyMessageXe, "xml"));
			}
			String messageHeaderNamespaceDefs = "mh=" + getMessageHeaderNamespace();
			String messageHeaderNd = soapNamespaceDefs + "\n"  + messageHeaderNamespaceDefs;
			String messageHeaderXe;
			if (isMessageHeaderInSoapBody()) {
				messageHeaderXe = soapBodyXPath + "/" + messageHeaderXPath;
			} else {
				messageHeaderXe = soapHeaderXPath + "/" + messageHeaderXPath;
			}
			messageHeaderTp = new TransformerPool(XmlUtils.createXPathEvaluatorSource(messageHeaderNd, messageHeaderXe, "xml"));
			messageHeaderConversationIdTp = new TransformerPool(XmlUtils.createXPathEvaluatorSource(messageHeaderNamespaceDefs, messageHeaderConversationIdXPath, "text"));
			messageHeaderExternalRefToMessageIdTp = new TransformerPool(XmlUtils.createXPathEvaluatorSource(messageHeaderNamespaceDefs, messageHeaderExternalRefToMessageIdXPath, "text"));

			String resultNamespaceDefs = "re=" + getResultNamespace();
			String resultErrorNd = soapNamespaceDefs + "\n"  + resultNamespaceDefs;
			if (isResultInPayload()) {
				resultErrorXe = soapBodyXPath + "/*/" + resultErrorXPath;
			} else {
				resultErrorXe = soapBodyXPath + "/" + resultErrorXPath;
			}
			resultErrorXe = resultErrorXe + " or string-length(" + soapBodyXPath + "/" + soapErrorXPath + ")&gt;0";
			resultErrorTp = new TransformerPool(XmlUtils.createXPathEvaluatorSource(resultErrorNd, resultErrorXe, "text"));
			if (isRemoveOutputNamespaces()) {
				String removeOutputNamespaces_xslt = XmlUtils.makeRemoveNamespacesXslt(true, false);
				removeOutputNamespacesTp = new TransformerPool(removeOutputNamespaces_xslt);
			}
			if (StringUtils.isNotEmpty(getOutputNamespace())) {
				String outputNamespace_xslt = XmlUtils.makeAddRootNamespaceXslt(getOutputNamespace(), true, false);
				outputNamespaceTp = new TransformerPool(outputNamespace_xslt);
			}
		} catch (TransformerConfigurationException e) {
			throw new ConfigurationException(getLogPrefix(null) + "cannot create transformer", e);
		}
	}

	public PipeRunResult doPipe(Object input, PipeLineSession session) throws PipeRunException {
		String output;
		try {
			if ("wrap".equalsIgnoreCase(getDirection())) {
				String originalMessageHeader = (String) session.get(getMessageHeaderSessionKey());
				String messageHeaderConversationId = null;
				String messageHeaderExternalRefToMessageId = null;
				if (originalMessageHeader == null) {
					if (StringUtils.isNotEmpty(getMessageHeaderConversationIdSessionKey())) {
						messageHeaderConversationId = (String) session.get(getMessageHeaderConversationIdSessionKey());
					}
					if (StringUtils.isNotEmpty(getMessageHeaderExternalRefToMessageIdSessionKey())) {
						messageHeaderExternalRefToMessageId = (String) session.get(getMessageHeaderExternalRefToMessageIdSessionKey());
					}
				}
				String messageHeader = prepareMessageHeader(originalMessageHeader, messageHeaderConversationId, messageHeaderExternalRefToMessageId);

				String resultErrorCode = null;
				if (StringUtils.isNotEmpty(getResultErrorCodeSessionKey())) {
					resultErrorCode = (String) session.get(getResultErrorCodeSessionKey());
				}
				String resultErrorText = null;
				String resultDetailText = null;
				if (resultErrorCode != null) {
					if (StringUtils.isNotEmpty(getResultErrorTextSessionKey())) {
						resultErrorText = (String) session.get(getResultErrorTextSessionKey());
					}
					if (resultErrorText == null) {
						resultErrorText = errorCodeToText(resultErrorCode);
					}
					if (StringUtils.isNotEmpty(getResultErrorReasonSessionKey())) {
						resultDetailText = (String) session.get(getResultErrorReasonSessionKey());
					}
					if (isOmitResult()) {
						throw new PipeRunException(this, getLogPrefix(session) + "error occured: code [" + resultErrorCode + "], text [" + resultErrorText + "]");
					}
				}
				String result = null;
				if (!isOmitResult()) {
					result = prepareResult(resultErrorCode, resultErrorText, getResultServiceName(), getResultActionName(), resultDetailText);
				}
				String payload;
				if (resultErrorCode != null && StringUtils.isNotEmpty(getOutputRootOnError())) {
					XmlBuilder outputElement = new XmlBuilder(getOutputRootOnError());
					if (StringUtils.isNotEmpty(getOutputNamespaceOnError())) {
						outputElement.addAttribute("xmlns", getOutputNamespaceOnError());
					}
					payload = outputElement.toXML();
				} else {
					if (outputNamespaceTp != null) {
						payload = outputNamespaceTp.transform(input.toString(), null, true);
					} else {
						payload = input.toString();
					}
				}
				payload = prepareReply(payload, isMessageHeaderInSoapBody() ? messageHeader : null, result, isResultInPayload());

				output = wrapMessage(payload, isMessageHeaderInSoapBody() ? null : messageHeader);
			} else {
				String body = unwrapMessage(input.toString());
				if (StringUtils.isEmpty(body)) {
					throw new PipeRunException(this, getLogPrefix(session) + "SOAP body is empty or message is not a SOAP message");
				}
				if (messageHeaderTp != null) {
					String messageHeader = messageHeaderTp.transform(input.toString(), null, true);
					if (messageHeader != null) {
						session.put(getMessageHeaderSessionKey(), messageHeader);
						log.debug(getLogPrefix(session) + "stored [" + messageHeader + "] in pipeLineSession under key [" + getMessageHeaderSessionKey() + "]");
					}
				}
				if (resultErrorTp != null) {
					String resultError = resultErrorTp.transform(input.toString(), null, true);
					if (Boolean.valueOf(resultError).booleanValue()) {
						throw new PipeRunException(this, getLogPrefix(session) + "resultErrorXPath [" + resultErrorXe + "] returns true");
					}
				}
				if (bodyMessageTp != null) {
					output = bodyMessageTp.transform(input.toString(), null, true);
				} else {
					output = body;
				}
				if (removeOutputNamespacesTp != null) {
					output = removeOutputNamespacesTp.transform(output, null, true);
				}
			}
		} catch (Throwable t) {
			throw new PipeRunException(this, getLogPrefix(session) + " Unexpected exception during (un)wrapping ", t);

		}
		return new PipeRunResult(getForward(), output);
	}

	private String prepareMessageHeader(String originalMessageHeader, String conversationId, String externalRefToMessageId) throws DomBuilderException, IOException, TransformerException {
		XmlBuilder messageHeaderElement = new XmlBuilder("MessageHeader");
		messageHeaderElement.addAttribute("xmlns", getMessageHeaderNamespace());
		XmlBuilder fromElement = new XmlBuilder("From");
		XmlBuilder idElement = new XmlBuilder("Id");
		idElement.setValue(AppConstants.getInstance().getProperty("instance.name", ""));
		fromElement.addSubElement(idElement);
		messageHeaderElement.addSubElement(fromElement);
		XmlBuilder headerFieldsElement = new XmlBuilder("HeaderFields");
		XmlBuilder conversationIdElement = new XmlBuilder("ConversationId");
		if (originalMessageHeader == null) {
			conversationIdElement.setValue(conversationId);
		} else {
			if (messageHeaderConversationIdTp != null) {
				conversationIdElement.setValue(messageHeaderConversationIdTp.transform(originalMessageHeader, null, true));
			}
		}
		headerFieldsElement.addSubElement(conversationIdElement);
		XmlBuilder messageIdElement = new XmlBuilder("MessageId");
		messageIdElement.setValue(Misc.getHostname() + "_" + Misc.createSimpleUUID());
		headerFieldsElement.addSubElement(messageIdElement);
		XmlBuilder externalRefToMessageIdElement = new XmlBuilder("ExternalRefToMessageId");
		if (originalMessageHeader == null) {
			externalRefToMessageIdElement.setValue(externalRefToMessageId);
		} else {
			if (messageHeaderExternalRefToMessageIdTp != null) {
				externalRefToMessageIdElement.setValue(messageHeaderExternalRefToMessageIdTp.transform(originalMessageHeader, null, true));
			}
		}
		headerFieldsElement.addSubElement(externalRefToMessageIdElement);
		XmlBuilder timestampElement = new XmlBuilder("Timestamp");
		timestampElement.setValue(DateUtils.format(new Date(), "yyyy-MM-dd'T'HH:mm:ss"));
		headerFieldsElement.addSubElement(timestampElement);
		messageHeaderElement.addSubElement(headerFieldsElement);
		return messageHeaderElement.toXML();
	}

	private String prepareResult(String errorCode, String errorText, String serviceName, String actionName, String detailText) throws DomBuilderException, IOException, TransformerException {
		XmlBuilder resultElement = new XmlBuilder("Result");
		resultElement.addAttribute("xmlns", getResultNamespace());
		XmlBuilder statusElement = new XmlBuilder("Status");
		if (errorCode == null) {
			statusElement.setValue("OK");
		} else {
			statusElement.setValue("ERROR");
		}
		resultElement.addSubElement(statusElement);
		if (errorCode != null) {
			XmlBuilder errorListElement = new XmlBuilder("ErrorList");
			XmlBuilder errorElement = new XmlBuilder("Error");
			XmlBuilder codeElement = new XmlBuilder("Code");
			codeElement.setValue(errorCode);
			errorElement.addSubElement(codeElement);
			XmlBuilder reasonElement = new XmlBuilder("Reason");
			reasonElement.setCdataValue(errorText);
			errorElement.addSubElement(reasonElement);
			XmlBuilder serviceElement = new XmlBuilder("Service");
			XmlBuilder serviceNameElement = new XmlBuilder("Name");
			serviceNameElement.setValue(serviceName);
			serviceElement.addSubElement(serviceNameElement);
			XmlBuilder serviceContextElement = new XmlBuilder("Context");
			serviceContextElement.setValue("1");
			serviceElement.addSubElement(serviceContextElement);
			XmlBuilder actionElement = new XmlBuilder("Action");
			XmlBuilder actionNameElement = new XmlBuilder("Name");
			actionNameElement.setValue(actionName);
			actionElement.addSubElement(actionNameElement);
			XmlBuilder actionVersionElement = new XmlBuilder("Version");
			actionVersionElement.setValue("1");
			actionElement.addSubElement(actionVersionElement);
			serviceElement.addSubElement(actionElement);
			errorElement.addSubElement(serviceElement);
			XmlBuilder detailListElement = new XmlBuilder("DetailList");
			XmlBuilder detailElement = new XmlBuilder("Detail");
			XmlBuilder detailCodeElement = new XmlBuilder("Code");
			detailElement.addSubElement(detailCodeElement);
			XmlBuilder detailTextElement = new XmlBuilder("Text");
			detailTextElement.setCdataValue(detailText);
			detailElement.addSubElement(detailTextElement);
			detailListElement.addSubElement(detailElement);
			errorElement.addSubElement(detailListElement);
			errorListElement.addSubElement(errorElement);
			resultElement.addSubElement(errorListElement);
		}
		return resultElement.toXML();
	}

	private String errorCodeToText(String errorCode) {
		for (int i = 0; i < ERRORS.length; i++) {
			if (errorCode.equals(ERRORS[i][0])) {
				return ERRORS[i][1];
			}
		}
		return null;
	}

	private String prepareReply(String rawReply, String messageHeader, String result, boolean resultInPayload) throws DomBuilderException, IOException, TransformerException {
		ArrayList messages = new ArrayList();
		if (messageHeader != null) {
			messages.add(messageHeader);
		}
		messages.add(rawReply);

		String payload = null;
		if (result == null) {
			payload = Misc.listToString(messages);
		} else {
			if (resultInPayload) {
				String message = Misc.listToString(messages);
				Document messageDoc = XmlUtils.buildDomDocument(message);
				Node messageRootNode = messageDoc.getFirstChild();
				Node resultNode = messageDoc.importNode(XmlUtils.buildNode(result), true);
				messageRootNode.appendChild(resultNode);
				payload = XmlUtils.nodeToString(messageDoc);
			} else {
				messages.add(result);
				payload = Misc.listToString(messages);
			}
		}
		return payload;
	}

	public void setInputXPath(String inputXPath) {
		this.inputXPath = inputXPath;
	}

	public String getInputXPath() {
		return inputXPath;
	}

	public void setInputNamespaceDefs(String inputNamespaceDefs) {
		this.inputNamespaceDefs = inputNamespaceDefs;
	}

	public String getInputNamespaceDefs() {
		return inputNamespaceDefs;
	}

	public void setMessageHeaderInSoapBody(boolean b) {
		messageHeaderInSoapBody = b;
	}

	public boolean isMessageHeaderInSoapBody() {
		return messageHeaderInSoapBody;
	}

	public void setMessageHeaderSessionKey(String messageHeaderSessionKey) {
		this.messageHeaderSessionKey = messageHeaderSessionKey;
	}

	public String getMessageHeaderSessionKey() {
		return messageHeaderSessionKey;
	}

	public void setMessageHeaderNamespace(String messageHeaderNamespace) {
		this.messageHeaderNamespace = messageHeaderNamespace;
	}

	public String getMessageHeaderNamespace() {
		return messageHeaderNamespace;
	}

	public void setMessageHeaderConversationIdSessionKey(String messageHeaderConversationIdSessionKey) {
		this.messageHeaderConversationIdSessionKey = messageHeaderConversationIdSessionKey;
	}

	public String getMessageHeaderConversationIdSessionKey() {
		return messageHeaderConversationIdSessionKey;
	}

	public void setMessageHeaderExternalRefToMessageIdSessionKey(String messageHeaderExternalRefToMessageIdSessionKey) {
		this.messageHeaderExternalRefToMessageIdSessionKey = messageHeaderExternalRefToMessageIdSessionKey;
	}

	public String getMessageHeaderExternalRefToMessageIdSessionKey() {
		return messageHeaderExternalRefToMessageIdSessionKey;
	}

	public void setResultInPayload(boolean b) {
		resultInPayload = b;
	}

	public boolean isResultInPayload() {
		return resultInPayload;
	}

	public void setResultNamespace(String resultNamespace) {
		this.resultNamespace = resultNamespace;
	}

	public String getResultNamespace() {
		return resultNamespace;
	}

	public void setResultErrorCodeSessionKey(String resultErrorCodeSessionKey) {
		this.resultErrorCodeSessionKey = resultErrorCodeSessionKey;
	}

	public String getResultErrorCodeSessionKey() {
		return resultErrorCodeSessionKey;
	}

	public void setResultErrorTextSessionKey(String resultErrorTextSessionKey) {
		this.resultErrorTextSessionKey = resultErrorTextSessionKey;
	}

	public String getResultErrorTextSessionKey() {
		return resultErrorTextSessionKey;
	}

	public void setResultErrorReasonSessionKey(String resultErrorReasonSessionKey) {
		this.resultErrorReasonSessionKey = resultErrorReasonSessionKey;
	}

	public String getResultErrorReasonSessionKey() {
		return resultErrorReasonSessionKey;
	}

	public void setResultServiceName(String resultServiceName) {
		this.resultServiceName = resultServiceName;
	}

	public String getResultServiceName() {
		return resultServiceName;
	}

	public void setResultActionName(String resultActionName) {
		this.resultActionName = resultActionName;
	}

	public String getResultActionName() {
		return resultActionName;
	}

	public void setOutputRootOnError(String outputRootOnError) {
		this.outputRootOnError = outputRootOnError;
	}

	public String getOutputRootOnError() {
		return outputRootOnError;
	}

	public void setOutputNamespaceOnError(String outputNamespaceOnError) {
		this.outputNamespaceOnError = outputNamespaceOnError;
	}

	public String getOutputNamespaceOnError() {
		return outputNamespaceOnError;
	}

	public void setRemoveOutputNamespaces(boolean b) {
		removeOutputNamespaces = b;
	}

	public boolean isRemoveOutputNamespaces() {
		return removeOutputNamespaces;
	}

	public void setOmitResult(boolean b) {
		omitResult = b;
	}

	public boolean isOmitResult() {
		return omitResult;
	}

	public void setOutputNamespace(String outputNamespace) {
		this.outputNamespace = outputNamespace;
	}

	public String getOutputNamespace() {
		return outputNamespace;
	}
}