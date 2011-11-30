/*
 * $Log: ReaderLineIterator.java,v $
 * Revision 1.3  2011-11-30 13:51:48  europe\m168309
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:44  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.1  2007/07/17 11:05:45  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * first version
 *
 */
package nl.nn.adapterframework.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;

import nl.nn.adapterframework.core.IDataIterator;
import nl.nn.adapterframework.core.SenderException;

/**
 * Iterator that iterates over all lines in a Reader.
 * 
 * @author  Gerrit van Brakel
 * @since   4.7
 * @version Id
 */
public class ReaderLineIterator implements IDataIterator {

	BufferedReader reader;
	String line;

	public ReaderLineIterator(Reader inputreader) throws SenderException {
		super();
		reader = new BufferedReader(inputreader);
		try {
			line=reader.readLine();
		} catch (IOException e) {
			throw new SenderException(e);
		}
	}

	public boolean hasNext() {
		return line!=null;
	}

	public Object next() throws SenderException {
		String result=line;
		try {
			line = reader.readLine();
		} catch (IOException e) {
			throw new SenderException(e);
		}
		return result; 
	}

	public void close() throws SenderException {
		try {
			reader.close();
		} catch (IOException e) {
			throw new SenderException(e);
		}
	}

}
