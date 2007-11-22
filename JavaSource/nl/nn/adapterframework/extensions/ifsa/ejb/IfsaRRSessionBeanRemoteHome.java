/*
 * $Log: IfsaRRSessionBeanRemoteHome.java,v $
 * Revision 1.2  2007-11-22 08:48:20  europe\L190409
 * update from ejb-branch
 *
 * Revision 1.1.2.3  2007/11/08 13:51:30  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * create() method should return Remote versions of EJB Bean interfaces
 *
 * Revision 1.1.2.2  2007/11/08 13:31:36  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * create() method should throw RemoteException
 *
 * Revision 1.1.2.1  2007/11/01 10:35:25  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * Add remote interfaces for IFSA Session beans, since that is what's expected by the IFSA libraries
 *
 */

package nl.nn.adapterframework.extensions.ifsa.ejb;

import javax.ejb.EJBHome;

/**
 *
 * @author Tim van der Leeuw
 * @version Id
 */
public interface IfsaRRSessionBeanRemoteHome extends EJBHome {
    public IfsaRRSessionBeanRemote create()
        throws javax.ejb.CreateException, java.rmi.RemoteException;

}
