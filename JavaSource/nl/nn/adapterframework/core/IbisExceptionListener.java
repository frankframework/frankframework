/*
 * $Log: IbisExceptionListener.java,v $
 * Revision 1.1  2004-06-30 10:05:01  L190409
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
