/*
 * $Log: IParameterHandler.java,v $
 * Revision 1.1  2005-10-24 09:59:23  europe\m00f531
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
