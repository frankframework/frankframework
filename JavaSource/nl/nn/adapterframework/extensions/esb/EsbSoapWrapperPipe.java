/*
 * $Log: EsbSoapWrapperPipe.java,v $
 * Revision 1.2  2011-12-20 10:50:33  europe\m168309
 * completed mode 'i2t'
 *
 * Revision 1.1  2011/12/15 10:53:08  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * initial version
 *
 *
 */
package nl.nn.adapterframework.extensions.esb;

import org.apache.commons.lang.StringUtils;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.ParameterList;
import nl.nn.adapterframework.soap.SoapWrapperPipe;
import nl.nn.adapterframework.util.AppConstants;

/**
 * Extension to SoapWrapperPipe for separate modes.
 * 
 * <p><b>Configuration </b><i>(where deviating from SoapWrapperPipe)</i><b>:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>{@link #setMode(String) mode}</td><td><code>i2t</code> (ifsa2tibco)</td><td>&nbsp;</td></tr>
 * </table></p>
 * <p>
 * When mode=<code>i2t</code> the following SOAP Header is created through a stylesheet (<code>soapHeaderStyleSheet</code>) in which various parameters are used:
 * <table border="1">
 * <tr><th>element</th><th>level</th><th>value</th></tr>
 * <tr><td>MessageHeader</td><td>0</td><td>&nbsp;</td></tr>
 * <tr><td>From</td><td>1</td><td>&nbsp;</td></tr>
 * <tr><td>Id</td><td>2</td><td>$fromId</td></tr>
 * <tr><td>To</td><td>1</td><td>&nbsp;</td></tr>
 * <tr><td>Location</td><td>2</td><td>if $messageLayer='P2P' then<br/>&nbsp;$messagingLayer.$businessDomain.$applicationName.$applicationFunction.$paradigm<br/>else<br/>&nbsp;$messagingLayer.$businessDomain.$serviceLayer.$serviceName.$serviceContext.$serviceContextVersion.$operationName.$operationVersion.$paradigm</td></tr>
 * <tr><td>HeaderFields</td><td>1</td><td>&nbsp;</td></tr>
 * <tr><td>CPAId</td><td>2</td><td>$cpaId</td></tr>
 * <tr><td>ConversationId</td><td>2</td><td>$conversationId</td></tr>
 * <tr><td>CorrelationId</td><td>2</td><td>$correlationId (if empty then skip element)</td></tr>
 * <tr><td>MessageId</td><td>2</td><td>$messageId</td></tr>
 * <tr><td>Timestamp</td><td>2</td><td>$timestamp</td></tr>
 * <tr><td>Service</td><td>1</td><td>&nbsp;</td></tr>
 * <tr><td>Name</td><td>2</td><td>$serviceName</td></tr>
 * <tr><td>Context</td><td>2</td><td>$serviceContext</td></tr>
 * <tr><td>Action</td><td>2</td><td>&nbsp;</td></tr>
 * <tr><td>Paradigm</td><td>3</td><td>$paradigm</td></tr>
 * <tr><td>Name</td><td>3</td><td>$operationName</td></tr>
 * <tr><td>Version</td><td>3</td><td>$operationVersion</td></tr>
 * </table>
 * </p><b>Parameters:</b>
 * <table border="1">
 * <tr><th>name</th><th>default</th></tr>
 * <tr><td>businessDomain</td><td>&nbsp;</td></tr>
 * <tr><td>serviceName</td><td>&nbsp;</td></tr>
 * <tr><td>serviceContext</td><td>&nbsp;</td></tr>
 * <tr><td>serviceContextVersion</td><td>1</td></tr>
 * <tr><td>operationName</td><td>&nbsp;</td></tr>
 * <tr><td>operationVersion</td><td>1</td></tr>
 * <tr><td>paradigm</td><td>&nbsp;</td></tr>
 * <tr><td>applicationName</td><td>&nbsp;</td></tr>
 * <tr><td>applicationFunction</td><td>&nbsp;</td></tr>
 * <tr><td>messagingLayer</td><td>ESB</td></tr>
 * <tr><td>serviceLayer</td><td>&nbsp;</td></tr>
 * <tr><td>fromId</td><td>property 'instance.name'</td></tr>
 * <tr><td>cpaId</td><td>if applicable, copied from the original (received) SOAP Header</td></tr>
 * <tr><td>conversationId</td><td>if applicable, copied from the original (received) SOAP Header</td></tr>
 * <tr><td>messageId</td><td>parameter pattern '{hostname}_{uid}'</td></tr>
 * <tr><td>correlationId</td><td>if applicable, copied from MessageId in the original (received) SOAP Header</td></tr>
 * <tr><td>timestamp</td><td>parameter pattern '{now,date,yyyy-MM-dd'T'HH:mm:ss}'</td></tr>
 * </table>
 * </p>
 * @version Id
 * @author Peter Leeuwenburgh
 */
public class EsbSoapWrapperPipe extends SoapWrapperPipe {
	private final static String FROMID= "fromId";
	private final static String CPAID = "cpaId";
	private final static String CONVERSATIONID = "conversationId";
	private final static String MESSAGEID = "messageId";
	private final static String CORRELATIONID = "correlationId";
	private final static String TIMESTAMP = "timestamp";

	private final static String MODE_I2T = "i2t";
	private final static String SOAPHEADER = "soapHeader";

	private String mode = null;

	public void configure() throws ConfigurationException {
		if (StringUtils.isEmpty(getMode())) {
			throw new ConfigurationException(getLogPrefix(null) + "mode must be set");
		}
		if (!getMode().equalsIgnoreCase(MODE_I2T)) {
			throw new ConfigurationException(getLogPrefix(null)+"illegal value for mode ["+getMode()+"], must be '"+MODE_I2T+"'");
		}
		if ("unwrap".equalsIgnoreCase(getDirection())) {
			setSoapHeaderSessionKey(SOAPHEADER);
		}
		if ("wrap".equalsIgnoreCase(getDirection())) {
			setSoapHeaderStyleSheet("/xml/xsl/esb/soapHeader.xsl");
			addParameters();
		}
		super.configure();
	}

	private void addParameters() {
		ParameterList parameterList = getParameterList();
		Parameter p;
		if (parameterList.findParameter(FROMID)==null) {
			p = new Parameter();
			p.setName(FROMID);
			p.setValue(AppConstants.getInstance().getProperty("instance.name", ""));
			addParameter(p);
		}
		if (parameterList.findParameter(CPAID)==null) {
			p = new Parameter();
			p.setName(CPAID);
			p.setSessionKey(SOAPHEADER);
			p.setXpathExpression("*/HeaderFields/CPAId");
			addParameter(p);
		}
		if (parameterList.findParameter(CONVERSATIONID)==null) {
			p = new Parameter();
			p.setName(CONVERSATIONID);
			p.setSessionKey(SOAPHEADER);
			p.setXpathExpression("*/HeaderFields/ConversationId");
			addParameter(p);
		}
		if (parameterList.findParameter(MESSAGEID)==null) {
			p = new Parameter();
			p.setName(MESSAGEID);
			p.setPattern("{hostname}_{uid}");
			addParameter(p);
		}
		if (parameterList.findParameter(CORRELATIONID)==null) {
			p = new Parameter();
			p.setName(CORRELATIONID);
			p.setSessionKey(SOAPHEADER);
			p.setXpathExpression("*/HeaderFields/MessageId");
			addParameter(p);
		}
		if (parameterList.findParameter(TIMESTAMP)==null) {
			p = new Parameter();
			p.setName(TIMESTAMP);
			p.setPattern("{now,date,yyyy-MM-dd'T'HH:mm:ss}");
			addParameter(p);
		}
	}

	public void setMode(String string) {
		mode = string;
	}

	public String getMode() {
		return mode;
	}

}