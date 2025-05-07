/*
   Copyright 2013, 2016 Nationale-Nederlanden, 2020-2024 WeAreFrank!

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
package org.frankframework.pipes;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;

import jakarta.annotation.Nonnull;

import org.apache.commons.lang3.StringUtils;
import org.springframework.core.task.TaskExecutor;
import org.xml.sax.SAXException;

import io.micrometer.core.instrument.DistributionSummary;
import lombok.Getter;
import lombok.Setter;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.configuration.ConfigurationWarnings;
import org.frankframework.core.IBlockEnabledSender;
import org.frankframework.core.IDataIterator;
import org.frankframework.core.ISender;
import org.frankframework.core.PipeForward;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunResult;
import org.frankframework.core.SenderException;
import org.frankframework.core.SenderResult;
import org.frankframework.core.TimeoutException;
import org.frankframework.doc.EnterpriseIntegrationPattern;
import org.frankframework.doc.Forward;
import org.frankframework.parameters.ParameterValueList;
import org.frankframework.receivers.ResourceLimiter;
import org.frankframework.senders.ParallelSenderExecutor;
import org.frankframework.statistics.FrankMeterType;
import org.frankframework.stream.Message;
import org.frankframework.stream.MessageBuilder;
import org.frankframework.util.TransformerPool;
import org.frankframework.util.TransformerPool.OutputType;
import org.frankframework.util.XmlEncodingUtils;
import org.frankframework.util.XmlUtils;

/**
 * Base class to send a message to a Sender for each item returned by a configurable iterator.
* <br/>
 * The output of each of the processing of each of the elements is returned in XML as follows:
 * <pre>
 *  &lt;results count="num_of_elements"&gt;
 *    &lt;result&gt;result of processing of first item&lt;/result&gt;
 *    &lt;result&gt;result of processing of second item&lt;/result&gt;
 *       ...
 *  &lt;/results&gt;
 * </pre>
 *
 * For more configuration options, see {@link MessageSendingPipe}.
 * <br/>
 * use parameters like:
 * <pre>
 *	&lt;param name="element-name-of-current-item"  xpathExpression="name(/*)" /&gt;
 *	&lt;param name="value-of-current-item"         xpathExpression="/*" /&gt;
 * </pre>
 *
 * @author  Gerrit van Brakel
 * @since   4.7
 */
@Forward(name = "maxItemsReached", description = "the iteration stopped when the configured maximum number of items was processed")
@Forward(name = "stopConditionMet", description = "the iteration stopped when the configured condition expression became true")
@EnterpriseIntegrationPattern(EnterpriseIntegrationPattern.Type.ITERATOR)
public abstract class IteratingPipe<I> extends MessageSendingPipe {

	protected static final String MAX_ITEMS_REACHED_FORWARD = "maxItemsReached";
	protected static final String STOP_CONDITION_MET_FORWARD = "stopConditionMet";

	private @Getter String styleSheetName;
	private @Getter String xpathExpression=null;
	private @Getter String namespaceDefs = null;
	private @Getter OutputType outputType=OutputType.TEXT;
	private @Getter boolean omitXmlDeclaration=true;

	private @Getter String itemNoSessionKey=null;

	private @Getter String stopConditionXPathExpression=null;
	private @Getter int maxItems;
	private @Getter boolean ignoreExceptions=false;

	private @Getter boolean collectResults=true;
	private @Getter boolean removeXmlDeclarationInResults=false;
	private @Getter boolean addInputToResult=false;
	private @Getter boolean removeDuplicates=false;

	private @Getter boolean closeIteratorOnExit=true;
	private @Getter boolean parallel = false;
	private @Getter int maxChildThreads = 0;

	private @Getter int blockSize=0;

	private @Getter @Setter TaskExecutor taskExecutor;
	protected TransformerPool msgTransformerPool;
	private TransformerPool stopConditionTp=null;

	private final Map<String, DistributionSummary> statisticsMap = new ConcurrentHashMap<>();

	private ResourceLimiter childLimiter = null;

	protected enum StopReason {
		MAX_ITEMS_REACHED(MAX_ITEMS_REACHED_FORWARD),
		STOP_CONDITION_MET(STOP_CONDITION_MET_FORWARD);

		private final @Getter String forwardName;

		StopReason(String forwardName) {
			this.forwardName=forwardName;
		}

	}

	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		msgTransformerPool = TransformerPool.configureTransformer(this, getNamespaceDefs(), getXpathExpression(), getStyleSheetName(), getOutputType(), !isOmitXmlDeclaration(), getParameterList(), false);
		try {
			if (StringUtils.isNotEmpty(getStopConditionXPathExpression())) {
				stopConditionTp=TransformerPool.getXPathTransformerPool(null,getStopConditionXPathExpression(),OutputType.XML);
			}
		} catch (TransformerConfigurationException e) {
			throw new ConfigurationException("Cannot compile stylesheet from stopConditionXPathExpression ["+getStopConditionXPathExpression()+"]", e);
		}
		if (getMaxChildThreads() == 0 && isParallel()) {
			setMaxChildThreads(20);
			ConfigurationWarnings.add(this, log, "\"parallel\" is set to \"true\" but \"maxChildThreads\" is not set. Defaulting \"maxChildThreads\" to 20");
		}
		if (getMaxChildThreads() > 0) {
			// Create semaphore with "fair" policy, meaning first-come-first-serve. If a child thread asks for a resource but doesn't get it before the
			// parent thread tries to wait until all tasks are done, the child gets the semaphore before parent is signalled it can continue.
			childLimiter = new ResourceLimiter(getMaxChildThreads(), true);
		}
	}

	protected IDataIterator<I> getIterator(Message input, PipeLineSession session, Map<String,Object> threadContext) throws SenderException {
		return null;
	}

	protected ItemCallback createItemCallBack(PipeLineSession session, ISender sender, Writer out) {
		return new ItemCallback(session, sender, out);
	}

	protected Message itemToMessage(I item) throws SenderException {
		return Message.asMessage(item);
	}

	protected StopReason iterateOverInput(Message input, PipeLineSession session, Map<String,Object> threadContext, ItemCallback callback) throws SenderException, TimeoutException, IOException {
		IDataIterator<I> it;
		StopReason stopReason = null;
		if (StringUtils.isNotEmpty(getItemNoSessionKey())) {
			session.put(getItemNoSessionKey(),"0"); // prefill session variable, to have a value if iterator is empty
		}
		it = getIterator(input,session, threadContext);
		try {
			callback.startIterating(); // perform startIterating even when it=null, to avoid empty result
			if (it!=null) {
				try {
					boolean keepGoing = true;
					while (keepGoing && (it.hasNext())) {
						if (Thread.currentThread().isInterrupted()) {
							throw new TimeoutException("Thread has been interrupted");
						}
						stopReason = callback.handleItem(getItem(it));
						keepGoing = stopReason == null;
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
		return stopReason;
	}

	protected class ItemCallback {
		private final PipeLineSession session;
		private final ISender sender;
		private final Writer results;
		private int itemsInBlock=0;
		private int totalItems=0;
		private boolean blockOpen=false;
		private Object blockHandle;
		private final List<I> inputItems = Collections.synchronizedList(new ArrayList<>());
		private List<ParallelSenderExecutor> executorList;

		public ItemCallback(PipeLineSession session, ISender sender, Writer out) {
			this.session=session;
			this.sender=sender;
			this.results=out;
			if (isParallel() && isCollectResults()) {
				executorList = new ArrayList<>();
			}
		}

		public void startIterating() throws SenderException, TimeoutException, IOException {
			if (isCollectResults()) {
				results.append("<results>\n");
			}
			if (!isParallel() && sender instanceof IBlockEnabledSender<?> enabledSender) {
				blockHandle = enabledSender.openBlock(session);
				blockOpen=true;
			}
		}
		public void endIterating() throws SenderException, IOException, TimeoutException {
			if (blockOpen && sender instanceof IBlockEnabledSender<?>) {
				((IBlockEnabledSender)sender).closeBlock(blockHandle, session);
			}
			if (isCollectResults()) {
				waitForResults();
				results.append("</results>");
			} else {
				results.append("<results count=\"").append(String.valueOf(getCount())).append("\"/>");
			}
		}
		public void startBlock() throws SenderException, TimeoutException {
			if (!isParallel() && !blockOpen && sender instanceof IBlockEnabledSender<?> enabledSender) {
				blockHandle = enabledSender.openBlock(session);
				blockOpen=true;
			}
		}
		/**
		 * @return true when looping should continue, false when stop is required.
		 */
		public boolean endBlock() throws SenderException {
			if (!isParallel() && sender instanceof IBlockEnabledSender<?>) {
				((IBlockEnabledSender)sender).closeBlock(blockHandle, session);
				blockOpen=false;
			}
			itemsInBlock=0;
			return true;
		}

		/**
		 * @return a non-null StopReason when stop is required
		 */
		public StopReason handleItem(I item) throws SenderException, TimeoutException, IOException {
			if (isRemoveDuplicates()) {
				if (inputItems.contains(item)) {
					log.debug("duplicate item [{}] will not be processed", item);
					return null;
				}
				inputItems.add(item);
			}
			String itemResult=null;
			totalItems++;

			if (StringUtils.isNotEmpty(getItemNoSessionKey())) {
				session.put(getItemNoSessionKey(), ""+totalItems);
			}

			Message message = itemToMessage(item);
			// TODO check for bug: sessionKey params not resolved when only parameters set on sender. Next line should check sender.parameterlist too.
			if (msgTransformerPool != null) {
				Message transformedMessage = transformMessage(message);
				log.debug("iteration [{}] transformed item [{}] into [{}]", totalItems, message, transformedMessage);
				message.close();
				message = transformedMessage;
			} else {
				log.debug("iteration [{}] item [{}]", totalItems, message);
			}
			message.closeOnCloseOf(session, "iteratingPipeItem"+totalItems);

			if (childLimiter != null) {
				try {
					childLimiter.acquire();
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					throw new SenderException("interrupted waiting for thread", e);
				}
			}
			try {
				try {
					DistributionSummary senderStatistics = getStatisticsKeeper(sender.getName());
					if (isParallel()) {
						ParallelSenderExecutor pse = new ParallelSenderExecutor(sender, message, session, senderStatistics);
						pse.setThreadLimiter(childLimiter);
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
							SenderResult senderResult=((IBlockEnabledSender)sender).sendMessage(blockHandle, message, session);
							if (senderResult.isSuccess()) {
								itemResult = senderResult.getResult().asString();
							} else {
								throw new SenderException(senderResult.getErrorMessage());
							}
						} else {
							itemResult = sender.sendMessageOrThrow(message, session).asString();
						}
						senderStatistics.record((double) System.currentTimeMillis() - senderStartTime);
						if (getBlockSize()>0 && ++itemsInBlock >= getBlockSize()) {
							endBlock();
						}
					}
					if (StringUtils.isNotEmpty(getTimeoutOnResult()) && getTimeoutOnResult().equals(itemResult)) {
						throw new TimeoutException("timeoutOnResult ["+ getTimeoutOnResult()+"]");
					}
					if (StringUtils.isNotEmpty(getExceptionOnResult()) && getExceptionOnResult().equals(itemResult)) {
						throw new SenderException("exceptionOnResult ["+getExceptionOnResult()+"]");
					}
				} catch (SenderException e) {
					if (isIgnoreExceptions()) {
						log.info("ignoring SenderException after execution of sender for item [{}]", item, e);
						itemResult="<exception>"+ XmlEncodingUtils.encodeChars(e.getMessage())+"</exception>";
					} else {
						throw e;
					}
				} catch (TimeoutException e) {
					if (isIgnoreExceptions()) {
						log.info("ignoring TimeOutException after execution of sender item [{}]", item, e);
						itemResult="<timeout>"+ XmlEncodingUtils.encodeChars(e.getMessage())+"</timeout>";
					} else {
						throw e;
					}
				}
				try {
					if (isCollectResults() && !isParallel()) {
						addResult(totalItems, message, itemResult);
					}
					if (getMaxItems()>0 && totalItems>=getMaxItems()) {
						log.debug("count [{}] reached maxItems [{}], stopping loop", totalItems, getMaxItems());
						return StopReason.MAX_ITEMS_REACHED;
					}
					if (getStopConditionTp()!=null) {
						long stopConditionStartTime = System.currentTimeMillis();
						String stopConditionResult = getStopConditionTp().transform(itemResult,null);
						long stopConditionEndTime = System.currentTimeMillis();
						long stopConditionDuration = stopConditionEndTime - stopConditionStartTime;
						getStatisticsKeeper("stop condition determination").record(stopConditionDuration);
						if (StringUtils.isNotEmpty(stopConditionResult) && !"false".equalsIgnoreCase(stopConditionResult)) {
							log.debug("itemResult [{}] stopcondition result [{}], stopping loop", itemResult, stopConditionResult);
							return StopReason.STOP_CONDITION_MET;
						}
						log.debug("itemResult [{}] stopcondition result [{}], continueing loop", itemResult, stopConditionResult);
					}
					return null;
				} catch (SAXException e) {
					throw new SenderException("cannot parse input",e);
				} catch (TransformerException e) {
					throw new SenderException("cannot serialize item",e);
				}
			} finally {
				if (!isParallel() && childLimiter !=null) {
					// only release the semaphore for non-parallel. For parallel, it is done in the 'finally' of ParallelSenderExecutor.run()
					childLimiter.release();
				}
			}
		}

		private Message transformMessage(Message message) throws SenderException {
			try {
				long preprocessingStartTime = System.currentTimeMillis();
				ParameterValueList parameterValueList = getParameterList() != null ? getParameterList().getValues(message, session) : null;
				Message transformedMsg = msgTransformerPool.transform(message, parameterValueList);
				long preprocessingDuration = System.currentTimeMillis() - preprocessingStartTime;
				getStatisticsKeeper("message preprocessing").record(preprocessingDuration);
				return transformedMsg;
			} catch (Exception e) {
				throw new SenderException("cannot transform item", e);
			}
		}

		private void addResult(int count, Message message, String itemResult) throws IOException {
			if (isRemoveXmlDeclarationInResults()) {
				log.debug("removing XML declaration from [{}]", itemResult);
				itemResult = XmlUtils.skipXmlDeclaration(itemResult);
			}
			log.debug("partial result [{}]", itemResult);
			String itemInput="";
			if (isAddInputToResult()) {
				itemInput = "<input>"+(isRemoveXmlDeclarationInResults()?XmlUtils.skipXmlDeclaration(message.asString()):message.asString())+"</input>";
			}
			itemResult = "<result item=\"" + count + "\">\n"+itemInput+itemResult+"\n</result>";
			results.append(itemResult).append("\n");
		}

		public void waitForResults() throws SenderException, IOException {
			if (isParallel()) {
				try {
					// ChildLimiter is an extended semaphore that is used to track created threads when running in parallel.
					// Here we wait until all child threads have finished.
					childLimiter.waitUntilAllResourcesAvailable();
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					throw new SenderException("interrupted while waiting for results", e);
				}
				collectResultsOrThrowExceptions();
			}
		}

		private void collectResultsOrThrowExceptions() throws SenderException, IOException {
			int count = 0;
			List<Throwable> exceptions = new ArrayList<>();
			for(ParallelSenderExecutor pse : executorList) {
				count++;
				final String itemResult;
				if(pse.getThrowable() == null) {
					SenderResult senderResult = pse.getReply();
					if(senderResult.isSuccess()) {
						itemResult = senderResult.getResult().asString();
					} else {
						itemResult = "<exception>" + XmlEncodingUtils.encodeChars(senderResult.getResult().asString()) + "</exception>";
						exceptions.add(new SenderException(senderResult.getResult().asString()));
					}
				} else {
					itemResult = "<exception>" + XmlEncodingUtils.encodeChars(pse.getThrowable().getMessage()) + "</exception>";
					exceptions.add(pse.getThrowable());
				}
				addResult(count, pse.getRequest(), itemResult);
			}
			if(!isIgnoreExceptions() && !exceptions.isEmpty()) {
				SenderException se = new SenderException("an error occurred during parallel execution");
				exceptions.stream().forEach(se::addSuppressed);
				throw se;
			}
		}

		public int getCount() {
			return totalItems;
		}
	}

	@Override
	protected PipeRunResult sendMessage(Message input, PipeLineSession session, ISender sender, Map<String,Object> threadContext) throws SenderException, TimeoutException, IOException {
		// sendResult has a messageID for async senders, the result for sync senders
		StopReason stopReason;
		MessageBuilder messageBuilder = new MessageBuilder();
		try {
			try (Writer resultWriter = messageBuilder.asWriter()) {
				ItemCallback callback = createItemCallBack(session, sender, resultWriter);
				stopReason = iterateOverInput(input,session,threadContext, callback);
			}

			PipeRunResult prr = new PipeRunResult(getSuccessForward(), messageBuilder.build());
			if(stopReason != null) {
				PipeForward forward = getForwards().get(stopReason.getForwardName());
				if(forward != null) {
					prr.setPipeForward(forward);
				}
			}
			return prr;
		} catch (SenderException | TimeoutException | IOException e) {
			throw e;
		} catch (Exception e) {
			throw new SenderException("Exception on transforming input", e);
		}
	}

	protected I getItem(IDataIterator<I> it) throws SenderException {
		return it.next();
	}

	protected @Nonnull DistributionSummary getStatisticsKeeper(String name) {
		return statisticsMap.computeIfAbsent(name, ignored -> configurationMetrics.createSubDistributionSummary(this, name, FrankMeterType.PIPE_DURATION));
	}

	@Override
	public void setSender(ISender sender) {
		super.setSender(sender);
	}

	protected TransformerPool getStopConditionTp() {
		return stopConditionTp;
	}

	/** Stylesheet to apply to each message, before sending it */
	public void setStyleSheetName(String styleSheetName){
		this.styleSheetName=styleSheetName;
	}

	/** Alternatively: xpath-expression to create stylesheet from */
	public void setXpathExpression(String string) {
		xpathExpression = string;
	}

	/** Namespace defintions for xpathExpression. Must be in the form of a comma or space separated list of <code>prefix=namespaceuri</code>-definitions. For some use other cases (NOT xpathExpression), one entry can be without a prefix, that will define the default namespace. */
	public void setNamespaceDefs(String namespaceDefs) {
		this.namespaceDefs = namespaceDefs;
	}

	/**
	 * Only valid for xpathexpression
	 * @ff.default text
	 */
	public void setOutputType(OutputType outputType) {
		this.outputType = outputType;
	}

	/**
	 * Force the transformer generated from the xpath-expression to omit the xml declaration
	 * @ff.default true
	 */
	public void setOmitXmlDeclaration(boolean b) {
		omitXmlDeclaration = b;
	}

	/** Key of session variable to store number of items processed, i.e. the position or index in the set of items to be processed. When handling the first item, the value will be 1. */
	public void setItemNoSessionKey(String string) {
		itemNoSessionKey = string;
	}

	/**
	 * The maximum number of items returned. The (default) value of 0 means unlimited, all available items will be returned. Special forward {@value #MAX_ITEMS_REACHED_FORWARD} can be configured to follow
	 * @ff.default 0
	 */
	public void setMaxItems(int maxItems) {
		this.maxItems = maxItems;
	}

	/**
	 * Expression evaluated on each result and forwards to [{@value #STOP_CONDITION_MET_FORWARD}] forward if configured.
	 * Iteration stops if condition returns anything other than an empty result. To test for the root element to have an attribute 'finished' with the value 'yes', the expression <code>*[@finished='yes']</code> can be used.
	 * This can be used if the condition to stop can be derived from the item result. To stop after a maximum number of items has been processed, use <code>maxItems</code>.
	 * Previous versions documented that <code>position()=2</code> could be used. This is not working as expected; Use maxItems instead
	 */
	public void setStopConditionXPathExpression(String string) {
		stopConditionXPathExpression = string;
	}

	/**
	 * When <code>true</code> ignore any exception thrown by executing sender
	 * @ff.default false
	 */
	public void setIgnoreExceptions(boolean b) {
		ignoreExceptions = b;
	}

	/**
	 * Controls whether all the results of each iteration will be collected in one result message. If set <code>false</code>, only a small summary is returned.
	 * Setting this attributes to <code>false</code> is often required to enable processing of very large files. N.B. Remember in such a case that setting transactionAttribute to NotSupported might be necessary too
	 * @ff.default true
	 */
	public void setCollectResults(boolean b) {
		collectResults = b;
	}

	/**
	 * Postprocess each partial result, to remove the xml-declaration, as this is not allowed inside an xml-document
	 * @ff.default false
	 */
	public void setRemoveXmlDeclarationInResults(boolean b) {
		removeXmlDeclarationInResults = b;
	}

	/**
	 * When <code>true</code> the input is added to the result in an input element
	 * @ff.default false
	 */
	public void setAddInputToResult(boolean b) {
		addInputToResult = b;
	}

	/**
	 * When <code>true</code> duplicate input elements are removed, i.e. they are handled only once
	 * @ff.default false
	 */
	public void setRemoveDuplicates(boolean b) {
		removeDuplicates = b;
	}

	protected void setCloseIteratorOnExit(boolean b) {
		closeIteratorOnExit = b;
	}

	/**
	 * When set <code>true</code>, the calls for all items are done in parallel (a new thread is started for each call). when collectresults set <code>true</code>, this pipe will wait for all calls to finish before results are collected and pipe result is returned
	 * @ff.default false
	 */
	public void setParallel(boolean parallel) {
		this.parallel = parallel;
	}

	/**
	 * Maximum number of child threads that may run in parallel simultaneously (combined total of all threads calling this pipe). Use <code>0</code> for unlimited threads
	 * @ff.default 0
	 */
	public void setMaxChildThreads(int maxChildThreads) {
		this.maxChildThreads = maxChildThreads;
	}

	/**
	 * Controls multiline behaviour. When set to a value greater than 0, it specifies the number of rows send, in a one block, to the sender.
	 * @ff.default 0
	 */
	public void setBlockSize(int i) {
		blockSize = i;
	}

}
