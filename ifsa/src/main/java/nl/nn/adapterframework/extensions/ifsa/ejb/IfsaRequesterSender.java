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
package nl.nn.adapterframework.extensions.ifsa.ejb;

import com.ing.ifsa.exceptions.IFSAException;
import com.ing.ifsa.api.BusinessMessage;
import com.ing.ifsa.api.Connection;
import com.ing.ifsa.api.ConnectionManager;
import com.ing.ifsa.api.FireForgetAccessBean;
import com.ing.ifsa.api.RequestReplyAccessBean;
import com.ing.ifsa.api.ServiceReply;
import com.ing.ifsa.api.ServiceRequest;
import com.ing.ifsa.api.ServiceURI;
import java.util.HashMap;
import java.util.Map;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.HasPhysicalDestination;
import nl.nn.adapterframework.core.INamedObject;
import nl.nn.adapterframework.core.ISenderWithParameters;
import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.extensions.ifsa.IfsaMessageProtocolEnum;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.ParameterList;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.parameters.ParameterValueList;

/**
 * IFSA Request sender for FF and RR requests implemented using the IFSA
 * J2EE api.
 * 
 * @author Tim van der Leeuw
 */
public class IfsaRequesterSender extends IfsaEjbBase implements ISenderWithParameters, INamedObject, HasPhysicalDestination {
    
    protected ParameterList paramList = null;
    
	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		if (paramList != null) {
			paramList.configure();
		}

		log.info(getLogPrefix() + " configured sender on " + getPhysicalDestinationName());

//		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void open() throws SenderException {
		// nothing special
	}

	@Override
	public void close() throws SenderException {
		// nothing special
	}

	protected Map<String, String> convertParametersToMap(ParameterResolutionContext prc) throws SenderException {
		ParameterValueList paramValueList;
		try {
			paramValueList = prc.getValues(paramList);
		} catch (ParameterException e) {
			throw new SenderException(getLogPrefix() + "caught ParameterException in sendMessage() determining serviceId", e);
		}
		Map<String, String> params = new HashMap<String, String>();
		if (paramValueList != null && paramList != null) {
			for (int i = 0; i < paramList.size(); i++) {
				String key = paramList.getParameter(i).getName();
				String value = paramValueList.getParameterValue(i).asStringValue(null);
				params.put(key, value);
			}
		}
		return params;
	}

	@Override
	public boolean isSynchronous() {
		return getMessageProtocolEnum().equals(IfsaMessageProtocolEnum.REQUEST_REPLY);
	}

	@Override
	public String sendMessage(String dummyCorrelationId, String message) throws SenderException, TimeOutException {
		return sendMessage(dummyCorrelationId, message, (Map<String, String>) null);
	}

	@Override
	public String sendMessage(String dummyCorrelationId, String message, ParameterResolutionContext prc) throws SenderException, TimeOutException {
		Map<String, String> params = convertParametersToMap(prc);
		return sendMessage(dummyCorrelationId, message, params);
	}

    /**
     * Execute a request to the IFSA service.
     * @return in Request/Reply, the retrieved message or TIMEOUT, otherwise null
     */
    public String sendMessage(String dummyCorrelationId, String message, Map<String, String> params) throws SenderException, TimeOutException {
        Connection conn = null;
        Map<String, String> udzMap = null;
        
        try {
            String realServiceId;
            // Extract parameters
            if (params != null && params.size() > 0) {
                // Use first param as serviceId
                realServiceId = (String)params.get("serviceId");
                if (realServiceId == null) {
                        realServiceId = getServiceId();
                }
                String occurrence = (String)params.get("occurrence");
                if (occurrence != null) {
                    int i = realServiceId.indexOf('/', realServiceId.indexOf('/', realServiceId.indexOf('/', realServiceId.indexOf('/') + 1) + 1) + 1);
                    int j = realServiceId.indexOf('/', i + 1);
                    realServiceId = realServiceId.substring(0, i + 1) + occurrence + realServiceId.substring(j);
                }

                // Use remaining params as outgoing UDZs
                udzMap = new HashMap<String, String>(params);
                udzMap.remove("serviceId");
                udzMap.remove("occurrence");
            } else {
                realServiceId = getServiceId();
            }
            
            // Open connection to the Application ID
            conn = ConnectionManager.getConnection(getApplicationId());

            // Create the request, and set the Service URI to the Service ID
            ServiceRequest request = new ServiceRequest(new BusinessMessage(message));
            request.setServiceURI(new ServiceURI(realServiceId));
            addUdzMapToRequest(udzMap, request);
            if (isSynchronous()) {
                // RR handling
                if (timeOut > 0) {
                    request.setTimeout(timeOut);
                }
                RequestReplyAccessBean rrBean = RequestReplyAccessBean.getInstance();
                ServiceReply reply = rrBean.sendReceive(conn, request);
                return reply.getBusinessMessage().getText();
            } else {
                // FF handling
                FireForgetAccessBean ffBean = FireForgetAccessBean.getInstance();
                ffBean.send(conn, request);
                return null;
            }
        } catch (com.ing.ifsa.exceptions.TimeoutException toe) {
            throw new TimeOutException(toe);
        } catch (IFSAException e) {
            throw new SenderException(e);
        } finally {
            if (conn != null) {
                conn.close();
            }
        }
    }

	@Override
	public void addParameter(Parameter p) {
		if (paramList == null) {
			paramList = new ParameterList();
		}
		paramList.add(p);
	}

	@Override
	public ParameterList getParameterList() {
		return paramList;
	}

}
