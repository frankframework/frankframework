/*
   Copyright 2013 Nationale-Nederlanden

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
package nl.nn.adapterframework.extensions.ifsa;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.jms.JmsException;
import nl.nn.adapterframework.jms.JmsSender;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.util.AppConstants;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.naming.NamingException;

/**
 * Extension of JmsSender which only adds parameters to simulate IFSA.
 *
 * <p><b>Configuration </b><i>(where deviating from JmsSender)</i><b>:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>{@link #setMessageType(String) messageType}</td><td>type of messages. Possible values:
 * <ul>
 *   <li>rr_request</li>
 *   <li>rr_reply</li>
 *   <li>ff_request</li>
 * </ul> When messageType=rr_reply, the destination is retrieved from session key <code>replyTo</code></td><td>&nbsp;</td></tr>
 * </table></p>
 * <p><b>added parameters:</b>
 * <table border="1">
 * <tr><th>name</th><th>type</th><th>sessionKey</th><th>defaultValue</th><th>pattern</th><th>value</th><th>minLength</th><th/></tr>
 * <tr><td>ifsa</td><td></td><td></td><td></td><td></td><td>_IFSA_HEADER_</td><td></td><td></td></tr>
 * <tr><td>ifsa_api</td><td></td><td></td><td></td><td></td><td>jms_wrapper</td><td></td><td></td></tr>
 * <tr><td>ifsa_api_version</td><td></td><td></td><td></td><td></td><td>22.30.020</td><td></td><td></td></tr>
 * <tr><td>ifsa_auth_flag</td><td></td><td></td><td></td><td></td><td>1</td><td></td><td></td></tr>
 * <tr><td>ifsa_bif_id</td><td></td><td><code>rr_reply: </code>ifsa_bif_id</td><td></td><td><code>rr_request/ff_request: </code>${IFSAApplicationID}#{ifsa_destination}#{uid}</td><td></td><td></td><td></td></tr>
 * <tr><td>ifsa_bif_type</td><td></td><td></td><td></td><td></td><td><code>rr_request/rr_reply: </code>0<br/><code>ff_request: </code>2</td><td></td><td></td></tr>
 * <tr><td>ifsa_bulk</td><td></td><td></td><td></td><td></td><td>0</td><td></td><td></td></tr>
 * <tr><td>ifsa_bulk_auth_flag</td><td></td><td></td><td></td><td></td><td>0</td><td></td><td></td></tr>
 * <tr><td>ifsa_cil_version</td><td></td><td></td><td></td><td></td><td>22.30.009</td><td></td><td></td></tr>
 * <tr><td>ifsa_comp_algo</td><td></td><td>null</td><td>""</td><td></td><td></td><td></td><td></td></tr>
 * <tr><td>ifsa_destination</td><td></td><td><code>rr_reply: </code>ifsa_source</td><td></td><td></td><td></td><td></td><td><code>rr_request/ff_request: </code>to be set in IBIS configuration</td></tr>
 * <tr><td>ifsa_expiry</td><td></td><td></td><td></td><td></td><td><code>rr_request/rr_reply: </code>${timeOutIFSARR}<br/><code>ff_request: </code>0</td><td></td><td></td></tr>
 * <tr><td>ifsa_header_version</td><td></td><td></td><td></td><td></td><td>02.02.000</td><td></td><td></td></tr>
 * <tr><td>ifsa_hop_count</td><td></td><td></td><td></td><td></td><td>000</td><td></td><td></td></tr>
 * <tr><td>ifsa_node_id</td><td></td><td></td><td></td><td></td><td>${ifsa_node_id}</td><td></td><td></td></tr>
 * <tr><td>ifsa_ori_area</td><td></td><td><code>ff_request: </code>null</td><td><code>ff_request: </code>""</td><td></td><td><code>rr_request/rr_reply: </code>${ifsa_ori_area}</td><td>60</td><td></td></tr>
 * <tr><td>ifsa_ori_format</td><td></td><td>null</td><td>""</td><td></td><td></td><td></td><td></td></tr>
 * <tr><td>ifsa_ori_length</td><td></td><td>null</td><td>""</td><td></td><td></td><td></td><td></td></tr>
 * <tr><td>ifsa_ori_rtq</td><td></td><td><code>rr_reply/ff_request: </code>null</td><td><code>rr_reply/ff_request: </code>""</td><td></td><td><code>rr_request: </code>${ifsa_ori_rtq}</td><td>48</td><td></td></tr>
 * <tr><td>ifsa_ori_rtqm</td><td></td><td><code>rr_reply/ff_request: </code>null</td><td><code>rr_reply/ff_request: </code>""</td><td></td><td><code>rr_request: </code>${ifsa_ori_rtqm}</td><td>48</td><td></td></tr>
 * <tr><td>ifsa_priority</td><td></td><td></td><td></td><td></td><td><code>rr_request/rr_reply: </code>3<br/><code>ff_request: </code>2</td><td></td><td></td></tr>
 * <tr><td>ifsa_source</td><td></td><td></td><td></td><td><code>rr_reply: </code>${IFSAApplicationID}#{ifsa_destination}</td><td><code>rr_request/ff_request: </code>${IFSAApplicationID}</td><td></td><td></td></tr>
 * <tr><td>ifsa_unique_id</td><td></td><td></td><td></td><td>{uid}</td><td></td><td></td><td></td></tr>
 * <tr><td>JMS_IBM_MsgType</td><td>integer</td><td></td><td></td><td></td><td><code>rr_request: </code>1<br/><code>rr_reply: </code>2<br/><code>ff_request: </code>8</td><td></td><td></td></tr>
 * </table>
 * </p>
 *
 * @author  Peter Leeuwenburgh
 * @version $Id$
 */
public class IfsaSimulatorJmsSender extends JmsSender {
	private final static String RR_REQUEST = "rr_request";
	private final static String RR_REPLY = "rr_reply";
	private final static String FF_REQUEST = "ff_request";

	private String messageType = "";

	public void configure() throws ConfigurationException {
		if (!getMessageType().equalsIgnoreCase(RR_REQUEST) && !getMessageType().equalsIgnoreCase(RR_REPLY) && !getMessageType().equalsIgnoreCase(FF_REQUEST)) {
			throw new ConfigurationException(getLogPrefix() + "illegal value for messageType [" + getMessageType() + "], must be '" + RR_REQUEST + "', '" + RR_REPLY + "'"+ "' or '" + FF_REQUEST + "'");
		}

		Parameter p = new Parameter();
		p.setName("ifsa");
		p.setValue("_IFSA_HEADER_");
		addParameter(p);

		p = new Parameter();
		p.setName("ifsa_api");
		p.setValue("jms_wrapper");
		addParameter(p);

		p = new Parameter();
		p.setName("ifsa_api_version");
		p.setValue("22.30.020");
		addParameter(p);

		p = new Parameter();
		p.setName("ifsa_auth_flag");
		p.setValue("1");
		addParameter(p);

		p = new Parameter();
		p.setName("ifsa_bif_id");
		if (getMessageType().equalsIgnoreCase(RR_REQUEST)) {
			String iad = AppConstants.getInstance().getProperty("IFSAApplicationID", "");
			p.setPattern(iad+"#{ifsa_destination}#{uid}");
		} else if (getMessageType().equalsIgnoreCase(RR_REPLY)) {
			p.setSessionKey("ifsa_bif_id");
		} else if (getMessageType().equalsIgnoreCase(FF_REQUEST)) {
			String iad = AppConstants.getInstance().getProperty("IFSAApplicationID", "");
			p.setPattern(iad+"#{ifsa_destination}#{uid}");
		}
		addParameter(p);

		p = new Parameter();
		p.setName("ifsa_bif_type");
		if (getMessageType().equalsIgnoreCase(RR_REQUEST)) {
			p.setValue("0");
		} else if (getMessageType().equalsIgnoreCase(RR_REPLY)) {
			p.setValue("0");
		} else if (getMessageType().equalsIgnoreCase(FF_REQUEST)) {
			p.setValue("2");
		}
		addParameter(p);

		p = new Parameter();
		p.setName("ifsa_bulk");
		p.setValue("0");
		addParameter(p);

		p = new Parameter();
		p.setName("ifsa_bulk_auth_flag");
		p.setValue("0");
		addParameter(p);

		p = new Parameter();
		p.setName("ifsa_cil_version");
		p.setValue("22.30.009");
		addParameter(p);

		p = new Parameter();
		p.setName("ifsa_comp_algo");
		p.setDefaultValue("");
		p.setSessionKey("null");
		addParameter(p);

		if (getMessageType().equalsIgnoreCase(RR_REQUEST)) {
			//overruled in IBIS configuration
		} else if (getMessageType().equalsIgnoreCase(RR_REPLY)) {
			p = new Parameter();
			p.setName("ifsa_destination");
			p.setSessionKey("ifsa_source");
			addParameter(p);
		} else if (getMessageType().equalsIgnoreCase(FF_REQUEST)) {
			//overruled in IBIS configuration
		}

		p = new Parameter();
		p.setName("ifsa_expiry");
		if (getMessageType().equalsIgnoreCase(RR_REQUEST)) {
			p.setValue(AppConstants.getInstance().getProperty("timeOutIFSARR", ""));
		} else if (getMessageType().equalsIgnoreCase(RR_REPLY)) {
			p.setValue(AppConstants.getInstance().getProperty("timeOutIFSARR", ""));
		} else if (getMessageType().equalsIgnoreCase(FF_REQUEST)) {
			p.setValue("0");
		}
		addParameter(p);

		p = new Parameter();
		p.setName("ifsa_header_version");
		p.setValue("02.02.000");
		addParameter(p);

		p = new Parameter();
		p.setName("ifsa_hop_count");
		p.setValue("000");
		addParameter(p);

		p = new Parameter();
		p.setName("ifsa_node_id");
		p.setValue(AppConstants.getInstance().getProperty("ifsa_node_id", ""));
		addParameter(p);

		p = new Parameter();
		p.setName("ifsa_ori_area");
		if (getMessageType().equalsIgnoreCase(RR_REQUEST)) {
			p.setValue(AppConstants.getInstance().getProperty("ifsa_ori_area", ""));
		} else if (getMessageType().equalsIgnoreCase(RR_REPLY)) {
			p.setValue(AppConstants.getInstance().getProperty("ifsa_ori_area", ""));
		} else if (getMessageType().equalsIgnoreCase(FF_REQUEST)) {
			p.setDefaultValue("");
			p.setSessionKey("null");
		}
		p.setMinLength(60);
		addParameter(p);

		p = new Parameter();
		p.setName("ifsa_ori_format");
		p.setDefaultValue("");
		p.setSessionKey("null");
		addParameter(p);

		p = new Parameter();
		p.setName("ifsa_ori_length");
		p.setDefaultValue("");
		p.setSessionKey("null");
		addParameter(p);

		p = new Parameter();
		p.setName("ifsa_ori_rtq");
		if (getMessageType().equalsIgnoreCase(RR_REQUEST)) {
			p.setValue(AppConstants.getInstance().getProperty("ifsa_ori_rtq", ""));
		} else if (getMessageType().equalsIgnoreCase(RR_REPLY)) {
			p.setDefaultValue("");
			p.setSessionKey("null");
		} else if (getMessageType().equalsIgnoreCase(FF_REQUEST)) {
			p.setDefaultValue("");
			p.setSessionKey("null");
		}
		p.setMinLength(48);
		addParameter(p);

		p = new Parameter();
		p.setName("ifsa_ori_rtqm");
		if (getMessageType().equalsIgnoreCase(RR_REQUEST)) {
			p.setValue(AppConstants.getInstance().getProperty("ifsa_ori_rtqm", ""));
		} else if (getMessageType().equalsIgnoreCase(RR_REPLY)) {
			p.setDefaultValue("");
			p.setSessionKey("null");
		} else if (getMessageType().equalsIgnoreCase(FF_REQUEST)) {
			p.setDefaultValue("");
			p.setSessionKey("null");
		}
		p.setMinLength(48);
		addParameter(p);

		p = new Parameter();
		p.setName("ifsa_priority");
		if (getMessageType().equalsIgnoreCase(RR_REQUEST)) {
			p.setValue("3");
		} else if (getMessageType().equalsIgnoreCase(RR_REPLY)) {
			p.setValue("3");
		} else if (getMessageType().equalsIgnoreCase(FF_REQUEST)) {
			p.setValue("2");
		}
		addParameter(p);

		p = new Parameter();
		p.setName("ifsa_source");
		if (getMessageType().equalsIgnoreCase(RR_REQUEST)) {
			p.setValue(AppConstants.getInstance().getProperty("IFSAApplicationID", ""));
		} else if (getMessageType().equalsIgnoreCase(RR_REPLY)) {
			String iad = AppConstants.getInstance().getProperty("IFSAApplicationID", "");
			p.setPattern(iad+"#{ifsa_destination}");
		} else if (getMessageType().equalsIgnoreCase(FF_REQUEST)) {
			p.setValue(AppConstants.getInstance().getProperty("IFSAApplicationID", ""));
		}
		addParameter(p);

		p = new Parameter();
		p.setName("ifsa_unique_id");
		p.setPattern("{uid}");
		addParameter(p);

		p = new Parameter();
		p.setName("JMS_IBM_MsgType");
		if (getMessageType().equalsIgnoreCase(RR_REQUEST)) {
			p.setValue("1");
		} else if (getMessageType().equalsIgnoreCase(RR_REPLY)) {
			p.setValue("2");
		} else if (getMessageType().equalsIgnoreCase(FF_REQUEST)) {
			p.setValue("8");
		}
		p.setType(Parameter.TYPE_INTEGER);
		addParameter(p);

		if (getMessageType().equalsIgnoreCase(RR_REPLY) && getDestinationName()==null) {
 			if (paramList!=null) {
 				paramList.configure();
 			}
		} else {
 			super.configure();
		}
	}

	public Destination getDestination() throws NamingException, JMSException, JmsException  {
    	if (getMessageType().equalsIgnoreCase(RR_REPLY) && getDestinationName()==null) {
 			return null;
    	} else {
 		   return super.getDestination();
    	}
	}

	@Override
	public Destination getDestination(IPipeLineSession session) throws JmsException, NamingException, JMSException {
		if (getMessageType().equalsIgnoreCase(RR_REPLY) && getDestinationName()==null) {
			return (Destination) session.get("replyTo");
		} else {
			return super.getDestination(session);
		}
	}

	@Override
	public void setMessageType(String string) {
		messageType = string;
	}

	public String getMessageType() {
		return messageType;
	}
}
