/*
   Copyright 2013, 2016 Nationale-Nederlanden, 2020 WeAreFrank!

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
import java.io.Writer;
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
import nl.nn.adapterframework.core.IBlockEnabledSender;
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
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.stream.MessageOutputStream;
import nl.nn.adapterframework.stream.StreamingException;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.Guard;
import nl.nn.adapterframework.util.Semaphore;
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
	private int maxChildThreads = 0;
	
	private int blockSize=0;

	private TaskExecutor taskExecutor;
	protected TransformerPool msgTransformerPool;
	private TransformerPool stopConditionTp=null;
	private StatisticsKeeper preprocessingStatisticsKeeper;
	private StatisticsKeeper senderStatisticsKeeper;
	private StatisticsKeeper stopConditionStatisticsKeeper;

	private Semaphore childThreadSemaphore=null;

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
		if (getMaxChildThreads()>0) {
			childThreadSemaphore=new Semaphore(getMaxChildThreads());
		}
	}

	protected IDataIterator<I> getIterator(Message input, IPipeLineSession session, Map<String,Object> threadContext) throws SenderException {
		return null;
	}

	protected ItemCallback createItemCallBack(IPipeLineSession session, ISender sender, Writer out) {
		return new ItemCallback(session, sender, out);
	}
	
	protected Message itemToMessage(I item) throws SenderException {
		return Message.asMessage(item);
	}

	protected void iterateOverInput(Message input, IPipeLineSession session, Map<String,Object> threadContext, ItemCallback callback) throws SenderException, TimeOutException, IOException {
		IDataIterator<I> it=null;
		it = getIterator(input,session, threadContext);
		try {
			callback.startIterating(); // perform startIterating even when it=null, to avoid empty result
			if (it!=null) {
				try {
					boolean keepGoing = true;
					while (keepGoing && (it.hasNext())) {
						if (Thread.currentThread().isInterrupted()) {
							throw new TimeOutException("Thread has been interrupted");
						}
 						keepGoing = callback.handleItem(getItem(it));
					}
				} finally {
					try {
						if (isCloseIteratorOnExit()) {
							it.close();
						}
					} catch (Exception e) {
						log.warn("Exception closing iterator", e);
					} 
				}
			}
		} finally {
			callback.endIterating();
		}
	}

	protected class ItemCallback {
		private IPipeLineSession session;
		private ISender sender; 
		private Writer results;
		private int itemsInBlock=0;
		private int totalItems=0;
		private boolean blockOpen=false;
		private Object blockHandle;
		private Vector<I> inputItems = new Vector<I>();
		private Guard guard;
		private List<ParallelSenderExecutor> executorList;

		public ItemCallback(IPipeLineSession session, ISender sender, Writer out) {
			this.session=session;
			this.sender=sender;
			this.results=out;
			if (isParallel() && isCollectResults()) {
				guard = new Guard();
				executorList = new ArrayList<ParallelSenderExecutor>();
			}
		}
		
		public void startIterating() throws SenderException, TimeOutException, IOException {
			if (isCollectResults()) {
				results.append("<results>\n");
			}
			if (!isParallel() && sender instanceof IBlockEnabledSender<?>) {
				blockHandle = ((IBlockEnabledSender)sender).openBlock(session);
				blockOpen=true;
			}
		}
		public void endIterating() throws SenderException, TimeOutException, IOException {
			if (blockOpen && sender instanceof IBlockEnabledSender<?>) {
				((IBlockEnabledSender)sender).closeBlock(blockHandle, session);
			}
			if (isCollectResults()) {
				waitForResults();
				results.append("</results>");
			} else {
				results.append("<results count=\""+getCount()+"\"/>");
			}
		}
		public void startBlock() throws SenderException, TimeOutException, IOException {
			if (!isParallel() && !blockOpen && sender instanceof IBlockEnabledSender<?>) {
				blockHandle = ((IBlockEnabledSender)sender).openBlock(session);
				blockOpen=true;
			}
		}
		/**
		 * @return true when looping should continue, false when stop is required. 
		 */
		public boolean endBlock() throws SenderException, TimeOutException, IOException {
			if (!isParallel() && sender instanceof IBlockEnabledSender<?>) {
				((IBlockEnabledSender)sender).closeBlock(blockHandle, session);
				blockOpen=false;
			}
			itemsInBlock=0;
			return true;
		}
		
		/**
		 * @return true when looping should continue, false when stop is required. 
		 */
		public boolean handleItem(I item) throws SenderException, TimeOutException, IOException {
			if (isRemoveDuplicates()) {
				if (inputItems.indexOf(item)>=0) {
					log.debug(getLogPrefix(session)+"duplicate item ["+item+"] will not be processed");
					return true;
				} else {
					inputItems.add(item);
				}
			}
			String itemResult=null;
			totalItems++;
			if (StringUtils.isNotEmpty(getItemNoSessionKey())) {
				session.put(getItemNoSessionKey(),""+totalItems);
			}
			Message message=itemToMessage(item);
			// TODO check for bug: sessionKey params not resolved when only parameters set on sender. Next line should check sender.parameterlist too.
			if (msgTransformerPool!=null) {
				try {
					long preprocessingStartTime = System.currentTimeMillis();
					
					Map<String,Object>parameterValueMap = getParameterList()!=null?getParameterList().getValues(message, session).getValueMap():null;
					String transformedMsg=msgTransformerPool.transform(message.asSource(),parameterValueMap);
					if (log.isDebugEnabled()) {
						log.debug(getLogPrefix(session)+"iteration ["+totalItems+"] transformed item ["+message+"] into ["+transformedMsg+"]");
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
					log.debug(getLogPrefix(session)+"iteration ["+totalItems+"] item ["+message+"]");
				} 
			}
			if (childThreadSemaphore!=null) {
				try {
					childThreadSemaphore.acquire();
				} catch (InterruptedException e) {
					throw new SenderException(getLogPrefix(session)+ " interrupted waiting for thread",e);
				}
			}
			try { 
				try {
					if (isParallel()) {
						if (isCollectResults()) {
							guard.addResource();
						}
						ParallelSenderExecutor pse= new ParallelSenderExecutor(sender, message, session, childThreadSemaphore, guard, senderStatisticsKeeper);
						if (isCollectResults()) {
							executorList.add(pse);
						}
						getTaskExecutor().execute(pse);
					} else {
						if (getBlockSize()>0 && itemsInBlock==0) {
							startBlock();
						}
						long senderStartTime= System.currentTimeMillis();
						if (sender instanceof IBlockEnabledSender<?>) {
							itemResult = ((IBlockEnabledSender)sender).sendMessage(blockHandle, message, session).asString();
						} else {
							itemResult = sender.sendMessage(message, session).asString();
						}
						long senderEndTime = System.currentTimeMillis();
						long senderDuration = senderEndTime - senderStartTime;
						senderStatisticsKeeper.addValue(senderDuration);
						if (getBlockSize()>0 && ++itemsInBlock >= getBlockSize()) {
							itemsInBlock=0;
							endBlock();
						}
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
						addResult(totalItems, message, itemResult);
					}
					if (getMaxItems()>0 && totalItems>=getMaxItems()) {
						log.debug(getLogPrefix(session)+"count ["+totalItems+"] reached maxItems ["+getMaxItems()+"], stopping loop");
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
			} finally {
				if (!isParallel() && childThreadSemaphore!=null) {
					// only release the semaphore for non-parallel. For parallel, it is done in the 'finally' of ParallelSenderExecutor.run()
					childThreadSemaphore.release();
				}
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
		
		public void waitForResults() throws SenderException, IOException {
			if (isParallel()) {
				try {
					guard.waitForAllResources();
					int count = 0;
					for (ParallelSenderExecutor pse : executorList) {
						count++;
						String itemResult;
						if (pse.getThrowable() == null) {
							itemResult = pse.getReply().asString();
						} else {
							itemResult = "<exception>"+XmlUtils.encodeChars(pse.getThrowable().getMessage())+"</exception>";
						}
						addResult(count, pse.getRequest(), itemResult);
					}
				} catch (InterruptedException e) {
					throw new SenderException(getLogPrefix(session)+"was interupted",e);
				}
			}
		}
		
		public int getCount() {
			return totalItems;
		}
	}
	
	@Override
	public MessageOutputStream provideOutputStream(IPipeLineSession session) throws StreamingException {
		return null; // ancestor MessageSendingPipe forwards provideOutputStream to sender, which is not correct for IteratingPipe
	}

	@Override
	public boolean canStreamToNextPipe() {
		return !isCollectResults() && super.canStreamToNextPipe(); // when collectResults is false, streaming is not necessary or useful
	}

	@Override
	protected PipeRunResult sendMessage(Message input, IPipeLineSession session, ISender sender, Map<String,Object> threadContext) throws SenderException, TimeOutException, IOException {
		// sendResult has a messageID for async senders, the result for sync senders
		try (MessageOutputStream target=getTargetStream(session)) { 
			try (Writer resultWriter = target.asWriter()) {
				ItemCallback callback = createItemCallBack(session,sender, resultWriter);
				iterateOverInput(input,session,threadContext, callback);
			}
			return target.getPipeRunResult();
		} catch (SenderException | TimeOutException | IOException e) {
			throw e;
		} catch (Exception e) {
			throw new SenderException(getLogPrefix(session)+"Exception on transforming input", e);
		}
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
	+ "Previous versions documented that <code>position()=2</code> could be used. This is not working as expected; Use maxItems instead", ""})
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

	@IbisDoc({"14", "maximum number of child threads that may run in parallel simultaneously (combined total of all threads calling this pipe)", "0 (unlimited)"})
	public void setMaxChildThreads(int maxChildThreads) {
		this.maxChildThreads = maxChildThreads;
	}
	public int getMaxChildThreads() {
		return maxChildThreads;
	}


	@IbisDoc({"15", "Controls multiline behaviour. when set to a value greater than 0, it specifies the number of rows send in a block to the sender.", "0 (one line at a time, no prefix of suffix)"})
	public void setBlockSize(int i) {
		blockSize = i;
	}
	public int getBlockSize() {
		return blockSize;
	}

}
