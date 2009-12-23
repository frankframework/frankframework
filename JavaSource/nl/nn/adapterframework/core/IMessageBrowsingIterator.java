/*
 * $Log: IMessageBrowsingIterator.java,v $
 * Revision 1.2  2009-12-23 17:05:16  L190409
 * modified MessageBrowsing interface to reenable and improve export of messages
 *
 * Revision 1.1  2005/07/19 12:14:30  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
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

	boolean hasNext() throws ListenerException;
	IMessageBrowsingIteratorItem  next() throws ListenerException;
	void    close() throws ListenerException;

}
