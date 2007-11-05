/*
 * $Log: IPortConnectedListener.java,v $
 * Revision 1.1  2007-11-05 12:15:09  europe\M00035F
 * Add new interface for pushing listeners which receive messages from named enpoints, and which require additional glue to make the connection
 *
 * 
 * Created on 5-nov-07
 *
 */
package nl.nn.adapterframework.core;

/**
 * Interface extending IPushingListener for listeners which connect to a
 * ListenerPort or other type of named endpoint, from which they receive
 * their messages.
 * 
 * Current implementations are PushingJmsListener and the EJB version of
 * IfsaProviderListener.
 * 
 * @author Tim van der Leeuw
 * @version Id
 *
 */
public interface IPortConnectedListener extends IPushingListener {
    String getListenerPort();
    IReceiver getReceiver();
}
