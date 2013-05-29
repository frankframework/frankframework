/*
   Copyright 2013 Nationale-Nederlanden

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
package nl.nn.adapterframework.batch;

import java.io.InputStream;
import java.io.Reader;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.SenderException;

/**
 * {@link IInputStreamReaderFactory} that provides a reader that reads Delphi records containing Strings.
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>classname</td><td>nl.nn.adapterframework.batch.DelphiRecordReaderFactory</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setStringLength(int) stringLength}</td><td>the maximum length of each string. Each string is preceded by a one byte length indication.</td><td>50</td></tr>
 * <tr><td>{@link #setStringsPerRecord(int) stringsPerRecord}</td><td>The number of strings read for each record. 0 means file consists of one logical record</td><td>0</td></tr>
 * <tr><td>{@link #setSeparator(String) separator}</td><td>separator placed between each string read</td><td>|</td></tr>
 * <tr><td>{@link #setSeparatorReplacement(String) separatorReplacement}</td><td>Replacement character, used when separator is found in string read</td><td>_</td></tr>
 * </table>
 * </p>
 * 
 * @author  Gerrit van Brakel
 * @since   4.10  
 * @version $Id$
 */
public class DelphiStringRecordReaderFactory implements IInputStreamReaderFactory {

	private int stringLength=50;
	private int stringsPerRecord=0; // 0 means read till end of file
	private String separator="|";
	private String separatorReplacement="_";

	public void configure() throws ConfigurationException {
	}

	public Reader getReader(InputStream in, String charset, String streamId, IPipeLineSession session) throws SenderException {
		return new DelphiStringRecordReader(in,charset,getStringLength(),getStringsPerRecord(),getSeparator(),getSeparatorReplacement()); 
	}

	public void setSeparator(String string) {
		separator = string;
	}
	public String getSeparator() {
		return separator;
	}

	public void setStringLength(int i) {
		stringLength = i;
	}
	public int getStringLength() {
		return stringLength;
	}

	public void setStringsPerRecord(int i) {
		stringsPerRecord = i;
	}
	public int getStringsPerRecord() {
		return stringsPerRecord;
	}

	public void setSeparatorReplacement(String string) {
		separatorReplacement = string;
	}
	public String getSeparatorReplacement() {
		return separatorReplacement;
	}

}
