/* 
 * $Log: IXAEnabled.java,v $
 * Revision 1.2  2004-03-23 17:20:11  L190409
 * one little puntkomma
 *
 * Revision 1.1  2004/03/23 16:52:12  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * initial version
 *
 */
package nl.nn.adapterframework.core;

/**
 * Indicates a Pipe, Sender or Listener to be capable of supporting XA-transactions. 
 * When isTransacted() returns true, alternative XA enabled versions of resources like
 * connectionfactories should be used by implementing classes.
 * <p>$Id: IXAEnabled.java,v 1.2 2004-03-23 17:20:11 L190409 Exp $</p>
 * @author Gerrit van Brakel
 * @since  4.1
 */
public interface IXAEnabled {
	public static final String version="$Id: IXAEnabled.java,v 1.2 2004-03-23 17:20:11 L190409 Exp $";
	
	/**
	 *  indicates implementing object is under transaction control, using XA-transactions
	 */
	public boolean isTransacted();

}
