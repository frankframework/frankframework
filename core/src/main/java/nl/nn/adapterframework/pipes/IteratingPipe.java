/*
   Copyright 2013, 2016, 2020 Nationale-Nederlanden

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
package nl.nn.adapterframework.pipes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.commons.lang.StringUtils;
import org.springframework.core.task.TaskExecutor;
import org.xml.sax.SAXException;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IDataIterator;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.ISender;
import nl.nn.adapterframework.core.ISenderWithParameters;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.senders.ParallelSenderExecutor;
import nl.nn.adapterframework.statistics.StatisticsKeeper;
import nl.nn.adapterframework.statistics.StatisticsKeeperIterationHandler;
import nl.nn.adapterframework.stream.IOutputStreamingSupport;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.Guard;
import nl.nn.adapterframework.util.TransformerPool;
import nl.nn.adapterframework.util.XmlUtils;

/**
 * Abstract base class to sends a message to a Sender for each item returned by a configurable iterator.
 * 
 * <tr><td>{@link #setResultOnTimeOut(String) resultOnTimeOut}</td><td>result returned when no return-message was received within the timeout limit (e.g. "receiver timed out").</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setLinkMethod(String) linkMethod}</td><td>Indicates wether the server uses the correlationID or the messageID in the correlationID field of the reply</td><td>CORRELATIONID</td></tr>
 * <tr><td>{@link #setAuditTrailXPath(String) auditTrailXPath}</td><td>xpath expression to extract audit trail from message</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setCorrelationIDXPath(String) correlationIDXPath}</td><td>xpath expression to extract correlationID from message</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setStyleSheetName(String) styleSheetName}</td><td>stylesheet to apply to each message, before sending it</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setXpathExpression(String) xpathExpression}</td><td>alternatively: XPath-expression to create stylesheet from</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setNamespaceDefs(String) namespaceDefs}</td><td>namespace defintions for xpathExpression. Must be in the form of a comma or space separated list of <code>prefix=namespaceuri</code>-definitions</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setOutputType(String) outputType}</td><td>either 'text' or 'xml'. Only valid for xpathExpression</td><td>text</td></tr>
 * <tr><td>{@link #setOmitXmlDeclaration(boolean) omitXmlDeclaration}</td><td>force the transformer generated from the XPath-expression to omit the xml declaration</td><td>true</td></tr>
 * <tr><td>{@link #setIgnoreExceptions(boolean) ignoreExceptions}</td><td>when <code>true</code> ignore any exception thrown by executing sender</td><td>false</td></tr>
 * <tr><td>{@link #setStopConditionXPathExpression(String) stopConditionXPathExpression}</td><td>expression evaluated on each result if set. 
 * 		Iteration stops if condition returns anything other than <code>false</code> or an empty result.
 * For example, to stop after the second child element has been processed, one of the following expressions could be used:
 * <table> 
 * <tr><td><li><code>result[position()='2']</code></td><td>returns result element after second child element has been processed</td></tr>
 * <tr><td><li><code>position()='2'</code></td><td>returns <code>false</code> after second child element has been processed, <code>true</code> for others</td></tr>
 * </table> 
 * </td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setRemoveXmlDeclarationInResults(boolean) removeXmlDeclarationInResults}</td><td>postprocess each partial result, to remove the xml-declaration, as this is not allowed inside an xml-document</td><td>false</td></tr>
 * <tr><td>{@link #setCollectResults(boolean) collectResults}</td><td>controls whether all the results of each iteration will be collected in one result message. If set <code>false</code>, only a small summary is returned</td><td>true</td></tr>
 * <tr><td>{@link #setBlockSize(int) blockSize}</td><td>controls multiline behaviour. when set to a value greater than 0, it specifies the number of rows send in a block to the sender.</td><td>0 (one line at a time, no prefix of suffix)</td></tr>
 * <tr><td>{@link #setBlockPrefix(String) blockPrefix}</td><td>When <code>blockSize &gt; 0</code>, this string is inserted at the start of the set of lines.</td><td>&lt;block&gt;</td></tr>
 * <tr><td>{@link #setBlockSuffix(String) blockSuffix}</td><td>When <code>blockSize &gt; 0</code>, this string is inserted at the end of the set of lines.</td><td>&lt;/block&gt;</td></tr>
 * <tr><td>{@link #setStartPosition(int) startPosition}</td><td>When <code>startPosition &gt;= 0</code>, this field contains the start position of the key in the current record (first character is 0); all sequenced lines with the same key are put in one block and send to the sender</td><td>-1</td></tr>
 * <tr><td>{@link #setEndPosition(int) endPosition}</td><td>When <code>endPosition &gt;= startPosition</code>, this field contains the end position of the key in the current record</td><td>-1</td></tr>
 * <tr><td>{@link #setLinePrefix(String) linePrefix}</td><td>this string is inserted at the start of each line</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setLineSuffix(String) lineSuffix}</td><td>this string is inserted at the end of each line</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setItemNoSessionKey(String) itemNoSessionKey}</td><td>key of session variable to store number of item processed.</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setAddInputToResult(boolean) addInputToResult}</td><td>when <code>true</code> the input is added to the result in an input element</td><td>false</td></tr>
 * <tr><td>{@link #setRemoveDuplicates(boolean) removeDuplicates}</td><td>when <code>true</code> duplicate input elements are removed</td><td>false</td></tr>
 * </table>
 * <table border="1">
 * <tr><th>nested elements</th><th>description</th></tr>
 * <tr><td>{@link ISender sender}</td><td>specification of sender to send messages with</td></tr>
 * <tr><td>{@link nl.nn.adapterframework.core.ICorrelatedPullingListener listener}</td><td>specification of listener to listen to for replies</td></tr>
 * <tr><td>{@link nl.nn.adapterframework.parameters.Parameter param}</td><td>any parameters defined on the pipe will be handed to the sender, if this is a {@link ISenderWithParameters ISenderWithParameters}</td></tr>
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
 */
public abstract class IteratingPipe<I> extends MessageSendingPipe {

	private String styleSheetName;
	private String xpathExpression=null;
	private String namespaceDefs = null; 
	private String outputType="text";
	private boolean omitXmlDeclaration=true;

	private String itemNoSessionKey=null;

	private String stopConditionXPathExpression=null;
	private int maxItems;
	private boolean ignoreExceptions=false;

	private boolean collectResults=true;
	private boolean removeXmlDeclarationInResults=false;
	private boolean addInputToResult=false;
	private boolean removeDuplicates=false;
	
	private boolean closeIteratorOnExit=true;
	private boolean parallel = false;
	
	private int blockSize=0;
	private String blockPrefix="<block>";
	private String blockSuffix="</block>";
	private String linePrefix="";
	private String lineSuffix="";

	private int startPosition=-1;
	private int endPosition=-1;

	private TaskExecutor taskExecutor;
	protected TransformerPool msgTransformerPool;
	private TransformerPool stopConditionTp=null;
	private StatisticsKeeper preprocessingStatisticsKeeper;
	private StatisticsKeeper senderStatisticsKeeper;
	private StatisticsKeeper stopConditionStatisticsKeeper;


	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		msgTransformerPool = TransformerPool.configureTransformer(getLogPrefix(null), getConfigurationClassLoader(), getNamespaceDefs(), getXpathExpression(), getStyleSheetName(), getOutputType(), !isOmitXmlDeclaration(), getParameterList(), false);
		if (msgTransformerPool!=null) {
			preprocessingStatisticsKeeper =  new StatisticsKeeper("-> message preprocessing");
		}
		try {
			if (StringUtils.isNotEmpty(getStopConditionXPathExpression())) {
				stopConditionTp=TransformerPool.getInstance(XmlUtils.createXPathEvaluatorSource(null,getStopConditionXPathExpression(),"xml",false));
				stopConditionStatisticsKeeper =  new StatisticsKeeper("-> stop condition determination");
			}
		} catch (TransformerConfigurationException e) {
			throw new ConfigurationException("Cannot compile stylesheet from stopConditionXPathExpression ["+getStopConditionXPathExpression()+"]", e);
		}
	}

	protected IDataIterator<I> getIterator(Message input, IPipeLineSession session, Map<String,Object> threadContext) throws SenderException {
		return null;
	}

	protected Message itemToMessage(I item) throws SenderException {
		return Message.asMessage(item);
	}

	/**
	 * Alternative way to provide iteration, for classes that cannot provide an Iterator via {@link #getIterator}.
	 * For each item in the input callback.handleItem(item) is called.
	 */
	protected void iterateOverInput(Message input, IPipeLineSession session, Map<String,Object> threadContext, ItemCallback callback) throws SenderException, TimeOutException {
		 throw new SenderException("Could not obtain iterator and no iterateInput method provided by class ["+ClassUtils.nameOf(this)+"]");
	}

	protected class ItemCallback {
		private IPipeLineSession session;
		private ISender sender; 
		private StringBuffer results = new StringBuffer();
		int count=0;
		private Vector<I> inputItems = new Vector<I>();
		private Guard guard;
		List<ParallelSenderExecutor> executorList;

		public ItemCallback(IPipeLineSession session, ISender sender) {
			this.session=session;
			this.sender=sender;
			if (isParallel() && isCollectResults()) {
				guard = new Guard();
				executorList = new ArrayList<ParallelSenderExecutor>();
			}
		}
		public boolean handleItem(I item) throws SenderException, TimeOutException, IOException {
			if (isParallel() && isCollectResults()) {
				guard.addResource();
			}
			if (isRemoveDuplicates()) {
				if (inputItems.indexOf(item)>=0) {
					log.debug(getLogPrefix(session)+"duplicate item ["+item+"] will not be processed");
					return true;
				} else {
					inputItems.add(item);
				}
			}
			String itemResult=null;
			count++;
			if (StringUtils.isNotEmpty(getItemNoSessionKey())) {
				session.put(getItemNoSessionKey(),""+count);
			}
			Message message=itemToMessage(item);
			// TODO check for bug: sessionKey params not resolved when only parameters set on sender. Next line should check sender.parameterlist too.
			if (msgTransformerPool!=null) {
				try {
					long preprocessingStartTime = System.currentTimeMillis();
					
					Map<String,Object>parameterValueMap = getParameterList()!=null?getParameterList().getValues(message, session).getValueMap():null;
					String transformedMsg=msgTransformerPool.transform(message.asSource(),parameterValueMap);
					if (log.isDebugEnabled()) {
						log.debug(getLogPrefix(session)+"iteration ["+count+"] transformed item ["+message+"] into ["+transformedMsg+"]");
					}
					message=new Message(transformedMsg);
					long preprocessingEndTime = System.currentTimeMillis();
					long preprocessingDuration = preprocessingEndTime - preprocessingStartTime;
					preprocessingStatisticsKeeper.addValue(preprocessingDuration);
				} catch (Exception e) {
					throw new SenderException(getLogPrefix(session)+"cannot transform item",e);
				}
			} else {
				if (log.isDebugEnabled()) {
					log.debug(getLogPrefix(session)+"iteration ["+count+"] item ["+message+"]");
				} 
			}
			try {
				if (isParallel()) {
					ParallelSenderExecutor pse= new ParallelSenderExecutor(sender, message, session, guard, senderStatisticsKeeper);
					if (isCollectResults()) {
						executorList.add(pse);
					}
					getTaskExecutor().execute(pse);
				} else {
					long senderStartTime= System.currentTimeMillis();
					itemResult = sender.sendMessage(message, session).asString();
					long senderEndTime = System.currentTimeMillis();
					long senderDuration = senderEndTime - senderStartTime;
					senderStatisticsKeeper.addValue(senderDuration);
				}
				if (StringUtils.isNotEmpty(getTimeOutOnResult()) && getTimeOutOnResult().equals(itemResult)) {
					throw new TimeOutException(getLogPrefix(session)+"timeOutOnResult ["+getTimeOutOnResult()+"]");
				}
				if (StringUtils.isNotEmpty(getExceptionOnResult()) && getExceptionOnResult().equals(itemResult)) {
					throw new SenderException(getLogPrefix(session)+"exceptionOnResult ["+getExceptionOnResult()+"]");
				}
			} catch (SenderException e) {
				if (isIgnoreExceptions()) {
					log.info(getLogPrefix(session)+"ignoring SenderException after excution of sender for item ["+item+"]",e);
					itemResult="<exception>"+XmlUtils.encodeChars(e.getMessage())+"</exception>";
				} else {
					throw e;
				}
			} catch (TimeOutException e) {
				if (isIgnoreExceptions()) {
					log.info(getLogPrefix(session)+"ignoring TimeOutException after excution of sender for item ["+item+"]",e);
					itemResult="<timeout>"+XmlUtils.encodeChars(e.getMessage())+"</timeout>";
				} else {
					throw e;
				}
			}
			try {
				if (isCollectResults() && !isParallel()) {
					addResult(count, message, itemResult);
				}
				if (getMaxItems()>0 && count>=getMaxItems()) {
					log.debug(getLogPrefix(session)+"count ["+count+"] reached maxItems ["+getMaxItems()+"], stopping loop");
					return false;
				}
				if (getStopConditionTp()!=null) {
					long stopConditionStartTime = System.currentTimeMillis();
					String stopConditionResult = getStopConditionTp().transform(itemResult,null);
					long stopConditionEndTime = System.currentTimeMillis();
					long stopConditionDuration = stopConditionEndTime - stopConditionStartTime;
					stopConditionStatisticsKeeper.addValue(stopConditionDuration);
					if (StringUtils.isNotEmpty(stopConditionResult) && !stopConditionResult.equalsIgnoreCase("false")) {
						log.debug(getLogPrefix(session)+"itemResult ["+itemResult+"] stopcondition result ["+stopConditionResult+"], stopping loop");
						return false;
					} else {
						log.debug(getLogPrefix(session)+"itemResult ["+itemResult+"] stopcondition result ["+stopConditionResult+"], continueing loop");
					}
				}
				return true;
			} catch (SAXException e) {
				throw new SenderException(getLogPrefix(session)+"cannot parse input",e);
			} catch (TransformerException e) {
				throw new SenderException(getLogPrefix(session)+"cannot serialize item",e);
			} catch (IOException e) {
				throw new SenderException(getLogPrefix(session)+"cannot serialize item",e);
			}
		}
		private void addResult(int count, Message message, String itemResult) throws IOException {
			if (isRemoveXmlDeclarationInResults()) {
				if (log.isDebugEnabled()) log.debug(getLogPrefix(session)+"removing XML declaration from ["+itemResult+"]");
				itemResult = XmlUtils.skipXmlDeclaration(itemResult);
			} 
			if (log.isDebugEnabled()) log.debug(getLogPrefix(session)+"partial result ["+itemResult+"]");
			String itemInput="";
			if (isAddInputToResult()) {
				itemInput = "<input>"+(isRemoveXmlDeclarationInResults()?XmlUtils.skipXmlDeclaration(message.asString()):message.asString())+"</input>";
			}
			itemResult = "<result item=\"" + count + "\">\n"+itemInput+itemResult+"\n</result>";
			results.append(itemResult+"\n");
		}
		
		public StringBuffer getResults() throws SenderException, IOException {
			if (isParallel()) {
				try {
					guard.waitForAllResources();
					int count = 0;
					for (ParallelSenderExecutor pse : executorList) {
						count++;
						String itemResult;
						if (pse.getThrowable() == null) {
							itemResult = pse.getReply().toString();
						} else {
							itemResult = "<exception>"+XmlUtils.encodeChars(pse.getThrowable().getMessage())+"</exception>";
						}
						addResult(count, pse.getRequest(), itemResult);
					}
				} catch (InterruptedException e) {
					throw new SenderException(getLogPrefix(session)+"was interupted",e);
				}
			}
			return results;
		}
		
		public int getCount() {
			return count;
		}
	}

	@Override
	protected PipeRunResult sendMessage(Message input, IPipeLineSession session, ISender sender, Map<String,Object> threadContext, IOutputStreamingSupport nextProvider) throws SenderException, TimeOutException, IOException {
		// sendResult has a messageID for async senders, the result for sync senders
		boolean keepGoing = true;
		IDataIterator<I> it=null;
		try {
			ItemCallback callback = new ItemCallback(session,sender);
			it = getIterator(input,session, threadContext);
			if (it==null) {
				iterateOverInput(input,session,threadContext, callback);
			} else {
				String nextItemStored = null;
				while (keepGoing && (it.hasNext() || nextItemStored!=null)) {
					if (Thread.currentThread().isInterrupted()) {
						throw new TimeOutException("Thread has been interrupted");
					}
					StringBuffer items = new StringBuffer();
					if (getBlockSize()>0) { // blockSize>0 requires item type 'I' to be String
						items.append(getBlockPrefix());
						for (int i=0; i<getBlockSize() && it.hasNext(); i++) {
							String item = (String)it.next();
							items.append(getLinePrefix());
							items.append(item);
							items.append(getLineSuffix());
						}
						items.append(getBlockSuffix());
 						keepGoing = callback.handleItem((I)items.toString());  // cannot just cast to I, but anyhow....
						
					} else {
						if (getStartPosition()>=0 && getEndPosition()>getStartPosition()) {
							items.append(getBlockPrefix());
							String keyPreviousItem = null;
							boolean sameKey = true;
							while (sameKey && (it.hasNext() || nextItemStored!=null)) {
								String item;
								if (nextItemStored==null) {
									item = (String)it.next();
								} else {
									item = nextItemStored;
									nextItemStored = null;
								}
								String key;
								if (getEndPosition() >= item.length()) {
									key = item.substring(getStartPosition());
								}
								else {
									key = item.substring(getStartPosition(), getEndPosition());
								}
								if (keyPreviousItem==null || key.equals(keyPreviousItem)) {
									items.append(getLinePrefix());
									items.append(item);
									items.append(getLineSuffix());
									if (keyPreviousItem==null) {
										keyPreviousItem = key;
									}
								} else {
									sameKey = false;
									nextItemStored = item;
								}
							}
							items.append(getBlockSuffix());
	 						keepGoing = callback.handleItem((I)items.toString()); // cannot just cast to I, but anyhow....
						} else {
							I item = getItem(it);
							items.append(getLinePrefix());
							items.append(item);
							items.append(getLineSuffix());
							keepGoing = callback.handleItem(item); 
						}
					}
				}
			}
			String results = "";
			if (isCollectResults()) {
				StringBuffer callbackResults = callback.getResults();
				callbackResults.insert(0, "<results>\n");
				callbackResults.append("</results>");
				results = callbackResults.toString();
			} else {
				results = "<results count=\""+callback.getCount()+"\"/>";
			}
			return new PipeRunResult(getForward(), results);
		} finally {
			if (it!=null) {
				try {
					if (isCloseIteratorOnExit()) {
						it.close();
					}
				} catch (Exception e) {
					log.warn("Exception closing iterator", e);
				} 
			}
		}
	}

	@Override
	public boolean requiresOutputStream() {
		//return super.requiresOutputStream();
		return false; // TODO must modify result collection to support output streaming 
	}

	protected I getItem(IDataIterator<I> it) throws SenderException {
		return it.next();
	}

	@Override
	public void iterateOverStatistics(StatisticsKeeperIterationHandler hski, Object data, int action) throws SenderException {
		super.iterateOverStatistics(hski, data, action);
		if (preprocessingStatisticsKeeper!=null) {
			hski.handleStatisticsKeeper(data, preprocessingStatisticsKeeper);
		}
		hski.handleStatisticsKeeper(data, senderStatisticsKeeper);
		if (stopConditionStatisticsKeeper!=null) {
			hski.handleStatisticsKeeper(data, stopConditionStatisticsKeeper);
		}
	}

	@Override
	public void setSender(ISender sender) {
		super.setSender(sender);
		senderStatisticsKeeper =  new StatisticsKeeper("-> "+(StringUtils.isNotEmpty(sender.getName())?sender.getName():ClassUtils.nameOf(sender)));
	}

	public void setTaskExecutor(TaskExecutor executor) {
		taskExecutor = executor;
	}
	public TaskExecutor getTaskExecutor() {
		return taskExecutor;
	}

	protected TransformerPool getStopConditionTp() {
		return stopConditionTp;
	}

	@IbisDoc({"1", "Stylesheet to apply to each message, before sending it", ""})
	public void setStyleSheetName(String stylesheetName){
		this.styleSheetName=stylesheetName;
	}
	public String getStyleSheetName() {
		return styleSheetName;
	}

	@IbisDoc({"2", "Alternatively: xpath-expression to create stylesheet from", ""})
	public void setXpathExpression(String string) {
		xpathExpression = string;
	}
	public String getXpathExpression() {
		return xpathExpression;
	}

	@IbisDoc({"3", "Namespace defintions for xpathExpression. Must be in the form of a comma or space separated list of <code>prefix=namespaceuri</code>-definitions. For some use other cases (NOT xpathExpression), one entry can be without a prefix, that will define the default namespace.", ""})
	public void setNamespaceDefs(String namespaceDefs) {
		this.namespaceDefs = namespaceDefs;
	}
	public String getNamespaceDefs() {
		return namespaceDefs;
	}

	@IbisDoc({"4", "Either 'text' or 'xml'. only valid for xpathexpression", "text"})
	public void setOutputType(String string) {
		outputType = string;
	}
	public String getOutputType() {
		return outputType;
	}

	@IbisDoc({"5", "Force the transformer generated from the xpath-expression to omit the xml declaration", "true"})
	public void setOmitXmlDeclaration(boolean b) {
		omitXmlDeclaration = b;
	}
	public boolean isOmitXmlDeclaration() {
		return omitXmlDeclaration;
	}


	@IbisDoc({"6", "Key of session variable to store number of items processed, i.e. the position or index in the set of items to be processed.", ""})
	public void setItemNoSessionKey(String string) {
		itemNoSessionKey = string;
	}
	public String getItemNoSessionKey() {
		return itemNoSessionKey;
	}

	@IbisDoc({"7", "The maximum number of items returned. The (default) value of 0 means unlimited, all available items will be returned","0"})
	public void setMaxItems(int maxItems) {
		this.maxItems = maxItems;
	}
	public int getMaxItems() {
		return maxItems;
	}

	@IbisDoc({"7", "Expression evaluated on each result if set. "
	+ "Iteration stops if condition returns anything other than an empty result. To test for the root element to have an attribute 'finished' with the value 'yes', the expression <code>*[@finished='yes']</code> can be used. "
	+ "This can be used if the condition to stop can be derived from the item result. To stop after a maximum number of items has been processed, use <code>maxItems</code>."
	+ "Previous versions documented that <code>position()=2</code> could be used. This is not working as expected.", ""})
	public void setStopConditionXPathExpression(String string) {
		stopConditionXPathExpression = string;
	}
	public String getStopConditionXPathExpression() {
		return stopConditionXPathExpression;
	}

	@IbisDoc({"8", "When <code>true</code> ignore any exception thrown by executing sender", "false"})
	public void setIgnoreExceptions(boolean b) {
		ignoreExceptions = b;
	}
	public boolean isIgnoreExceptions() {
		return ignoreExceptions;
	}

	
	@IbisDoc({"9", "Controls whether all the results of each iteration will be collected in one result message. If set <code>false</code>, only a small summary is returned. "
		+ "Setting this attributes to <code>false</code> is often required to enable processing of very large files. N.B. Remember in such a case that setting transactionAttribute to NotSupported might be necessary too", "true"})
	public void setCollectResults(boolean b) {
		collectResults = b;
	}
	public boolean isCollectResults() {
		return collectResults;
	}

	@IbisDoc({"10", "Postprocess each partial result, to remove the xml-declaration, as this is not allowed inside an xml-document", "false"})
	public void setRemoveXmlDeclarationInResults(boolean b) {
		removeXmlDeclarationInResults = b;
	}
	public boolean isRemoveXmlDeclarationInResults() {
		return removeXmlDeclarationInResults;
	}

	@IbisDoc({"11", "When <code>true</code> the input is added to the result in an input element", "false"})
	public void setAddInputToResult(boolean b) {
		addInputToResult = b;
	}
	public boolean isAddInputToResult() {
		return addInputToResult;
	}

	@IbisDoc({"12", "When <code>true</code> duplicate input elements are removed, i.e. they are handled only once", "false"})
	public void setRemoveDuplicates(boolean b) {
		removeDuplicates = b;
	}
	public boolean isRemoveDuplicates() {
		return removeDuplicates;
	}

	protected void setCloseIteratorOnExit(boolean b) {
		closeIteratorOnExit = b;
	}
	protected boolean isCloseIteratorOnExit() {
		return closeIteratorOnExit;
	}

	@IbisDoc({"13", "When set <code>true</code>, the calls for all items are done in parallel (a new thread is started for each call). when collectresults set <code>true</code>, this pipe will wait for all calls to finish before results are collected and pipe result is returned", "false"})
	public void setParallel(boolean parallel) {
		this.parallel = parallel;
	}
	public boolean isParallel() {
		return parallel;
	}


	
	@IbisDoc({"14", "Controls multiline behaviour. when set to a value greater than 0, it specifies the number of rows send in a block to the sender.", "0 (one line at a time, no prefix of suffix)"})
	public void setBlockSize(int i) {
		blockSize = i;
	}
	public int getBlockSize() {
		return blockSize;
	}

	@IbisDoc({"15", "When <code>blocksize &gt; 0</code>, this string is inserted at the start of the set of lines.", "&lt;block&gt;"})
	public void setBlockPrefix(String string) {
		blockPrefix = string;
	}
	public String getBlockPrefix() {
		return blockPrefix;
	}

	@IbisDoc({"16", "When <code>blocksize &gt; 0</code>, this string is inserted at the end of the set of lines.", "&lt;/block&gt;"})
	public void setBlockSuffix(String string) {
		blockSuffix = string;
	}
	public String getBlockSuffix() {
		return blockSuffix;
	}

	@IbisDoc({"17", "This string is inserted at the start of each line", ""})
	public void setLinePrefix(String string) {
		linePrefix = string;
	}
	public String getLinePrefix() {
		return linePrefix;
	}

	@IbisDoc({"18", "This string is inserted at the end of each line", ""})
	public void setLineSuffix(String string) {
		lineSuffix = string;
	}
	public String getLineSuffix() {
		return lineSuffix;
	}

	@IbisDoc({"19", "When <code>startposition &gt;= 0</code>, this field contains the start position of the key in the current record (first character is 0); all sequenced lines with the same key are put in one block and send to the sender", "-1"})
	public void setStartPosition(int i) {
		startPosition = i;
	}
	public int getStartPosition() {
		return startPosition;
	}

	@IbisDoc({"20", "When <code>endposition &gt;= startposition</code>, this field contains the end position of the key in the current record", "-1"})
	public void setEndPosition(int i) {
		endPosition = i;
	}
	public int getEndPosition() {
		return endPosition;
	}

}
