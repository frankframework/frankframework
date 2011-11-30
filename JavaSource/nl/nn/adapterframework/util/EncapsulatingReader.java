/*
 * $Log: EncapsulatingReader.java,v $
 * Revision 1.4  2011-11-30 13:51:49  europe\m168309
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:44  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.2  2005/10/17 11:04:20  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added encodePrintable-feature
 *
 * Revision 1.1  2005/09/22 15:53:31  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
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
	boolean encodePrintable;

	boolean readPrefix=false;
	boolean readReader=false;
	int position=0;

	public EncapsulatingReader(Reader in, String prefix, String postfix, boolean encodePrintable) {
		super(in);
		this.prefix=prefix;
		this.postfix=postfix;
		this.encodePrintable = encodePrintable;
	}

	public EncapsulatingReader(Reader in, String prefix, String postfix) {
		this(in, prefix, postfix, false);
	}		

	private char charPrintable(char c) {
		if (!encodePrintable || XmlUtils.isPrintableUnicodeChar(c)) {
			return c;
		} else {
			return 0x00BF;
		}
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
				return charPrintable((char)result);
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
				if (encodePrintable) {
					for (int i=off; i<off+charsRead; i++) {
						cbuf[i]=charPrintable(cbuf[i]);
					}
				}
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

