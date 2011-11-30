/*
 * $Log: IDataIterator.java,v $
 * Revision 1.3  2011-11-30 13:51:55  europe\m168309
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:46  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.1  2007/07/17 09:18:41  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * introduction of IDataIterator
 *
 */
package nl.nn.adapterframework.core;

/**
 * Interface to handle generic iterations.
 * 
 * 
 * @author  Gerrit van Brakel
 * @since   6.4.1
 * @version Id
 */
public interface IDataIterator {
	
	public boolean hasNext() throws SenderException; 
	public Object next() throws SenderException;

	public void close() throws SenderException;
}
