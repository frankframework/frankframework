/*
 * $Log: IPostboxSender.java,v $
 * Revision 1.5  2011-11-30 13:51:55  europe\m168309
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:46  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.3  2004/10/19 06:39:20  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
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
