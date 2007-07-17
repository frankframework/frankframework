/*
 * $Log: IDataIterator.java,v $
 * Revision 1.1  2007-07-17 09:18:41  europe\L190409
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
