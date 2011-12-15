/*
 * $Log: EsbSoapWrapperPipe.java,v $
 * Revision 1.1  2011-12-15 10:53:08  europe\m168309
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
 * @version Id
 * @author Peter Leeuwenburgh
 */
public class EsbSoapWrapperPipe extends SoapWrapperPipe {
	private final static String FROMID= "fromId";
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
		boolean fromIdExists = false;
		boolean conversationIdExists = false;
		boolean messageIdExists = false;
		boolean correlationIdExists = false;
		boolean timestampExists = false;
		ParameterList parameterList = getParameterList();
		for (int i = 0; i < parameterList.size(); i++) {
			Parameter parameter = parameterList.getParameter(i);
			if (parameter.getName().equalsIgnoreCase(FROMID)) {
				fromIdExists = true;
			} else if (parameter.getName().equalsIgnoreCase(CONVERSATIONID)) {
				conversationIdExists = true;
			} else if (parameter.getName().equalsIgnoreCase(MESSAGEID)) {
				messageIdExists = true;
			} else if (parameter.getName().equalsIgnoreCase(CORRELATIONID)) {
				correlationIdExists = true;
			} else if (parameter.getName().equalsIgnoreCase(TIMESTAMP)) {
				timestampExists = true;
			}
		}
		Parameter p;
		if (!fromIdExists) {
			p = new Parameter();
			p.setName(FROMID);
			p.setValue(AppConstants.getInstance().getProperty("instance.name", ""));
			addParameter(p);
		}
		if (!conversationIdExists) {
			p = new Parameter();
			p.setName(CONVERSATIONID);
			p.setSessionKey(SOAPHEADER);
			p.setXpathExpression("*/HeaderFields/ConversationId");
			addParameter(p);
		}
		if (!messageIdExists) {
			p = new Parameter();
			p.setName(MESSAGEID);
			p.setPattern("{hostname}_{uid}");
			addParameter(p);
		}
		if (!correlationIdExists) {
			p = new Parameter();
			p.setName(CORRELATIONID);
			p.setSessionKey(SOAPHEADER);
			p.setXpathExpression("*/HeaderFields/MessageId");
			addParameter(p);
		}
		if (!timestampExists) {
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