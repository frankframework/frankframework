/*
 * $Log: IWithParameters.java,v $
 * Revision 1.1  2004-10-19 06:39:20  L190409
 * modified parameter handling, introduced IWithParameters
 *
 */
package nl.nn.adapterframework.core;

import nl.nn.adapterframework.parameters.Parameter;


/**
 * Base interface to allow objects to declare that they accept paramters. 
 * 
 * @version HasSender.java,v 1.3 2004/03/23 16:42:59 L190409 Exp $
 * @author  Gerrit van Brakel
 */
public interface IWithParameters {
	public static final String version="$Id: IWithParameters.java,v 1.1 2004-10-19 06:39:20 L190409 Exp $";

	public void addParameter(Parameter p); 
}
