/*
 * $Log: InputStreamReaderFactory.java,v $
 * Revision 1.3  2011-11-30 13:51:56  europe\m168309
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:47  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.1  2010/05/03 17:02:03  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * default implementation of IInputstreamReaderFactory
 *
 */
package nl.nn.adapterframework.batch;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.SenderException;

/**
 * Basic InputStreamReaderFactory.
 * 
 * @author  Gerrit van Brakel
 * @since   4.11
 * @version Id
 */
public class InputStreamReaderFactory implements IInputStreamReaderFactory {

	public void configure() throws ConfigurationException {
	}

	public Reader getReader(InputStream inputstream, String charset, String streamId, PipeLineSession session) throws SenderException {
		try {
			return new InputStreamReader(inputstream, charset);
		} catch (UnsupportedEncodingException e) {
			throw new SenderException("cannot use charset ["+charset+"] to read stream ["+streamId+"]",e);
		}
	}


}
