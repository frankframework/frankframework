/*
 * $Log: ConnectionFactoryBase.java,v $
 * Revision 1.1  2005-05-03 15:59:55  L190409
 * rework of shared connection code
 *
 */
package nl.nn.adapterframework.jms;

import java.util.HashMap;

import javax.jms.ConnectionFactory;
import javax.naming.Context;
import javax.naming.NamingException;

import nl.nn.adapterframework.core.IbisException;

import org.apache.log4j.Logger;

/**
 * Factory for JMS connections, to share them for JMS Objects that can use the same. 
 * 
 * @author Gerrit van Brakel
 * @version Id
 */
public abstract class ConnectionFactoryBase  {
	public static final String version="$Id: ConnectionFactoryBase.java,v 1.1 2005-05-03 15:59:55 L190409 Exp $";
	protected Logger log = Logger.getLogger(this.getClass());

	protected abstract HashMap getConnectionMap();
	protected abstract Context createContext() throws NamingException;
	protected abstract ConnectionFactory createConnectionFactory(Context context, String id) throws IbisException, NamingException;
	
	protected JmsConnection createJmsConnection(String id) throws IbisException {
		Context context = getContext();
		ConnectionFactory connectionFactory = getConnectionFactory(context, id); 
		return new JmsConnection(id, context, connectionFactory, getConnectionMap());
	}
	
	public JmsConnection getConnection(String id) throws IbisException {
		HashMap connectionMap = getConnectionMap();
		JmsConnection result = (JmsConnection)connectionMap.get(id);
		if (result == null) {
			synchronized (this) {
				result = (JmsConnection)connectionMap.get(id);
				if (result == null) {
					result = createJmsConnection(id);
					log.debug("created new Connection-object for ["+id+"]");
				}
			}
		}
		result.increaseReferences();
		return result;
	}
	
	protected Context getContext() throws IbisException {
		try {
			return createContext();
		} catch (NamingException e) {
			throw new IbisException("could not obtain context", e);
		}
	}

	protected ConnectionFactory getConnectionFactory(Context context, String id) throws IbisException {
		try {
			return createConnectionFactory(context, id);
		} catch (NamingException e) {
			throw new IbisException("could not obtain connectionFactory", e);
		}
	}
	
}
