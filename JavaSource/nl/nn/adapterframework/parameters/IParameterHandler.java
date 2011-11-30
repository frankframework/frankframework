/*
 * $Log: IParameterHandler.java,v $
 * Revision 1.3  2011-11-30 13:52:03  europe\m168309
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:50  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.1  2005/10/24 09:59:23  John Dekker <john.dekker@ibissource.org>
 * Add support for pattern parameters, and include them into several listeners,
 * senders and pipes that are file related
 *
 */
package nl.nn.adapterframework.parameters;

import nl.nn.adapterframework.core.ParameterException;

/**
 * Helper interface to quickly do something for all resolved parameters 
 * 
 * @author John Dekker
 */
public interface IParameterHandler {
	
	/**
	 * Methods is called for each resolved parameter in the order in which they are defined
	 * @param paramName name of the parameter
	 * @param paramValue value of the parameter
	 * @throws ParameterException
	 */
	void handleParam(String paramName, Object paramValue) throws ParameterException;
}
