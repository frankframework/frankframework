/*
 * $Log: IbisManagerEjbLocal.java,v $
 * Revision 1.2  2007-10-09 16:07:37  europe\L190409
 * Direct copy from Ibis-EJB:
 * first version in HEAD
 *
 */
package nl.nn.adapterframework.ejb;

import javax.ejb.EJBLocalObject;

import nl.nn.adapterframework.configuration.IbisManager;

/**
 * Local interface for Enterprise Bean: IbisManagerEjb
 *
 * @author  Tim van der Leeuw
 * @since   4.8
 * @version Id
 */
public interface IbisManagerEjbLocal extends IbisManager, EJBLocalObject {
	
}
