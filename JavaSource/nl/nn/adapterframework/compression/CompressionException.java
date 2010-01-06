/*
 * $Log: CompressionException.java,v $
 * Revision 1.1  2010-01-06 17:57:35  L190409
 * classes for reading and writing zip archives
 *
 */
package nl.nn.adapterframework.compression;

import nl.nn.adapterframework.core.IbisException;

/**
 * Wrapper for compression related exceptions.
 * 
 * @author  Gerrit van Brakel
 * @since   4.9.10  
 * @version Id
 */
public class CompressionException extends IbisException {

	public CompressionException() {
		super();
	}

	public CompressionException(String msg) {
		super(msg);
	}

	public CompressionException(String msg, Throwable cause) {
		super(msg, cause);
	}

	public CompressionException(Throwable cause) {
		super(cause);
	}

}
