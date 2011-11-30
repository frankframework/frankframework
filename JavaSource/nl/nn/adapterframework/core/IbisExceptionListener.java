/*
 * $Log: IbisExceptionListener.java,v $
 * Revision 1.3  2011-11-30 13:51:55  europe\m168309
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:46  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.1  2004/06/30 10:05:01  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * first version
 *
 */
package nl.nn.adapterframework.core;

/**
 * ExeceptionListener-class to signal exceptions to other objects, for instance 
 * MessagePushers to PushingReceivers.
 * 
 * @author Gerrit van Brakel
 * @since 4.2
 */
public interface IbisExceptionListener {

	/**
	 * Inform the implementing class that the exception <code>t</code> occured in <code>object</code>.
	 */
	void exceptionThrown(INamedObject object, Throwable t);
}
