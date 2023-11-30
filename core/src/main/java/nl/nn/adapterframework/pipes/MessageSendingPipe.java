/*
   Copyright 2013, 2015-2019 Nationale-Nederlanden, 2020-2023 WeAreFrank!

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
import java.io.InputStream;
import java.net.URL;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.xml.transform.TransformerException;

import org.apache.commons.codec.binary.Base64InputStream;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.CloseableThreadContext;
import org.apache.logging.log4j.Logger;
import org.xml.sax.SAXException;

import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationUtils;
import nl.nn.adapterframework.configuration.ConfigurationWarning;
import nl.nn.adapterframework.configuration.ConfigurationWarnings;
import nl.nn.adapterframework.configuration.SuppressKeys;
import nl.nn.adapterframework.core.Adapter;
import nl.nn.adapterframework.core.HasPhysicalDestination;
import nl.nn.adapterframework.core.HasSender;
import nl.nn.adapterframework.core.ICorrelatedPullingListener;
import nl.nn.adapterframework.core.IDualModeValidator;
import nl.nn.adapterframework.core.IMessageBrowser;
import nl.nn.adapterframework.core.IMessageBrowser.HideMethod;
import nl.nn.adapterframework.core.IPipe;
import nl.nn.adapterframework.core.ISender;
import nl.nn.adapterframework.core.ISenderWithParameters;
import nl.nn.adapterframework.core.ITransactionalStorage;
import nl.nn.adapterframework.core.IValidator;
import nl.nn.adapterframework.core.IWrapperPipe;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.core.PipeStartException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.SenderResult;
import nl.nn.adapterframework.core.TimeoutException;
import nl.nn.adapterframework.errormessageformatters.ErrorMessageFormatter;
import nl.nn.adapterframework.extensions.esb.EsbSoapWrapperPipe;
import nl.nn.adapterframework.http.RestListenerUtils;
import nl.nn.adapterframework.jdbc.DirectQuerySender;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.ParameterList;
import nl.nn.adapterframework.processors.ListenerProcessor;
import nl.nn.adapterframework.processors.PipeProcessor;
import nl.nn.adapterframework.receivers.MessageWrapper;
import nl.nn.adapterframework.statistics.HasStatistics;
import nl.nn.adapterframework.statistics.StatisticsKeeper;
import nl.nn.adapterframework.statistics.StatisticsKeeperIterationHandler;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.ClassLoaderUtils;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.Misc;
import nl.nn.adapterframework.util.StreamUtil;
import nl.nn.adapterframework.util.TransformerPool;
import nl.nn.adapterframework.util.TransformerPool.OutputType;
import nl.nn.adapterframework.util.XmlUtils;

/**
 * Sends a message using a {@link ISender sender} and optionally receives a reply from the same sender, or
 * from a {@link ICorrelatedPullingListener listener}.
 *
 * @ff.parameters any parameters defined on the pipe will be handed to the sender, if this is a {@link ISenderWithParameters ISenderWithParameters}
 * @ff.parameter  stubFilename will <u>not</u> be handed to the sender
 * and it is used at runtime instead of the stubFilename specified by the attribute. A lookup of the
 * file for this stubFilename will be done at runtime, while the file for the stubFilename specified
 * as an attribute will be done at configuration time.

 * @ff.forward timeout
 * @ff.forward illegalResult
 * @ff.forward presumedTimeout
 * @ff.forward interrupt
 * @ff.forward "&lt;defined-by-sender&gt;" any forward, as returned by name by {@link ISender sender}
 *
 * @author  Gerrit van Brakel
 */
public class MessageSendingPipe extends FixedForwardPipe implements HasSender, HasStatistics {
	protected Logger msgLog = LogUtil.getLogger(LogUtil.MESSAGE_LOGGER);

	public static final String PIPE_TIMEOUT_MONITOR_EVENT = "Sender Timeout";
	public static final String PIPE_CLEAR_TIMEOUT_MONITOR_EVENT = "Sender Received Result on Time";
	public static final String PIPE_EXCEPTION_MONITOR_EVENT = "Sender Exception Caught";

	private static final String TIMEOUT_FORWARD = "timeout";
	private static final String ILLEGAL_RESULT_FORWARD = "illegalResult";
	private static final String PRESUMED_TIMEOUT_FORWARD = "presumedTimeout";
	private static final String INTERRUPT_FORWARD = "interrupt";

	private static final String STUBFILENAME = "stubFilename";

	public static final int MIN_RETRY_INTERVAL=1;
	public static final int MAX_RETRY_INTERVAL=600;

	private @Getter LinkMethod linkMethod = LinkMethod.CORRELATIONID;

	private @Getter String correlationIDStyleSheet;
	private @Getter String correlationIDXPath;
	private @Getter String correlationIDNamespaceDefs;
	private @Getter String correlationIDSessionKey = null;
	private @Getter String labelStyleSheet;
	private @Getter String labelXPath;
	private @Getter String labelNamespaceDefs;
	private @Getter String auditTrailSessionKey = null;
	private @Getter String auditTrailXPath;
	private @Getter String auditTrailNamespaceDefs;
	private @Getter boolean useInputForExtract = true;
	private @Getter HideMethod hideMethod = HideMethod.ALL;

	private @Getter boolean checkXmlWellFormed = false;
	private @Getter String checkRootTag;

	private @Getter String resultOnTimeOut;
	private @Getter int maxRetries=0;
	private @Getter int retryMinInterval=1;
	private @Getter int retryMaxInterval=1;
	private @Getter String retryXPath;
	private @Getter String retryNamespaceDefs;
	private @Getter int presumedTimeOutInterval=0;

	private @Getter boolean streamResultToServlet=false;

	private @Getter String stubFilename;
	private @Getter String timeOutOnResult;
	private @Getter String exceptionOnResult;

	private @Getter ISender sender = null;
	private @Getter ICorrelatedPullingListener listener = null;
	private @Getter ITransactionalStorage messageLog=null;

	private String returnString; // contains contents of stubUrl
	private TransformerPool auditTrailTp=null;
	private TransformerPool correlationIDTp=null;
	private TransformerPool labelTp=null;
	private TransformerPool retryTp=null;

	public static final String INPUT_VALIDATOR_NAME_PREFIX="- ";
	public static final String INPUT_VALIDATOR_NAME_SUFFIX=": validate input";
	public static final String OUTPUT_VALIDATOR_NAME_PREFIX="- ";
	public static final String OUTPUT_VALIDATOR_NAME_SUFFIX=": validate output";
	public static final String INPUT_WRAPPER_NAME_PREFIX="- ";
	public static final String INPUT_WRAPPER_NAME_SUFFIX=": wrap input";
	public static final String OUTPUT_WRAPPER_NAME_PREFIX="- ";
	public static final String OUTPUT_WRAPPER_NAME_SUFFIX=": wrap output";
	public static final String MESSAGE_LOG_NAME_PREFIX="- ";
	public static final String MESSAGE_LOG_NAME_SUFFIX=": message log";

	private @Getter IValidator inputValidator=null;
	private @Getter IValidator outputValidator=null;
	private @Getter IWrapperPipe inputWrapper=null;
	private @Getter IWrapperPipe outputWrapper=null;

	private boolean timeoutPending=false;

	private boolean isConfigurationStubbed = ConfigurationUtils.isConfigurationStubbed(getConfigurationClassLoader());
	private boolean msgLogHumanReadable = AppConstants.getInstance(getConfigurationClassLoader()).getBoolean("msg.log.humanReadable", false);

	private @Setter PipeProcessor pipeProcessor;
	private @Setter ListenerProcessor listenerProcessor;

	public enum LinkMethod {
		MESSAGEID, CORRELATIONID
	}

	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		msgLog = LogUtil.getMsgLogger(getAdapter(), this);
		if (StringUtils.isNotEmpty(getStubFilename())) {
			URL stubUrl;
			try {
				stubUrl = ClassLoaderUtils.getResourceURL(this, getStubFilename());
			} catch (Throwable e) {
				throw new ConfigurationException("got exception finding resource for stubfile ["+getStubFilename()+"]", e);
			}
			if (stubUrl==null) {
				throw new ConfigurationException("could not find resource for stubfile ["+getStubFilename()+"]");
			}
			try {
				returnString = StreamUtil.resourceToString(stubUrl, Misc.LINE_SEPARATOR);
			} catch (Throwable e) {
				throw new ConfigurationException("got exception loading stubfile ["+getStubFilename()+"] from resource ["+stubUrl.toExternalForm()+"]", e);
			}
		} else {
			propagateName();
			if (getSender() == null) {
				throw new ConfigurationException("no sender defined ");
			}
			// copying of pipe parameters to sender must be done at configure(), not by overriding addParam()
			// because sender might not have been set when addPipe() is called.
			if (getParameterList()!=null && getSender() instanceof ISenderWithParameters) {
				for (Parameter p:getParameterList()) {
					if (!p.getName().equals(STUBFILENAME)) {
						((ISenderWithParameters)getSender()).addParameter(p);
					}
				}
			}

			try {
				//In order to be able to suppress 'xxxSender may cause potential SQL injections!' config warnings
				if(sender instanceof DirectQuerySender) {
					((DirectQuerySender) getSender()).configure(getAdapter());
				} else {
					getSender().configure();
				}
			} catch (ConfigurationException e) {
				throw new ConfigurationException("while configuring sender",e);
			}
			if (getSender() instanceof HasPhysicalDestination) {
				log.debug("has sender on {}", ((HasPhysicalDestination)sender)::getPhysicalDestinationName);
			}
			if (getListener() != null) {
				if (getSender().isSynchronous()) {
					throw new ConfigurationException("cannot have listener with synchronous sender");
				}
				try {
					getListener().configure();
				} catch (ConfigurationException e) {
					throw new ConfigurationException("while configuring listener",e);
				}
				if (getListener() instanceof HasPhysicalDestination) {
					log.debug("has listener on {}", ((HasPhysicalDestination)getListener()).getPhysicalDestinationName());
				}
			}

			if (isCheckXmlWellFormed() || StringUtils.isNotEmpty(getCheckRootTag())) {
				if (findForward(ILLEGAL_RESULT_FORWARD) == null)
					throw new ConfigurationException("has no forward with name [illegalResult]");
			}
			if (!ConfigurationUtils.isConfigurationStubbed(getConfigurationClassLoader())) {
				if (StringUtils.isNotEmpty(getTimeOutOnResult())) {
					throw new ConfigurationException("timeOutOnResult only allowed in stub mode");
				}
				if (StringUtils.isNotEmpty(getExceptionOnResult())) {
					throw new ConfigurationException("exceptionOnResult only allowed in stub mode");
				}
			}
			if (getMaxRetries()>0) {
				if (getRetryMinInterval() < MIN_RETRY_INTERVAL) {
					ConfigurationWarnings.add(this, log, "retryMinInterval ["+getRetryMinInterval()+"] should be greater than or equal to ["+MIN_RETRY_INTERVAL+"], assuming the lower limit");
					setRetryMinInterval(MIN_RETRY_INTERVAL);
				}
				if (getRetryMaxInterval() > MAX_RETRY_INTERVAL) {
					ConfigurationWarnings.add(this, log, "retryMaxInterval ["+getRetryMaxInterval()+"] should be less than or equal to ["+MAX_RETRY_INTERVAL+"], assuming the upper limit");
					setRetryMaxInterval(MAX_RETRY_INTERVAL);
				}
				if (getRetryMaxInterval() < getRetryMinInterval()) {
					ConfigurationWarnings.add(this, log, "retryMaxInterval ["+getRetryMaxInterval()+"] should be greater than or equal to ["+getRetryMinInterval()+"], assuming the lower limit");
					setRetryMaxInterval(getRetryMinInterval());
				}
			}
		}
		ITransactionalStorage messageLog = getMessageLog();
		if (messageLog==null) {
			if (StringUtils.isEmpty(getStubFilename()) && !getSender().isSynchronous() && getListener()==null
					&& !(getSender() instanceof nl.nn.adapterframework.senders.IbisLocalSender)
					&& !(getSender() instanceof nl.nn.adapterframework.jdbc.MessageStoreSender)) { // sender is asynchronous and not a local sender or messageStoreSender, but has no messageLog
				boolean suppressIntegrityCheckWarning = ConfigurationWarnings.isSuppressed(SuppressKeys.INTEGRITY_CHECK_SUPPRESS_KEY, getAdapter());
				if (!suppressIntegrityCheckWarning) {
					boolean legacyCheckMessageLog = AppConstants.getInstance(getConfigurationClassLoader()).getBoolean("messageLog.check", true);
					if (!legacyCheckMessageLog) {
						ConfigurationWarnings.add(this, log, "Suppressing integrityCheck warnings by setting property 'messageLog.check=false' has been replaced by by setting property 'warnings.suppress.integrityCheck=true'");
						suppressIntegrityCheckWarning=true;
					}
				}
				if (!suppressIntegrityCheckWarning) {
					ConfigurationWarnings.add(this, log, "asynchronous sender [" + getSender().getName() + "] without sibling listener has no messageLog. " +
						"Service Managers will not be able to perform an integrity check (matching messages received by the adapter to messages sent by this pipe). " +
						"This warning can be suppressed globally by setting property 'warnings.suppress.integrityCheck=true', "+
						"or for this adapter only by setting property 'warnings.suppress.integrityCheck."+getAdapter().getName()+"=true'");
				}
			}
		} else {
			if (StringUtils.isNotEmpty(getHideRegex()) && StringUtils.isEmpty(messageLog.getHideRegex())) {
				messageLog.setHideRegex(getHideRegex());
				messageLog.setHideMethod(getHideMethod());
			}
			messageLog.configure();
			if (messageLog instanceof HasPhysicalDestination) {
				String msg = "has messageLog in "+((HasPhysicalDestination)messageLog).getPhysicalDestinationName();
				log.debug(msg);
				if (getAdapter() != null)
					getAdapter().getMessageKeeper().add(msg);
			}
			if (StringUtils.isNotEmpty(getAuditTrailXPath())) {
				auditTrailTp = TransformerPool.configureTransformer(this, getAuditTrailNamespaceDefs(), getAuditTrailXPath(), null, OutputType.TEXT,false,null);
			}
			if (StringUtils.isNotEmpty(getCorrelationIDXPath()) || StringUtils.isNotEmpty(getCorrelationIDStyleSheet())) {
				correlationIDTp=TransformerPool.configureTransformer(this, getCorrelationIDNamespaceDefs(), getCorrelationIDXPath(), getCorrelationIDStyleSheet(), OutputType.TEXT,false,null);
			}
			if (StringUtils.isNotEmpty(getLabelXPath()) || StringUtils.isNotEmpty(getLabelStyleSheet())) {
				labelTp=TransformerPool.configureTransformer(this, getLabelNamespaceDefs(), getLabelXPath(), getLabelStyleSheet(), OutputType.TEXT,false,null);
			}
		}
		if (StringUtils.isNotEmpty(getRetryXPath())) {
			retryTp = TransformerPool.configureTransformer(this, getRetryNamespaceDefs(), getRetryXPath(), null, OutputType.TEXT,false,null);
		}

		IValidator inputValidator = getInputValidator();
		IValidator outputValidator = getOutputValidator();
		if (outputValidator == null && inputValidator instanceof IDualModeValidator) {
			outputValidator = ((IDualModeValidator) inputValidator).getResponseValidator();
			setOutputValidator(outputValidator);
		}
		if (inputValidator != null) {
			configureElement(inputValidator);
		}
		if (outputValidator != null) {
			configureElement(outputValidator);
		}
		IWrapperPipe inputWrapper = getInputWrapper();
		if (inputWrapper instanceof EsbSoapWrapperPipe) {
			EsbSoapWrapperPipe eswPipe = (EsbSoapWrapperPipe) inputWrapper;
			ISender sender = getSender();
			eswPipe.retrievePhysicalDestinationFromSender(sender);
		}
		if (inputWrapper != null) {
			configureElement(inputWrapper);
		}
		IWrapperPipe outputWrapper = getOutputWrapper();
		if (outputWrapper != null) {
			configureElement(outputWrapper);
		}

		registerEvent(PIPE_TIMEOUT_MONITOR_EVENT);
		registerEvent(PIPE_CLEAR_TIMEOUT_MONITOR_EVENT);
		registerEvent(PIPE_EXCEPTION_MONITOR_EVENT);
	}

	private void configureElement(@Nonnull final IPipe pipe) throws ConfigurationException {
		PipeForward pf = new PipeForward();
		pf.setName(PipeForward.SUCCESS_FORWARD_NAME);
		pipe.registerForward(pf);
		configure(pipe);
	}

	// configure wrappers/validators
	private void configure(IPipe pipe) throws ConfigurationException {
		if(getPipeLine() == null) {
			throw new ConfigurationException("unable to configure "+ ClassUtils.nameOf(pipe));
		}

		getPipeLine().configure(pipe);
	}

	protected void propagateName() {
		ISender sender=getSender();
		if (sender!=null && StringUtils.isEmpty(sender.getName())) {
			sender.setName(getName() + "-sender");
		}
		ICorrelatedPullingListener listener=getListener();
		if (listener!=null && StringUtils.isEmpty(listener.getName())) {
			listener.setName(getName() + "-replylistener");
		}
	}

	@Override
	public void setName(String name) {
		super.setName(name);
		propagateName();
	}

	/**
	 * Call {@link Message#preserve()} so it can be consumed multiple times, and wrap potential {@link IOException}
	 * in a {@link PipeRunException}.
	 *
	 * @param input The {@link Message} to be preserved.
	 * @throws PipeRunException If an {@link IOException} is thrown from {@link Message#preserve()}, wrap and rethrow it
	 * in a {@link PipeRunException}.
	 *
	 */
	protected void preserve(@Nonnull Message input) throws PipeRunException {
		try {
			input.preserve();
		} catch (IOException e) {
			throw new PipeRunException(this,"cannot preserve message",e);
		}
	}

	@Override
	public PipeRunResult doPipe(@Nonnull Message input, @Nonnull PipeLineSession session) throws PipeRunException {
		Message originalMessage = null;
		PipeForward forward = getSuccessForward();

		if (messageLog != null) {
			preserve(input);
			originalMessage = input;
		}
		PipeRunResult preProcessingResult = preProcessInput(input, session);
		if (!preProcessingResult.isSuccessful()) {
			return preProcessingResult;
		}
		input = preProcessingResult.getResult();

		Message result;
		if (StringUtils.isNotEmpty(getStubFilename())) {
			result = getStubbedResult(input, session);
		} else {
			PipeRunResult sendResult;
			try {
				sendResult = sendMessageWithRetries(input, originalMessage, session);
			} catch (TimeoutException toe) {
				throwEvent(PIPE_TIMEOUT_MONITOR_EVENT);
				if (!timeoutPending) {
					timeoutPending = true;
				}
				PipeForward timeoutForward = findForward(TIMEOUT_FORWARD);
				log.warn("timeout occurred");
				if (timeoutForward == null) {
					if (StringUtils.isEmpty(getResultOnTimeOut())) {
						timeoutForward = findForward(PipeForward.EXCEPTION_FORWARD_NAME);
					} else {
						timeoutForward = getSuccessForward();
					}
				}
				if (timeoutForward != null) {
					Message resultMessage;
					if (StringUtils.isNotEmpty(getResultOnTimeOut())) {
						resultMessage =new Message(getResultOnTimeOut());
					} else {
						resultMessage = new ErrorMessageFormatter().format(null,toe,this,input,session.getMessageId(),0);
					}
					return new PipeRunResult(timeoutForward,resultMessage);
				}
				throw new PipeRunException(this, "caught timeout-exception", toe);

			} catch (Throwable t) {
				throwEvent(PIPE_EXCEPTION_MONITOR_EVENT);
				PipeForward exceptionForward = findForward(PipeForward.EXCEPTION_FORWARD_NAME);
				if (exceptionForward != null) {
					log.warn("exception occured, forwarding to exception-forward ["+exceptionForward.getPath()+"], exception:\n", t);
					return new PipeRunResult(exceptionForward, new ErrorMessageFormatter().format(null,t,this,input,session.getMessageId(),0));
				}
				throw new PipeRunException(this, "caught exception", t);
			}
			result = sendResult.getResult();
			if (sendResult.getPipeForward() != null) {
				forward = sendResult.getPipeForward();
			}
		}

		try {
			if (!validResult(result)) {
				PipeForward illegalResultForward = findForward(ILLEGAL_RESULT_FORWARD);
				return new PipeRunResult(illegalResultForward, result);
			}
		} catch (IOException e) {
			throw new PipeRunException(this, "caught exception", e);
		}
		PipeRunResult postProcessingResult = postProcessOutput(result, session);
		if (!postProcessingResult.isSuccessful()) {
			return postProcessingResult;
		}
		result = postProcessingResult.getResult();

		if (isStreamResultToServlet()) {
			try (Message mia = result;
				 InputStream resultStream=new Base64InputStream(mia.asInputStream(),false);) {

				String contentType = session.getString("contentType");
				if (StringUtils.isNotEmpty(contentType)) {
					RestListenerUtils.setResponseContentType(session, contentType);
				}
				RestListenerUtils.writeToResponseOutputStream(session, resultStream);
			} catch (IOException e) {
				throw new PipeRunException(this, "caught exception", e);
			}
			return new PipeRunResult(forward, "");
		}
		return new PipeRunResult(forward, result);
	}

	private PipeRunResult sendMessageWithRetries(Message input, Message originalMessage, PipeLineSession session) throws IOException, InterruptedException, TransformerException, SAXException, TimeoutException, SenderException, PipeRunException, ListenerException {
		Map<String,Object> threadContext = new LinkedHashMap<>();
		String correlationID = session.getCorrelationId();
		String messageID = null;
		// sendResult has a messageID for async senders, the result for sync senders
		int retryInterval = getRetryMinInterval();
		PipeRunResult sendResult = null;
		boolean replyIsValid = false;
		int retriesLeft;
		if (getMaxRetries() > 0) {
			retriesLeft = getMaxRetries() + 1;
		} else {
			retriesLeft = 1;
		}
		while (retriesLeft-- >= 1 && !replyIsValid) {
			try {
				sendResult = sendMessage(input, session, getSender(), threadContext);
				if (retryTp != null) {
					String retry = retryTp.transform(sendResult.getResult().asString(),null);
					if (retry.equalsIgnoreCase("true")) {
						if (retriesLeft >= 1) {
							retryInterval = increaseRetryIntervalAndWait(session, retryInterval, "xpathRetry result ["+retry+"], retries left [" + retriesLeft + "]");
						}
					} else {
						replyIsValid = true;
					}
				} else {
					replyIsValid = true;
				}
			} catch (TimeoutException toe) {
				if (retriesLeft >= 1) {
					retryInterval = increaseRetryIntervalAndWait(session, retryInterval, "timeout occurred, retries left [" + retriesLeft + "]");
				} else {
					throw toe;
				}
			} catch (SenderException se) {
				if (retriesLeft >= 1) {
					retryInterval = increaseRetryIntervalAndWait(session, retryInterval, "exception ["+se.getMessage()+"] occurred, retries left [" + retriesLeft + "]");
				} else {
					throw se;
				}
			}
		}

		if (!replyIsValid){
			throw new PipeRunException(this, "invalid reply message is received");
		}

		if (sendResult == null){
			throw new PipeRunException(this, "retrieved null result from sender");
		}

		if (getSender().isSynchronous()) {
			if (log.isInfoEnabled()) {
				log.info("sent message to [{}] synchronously", getSender().getName());
			}
		} else {
			messageID = sendResult.getResult().asString();
			if (log.isInfoEnabled()) {
				log.info("sent message to [{}] messageID [{}] linkMethod [{}]", getSender().getName(), messageID, getLinkMethod());
			}
			// if linkMethod is MESSAGEID overwrite correlationID with the messageID
			// as this will be used with the listener
			if (getLinkMethod() == LinkMethod.MESSAGEID) {
				correlationID = sendResult.getResult().asString();
				log.debug("setting correlationId to listen for to messageId [{}]", correlationID);
			}
		}

		correlationID = logToMessageLog(input, session, originalMessage, messageID, correlationID);

		if (getListener() != null) {
			Message result = Message.asMessage(listenerProcessor.getMessage(getListener(), correlationID, session));
			sendResult.setResult(result);
		}
		if (Message.isNull(sendResult.getResult())) {
			sendResult.setResult(new Message(""));
		}
		if (timeoutPending) {
			timeoutPending = false;
			throwEvent(PIPE_CLEAR_TIMEOUT_MONITOR_EVENT);
		}

		return sendResult;
	}

	private String logToMessageLog(final Message input, final PipeLineSession session, final Message originalMessage, final String messageID, String correlationID) throws TransformerException, IOException, SAXException, SenderException {
		ITransactionalStorage messageLog = getMessageLog();
		if (messageLog == null) {
			return correlationID;
		}
		long messageLogStartTime= System.currentTimeMillis();
		String messageTrail="no audit trail";
		if (auditTrailTp!=null) {
			if (isUseInputForExtract()){
				messageTrail=auditTrailTp.transform(originalMessage,null);
			} else {
				messageTrail=auditTrailTp.transform(input,null);
			}
		} else {
			if (StringUtils.isNotEmpty(getAuditTrailSessionKey())) {
				messageTrail = session.getString(getAuditTrailSessionKey());
			}
		}
		String storedMessageID= messageID;
		if (storedMessageID==null) {
			storedMessageID="-";
		}
		if (correlationIDTp!=null) {
			if (StringUtils.isNotEmpty(getCorrelationIDSessionKey())) {
				String sourceString = session.getString(getCorrelationIDSessionKey());
				correlationID =correlationIDTp.transform(sourceString,null);
			} else {
				if (isUseInputForExtract()) {
					correlationID =correlationIDTp.transform(originalMessage,null);
				} else {
					correlationID =correlationIDTp.transform(input,null);
				}
			}
			if (StringUtils.isEmpty(correlationID)) {
				correlationID ="-";
			}
		}
		String label=null;
		if (labelTp!=null) {
			if (isUseInputForExtract()) {
				label=labelTp.transform(originalMessage,null);
			} else {
				label=labelTp.transform(input,null);
			}
		}
		messageLog.storeMessage(storedMessageID, correlationID,new Date(),messageTrail,label, new MessageWrapper(input, storedMessageID, correlationID));

		long messageLogEndTime = System.currentTimeMillis();
		long messageLogDuration = messageLogEndTime - messageLogStartTime;
		StatisticsKeeper sk = getPipeLine().getPipeStatistics(messageLog);
		sk.addValue(messageLogDuration);
		return correlationID;
	}

	private Message getStubbedResult(final Message input, final PipeLineSession session) throws PipeRunException {
		return getStubFileName(input, session)
				.map(stubFileName -> {
					Message result = loadMessageFromClasspathResource(stubFileName);
					log.info("returning result from dynamic stub [{}]", stubFileName);
					return result;
				})
				.orElseGet(() -> {
					log.info("returning result from static stub [{}]", getStubFilename());
					return new Message(returnString);
				});
	}

	@SneakyThrows
	private Message loadMessageFromClasspathResource(final String stubFileName) {
		Message result;
		try {
			result = new Message(StreamUtil.resourceToString(ClassLoaderUtils.getResourceURL(this, stubFileName), Misc.LINE_SEPARATOR));
		} catch (Throwable e) {
			throw new PipeRunException(this, "got exception loading result from stub [" + stubFileName + "]", e);
		}
		return result;
	}

	private Optional<String> getStubFileName(final Message input, final PipeLineSession session) throws PipeRunException {
		ParameterList pl = getParameterList();
		if (pl == null) {
			return Optional.empty();
		} else {
			Map<String, Object> params;
			try {
				params = pl.getValues(input, session).getValueMap();
			} catch (ParameterException e1) {
				throw new PipeRunException(this, "got exception evaluating parameters", e1);
			}
			return !params.isEmpty() ? Optional.ofNullable((String) params.get(STUBFILENAME)) : Optional.empty();
		}
	}

	private PipeRunResult preProcessInput(Message input, PipeLineSession session) throws PipeRunException {
		if (inputWrapper != null) {
			log.debug("wrapping input");
			PipeRunResult wrapResult = pipeProcessor.processPipe(getPipeLine(), inputWrapper, input, session);
			if (wrapResult == null) {
				throw new PipeRunException(inputWrapper, "retrieved null result from inputWrapper");
			}
			if (!wrapResult.isSuccessful()) {
				return wrapResult;
			}
			input = wrapResult.getResult();
			if (messageLog != null) {
				preserve(input);
			}
			log.debug("input after wrapping [{}]", input);
		}

		if (inputValidator != null) {
			preserve(input);
			log.debug("validating input");
			PipeRunResult validationResult = pipeProcessor.processPipe(getPipeLine(), inputValidator, input, session);
			if (validationResult == null) {
				throw new PipeRunException(inputValidator, "retrieved null result from inputValidator");
			}
			if (!validationResult.isSuccessful()) {
				return validationResult;
			}
			input = validationResult.getResult();
		}
		return new PipeRunResult(new PipeForward(PipeForward.SUCCESS_FORWARD_NAME, "dummy"), input);
	}

	private PipeRunResult postProcessOutput(Message output, PipeLineSession session) throws PipeRunException {
		if (outputValidator != null) {
			log.debug("validating response");
			PipeRunResult validationResult;
			validationResult = pipeProcessor.processPipe(getPipeLine(), outputValidator, Message.asMessage(output), session);
			if (validationResult!=null) {
				if (!validationResult.isSuccessful()) {
					return validationResult;
				}
				output = validationResult.getResult();
			}
			log.debug("response after validating ({}) [{}]", () -> ClassUtils.nameOf(validationResult.getResult()), validationResult::getResult);
		}

		if (outputWrapper!=null) {
			log.debug("wrapping response");
			PipeRunResult wrapResult = pipeProcessor.processPipe(getPipeLine(), outputWrapper, output, session);
			if (wrapResult == null) {
				throw new PipeRunException(outputWrapper, "retrieved null result from outputWrapper");
			}
			if (!wrapResult.isSuccessful()) {
				return wrapResult;
			}
			output = wrapResult.getResult();
			log.debug("response after wrapping ({}) [{}]", () -> ClassUtils.nameOf(wrapResult.getResult()), wrapResult::getResult);
		}
		return new PipeRunResult(new PipeForward(PipeForward.SUCCESS_FORWARD_NAME, "dummy"), output);
	}

	private boolean validResult(Object result) throws IOException {
		boolean validResult = true;
		if (isCheckXmlWellFormed() || StringUtils.isNotEmpty(getCheckRootTag())) {
			if (!XmlUtils.isWellFormed(Message.asString(result), getCheckRootTag())) {
				validResult = false;
			}
		}
		return validResult;
	}

	protected PipeRunResult sendMessage(Message input, PipeLineSession session, ISender sender, Map<String,Object> threadContext) throws SenderException, TimeoutException, InterruptedException, IOException {
		long startTime = System.currentTimeMillis();
		PipeRunResult sendResult;
		String exitState = null;
		try {
			if (isPresumedTimeout(startTime)) {
				exitState = PRESUMED_TIMEOUT_FORWARD;
				throw new TimeoutException(PRESUMED_TIMEOUT_FORWARD);
			}
			try {
				SenderResult senderResult = sender.sendMessage(input, session);
				PipeForward forward = findForwardForResult(senderResult);
				sendResult = new PipeRunResult(forward, senderResult.getResult());
			} catch (SenderException se) {
				exitState = PipeForward.EXCEPTION_FORWARD_NAME;
				throw se;
			} catch (TimeoutException toe) {
				exitState = TIMEOUT_FORWARD;
				throw toe;
			}
			if (Thread.currentThread().isInterrupted()) {
				exitState = INTERRUPT_FORWARD;
				throw new InterruptedException();
			}
			Message sendResultMessage = sendResult.getResult();
			if (sendResultMessage.asObject() instanceof String) {
				String result = (String)sendResultMessage.asObject();
				if (StringUtils.isNotEmpty(getTimeOutOnResult()) && getTimeOutOnResult().equals(result)) {
					exitState = TIMEOUT_FORWARD;
					throw new TimeoutException("timeOutOnResult ["+getTimeOutOnResult()+"]");
				}
				if (StringUtils.isNotEmpty(getExceptionOnResult()) && getExceptionOnResult().equals(result)) {
					exitState = PipeForward.EXCEPTION_FORWARD_NAME;
					throw new SenderException("exceptionOnResult [" + getExceptionOnResult() + "]");
				}
			}
		} finally {
			if (exitState == null) {
				exitState = PipeForward.SUCCESS_FORWARD_NAME;
			}
			updatePresumedTimeoutStats(exitState);
			logSendMessageResults(sender, startTime, exitState);
		}
		return sendResult;
	}

	private void logSendMessageResults(final ISender sender, final long startTime, final String exitState) {
		String duration;
		if(msgLogHumanReadable) {
			duration = Misc.getAge(startTime);
		} else {
			duration = Misc.getDurationInMs(startTime);
		}

		if(msgLog.isDebugEnabled()) {
			try (final CloseableThreadContext.Instance ctc = CloseableThreadContext
					.put("pipe", getName())
					.put("sender.type", ClassUtils.classNameOf(sender))
					.put("duration", duration)
					.put("exit-state", exitState)
					) {
				msgLog.debug("Sender returned");
			}
		}
	}

	private PipeForward findForwardForResult(final SenderResult senderResult) {
		String forwardName = senderResult.getForwardName();
		PipeForward forward = findForward(forwardName);
		if (forward == null) {
			forwardName = senderResult.isSuccess() ? PipeForward.SUCCESS_FORWARD_NAME : PipeForward.EXCEPTION_FORWARD_NAME;
			forward = findForward(forwardName);
		}
		return forward;
	}

	private boolean isPresumedTimeout(final long startTime) {
		Adapter adapter = getAdapter();
		if (adapter == null) {
			return false;
		}
		if (getPresumedTimeOutInterval() > 0 && !isConfigurationStubbed) {
			long lastExitIsTimeoutDate = adapter.getLastExitIsTimeoutDate(getName());
			if (lastExitIsTimeoutDate>0) {
				long duration = startTime - lastExitIsTimeoutDate;
				return duration < (1000L * getPresumedTimeOutInterval());
			}
		}
		return false;
	}

	private void updatePresumedTimeoutStats(final String exitState) {
		Adapter adapter = getAdapter();
		if (adapter == null) {
			return;
		}
		if (getPresumedTimeOutInterval() > 0 && !ConfigurationUtils.isConfigurationStubbed(getConfigurationClassLoader())) {
			if (!PRESUMED_TIMEOUT_FORWARD.equals(exitState)) {
				adapter.setLastExitState(getName(), System.currentTimeMillis(), exitState);
			}
		}
	}

	public int increaseRetryIntervalAndWait(PipeLineSession session, int retryInterval, String description) throws InterruptedException {
		long currentInterval;
		synchronized (this) {
			if (retryInterval < getRetryMinInterval()) {
				retryInterval = getRetryMinInterval();
			}
			if (retryInterval > getRetryMaxInterval()) {
				retryInterval = getRetryMaxInterval();
			}
			currentInterval = retryInterval;
			retryInterval = retryInterval * 2;
		}
		log.warn(description+", starts waiting for [" + currentInterval + "] seconds");
		while (currentInterval-- > 0) {
			Thread.sleep(1000);
		}
		return retryInterval;
	}

	@Override
	public void start() throws PipeStartException {
		if (StringUtils.isEmpty(getStubFilename())) {
			try {
				getSender().open();
				if (getListener() != null) {
					getListener().open();
				}

			} catch (Throwable t) {
				PipeStartException pse = new PipeStartException("could not start", t);
				pse.setPipeNameInError(getName());
				throw pse;
			}
		}

		if (getInputValidator() != null) {
			getInputValidator().start();
		}
		if (getOutputValidator() != null) {
			getOutputValidator().start();
		}
		if (getInputWrapper() != null) {
			getInputWrapper().start();
		}
		if (getOutputWrapper() != null) {
			getOutputWrapper().start();
		}

		ITransactionalStorage<?> messageLog = getMessageLog();
		if (messageLog!=null) {
			try {
				messageLog.open();
			} catch (Exception e) {
				PipeStartException pse = new PipeStartException("could not open messagelog", e);
				pse.setPipeNameInError(getName());
				throw pse;
			}
		}
	}
	@Override
	public void stop() {
		if (StringUtils.isEmpty(getStubFilename())) {
			log.info("is closing");
			try {
				getSender().close();
			} catch (SenderException e) {
				log.warn("exception closing sender", e);
			}
			if (getListener() != null) {
				try {
					log.info("is closing; closing listener");
					getListener().close();
				} catch (ListenerException e) {
					log.warn("Exception closing listener", e);
				}
			}
		}

		if (getInputValidator() != null) {
			getInputValidator().stop();
		}
		if (getOutputValidator() != null) {
			getOutputValidator().stop();
		}
		if (getInputWrapper() != null) {
			getInputWrapper().stop();
		}
		if (getOutputWrapper() != null) {
			getOutputWrapper().stop();
		}

		ITransactionalStorage messageLog = getMessageLog();
		if (messageLog!=null) {
			try {
				messageLog.close();
			} catch (Exception e) {
				log.warn("Exception closing messageLog", e);
			}
		}
	}

	@Override
	public void iterateOverStatistics(StatisticsKeeperIterationHandler hski, Object data, Action action) throws SenderException {
		if (sender instanceof HasStatistics) {
			((HasStatistics)sender).iterateOverStatistics(hski,data,action);
		}
	}

	@Override
	public boolean hasSizeStatistics() {
		if (!super.hasSizeStatistics()) {
			return getSender().isSynchronous();
		}
		return super.hasSizeStatistics();
	}

	@Override
	public boolean consumesSessionVariable(String sessionKey) {
		return super.consumesSessionVariable(sessionKey) || getSender().consumesSessionVariable(sessionKey);
	}



	/**
	 * The sender that should send the message
	 * @ff.mandatory
	 */
	protected void setSender(ISender sender) {
		this.sender = sender;
		log.debug("pipe [{}] registered sender [{}] with properties [{}]", this::getName, sender::getName, sender::toString);
	}

	/** Listener for responses on the request sent */
	protected void setListener(ICorrelatedPullingListener listener) {
		this.listener = listener;
		log.debug("pipe [{}] registered listener [{}]", this::getName, listener::toString);
	}

	/** log of all messages sent */
	public void setMessageLog(ITransactionalStorage messageLog) {
		this.messageLog = messageLog;
		messageLog.setName(MESSAGE_LOG_NAME_PREFIX+getName()+MESSAGE_LOG_NAME_SUFFIX);
		if (StringUtils.isEmpty(messageLog.getSlotId())) {
			messageLog.setSlotId(getName());
		}
		if (StringUtils.isEmpty(messageLog.getType())) {
			messageLog.setType(IMessageBrowser.StorageType.MESSAGELOG_PIPE.getCode());
		}
	}

	/** specification of Pipe to validate request messages, or request and response message if configured as mixed mode validator */
	public void setInputValidator(IValidator inputValidator) {
		inputValidator.setName(INPUT_VALIDATOR_NAME_PREFIX+getName()+INPUT_VALIDATOR_NAME_SUFFIX);
		this.inputValidator = inputValidator;
	}

	/** specification of Pipe to validate response messages */
	public void setOutputValidator(IValidator outputValidator) {
		if (outputValidator!=null) {
			outputValidator.setName(OUTPUT_VALIDATOR_NAME_PREFIX+getName()+OUTPUT_VALIDATOR_NAME_SUFFIX);
		}
		this.outputValidator = outputValidator;
	}

	/** specification of Pipe to wrap or unwrap request messages */
	public void setInputWrapper(IWrapperPipe inputWrapper) {
		inputWrapper.setName(INPUT_WRAPPER_NAME_PREFIX+getName()+INPUT_WRAPPER_NAME_SUFFIX);
		this.inputWrapper = inputWrapper;
	}

	/** specification of Pipe to wrap or unwrap response messages */
	public void setOutputWrapper(IWrapperPipe outputWrapper) {
		outputWrapper.setName(OUTPUT_WRAPPER_NAME_PREFIX+getName()+OUTPUT_WRAPPER_NAME_SUFFIX);
		this.outputWrapper = outputWrapper;
	}




	/*
	 * For asynchronous communication, the server side may either use the messageID or the correlationID
	 * in the correlationID field of the reply message. Use this property to set the behaviour of the reply-listener.
	 * <ul>
	 * <li>Use <code>MESSAGEID</code> to let the listener wait for a message with the messageID of the
	 * sent message in the correlation ID field</li>
	 * <li>Use <code>CORRELATIONID</code> to let the listener wait for a message with the correlationID of the
	 * sent message in the correlation ID field</li>
	 * </ul>
	 * When you use the method CORRELATIONID you have the advantage that you can trace your request
	 * as the messageID as it is known in the Adapter is used as the correlationID. In the logging you should be able
	 * to follow the message more clearly. When you use the method MESSAGEID, the messageID (unique for every
	 * message) will be expected in the correlationID field of the returned message.
	 *
	 * @param method either MESSAGEID or CORRELATIONID
	 */
	/** For asynchronous communication, the server side may either use the messageID or the correlationID
	 * in the correlationID field of the reply message. Use this property to set the behaviour of the reply-listener.
	 * @ff.default CORRELATIONID
	 */
	public void setLinkMethod(LinkMethod method) {
		linkMethod = method;
	}


	/** Stylesheet to extract correlationid from message */
	public void setCorrelationIDStyleSheet(String string) {
		correlationIDStyleSheet = string;
	}

	/** XPath expression to extract correlationid from message */
	public void setCorrelationIDXPath(String string) {
		correlationIDXPath = string;
	}

	/** Namespace defintions for correlationIDXPath. Must be in the form of a comma or space separated list of <code>prefix=namespaceUri</code>-definitions */
	public void setCorrelationIDNamespaceDefs(String correlationIDNamespaceDefs) {
		this.correlationIDNamespaceDefs = correlationIDNamespaceDefs;
	}

	/** Key of a PipelineSession-variable. If specified, the value of the PipelineSession variable is used as input for the XPathExpression or stylesheet, instead of the current input message */
	public void setCorrelationIDSessionKey(String string) {
		correlationIDSessionKey = string;
	}


	/** Stylesheet to extract label from message */
	public void setLabelStyleSheet(String string) {
		labelStyleSheet = string;
	}

	/** XPath expression to extract label from message */
	public void setLabelXPath(String string) {
		labelXPath = string;
	}

	/** Namespace defintions for labelXPath. Must be in the form of a comma or space separated list of <code>prefix=namespaceUri</code>-definitions */
	public void setLabelNamespaceDefs(String labelXNamespaceDefs) {
		this.labelNamespaceDefs = labelXNamespaceDefs;
	}


	/** XPath expression to extract audit trail from message */
	public void setAuditTrailXPath(String string) {
		auditTrailXPath = string;
	}

	/** Namespace defintions for auditTrailXPath. Must be in the form of a comma or space separated list of <code>prefix=namespaceUri</code>-definitions */
	public void setAuditTrailNamespaceDefs(String auditTrailNamespaceDefs) {
		this.auditTrailNamespaceDefs = auditTrailNamespaceDefs;
	}

	/** Key of a PipelineSession-variable. If specified, the value of the PipelineSession variable is used as audit trail (instead of the default 'no audit trail) */
	public void setAuditTrailSessionKey(String string) {
		auditTrailSessionKey = string;
	}

	/**
	 * If set <code>true</code>, the input of the Pipe is used to extract audit trail, correlationid and label (instead of the wrapped input)
	 * @ff.default true
	 */
	public void setUseInputForExtract(boolean b) {
		useInputForExtract = b;
	}

	@Override
	/** Next to common usage in {@link AbstractPipe}, also strings in the error/logstore are masked */
	public void setHideRegex(String hideRegex) {
		super.setHideRegex(hideRegex);
	}

	/**
	 * (Only used when hideRegex is not empty and only applies to error/logstore)
	 * @ff.default all
	 */
	public void setHideMethod(HideMethod hideMethod) {
		this.hideMethod = hideMethod;
	}



	/**
	 * If set <code>true</code>, the XML Well-Formedness of the result is checked
	 * @ff.default false
	 */
	public void setCheckXmlWellFormed(boolean b) {
		checkXmlWellFormed = b;
	}

	/** If set, besides the XML Well-Formedness the root element of the result is checked to be equal to the value set */
	public void setCheckRootTag(String s) {
		checkRootTag = s;
	}





	/**
	 * The message (e.g. 'receiver timed out') that is returned when the time listening for a reply message
	 * exceeds the timeout, or in other situations no reply message is received.
	 */
	public void setResultOnTimeOut(String newResultOnTimeOut) {
		resultOnTimeOut = newResultOnTimeOut;
	}

	/**
	 * The number of times a processing attempt is retried after a timeout or an exception is caught or after an incorrect reply is received (see also <code>retryXPath</code>)
	 * @ff.default 0
	 */
	public void setMaxRetries(int i) {
		maxRetries = i;
	}

	/**
	 * The starting number of seconds waited after an unsuccessful processing attempt before another processing attempt is made. Each next retry this interval is doubled with a upper limit of <code>retryMaxInterval</code>
	 * @ff.default 1
	 */
	public void setRetryMinInterval(int i) {
		retryMinInterval = i;
	}

	/**
	 * The maximum number of seconds waited after an unsuccessful processing attempt before another processing attempt is made
	 * @ff.default 600
	 */
	public void setRetryMaxInterval(int i) {
		retryMaxInterval = i;
	}

	/** XPath expression evaluated on each technical successful reply. Retry is done if condition returns true */
	public void setRetryXPath(String string) {
		retryXPath = string;
	}

	/** Namespace defintions for retryXPath. Must be in the form of a comma or space separated list of <code>prefix=namespaceUri</code>-definitions */
	public void setRetryNamespaceDefs(String retryNamespaceDefs) {
		this.retryNamespaceDefs = retryNamespaceDefs;
	}

	/**
	 * If the previous call was a timeout, the maximum time <i>in seconds</i> after this timeout to presume the current call is also a timeout.
	 * @ff.default 0
	 */
	public void setPresumedTimeOutInterval(int i) {
		presumedTimeOutInterval = i;
	}

	@Deprecated
	@ConfigurationWarning("Please use a base64pipe to decode the message and send the result to the pipeline exit")
	/**
	 * If set, the result is first base64 decoded and then streamed to the HttpServletResponse object
	 * @ff.default false
	 */
	public void setStreamResultToServlet(boolean b) {
		streamResultToServlet = b;
	}

	@Deprecated
	@ConfigurationWarning("attribute 'stubFileName' is replaced with 'stubFilename'")
	public void setStubFileName(String fileName) {
		setStubFilename(fileName);
	}

	/** If set, the pipe returns a message from a file, instead of doing the regular process */
	public void setStubFilename(String filename) {
		stubFilename = filename;
	}

	/** If not empty, a TimeoutException is thrown when the result equals this value (for testing purposes only) */
	public void setTimeOutOnResult(String string) {
		timeOutOnResult = string;
	}

	/** If not empty, a PipeRunException is thrown when the result equals this value (for testing purposes only) */
	public void setExceptionOnResult(String string) {
		exceptionOnResult = string;
	}

}
