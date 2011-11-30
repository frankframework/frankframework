/*
 * $Log: IfsaRRSessionBeanRemote.java,v $
 * Revision 1.4  2011-11-30 13:51:57  europe\m168309
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:46  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.2  2007/11/22 08:48:20  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * update from ejb-branch
 *
 * Revision 1.1.2.1  2007/11/01 10:35:25  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * Add remote interfaces for IFSA Session beans, since that is what's expected by the IFSA libraries
 *
 */

package nl.nn.adapterframework.extensions.ifsa.ejb;

import com.ing.ifsa.api.RequestReplyService;
import javax.ejb.EJBObject;

/**
 *
 * @author Tim van der Leeuw
 * @version Id
 */
public interface IfsaRRSessionBeanRemote extends RequestReplyService, EJBObject {

}
