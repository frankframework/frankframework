/*
 * $Log: IWithParameters.java,v $
 * Revision 1.3  2011-11-30 13:51:55  europe\m168309
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.2  2011/10/19 14:58:00  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * do not print versions anymore
 *
 * Revision 1.1  2011/10/19 14:49:46  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.1  2004/10/19 06:39:20  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
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

	public void addParameter(Parameter p); 
}
