/*
 * $Log: IParameterizedSender.java,v $
 * Revision 1.1  2004-10-05 10:03:21  L190409
 * introduction of IParameterizedSender
 *
 * Revision 1.5  2004/03/30 07:29:59  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * updated javadoc
 *
 * Revision 1.4  2004/03/26 10:42:45  Johan Verrips <johan.verrips@ibissource.org>
 * added @version tag in javadoc
 *
 */
package nl.nn.adapterframework.core;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.parameters.ParameterList;
import nl.nn.adapterframework.parameters.ParameterValueList;


/**
 * The <code>IParameterizedSender</code> allows Senders to declare that they accept and may use {@link nl.nn.adapterframework.parameters.Parameter parameters} 
 * 
 * @version HasSender.java,v 1.3 2004/03/23 16:42:59 L190409 Exp $
 * @author  Gerrit van Brakel
 */
public interface IParameterizedSender extends ISender {
	public static final String version="$Id: IParameterizedSender.java,v 1.1 2004-10-05 10:03:21 L190409 Exp $";

	/**
	 * configure() method that is called instead of configure(void) to indicate that parameters are present. 
	 */
	public void configure(ParameterList parameterList) throws ConfigurationException;
	
	public String sendMessage(String correlationID, String message, ParameterValueList parameters) throws SenderException, TimeOutException;
}
