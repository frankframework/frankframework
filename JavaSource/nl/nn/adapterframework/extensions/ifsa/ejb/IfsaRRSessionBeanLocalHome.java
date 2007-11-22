/*
 * $Log: IfsaRRSessionBeanLocalHome.java,v $
 * Revision 1.2  2007-11-22 08:48:19  europe\L190409
 * update from ejb-branch
 *
 * Revision 1.1.2.1  2007/10/29 12:25:34  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * Create EJb Beans required to connect to IFSA J2EE implementation as an IFSA Provider application
 *
 * 
 */

package nl.nn.adapterframework.extensions.ifsa.ejb;

import javax.ejb.EJBLocalHome;

/**
 *
 * @author Tim van der Leeuw
 * @version Id
 */
public interface IfsaRRSessionBeanLocalHome extends EJBLocalHome {
    public IfsaRRSessionBeanLocal create()
        throws javax.ejb.CreateException;

}
