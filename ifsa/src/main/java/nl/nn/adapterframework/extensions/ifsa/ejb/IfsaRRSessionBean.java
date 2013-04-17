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

import com.ing.ifsa.api.BusinessMessage;
import com.ing.ifsa.api.RequestReplyService;
import com.ing.ifsa.api.ServiceReply;
import com.ing.ifsa.api.ServiceRequest;
import com.ing.ifsa.exceptions.ServiceException;
import java.rmi.RemoteException;
import javax.ejb.SessionBean;

/**
 *
 * @author Tim van der Leeuw
 * @version $Id$
 */
public class IfsaRRSessionBean extends IfsaEjbBeanBase implements SessionBean, RequestReplyService
{

    public ServiceReply onServiceRequest(ServiceRequest request) throws RemoteException, ServiceException {
        log.debug(">>> onServiceRequest() Processing RR Request from IFSA");
        String replyText = processRequest(request);

        ServiceReply reply = new ServiceReply(request, new BusinessMessage(replyText));
        log.debug("<<< onServiceRequest() Done processing RR Request from IFSA");
        return reply;
    }

}
