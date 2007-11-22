/*
 * $Log: IfsaFFSessionBean.java,v $
 * Revision 1.2  2007-11-22 08:48:19  europe\L190409
 * update from ejb-branch
 *
 * Revision 1.1.2.2  2007/11/15 12:59:51  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * Add bit more logging
 *
 * Revision 1.1.2.1  2007/10/29 12:25:34  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * Create EJb Beans required to connect to IFSA J2EE implementation as an IFSA Provider application
 *
 * 
 */

package nl.nn.adapterframework.extensions.ifsa.ejb;

import com.ing.ifsa.api.FireForgetService;
import com.ing.ifsa.api.ServiceRequest;
import com.ing.ifsa.exceptions.ServiceException;
import java.rmi.RemoteException;
import javax.ejb.SessionBean;

/**
 *
 * @author Tim van der Leeuw
 * @version Id
 */
public class IfsaFFSessionBean extends IfsaEjbBeanBase implements SessionBean, FireForgetService {

    public void onServiceRequest(ServiceRequest request) throws RemoteException, ServiceException {
        log.debug(">>> onServiceRequest() Processing FF Request from IFSA");
        processRequest(request);
        log.debug("<<< onServiceRequest() Done processing FF Request from IFSA");
    }

}
