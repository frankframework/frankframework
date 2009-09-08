/*
 * $Log: IDoubleASynchronous.java,v $
 * Revision 1.1  2009-09-08 14:18:28  L190409
 * added IDoubleASynchronous
 *
 */
package nl.nn.adapterframework.core;

/**
 * Interface to be implemented by synchronous Senders that consist of two asynchronous parts.
 * Allows enclosing objects to specify whether to link on correlationID or messageID
 *  
 * @author  Gerrit van Brakel
 * @since   4.9.9  
 * @version Id
 */
public interface IDoubleASynchronous {

	void setLinkMethod(String method);
}
