/*
   Copyright 2013 Nationale-Nederlanden, 2023 WeAreFrank!

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package org.frankframework.batch;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;

import org.frankframework.configuration.ConfigurationWarning;
import org.frankframework.util.LogUtil;

/**
 *
 * @author  Gerrit van Brakel
 * @since   4.10
 * @deprecated Warning: non-maintained functionality.
 */
@Deprecated
@ConfigurationWarning("Warning: non-maintained functionality.")
public class DelphiStringRecordReader extends Reader {
	protected Logger log = LogUtil.getLogger(this);

	private InputStream in;
	private String charsetName;
	private int stringLength;
	private int stringsPerRecord; // 0 means read till end of file
	private String separator;
	private String separatorReplacement;

	private StringBuilder buffer;
	private int bufferLen=0;
	private int bufferPos=0;
	boolean eof=false;

	private final boolean trace=false;

	public DelphiStringRecordReader(InputStream in, String charsetName, int stringLength, int stringsPerRecord, String separator, String separatorReplacement) {
		super();
		this.in=in;
		this.charsetName=charsetName;
		this.stringLength=stringLength;
		this.stringsPerRecord=stringsPerRecord;
		this.separator=separator;
		this.separatorReplacement=separatorReplacement;
	}

	/*
	 * Fill buffer if empty, then copy characters as required.
	 */
	public int read(char[] cbuf, int off, int len) throws IOException {
		if (buffer==null || bufferPos>=bufferLen) {
			fillBuffer();
		}
		if (buffer==null) {
			return -1;
		}
		int bytesRead=0;
		while (bufferPos<bufferLen && bytesRead++<len) {
			cbuf[off++]=buffer.charAt(bufferPos++);
		}
		return bytesRead;
	}

	public void close() throws IOException {
		in.close();
	}

	/*
	 * read a single string from the input, then skip to stringLength.
	 */
	private String readString() throws IOException {
		int len;
		len=in.read(); // first read the byte that holds the length of the string
		if (len<0) {
			return null;
		}
		if (trace && log.isDebugEnabled()) log.debug("read byte for string length [{}]", len);
		byte[] buf=new byte[len]; // allocate space for the bytes of the string
		int bytesToRead=len;
		int pos=0;
		while (bytesToRead>0) {
			int bytesRead = in.read(buf,pos,bytesToRead);
			if (bytesRead>0) {
				pos+=bytesRead;
				bytesToRead-=bytesRead;
			} else {
				String currentResult=null;
				try {
					currentResult=new String(buf,charsetName);
				} catch (Exception e) {
					currentResult=e.getClass().getName()+": "+e.getMessage();
				}
				throw new EOFException("unexpected EOF after reading ["+pos+"] bytes of a string of length ["+len+"], current result ["+currentResult+"]");
			}
		}
		if (pos<stringLength) {
			if (trace && log.isDebugEnabled()) log.debug("skipping [{}] bytes", stringLength - pos);
			in.skip(stringLength-pos);
		}
		String result=new String(buf,charsetName);
		if (StringUtils.isNotEmpty(separatorReplacement)) {
			result= result.replace(separator, separatorReplacement);
		}
		if (trace && log.isDebugEnabled()) log.debug("read string [{}]", result);
		return result;
	}

	/*
	 * accumulate strings in buffer.
	 */
	private void fillBuffer() throws IOException {
		int stringsRead=0;
		buffer=new StringBuilder();
		while (!eof && (stringsPerRecord==0 || stringsRead<stringsPerRecord)) {
			String part=readString();
			if (part==null) {
				eof=true;
			} else {
				buffer.append(part).append(separator);
				stringsRead++;
			}
		}
		if (trace && log.isDebugEnabled()) log.debug("read [{}] strings", stringsRead);
		if (stringsRead==0) {
			buffer=null;
		} else {
			buffer.append("\n");
			bufferLen=buffer.length();
			bufferPos=0;
		}
	}

}
