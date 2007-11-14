/*
 * $Log: IfsaMessageWrapper.java,v $
 * Revision 1.2.2.1  2007-11-14 13:47:00  europe\L190409
 * *** empty log message ***
 *
 * Revision 1.1  2005/09/22 16:07:50  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * introduction of IfsaMessageWrapper
 *
 */
package nl.nn.adapterframework.extensions.ifsa;

import nl.nn.adapterframework.receivers.MessageWrapper;

/**
 * Wrapper for messages that are not serializable.
 * 
 * @deprecated this class is necessary here, as older entries of it might still exist in errorstores.
 * The class has been moved to nl.nn.adapterframework.receivers.MessageWrapper.
 * 
 * @author  Gerrit van Brakel
 * @since   4.3
 * @version Id
 */
public class IfsaMessageWrapper extends MessageWrapper {

}
