/*
 * $Log: IMessageBrowser.java,v $
 * Revision 1.1  2004-06-16 12:25:52  NNVZNL01#L180564
 * Initial version of Queue browsing functionality
 *
 *
 */
package nl.nn.adapterframework.core;

import java.util.Enumeration;
import nl.nn.adapterframework.core.MessageBrowseException;

/**
 * @version Id
 * @author Johan Verrips
 */
public interface IMessageBrowser{
	public static final String version = "$Id: IMessageBrowser.java,v 1.1 2004-06-16 12:25:52 NNVZNL01#L180564 Exp $";
	/**
	 * Gets an enumeration of messages. This includes setting up connections, sessions etc.
	 * @return Enumeration
	 * @throws MessageBrowseException
	 */
	public Enumeration getMessageEnumeration() throws MessageBrowseException;
	public void close();

}

