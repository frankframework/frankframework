/*
 * $Log: IInputStreamReaderFactory.java,v $
 * Revision 1.1  2010-05-03 17:01:34  L190409
 * interface to allow for binary records in batch processing.
 *
 */
package nl.nn.adapterframework.batch;

import java.io.InputStream;
import java.io.Reader;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.SenderException;

/**
 * @author  Gerrit van Brakel
 * @since  
 * @version Id
 */
public interface IInputStreamReaderFactory {

	void configure() throws ConfigurationException;
	
	/**
	 * Obtain a Reader that reads lines in the given characterset.
	 */
	Reader getReader(InputStream inputstream, String charset, String streamId, PipeLineSession session) throws SenderException;
}
