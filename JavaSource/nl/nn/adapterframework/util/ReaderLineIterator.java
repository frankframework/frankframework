/*
 * $Log: ReaderLineIterator.java,v $
 * Revision 1.1  2007-07-17 11:05:45  europe\L190409
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
