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
import nl.nn.adapterframework.configuration.ConfigurationWarning;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.SenderException;

/**
 * {@link IReaderFactory} that provides a reader that reads Delphi records containing Strings.
 *
 * @author  Gerrit van Brakel
 * @since   4.10
 * @deprecated Old and non-maintained functionality. Deprecated since v7.8
 */
@Deprecated
@ConfigurationWarning("Old and non-maintained functionality. Deprecated since v7.8")
public class DelphiStringRecordReaderFactory implements IReaderFactory {

	private int stringLength=50;
	private int stringsPerRecord=0; // 0 means read till end of file
	private String separator="|";
	private String separatorReplacement="_";

	@Override
	public void configure() throws ConfigurationException {
	}

	@Override
	public Reader getReader(InputStream in, String charset, String streamId, PipeLineSession session) throws SenderException {
		return new DelphiStringRecordReader(in,charset,getStringLength(),getStringsPerRecord(),getSeparator(),getSeparatorReplacement());
	}

	/**
	 * separator placed between each string read
	 * @ff.default |
	 */
	public void setSeparator(String string) {
		separator = string;
	}
	public String getSeparator() {
		return separator;
	}

	/**
	 * the maximum length of each string. each string is preceded by a one byte length indication.
	 * @ff.default 50
	 */
	public void setStringLength(int i) {
		stringLength = i;
	}
	public int getStringLength() {
		return stringLength;
	}

	/**
	 * the number of strings read for each record. 0 means file consists of one logical record
	 * @ff.default 0
	 */
	public void setStringsPerRecord(int i) {
		stringsPerRecord = i;
	}
	public int getStringsPerRecord() {
		return stringsPerRecord;
	}

	/**
	 * replacement character, used when separator is found in string read
	 * @ff.default _
	 */
	public void setSeparatorReplacement(String string) {
		separatorReplacement = string;
	}
	public String getSeparatorReplacement() {
		return separatorReplacement;
	}

}
