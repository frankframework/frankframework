/*
 * $Log: DelphiStringRecordReaderFactory.java,v $
 * Revision 1.4  2011-11-30 13:51:56  europe\m168309
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:48  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.2  2010/05/14 16:50:39  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added separatorReplacement
 *
 * Revision 1.1  2010/05/03 17:03:06  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * IInputstreamReader-classes to enable reading Delphi String records
 *
 */
package nl.nn.adapterframework.batch;

import java.io.InputStream;
import java.io.Reader;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeLineSession;
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
 * @version Id
 */
public class DelphiStringRecordReaderFactory implements IInputStreamReaderFactory {

	private int stringLength=50;
	private int stringsPerRecord=0; // 0 means read till end of file
	private String separator="|";
	private String separatorReplacement="_";

	public void configure() throws ConfigurationException {
	}

	public Reader getReader(InputStream in, String charset, String streamId, PipeLineSession session) throws SenderException {
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
