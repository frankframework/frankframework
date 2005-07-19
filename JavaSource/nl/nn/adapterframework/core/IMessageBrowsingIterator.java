/*
 * $Log: IMessageBrowsingIterator.java,v $
 * Revision 1.1  2005-07-19 12:14:30  europe\L190409
 * introduction of IMessageBrowsingIterator
 *
 */
package nl.nn.adapterframework.core;

/**
 * Interface for helper class for MessageBrowsers. 
 * 
 * @author  Gerrit van Brakel
 * @since   4.3
 * @version Id
 */
public interface IMessageBrowsingIterator {

	public boolean hasNext() throws ListenerException;
	public Object  next() throws ListenerException;
	public void    close() throws ListenerException;

}
