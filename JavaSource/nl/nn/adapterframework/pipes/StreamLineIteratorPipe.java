/*
 * $Log: StreamLineIteratorPipe.java,v $
 * Revision 1.12  2012-06-01 10:52:49  m00f069
 * Created IPipeLineSession (making it easier to write a debugger around it)
 *
 * Revision 1.11  2012/01/20 10:39:09  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * StreamLineIteratorPipe: added endOfLineString attribute
 *
 * Revision 1.10  2011/12/08 13:01:59  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * fixed javadoc
 *
 * Revision 1.9  2011/11/30 13:51:50  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:44  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.7  2010/03/25 12:58:09  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added attribute closeInputstreamOnExit
 *
 * Revision 1.6  2010/02/25 13:41:54  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * adjusted javadoc for resultOnTimeOut attribute
 *
 * Revision 1.5  2008/05/21 09:40:34  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added block info to javadoc
 *
 * Revision 1.4  2007/10/08 12:23:51  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * changed HashMap to Map where possible
 *
 * Revision 1.3  2007/09/13 09:10:14  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * extracted getReader()
 *
 * Revision 1.2  2007/07/17 10:57:00  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * update javadoc
 *
 * Revision 1.1  2007/07/10 08:05:38  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * first version
 *
 */
package nl.nn.adapterframework.pipes;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Map;

import nl.nn.adapterframework.core.IDataIterator;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.util.ReaderLineIterator;

/**
 * Sends a message to a Sender for each line of its input, that must be an InputStream.
 * 
 * <br>
 * The output of each of the processing of each of the elements is returned in XML as follows:
 * <pre>
 *  &lt;results count="num_of_elements"&gt;
 *    &lt;result&gt;result of processing of first item&lt;/result&gt;
 *    &lt;result&gt;result of processing of second item&lt;/result&gt;
 *       ...
 *  &lt;/results&gt;
 * </pre>
 *
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>className</td><td>nl.nn.adapterframework.pipes.StreamLineIteratorPipe</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setName(String) name}</td><td>name of the Pipe</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setMaxThreads(int) maxThreads}</td><td>maximum number of threads that may call {@link #doPipe(Object, nl.nn.adapterframework.core.PipeLineSession)} simultaneously</td><td>0 (unlimited)</td></tr>
 * <tr><td>{@link #setDurationThreshold(long) durationThreshold}</td><td>if durationThreshold >=0 and the duration (in milliseconds) of the message processing exceeded the value specified the message is logged informatory</td><td>-1</td></tr>
 * <tr><td>{@link #setGetInputFromSessionKey(String) getInputFromSessionKey}</td><td>when set, input is taken from this session key, instead of regular input</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setStoreResultInSessionKey(String) storeResultInSessionKey}</td><td>when set, the result is stored under this session key</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setNamespaceAware(boolean) namespaceAware}</td><td>controls namespace-awareness of possible XML parsing in descender-classes</td><td>application default</td></tr>
 * <tr><td>{@link #setForwardName(String) forwardName}</td>  <td>name of forward returned upon completion</td><td>"success"</td></tr>
 * <tr><td>{@link #setResultOnTimeOut(String) resultOnTimeOut}</td><td>result returned when no return-message was received within the timeout limit (e.g. "receiver timed out").</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setLinkMethod(String) linkMethod}</td><td>Indicates wether the server uses the correlationID or the messageID in the correlationID field of the reply</td><td>CORRELATIONID</td></tr>
 * <tr><td>{@link #setStopConditionXPathExpression(String) stopConditionXPathExpression}</td><td>expression evaluated on each result if set. 
 * 		Iteration stops if condition returns anything other than <code>false</code> or an empty result.
 * For example, to stop after the second child element has been processed, one of the following expressions could be used:
 * <table> 
 * <tr><td><li><code>result[position()='2']</code></td><td>returns result element after second child element has been processed</td></tr>
 * <tr><td><li><code>position()='2'</code></td><td>returns <code>false</code> after second child element has been processed, <code>true</code> for others</td></tr>
 * </table> 
 * </td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setElementXPathExpression(String) elementXPathExpression}</td><td>expression used to determine the set of elements iterated over, i.e. the set of child elements</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setRemoveXmlDeclarationInResults(boolean) removeXmlDeclarationInResults}</td><td>postprocess each partial result, to remove the xml-declaration, as this is not allowed inside an xml-document</td><td>false</td></tr>
 * <tr><td>{@link #setCollectResults(boolean) collectResults}</td><td>controls whether all the results of each iteration will be collected in one result message. If set <code>false</code>, only a small summary is returned</td><td>true</td></tr>
 * <tr><td>{@link #setBlockSize(int) blockSize}</td><td>controls multiline behaviour. when set to a value greater than 0, it specifies the number of rows send in a block to the sender.</td><td>0 (one line at a time, no prefix of suffix)</td></tr>
 * <tr><td>{@link #setBlockPrefix(String) blockPrefix}</td><td>When <code>blockSize &gt; 0</code>, this string is inserted at the start of the set of lines.</td><td>&lt;block&gt;</td></tr>
 * <tr><td>{@link #setBlockSuffix(String) blockSuffix}</td><td>When <code>blockSize &gt; 0</code>, this string is inserted at the end of the set of lines.</td><td>&lt;/block&gt;</td></tr>
 * <tr><td>{@link #setCloseInputstreamOnExit(boolean) closeInputstreamOnExit}</td><td>when set to <code>false</code>, the inputstream is not closed after it has been used</td><td>true</td></tr>
 * <tr><td>{@link #setEndOfLineString(String) endOfLineString}</td><td>when set, each line has to end with this string. If the line doesn't end with this string next lines are added (including line separators) until the total line ends with the given string</td><td>&nbsp;</td></tr>
 * </table>
 * <table border="1">
 * <tr><th>nested elements</th><th>description</th></tr>
 * <tr><td>{@link nl.nn.adapterframework.core.ISender sender}</td><td>specification of sender to send messages with</td></tr>
 * <tr><td>{@link nl.nn.adapterframework.core.ICorrelatedPullingListener listener}</td><td>specification of listener to listen to for replies</td></tr>
 * <tr><td>{@link nl.nn.adapterframework.parameters.Parameter param}</td><td>any parameters defined on the pipe will be handed to the sender, if this is a {@link nl.nn.adapterframework.core.ISenderWithParameters ISenderWithParameters}</td></tr>
 * </table>
 * </p>
 * 
 * For more configuration options, see {@link MessageSendingPipe}.
 * <br>
 * use parameters like:
 * <pre>
 *	&lt;param name="element-name-of-current-item"  xpathExpression="name(/*)" /&gt;
 *	&lt;param name="value-of-current-item"         xpathExpression="/*" /&gt;
 * </pre>
 * 
 * @author  Gerrit van Brakel
 * @since   4.7
 * @version Id
 */
public class StreamLineIteratorPipe extends IteratingPipe {
	public static final String version="$RCSfile: StreamLineIteratorPipe.java,v $ $Revision: 1.12 $ $Date: 2012-06-01 10:52:49 $";

	private String endOfLineString;
	
	protected Reader getReader(Object input, IPipeLineSession session, String correlationID, Map threadContext) throws SenderException {
		if (input==null) {
			throw new SenderException("input is null. Must supply stream as input");
		}
		if (!(input instanceof InputStream)) {
			throw new SenderException("input must be of type InputStream");
		}
		Reader reader=new InputStreamReader((InputStream)input);
		return reader;
	}

	protected IDataIterator getIterator(Object input, IPipeLineSession session, String correlationID, Map threadContext) throws SenderException {
		return new ReaderLineIterator(getReader(input,session, correlationID,threadContext));
	}

	protected String getItem(IDataIterator it) throws SenderException {
		String item = (String)it.next();
		if (getEndOfLineString()!=null) {
			while (!item.endsWith(getEndOfLineString()) && it.hasNext()) {
				item = item + System.getProperty("line.separator") + (String)it.next();
			}
		}
		return item;
	}

	public void setCloseInputstreamOnExit(boolean b) {
		setCloseIteratorOnExit(b);
	}
	public boolean isCloseInputstreamOnExit() {
		return isCloseIteratorOnExit();
	}

	public void setEndOfLineString(String string) {
		endOfLineString = string;
	}
	public String getEndOfLineString() {
		return endOfLineString;
	}
}
