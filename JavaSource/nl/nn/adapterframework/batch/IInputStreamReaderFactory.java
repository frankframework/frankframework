/*
 * $Log: IInputStreamReaderFactory.java,v $
 * Revision 1.3  2011-11-30 13:51:56  europe\m168309
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:47  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.1  2010/05/03 17:01:34  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
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
