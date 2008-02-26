/*
 * $Log: IteratingPipe.java,v $
 * Revision 1.7  2008-02-26 09:18:50  europe\L190409
 * updated javadoc
 *
 * Revision 1.6  2008/02/21 12:49:05  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added option for pushing iteration
 *
 * Revision 1.5  2007/10/08 12:23:51  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * changed HashMap to Map where possible
 *
 * Revision 1.4  2007/08/03 08:47:16  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * updated javadoc
 *
 * Revision 1.3  2007/07/17 10:54:25  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * removed unused code
 *
 * Revision 1.2  2007/07/17 10:49:42  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * uses now IDataIterator
 *
 * Revision 1.1  2007/07/10 08:01:46  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * first version, lightly based on ForEachChildElementPipe
 *
 */
package nl.nn.adapterframework.pipes;

import java.io.IOException;
import java.util.Map;

import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IDataIterator;
import nl.nn.adapterframework.core.ISender;
import nl.nn.adapterframework.core.ISenderWithParameters;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.DomBuilderException;
import nl.nn.adapterframework.util.TransformerPool;
import nl.nn.adapterframework.util.XmlUtils;

import org.apache.commons.lang.StringUtils;

/**
 * Abstract base class to sends a message to a Sender for each item returned by a configurable iterator.
 * 
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>className</td><td>nl.nn.adapterframework.pipes.IteratingPipe</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setName(String) name}</td><td>name of the Pipe</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setMaxThreads(int) maxThreads}</td><td>maximum number of threads that may call {@link #doPipe(Object, nl.nn.adapterframework.core.PipeLineSession)} simultaneously</td><td>0 (unlimited)</td></tr>
 * <tr><td>{@link #setDurationThreshold(long) durationThreshold}</td><td>if durationThreshold >=0 and the duration (in milliseconds) of the message processing exceeded the value specified the message is logged informatory</td><td>-1</td></tr>
 * <tr><td>{@link #setGetInputFromSessionKey(String) getInputFromSessionKey}</td><td>when set, input is taken from this session key, instead of regular input</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setStoreResultInSessionKey(String) storeResultInSessionKey}</td><td>when set, the result is stored under this session key</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setCheckXmlWellFormed(boolean) checkXmlWellFormed}</td><td>when set <code>true</code>, the XML well-formedness of the result is checked</td><td>false</td></tr>
 * <tr><td>{@link #setPreserveInput(boolean) preserveInput}</td><td>when set <code>true</code>, the input of a pipe is restored before processing the next one</td><td>false</td></tr>
 * <tr><td>{@link #setNamespaceAware(boolean) namespaceAware}</td><td>controls namespaceAwareness for parameters</td><td>application default</td></tr>
 * <tr><td>{@link #setTransactionAttribute(String) transactionAttribute}</td><td>Defines transaction and isolation behaviour. Equal to <A href="http://java.sun.com/j2ee/sdk_1.2.1/techdocs/guides/ejb/html/Transaction2.html#10494">EJB transaction attribute</a>. Possible values are: 
 *   <table border="1">
 *   <tr><th>transactionAttribute</th><th>callers Transaction</th><th>Pipe excecuted in Transaction</th></tr>
 *   <tr><td colspan="1" rowspan="2">Required</td>    <td>none</td><td>T2</td></tr>
 * 											      <tr><td>T1</td>  <td>T1</td></tr>
 *   <tr><td colspan="1" rowspan="2">RequiresNew</td> <td>none</td><td>T2</td></tr>
 * 											      <tr><td>T1</td>  <td>T2</td></tr>
 *   <tr><td colspan="1" rowspan="2">Mandatory</td>   <td>none</td><td>error</td></tr>
 * 											      <tr><td>T1</td>  <td>T1</td></tr>
 *   <tr><td colspan="1" rowspan="2">NotSupported</td><td>none</td><td>none</td></tr>
 * 											      <tr><td>T1</td>  <td>none</td></tr>
 *   <tr><td colspan="1" rowspan="2">Supports</td>    <td>none</td><td>none</td></tr>
 * 											      <tr><td>T1</td>  <td>T1</td></tr>
 *   <tr><td colspan="1" rowspan="2">Never</td>       <td>none</td><td>none</td></tr>
 * 											      <tr><td>T1</td>  <td>error</td></tr>
 *  </table></td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setBeforeEvent(int) beforeEvent}</td>      <td>METT eventnumber, fired just before a message is processed by this Pipe</td><td>-1 (disabled)</td></tr>
 * <tr><td>{@link #setAfterEvent(int) afterEvent}</td>        <td>METT eventnumber, fired just after message processing by this Pipe is finished</td><td>-1 (disabled)</td></tr>
 * <tr><td>{@link #setExceptionEvent(int) exceptionEvent}</td><td>METT eventnumber, fired when message processing by this Pipe resulted in an exception</td><td>-1 (disabled)</td></tr>
 * <tr><td>{@link #setForwardName(String) forwardName}</td>  <td>name of forward returned upon completion</td><td>"success"</td></tr>
 * <tr><td>{@link #setResultOnTimeOut(String) resultOnTimeOut}</td><td>result returned when no return-message was received within the timeout limit</td><td>"receiver timed out"</td></tr>
 * <tr><td>{@link #setLinkMethod(String) linkMethod}</td><td>Indicates wether the server uses the correlationID or the messageID in the correlationID field of the reply</td><td>CORRELATIONID</td></tr>
 * <tr><td>{@link #setAuditTrailXPath(String) auditTrailXPath}</td><td>xpath expression to extract audit trail from message</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setCorrelationIDXPath(String) correationIdXPath}</td><td>xpath expression to extract correlationID from message</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setStopConditionXPathExpression(String) stopConditionXPathExpression}</td><td>expression evaluated on each result if set. 
 * 		Iteration stops if condition returns anything other than <code>false</code> or an empty result.
 * For example, to stop after the second child element has been processed, one of the following expressions could be used:
 * <table> 
 * <tr><td><li><code>result[@item='2']</code></td><td>returns result element after second child element has been processed</td></tr>
 * <tr><td><li><code>result/@item='2'</code></td><td>returns <code>false</code> after second child element has been processed, <code>true</code> for others</td></tr>
 * </table> 
 * </td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setRemoveXmlDeclarationInResults(boolean) removeXmlDeclarationInResults}</td><td>postprocess each partial result, to remove the xml-declaration, as this is not allowed inside an xml-document</td><td>false</td></tr>
 * <tr><td>{@link #setCollectResults(boolean) collectResults}</td><td>controls whether all the results of each iteration will be collected in one result message. If set <code>false</code>, only a small summary is returned</td><td>true</td></tr>
 * </table>
 * <table border="1">
 * <tr><th>nested elements</th><th>description</th></tr>
 * <tr><td>{@link nl.nn.adapterframework.core.ISender sender}</td><td>specification of sender to send messages with</td></tr>
 * <tr><td>{@link nl.nn.adapterframework.core.ICorrelatedPullingListener listener}</td><td>specification of listener to listen to for replies</td></tr>
 * <tr><td>{@link nl.nn.adapterframework.parameters.Parameter param}</td><td>any parameters defined on the pipe will be handed to the sender, if this is a {@link IParameterizedSender}</td></tr>
 * <tr><td><code>inputValidator</code></td><td>specification of Pipe to validate input messages</td></tr>
 * <tr><td><code>outputValidator</code></td><td>specification of Pipe to validate output messages</td></tr>
 * <tr><td>{@link nl.nn.adapterframework.core.ITransactionalStorage messageLog}</td><td>log of all messages sent</td></tr>
 * </table>
 * </p>
 * <p><b>Exits:</b>
 * <table border="1">
 * <tr><th>state</th><th>condition</th></tr>
 * <tr><td>"success"</td><td>default when a good message was retrieved (synchronous sender), or the message was successfully sent and no listener was specified and the sender was not synchronous</td></tr>
 * <tr><td><i>{@link #setForwardName(String) forwardName}</i></td><td>if specified, and otherwise under same condition as "success"</td></tr>
 * <tr><td>"timeout"</td><td>no data was received (timeout on listening), if the sender was synchronous or a listener was specified.</td></tr>
 * <tr><td>"exception"</td><td>an exception was thrown by the Sender or its reply-Listener. The result passed to the next pipe is the exception that was caught.</td></tr>
 * </table>
 * </p>
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
public abstract class IteratingPipe extends MessageSendingPipe {
	public static final String version="$RCSfile: IteratingPipe.java,v $ $Revision: 1.7 $ $Date: 2008-02-26 09:18:50 $";

	private String stopConditionXPathExpression=null;
	private boolean removeXmlDeclarationInResults=false;
	private boolean collectResults=true;

	private TransformerPool stopConditionTp=null;
	private TransformerPool encapsulateResultsTp=null;

	protected String makeEncapsulatingXslt(String rootElementname,String xpathExpression) {
		return 
		"<xsl:stylesheet xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\" version=\"1.0\" xmlns:xalan=\"http://xml.apache.org/xslt\">" +
		"<xsl:output method=\"xml\" omit-xml-declaration=\"yes\"/>" +
		"<xsl:strip-space elements=\"*\"/>" +
		"<xsl:template match=\"/\">" +
		"<xsl:element name=\"" + rootElementname + "\">" +
		"<xsl:copy-of select=\"" + xpathExpression + "\"/>" +
		"</xsl:element>" +
		"</xsl:template>" +
		"</xsl:stylesheet>";
	}


	public void configure() throws ConfigurationException {
		super.configure();
		try {
			if (StringUtils.isNotEmpty(getStopConditionXPathExpression())) {
				stopConditionTp=new TransformerPool(XmlUtils.createXPathEvaluatorSource(null,getStopConditionXPathExpression(),"xml",false));
			}
			if (isRemoveXmlDeclarationInResults()) {
				encapsulateResultsTp=new TransformerPool( makeEncapsulatingXslt("result","*"));
			}
		} catch (TransformerConfigurationException e) {
			throw new ConfigurationException(e);
		}
	}

	protected IDataIterator getIterator(Object input, PipeLineSession session, String correlationID, Map threadContext) throws SenderException {
		return null;
	}

	protected void iterateInput(Object input, PipeLineSession session, String correlationID, Map threadContext, ItemCallback callback) throws SenderException {
		 throw new SenderException("Could not obtain iterator and no iterateInput method provided by class ["+ClassUtils.nameOf(this)+"]");
	}

	protected class ItemCallback {
		
		PipeLineSession session;
		String correlationID;
		ISender sender; 
		ISenderWithParameters psender=null;
		
		private String results="";
		int count=0;
		
		public ItemCallback(PipeLineSession session, String correlationID, ISender sender) {
			this.session=session;
			this.correlationID=correlationID;
			this.sender=sender;
			if (sender instanceof ISenderWithParameters && getParameterList()!=null) {
				psender = (ISenderWithParameters) sender;
			}		
		}
		public boolean handleItem(String item) throws SenderException, TimeOutException {
			String itemResult=null;
			count++;
			if (log.isDebugEnabled()) {
				//log.debug(getLogPrefix(session)+"set current item to ["+item+"]");
				log.debug(getLogPrefix(session)+"sending item no ["+count+"]");
			} 
			if (psender!=null) {
				//result = psender.sendMessage(correlationID, item, new ParameterResolutionContext(src, session));
				//TODO find out why ParameterResolutionContext cannot be constructed using dom-source
				ParameterResolutionContext prc = new ParameterResolutionContext(item, session, isNamespaceAware());
				itemResult = psender.sendMessage(correlationID, item, prc);
			} else {
				itemResult = sender.sendMessage(correlationID, item);
			}
			try {
				if (isCollectResults()) {
					if (isRemoveXmlDeclarationInResults()) {
						log.debug(getLogPrefix(session)+"post processing partial result ["+itemResult+"]");
						itemResult = getEncapsulateResultsTp().transform(itemResult,null);
					} else {
						log.debug(getLogPrefix(session)+"partial result ["+itemResult+"]");
						itemResult = "<result>\n"+itemResult+"\n</result>";
					}
					results += itemResult+"\n";
				}

				if (getStopConditionTp()!=null) {
					String stopConditionResult = getStopConditionTp().transform(itemResult,null);
					if (StringUtils.isNotEmpty(stopConditionResult) && !stopConditionResult.equalsIgnoreCase("false")) {
						log.debug(getLogPrefix(session)+"stopcondition result ["+stopConditionResult+"], stopping loop");
						return false;
					} else {
						log.debug(getLogPrefix(session)+"stopcondition result ["+stopConditionResult+"], continueing loop");
					}
				}
				return true;
			} catch (DomBuilderException e) {
				throw new SenderException(getLogPrefix(session)+"cannot parse input",e);
			} catch (TransformerException e) {
				throw new SenderException(getLogPrefix(session)+"cannot serialize item",e);
			} catch (IOException e) {
				throw new SenderException(getLogPrefix(session)+"cannot serialize item",e);
			}
		}
		public String getResults() {
			return results;
		}
		public int getCount() {
			return count;
		}
	}

	protected String sendMessage(Object input, PipeLineSession session, String correlationID, ISender sender, Map threadContext) throws SenderException, TimeOutException {
		// sendResult has a messageID for async senders, the result for sync senders
		boolean keepGoing = true;
		IDataIterator it=null;
		try {
			ItemCallback callback = new ItemCallback(session,correlationID,sender);
			it = getIterator(input,session, correlationID,threadContext);
			if (it==null) {
				iterateInput(input,session,correlationID, threadContext, callback);
			} else {
				while (keepGoing && it.hasNext()) {
					String item = (String)it.next();
					keepGoing = callback.handleItem(item); 
				}
			}
			String results = "";
			if (isCollectResults()) {
				results = "<results count=\""+callback.getCount()+"\">\n"+callback.getResults()+"</results>";
			} else {
				results = "<results count=\""+callback.getCount()+"\"/>";
			}
			return results;
		} finally {
			if (it!=null) {
				try {
					it.close();
				} catch (Exception e) {
					log.warn("Exception closing iterator", e);
				} 
			}
		}
	}


	public void setSender(ISender sender) {
		super.setSender(sender);
	}


	public void setStopConditionXPathExpression(String string) {
		stopConditionXPathExpression = string;
	}
	public String getStopConditionXPathExpression() {
		return stopConditionXPathExpression;
	}


	public void setRemoveXmlDeclarationInResults(boolean b) {
		removeXmlDeclarationInResults = b;
	}
	public boolean isRemoveXmlDeclarationInResults() {
		return removeXmlDeclarationInResults;
	}

	public void setCollectResults(boolean b) {
		collectResults = b;
	}
	public boolean isCollectResults() {
		return collectResults;
	}

	protected TransformerPool getEncapsulateResultsTp() {
		return encapsulateResultsTp;
	}

	protected TransformerPool getStopConditionTp() {
		return stopConditionTp;
	}

}
