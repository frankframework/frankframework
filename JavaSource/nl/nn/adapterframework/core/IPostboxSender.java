/*
 * $Log: IPostboxSender.java,v $
 * Revision 1.3  2004-10-19 06:39:20  L190409
 * modified parameter handling, introduced IWithParameters
 *
 * Revision 1.2  2004/10/05 10:03:04  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * replaced by IParameterizedSender
 *
 * Revision 1.1  2004/05/21 07:59:30  unknown <unknown@ibissource.org>
 * Add (modifications) due to the postbox sender implementation
 *
 */
package nl.nn.adapterframework.core;

/**
 * The <code>IPostboxSender</code> is responsible for storing a message
 * in a postbox
 *
 * @author John Dekker
 * @version Id
 */
public interface IPostboxSender extends ISenderWithParameters {
}
