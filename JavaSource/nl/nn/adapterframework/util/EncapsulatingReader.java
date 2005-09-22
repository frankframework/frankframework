/*
 * $Log: EncapsulatingReader.java,v $
 * Revision 1.1  2005-09-22 15:53:31  europe\L190409
 * introduction of encapsulating reader
 *
 */
package nl.nn.adapterframework.util;

import java.io.FilterReader;
import java.io.IOException;
import java.io.Reader;

/**
 * Reader that encapsulates a file within a prefix and a postfix.
 * 
 * @author  Gerrit van Brakel
 * @since   4.3
 * @version Id
 */
public class EncapsulatingReader extends FilterReader {

	String prefix;
	String postfix;
	boolean readPrefix=false;
	boolean readReader=false;
	int position=0;

	public EncapsulatingReader(Reader in, String prefix, String postfix) {
		super(in);
		this.prefix=prefix;
		this.postfix=postfix;
	}

	public int read() throws IOException {
		if (!readPrefix) {
			if (position<prefix.length()) {
				return prefix.charAt(position++);
			}
			readPrefix=true;
			position=0;
		}
		if (!readReader) {
			int result = in.read();
			if (result>=0) {
				return result;
			}
			readReader=true;
		}
		if (position<postfix.length()) {
			return postfix.charAt(position++);
		}
		return -1;
	}
	
	public int read(char[] cbuf, int off, int len) throws IOException {
		int charsRead=0;
		if (!readPrefix) {
			while (position<prefix.length() && charsRead<len) {
				cbuf[off+charsRead++]=prefix.charAt(position++);
			}
			readPrefix=true;
			position=0;
			if (charsRead>0) {
				return charsRead;
			} 
		}
		if (!readReader) {
			charsRead = in.read(cbuf, off, len);
			if (charsRead>0) {
				return charsRead;
			} 
			readReader=true;
			charsRead=0;
		}
		while (position<postfix.length() && charsRead<len) {
			cbuf[off+charsRead++]=postfix.charAt(position++);
		}
		if (charsRead>0) {
			return charsRead;
		} 
		return -1;
	}
		
	public boolean ready() throws IOException {
		return !readPrefix || (!readReader && in.ready()) || position<postfix.length();
	}
		
	public void reset() throws IOException {
		in.reset();
		readPrefix=false;
		readReader=false;
		position=0;
	}
		
}

