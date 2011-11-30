/*
 * $Log: FileLineIteratorPipe.java,v $
 * Revision 1.7  2011-11-30 13:51:50  europe\m168309
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:45  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.5  2010/02/25 13:41:54  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * adjusted javadoc for resultOnTimeOut attribute
 *
 * Revision 1.4  2008/05/21 09:40:34  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added block info to javadoc
 *
 * Revision 1.3  2007/10/08 12:23:51  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * changed HashMap to Map where possible
 *
 * Revision 1.2  2007/09/13 09:10:45  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * base on StreamLineIteratorPipe
 *
 * Revision 1.1  2007/09/13 08:58:38  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * first version
 *
 */
package nl.nn.adapterframework.pipes;

import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.util.Map;

import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.util.ClassUtils;

import org.apache.commons.lang.StringUtils;

/**
 * Sends a message to a Sender for each line of the file that the input message refers to.
 * 
 * <br>
 * The output of each of the processing of each of the lines is returned in XML as follows:
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
 * <tr><td>classname</td><td>nl.nn.adapterframework.pipes.FileLineIteratorPipe</td><td>&nbsp;</td></tr>
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
 * <tr><td>{@link #setMove2dirAfterTransform(String) move2dirAfterTransform}</td><td>Directory in which the transformed file(s) is stored</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setMove2dirAfterError(String) move2dirAfterError}</td><td>Directory to which the inputfile is moved in case an error occurs</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setBlockSize(int) blockSize}</td><td>controls multiline behaviour. when set to a value greater than 0, it specifies the number of rows send in a block to the sender.</td><td>0 (one line at a time, no prefix of suffix)</td></tr>
 * <tr><td>{@link #setBlockPrefix(String) blockPrefix}</td><td>When <code>blockSize &gt; 0</code>, this string is inserted at the start of the set of lines.</td><td>&lt;block&gt;</td></tr>
 * <tr><td>{@link #setBlockSuffix(String) blockSuffix}</td><td>When <code>blockSize &gt; 0</code>, this string is inserted at the end of the set of lines.</td><td>&lt;/block&gt;</td></tr>
 * </table>
 * <table border="1">
 * <tr><th>nested elements</th><th>description</th></tr>
 * <tr><td>{@link nl.nn.adapterframework.core.ISender sender}</td><td>specification of sender to send messages with</td></tr>
 * <tr><td>{@link nl.nn.adapterframework.core.ICorrelatedPullingListener listener}</td><td>specification of listener to listen to for replies</td></tr>
 * <tr><td>{@link nl.nn.adapterframework.parameters.Parameter param}</td><td>any parameters defined on the pipe will be handed to the sender, if this is a {@link IParameterizedSender}</td></tr>
 * </table>
 * </p>
 * 
 * @author  Gerrit van Brakel
 * @version Id
 */
public class FileLineIteratorPipe extends StreamLineIteratorPipe {
	public static final String version = "$RCSfile: FileLineIteratorPipe.java,v $  $Revision: 1.7 $ $Date: 2011-11-30 13:51:50 $";

	private String move2dirAfterTransform;
	private String move2dirAfterError;

	
	protected Reader getReader(Object input, PipeLineSession session, String correlationID, Map threadContext) throws SenderException {
		if (input==null) {
			throw new SenderException("got null input instead of String containing filename");
		}
		if (!(input instanceof File)) {
			throw new SenderException("expected File as input, got ["+ClassUtils.nameOf(input)+"], value ["+input+"]");
		}
		File file = (File)input;
		try {
			return new FileReader(file);
		} catch (Exception e) {
			throw new SenderException("cannot open file ["+file.getPath()+"]",e);
		}
	}

	/**
	 * Open a reader for the file named according the input messsage and 
	 * transform it.
	 * Move the input file to a done directory when transformation is finished
	 * and return the names of the generated files. 
	 * 
	 * @see nl.nn.adapterframework.core.IPipe#doPipe(java.lang.Object, nl.nn.adapterframework.core.PipeLineSession)
	 */
	public PipeRunResult doPipe(Object input, PipeLineSession session) throws PipeRunException {
		if (input==null) {
			throw new PipeRunException(this,"got null input instead of String containing filename");
		}
		if (!(input instanceof String)) {
			throw new PipeRunException(this,"expected String containing filename as input, got ["+ClassUtils.nameOf(input)+"], value ["+input+"]");
		}
		String filename	= input.toString();
		File file = new File(filename);

		try {
			
			PipeRunResult result = super.doPipe(file,session);
			if (! StringUtils.isEmpty(getMove2dirAfterTransform())) {
				File move2 = new File(getMove2dirAfterTransform(), file.getName());
				file.renameTo(move2); 
			}
			return result;
		} catch (PipeRunException e) {
			if (! StringUtils.isEmpty(getMove2dirAfterError())) {
				File move2 = new File(getMove2dirAfterError(), file.getName());
				file.renameTo(move2); 
			}
			throw e;
		}
	}

	
	/**
	 * @param readyDir directory where input file is moved to in case of a succesful transformation
	 */
	public void setMove2dirAfterTransform(String readyDir) {
		move2dirAfterTransform = readyDir;
	}
	public String getMove2dirAfterTransform() {
		return move2dirAfterTransform;
	}

	/**
	 * @param errorDir directory where input file is moved to in case of an error
	 */
	public void setMove2dirAfterError(String errorDir) {
		move2dirAfterError = errorDir;
	}
	public String getMove2dirAfterError() {
		return move2dirAfterError;
	}

}
