/*
 * $Log: ConnectionFactoryBase.java,v $
 * Revision 1.9  2008-07-24 12:20:00  europe\L190409
 * added support for authenticated JMS
 *
 * Revision 1.8  2007/10/08 12:20:04  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * changed HashMap to Map where possible
 *
 * Revision 1.7  2007/05/23 09:13:51  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * improved logging
 *
 * Revision 1.6  2007/02/12 13:58:11  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * Logger from LogUtil
 *
 * Revision 1.5  2006/12/13 16:28:05  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * catch more exceptions
 *
 * Revision 1.4  2005/10/26 08:21:42  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * renamed createJmsConnection() into createConnection()
 *
 * Revision 1.3  2005/10/20 15:35:25  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * corrected version String
 *
 * Revision 1.2  2005/10/20 15:34:09  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * renamed JmsConnection into ConnectionBase
 *
 * Revision 1.1  2005/05/03 15:59:55  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * rework of shared connection code
 *
 */
package nl.nn.adapterframework.jms;

import java.util.Map;

import javax.jms.ConnectionFactory;
import javax.naming.Context;
import javax.naming.NamingException;

import nl.nn.adapterframework.core.IbisException;
import nl.nn.adapterframework.util.LogUtil;

import org.apache.log4j.Logger;

/**
 * Factory for JMS connections, to share them for JMS Objects that can use the same. 
 * 
 * @author Gerrit van Brakel
 * @version Id
 */
public abstract class ConnectionFactoryBase  {
	public static final String version="$RCSfile: ConnectionFactoryBase.java,v $ $Revision: 1.9 $ $Date: 2008-07-24 12:20:00 $";
	protected Logger log = LogUtil.getLogger(this);

	protected abstract Map getConnectionMap();
	protected abstract Context createContext() throws NamingException;
	protected abstract ConnectionFactory createConnectionFactory(Context context, String id) throws IbisException, NamingException;
	
	protected ConnectionBase createConnection(String id, String authAlias) throws IbisException {
		Context context = getContext();
		ConnectionFactory connectionFactory = getConnectionFactory(context, id); 
		return new ConnectionBase(id, context, connectionFactory, getConnectionMap(), authAlias);
	}
	
	public synchronized ConnectionBase getConnection(String id, String authAlias) throws IbisException {
		Map connectionMap = getConnectionMap();
		ConnectionBase result = (ConnectionBase)connectionMap.get(id);
		if (result == null) {
			result = createConnection(id, authAlias);
			log.debug("created new Connection-object for ["+id+"]");
		}
		result.increaseReferences();
		return result;
	}
	
	protected Context getContext() throws IbisException {
		try {
			return createContext();
		} catch (Throwable t) {
			throw new IbisException("could not obtain context", t);
		}
	}

	protected ConnectionFactory getConnectionFactory(Context context, String id) throws IbisException {
		try {
			return createConnectionFactory(context, id);
		} catch (Throwable t) {
			throw new IbisException("could not obtain connectionFactory ["+id+"]", t);
		}
	}
	
}
