/*
   Copyright 2013, 2015-2019 Nationale-Nederlanden, 2020-2022 WeAreFrank!

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

import org.apache.commons.codec.binary.Base64InputStream;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;

import lombok.Getter;
import lombok.Setter;
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
import nl.nn.adapterframework.core.IPipe;
import nl.nn.adapterframework.core.ISender;
import nl.nn.adapterframework.core.ISenderWithParameters;
import nl.nn.adapterframework.core.ITransactionalStorage;
import nl.nn.adapterframework.core.IValidator;
import nl.nn.adapterframework.core.IWrapperPipe;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeLine;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.core.PipeStartException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeoutException;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.errormessageformatters.ErrorMessageFormatter;
import nl.nn.adapterframework.extensions.esb.EsbSoapWrapperPipe;
import nl.nn.adapterframework.http.RestListenerUtils;
import nl.nn.adapterframework.jdbc.DirectQuerySender;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.ParameterList;
import nl.nn.adapterframework.processors.ListenerProcessor;
import nl.nn.adapterframework.processors.PipeProcessor;
import nl.nn.adapterframework.statistics.HasStatistics;
import nl.nn.adapterframework.statistics.StatisticsKeeper;
import nl.nn.adapterframework.statistics.StatisticsKeeperIterationHandler;
import nl.nn.adapterframework.stream.IOutputStreamingSupport;
import nl.nn.adapterframework.stream.IStreamingSender;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.stream.MessageOutputStream;
import nl.nn.adapterframework.stream.StreamingException;
import nl.nn.adapterframework.stream.StreamingPipe;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.Misc;
import nl.nn.adapterframework.util.TransformerPool;
import nl.nn.adapterframework.util.TransformerPool.OutputType;
import nl.nn.adapterframework.util.XmlUtils;

/**
 * Sends a message using a {@link ISender sender} and optionally receives a reply from the same sender, or
 * from a {@link ICorrelatedPullingListener listener}.
 *  * 
 * @ff.parameters any parameters defined on the pipe will be handed to the sender, if this is a {@link ISenderWithParameters ISenderWithParameters}
 * @ff.parameter  stubFilename will <u>not</u> be handed to the sender 
 * and it is used at runtime instead of the stubFilename specified by the attribute. A lookup of the 
 * file for this stubFilename will be done at runtime, while the file for the stubFilename specified 
 * as an attribute will be done at configuration time.

 * @ff.forward timeout
 * @ff.forward illegalResult
 * @ff.forward presumedTimeout
 * @ff.forward interrupt
 * 
 * @author  Gerrit van Brakel
 */

public class MessageSendingPipe extends StreamingPipe implements HasSender, HasStatistics {
	protected Logger msgLog = LogUtil.getLogger("MSG");
	private Level MSGLOG_LEVEL_TERSE = Level.toLevel("TERSE");

	public static final String PIPE_TIMEOUT_MONITOR_EVENT = "Sender Timeout";
	public static final String PIPE_CLEAR_TIMEOUT_MONITOR_EVENT = "Sender Received Result on Time";
	public static final String PIPE_EXCEPTION_MONITOR_EVENT = "Sender Exception Caught";

	private final static String TIMEOUT_FORWARD = "timeout";
	private final static String ILLEGAL_RESULT_FORWARD = "illegalResult";
	private final static String PRESUMED_TIMEOUT_FORWARD = "presumedTimeout";
	private final static String INTERRUPT_FORWARD = "interrupt";
	
	private final static String STUBFILENAME = "stubFilename";

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
	private @Getter String hideMethod = "all";

	private @Getter boolean checkXmlWellFormed = false;
	private @Getter String checkRootTag;

	private @Getter String resultOnTimeOut;
	private @Getter int maxRetries=0;
	private @Getter int retryMinInterval=1;
	private @Getter int retryMaxInterval=1;
	private @Getter String retryXPath;
	private @Getter String retryNamespaceDefs;
	private @Getter int presumedTimeOutInterval=10;


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

	public final static String INPUT_VALIDATOR_NAME_PREFIX="- ";
	public final static String INPUT_VALIDATOR_NAME_SUFFIX=": validate input";
	public final static String OUTPUT_VALIDATOR_NAME_PREFIX="- ";
	public final static String OUTPUT_VALIDATOR_NAME_SUFFIX=": validate output";
	public final static String INPUT_WRAPPER_NAME_PREFIX="- ";
	public final static String INPUT_WRAPPER_NAME_SUFFIX=": wrap input";
	public final static String OUTPUT_WRAPPER_NAME_PREFIX="- ";
	public final static String OUTPUT_WRAPPER_NAME_SUFFIX=": wrap output";
	public final static String MESSAGE_LOG_NAME_PREFIX="- ";
	public final static String MESSAGE_LOG_NAME_SUFFIX=": message log";

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

	/**
	 * Checks whether a sender is defined for this pipe.
	 */
	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		msgLog = LogUtil.getMsgLogger(getAdapter(), this);
		if (StringUtils.isNotEmpty(getStubFilename())) {
			URL stubUrl;
			try {
				stubUrl = ClassUtils.getResourceURL(this, getStubFilename());
			} catch (Throwable e) {
				throw new ConfigurationException("got exception finding resource for stubfile ["+getStubFilename()+"]", e);
			}
			if (stubUrl==null) {
				throw new ConfigurationException("could not find resource for stubfile ["+getStubFilename()+"]");
			}
			try {
				returnString = Misc.resourceToString(stubUrl, Misc.LINE_SEPARATOR);
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
				log.info(getLogPrefix(null)+"has sender on "+((HasPhysicalDestination)getSender()).getPhysicalDestinationName());
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
					log.info(getLogPrefix(null)+"has listener on "+((HasPhysicalDestination)getListener()).getPhysicalDestinationName());
				}
			}

			if (!(getHideMethod().equalsIgnoreCase("all"))
					&& (!(getHideMethod().equalsIgnoreCase("firstHalf")))) {
				throw new ConfigurationException("invalid value for hideMethod [" + getHideMethod() + "], must be 'all' or 'firstHalf'");
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
			if (StringUtils.isEmpty(getStubFilename()) && !getSender().isSynchronous() && getListener()==null && !(getSender() instanceof nl.nn.adapterframework.senders.IbisLocalSender)) { // sender is asynchronous and not a local sender, but has no messageLog
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
				String msg = getLogPrefix(null)+"has messageLog in "+((HasPhysicalDestination)messageLog).getPhysicalDestinationName();
				log.info(msg);
				if (getAdapter() != null)
					getAdapter().getMessageKeeper().add(msg);
			}
			if (StringUtils.isNotEmpty(getAuditTrailXPath())) {
				auditTrailTp = TransformerPool.configureTransformer(getLogPrefix(null), this, getAuditTrailNamespaceDefs(), getAuditTrailXPath(), null,OutputType.TEXT,false,null);
			}
			if (StringUtils.isNotEmpty(getCorrelationIDXPath()) || StringUtils.isNotEmpty(getCorrelationIDStyleSheet())) {
				correlationIDTp=TransformerPool.configureTransformer(getLogPrefix(null), this, getCorrelationIDNamespaceDefs(), getCorrelationIDXPath(), getCorrelationIDStyleSheet(),OutputType.TEXT,false,null);
			}
			if (StringUtils.isNotEmpty(getLabelXPath()) || StringUtils.isNotEmpty(getLabelStyleSheet())) {
				labelTp=TransformerPool.configureTransformer(getLogPrefix(null), this, getLabelNamespaceDefs(), getLabelXPath(), getLabelStyleSheet(),OutputType.TEXT,false,null);
			}
		}
		if (StringUtils.isNotEmpty(getRetryXPath())) {
			retryTp = TransformerPool.configureTransformer(getLogPrefix(null), this, getRetryNamespaceDefs(), getRetryXPath(), null,OutputType.TEXT,false,null);
		}

		IValidator inputValidator = getInputValidator();
		IValidator outputValidator = getOutputValidator();
		if (inputValidator!=null && outputValidator==null && inputValidator instanceof IDualModeValidator) {
			outputValidator=((IDualModeValidator)inputValidator).getResponseValidator();
			setOutputValidator(outputValidator);
		}
		if (inputValidator!=null) {
			PipeForward pf = new PipeForward();
			pf.setName(PipeForward.SUCCESS_FORWARD_NAME);
			inputValidator.registerForward(pf);
			configure(inputValidator);
		}
		if (outputValidator!=null) {
			PipeForward pf = new PipeForward();
			pf.setName(PipeForward.SUCCESS_FORWARD_NAME);
			outputValidator.registerForward(pf);
			configure(outputValidator);
		}
		if (getInputWrapper()!=null) {
			PipeForward pf = new PipeForward();
			pf.setName(PipeForward.SUCCESS_FORWARD_NAME);
			getInputWrapper().registerForward(pf);
			if (getInputWrapper() instanceof EsbSoapWrapperPipe) {
				EsbSoapWrapperPipe eswPipe = (EsbSoapWrapperPipe)getInputWrapper();
				ISender sender = getSender();
				eswPipe.retrievePhysicalDestinationFromSender(sender);
			}
			configure(getInputWrapper());
		}
		if (getOutputWrapper()!=null) {
			PipeForward pf = new PipeForward();
			pf.setName(PipeForward.SUCCESS_FORWARD_NAME);
			getOutputWrapper().registerForward(pf);
			configure(getOutputWrapper());
		}

		registerEvent(PIPE_TIMEOUT_MONITOR_EVENT);
		registerEvent(PIPE_CLEAR_TIMEOUT_MONITOR_EVENT);
		registerEvent(PIPE_EXCEPTION_MONITOR_EVENT);
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

	@IbisDoc({"name of the pipe", ""})
	@Override
	public void setName(String name) {
		super.setName(name);
		propagateName();
	}

//	/**
//	 * When true, the streaming capability of the nested sender is taken into account to determine if the pipe can provide an OutputStream.
//	 * Descender classes may override this method when necessary.
//	 */
//	protected boolean senderAffectsStreamProvidingCapability() {
//		return true;
//	}
//	/**
//	 * When true, the ability of the nested sender to write to is taken into account to determine if the pipe can stream its output.
//	 * Descender classes may override this method when necessary.
//	 */
//	protected boolean senderAffectsStreamWritingCapability() {
//		return true;
//	}
//	
//	@Override
//	public protected canProvideOutputStream() {
//		return super.canProvideOutputStream() 
//				&& (!senderAffectsStreamProvidingCapability() || 
//					sender instanceof IOutputStreamingSupport && ((IOutputStreamingSupport)sender).canProvideOutputStream()
//				   )
//				&& getInputWrapper()==null
//				&& getInputValidator()==null;
//	}
//
//	@Override
//	public boolean requiresOutputStream() {
//		return super.requiresOutputStream() 
//				&& (!senderAffectsStreamWritingCapability() || 
//					sender instanceof IOutputStreamingSupport && ((IOutputStreamingSupport)sender).requiresOutputStream()
//				   )
//				&& getOutputWrapper()==null
//				&& getOutputValidator()==null
//				&& !isStreamResultToServlet();
//	}

	@Override
	public boolean supportsOutputStreamPassThrough() {
		return false; // TODO to be implemented!
	}

	@Override
	protected boolean canProvideOutputStream() {
		return super.canProvideOutputStream() && 
				getInputValidator()==null && getInputWrapper()==null && getOutputValidator()==null && getOutputWrapper()==null &&
				!isStreamResultToServlet() && StringUtils.isEmpty(getStubFilename()) && getMessageLog()==null && getListener()==null;
	}

	@Override
	protected boolean canStreamToNextPipe() {
		return super.canStreamToNextPipe() && getOutputValidator()==null && getOutputWrapper()==null && !isStreamResultToServlet();
	}

	@Override
	protected MessageOutputStream provideOutputStream(PipeLineSession session) throws StreamingException {
		MessageOutputStream result=null;
		if (sender instanceof IOutputStreamingSupport) {
			// TODO insert output validator
			// TODO insert output wrapper
			IOutputStreamingSupport streamingSender = (IOutputStreamingSupport)sender;
			result = streamingSender.provideOutputStream(session, getNextPipe());
			// TODO insert input wrapper
			// TODO insert input validator
		}
		return result;
	}

	protected void preserve(Message input, PipeLineSession session) throws PipeRunException {
		try {
			input.preserve();
		} catch (IOException e) {
			throw new PipeRunException(this,getLogPrefix(session)+"cannot preserve message",e);
		}
	}

	@Override
	public PipeRunResult doPipe(Message input, PipeLineSession session) throws PipeRunException {
		String correlationID = session==null?null:session.getMessageId();
 		Message originalMessage = null;
 		Message result = null;
		PipeForward forward = getSuccessForward();

		if (messageLog!=null) {
			preserve(input, session);
			originalMessage=input;
		}
		if (getInputWrapper()!=null) {
			log.debug(getLogPrefix(session)+"wrapping input");
			PipeRunResult wrapResult = pipeProcessor.processPipe(getPipeLine(), inputWrapper, input, session);
			if (wrapResult==null) {
				throw new PipeRunException(inputWrapper, "retrieved null result from inputWrapper");
			}
			if (!wrapResult.isSuccessful()) {
				return wrapResult;
			}
			input = wrapResult.getResult();
			if (messageLog!=null) {
				preserve(input, session);
			}
			log.debug(getLogPrefix(session)+"input after wrapping ("+ClassUtils.nameOf(input)+") [" + input.toString() + "]");
		}

		if (getInputValidator()!=null) {
			preserve(input, session);
			log.debug(getLogPrefix(session)+"validating input");
			PipeRunResult validationResult = pipeProcessor.processPipe(getPipeLine(), inputValidator, input, session);
			if (validationResult!=null && !validationResult.isSuccessful()) {
				return validationResult;
			}
		}

		if (StringUtils.isNotEmpty(getStubFilename())) {
			ParameterList pl = getParameterList();
			result=new Message(returnString);
			if (pl != null) {
				Map<String,Object> params;
				try {
					params = pl.getValues(input, session).getValueMap();
				} catch (ParameterException e1) {
					throw new PipeRunException(this,getLogPrefix(session)+"got exception evaluating parameters",e1);
				}
				String sfn = null;
				if (params != null && params.size() > 0) {
					sfn = (String)params.get(STUBFILENAME);
				}
				if (sfn != null) {
					try {
						result = new Message(Misc.resourceToString(ClassUtils.getResourceURL(this, sfn), Misc.LINE_SEPARATOR));
						log.info(getLogPrefix(session)+"returning result from dynamic stub ["+sfn+"]");
					} catch (Throwable e) {
						throw new PipeRunException(this,getLogPrefix(session)+"got exception loading result from stub [" + sfn + "]",e);
					}
				} else {
					log.info(getLogPrefix(session)+"returning result from static stub ["+getStubFilename()+"]");
				}
			} else {
				log.info(getLogPrefix(session)+"returning result from static stub ["+getStubFilename()+"]");
			}
		} else {
			Map<String,Object> threadContext=new LinkedHashMap<String,Object>();
			try {
				String messageID = null;
				// sendResult has a messageID for async senders, the result for sync senders
				int retryInterval = getRetryMinInterval();
				PipeRunResult sendResult = null;
				boolean replyIsValid = false;
				int retriesLeft = 0;
				if (getMaxRetries()>0) {
					retriesLeft = getMaxRetries() + 1;
				} else {
					retriesLeft = 1;
				}
				while (retriesLeft-->=1 && !replyIsValid) {
					try {
						sendResult = sendMessage(input, session, getSender(), threadContext);
						if (retryTp!=null) {
							String retry=retryTp.transform(sendResult.getResult().asString(),null);
							if (retry.equalsIgnoreCase("true")) {
								if (retriesLeft>=1) {
									retryInterval = increaseRetryIntervalAndWait(session, retryInterval, "xpathRetry result ["+retry+"], retries left [" + retriesLeft + "]");
								}
							} else {
								replyIsValid = true;
							} 
						} else {
							replyIsValid = true;
						}
					} catch (TimeoutException toe) {
						if (retriesLeft>=1) {
							retryInterval = increaseRetryIntervalAndWait(session, retryInterval, "timeout occured, retries left [" + retriesLeft + "]");
						} else {
							throw toe;
						}
					} catch (SenderException se) {
						if (retriesLeft>=1) {
							retryInterval = increaseRetryIntervalAndWait(session, retryInterval, "exception ["+se.getMessage()+"] occured, retries left [" + retriesLeft + "]");
						} else {
							throw se;
						}
					}
				}

				if (!replyIsValid){
					throw new PipeRunException(this, getLogPrefix(session)+"invalid reply message is received");
				}

				if (sendResult==null){
					throw new PipeRunException(this, getLogPrefix(session)+"retrieved null result from sender");
				}

				if (sendResult.getPipeForward()!=null) {
					forward = sendResult.getPipeForward();
				}
				
				if (getSender().isSynchronous()) {
					if (log.isInfoEnabled()) {
						log.info(getLogPrefix(session)+ "sent message to ["+ getSender().getName()+ "] synchronously");
					}
					result = sendResult.getResult();
				} else {
					messageID = sendResult.getResult().asString();
					if (log.isInfoEnabled()) {
						log.info(getLogPrefix(session) + "sent message to [" + getSender().getName()+ "] messageID ["+ messageID+ "] linkMethod ["+ getLinkMethod()	+ "]");
					}
					// if linkMethod is MESSAGEID overwrite correlationID with the messageID
					// as this will be used with the listener
					if (getLinkMethod() == LinkMethod.MESSAGEID) {
						correlationID = sendResult.getResult().asString();
						if (log.isDebugEnabled()) log.debug(getLogPrefix(session)+"setting correlationId to listen for to messageId ["+correlationID+"]");
					}
				}

				ITransactionalStorage messageLog = getMessageLog();
				if (messageLog!=null) {
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
							messageTrail = session.getMessage(getAuditTrailSessionKey()).asString();
						}
					}
					String storedMessageID=messageID;
					if (storedMessageID==null) {
						storedMessageID="-";
					}
					if (correlationIDTp!=null) {
						if (StringUtils.isNotEmpty(getCorrelationIDSessionKey())) {
							String sourceString = session.getMessage(getCorrelationIDSessionKey()).asString();
							correlationID=correlationIDTp.transform(sourceString,null);
						} else {
							if (isUseInputForExtract()) {
								correlationID=correlationIDTp.transform(originalMessage,null);
							} else {
								correlationID=correlationIDTp.transform(input,null);
							}
						}
						if (StringUtils.isEmpty(correlationID)) {
							correlationID="-";
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
					messageLog.storeMessage(storedMessageID,correlationID,new Date(),messageTrail,label, input);

					long messageLogEndTime = System.currentTimeMillis();
					long messageLogDuration = messageLogEndTime - messageLogStartTime;
					StatisticsKeeper sk = getPipeLine().getPipeStatistics(messageLog);
					sk.addValue(messageLogDuration);
				}

				if (getListener() != null) {
					result = Message.asMessage(listenerProcessor.getMessage(getListener(), correlationID, session));
				} else {
					result = sendResult.getResult(); // is this correct? result was already set at line 634!
				}
				if (result == null || result.asObject()==null) {
					result = new Message("");
				}
				if (timeoutPending) {
					timeoutPending=false;
					throwEvent(PIPE_CLEAR_TIMEOUT_MONITOR_EVENT);
				}
		
			} catch (TimeoutException toe) {
				throwEvent(PIPE_TIMEOUT_MONITOR_EVENT);
				if (!timeoutPending) {
					timeoutPending=true;
				}
				PipeForward timeoutForward = findForward(TIMEOUT_FORWARD);
				log.warn(getLogPrefix(session) + "timeout occured");
				if (timeoutForward==null) {
					if (StringUtils.isEmpty(getResultOnTimeOut())) {
						timeoutForward=findForward(PipeForward.EXCEPTION_FORWARD_NAME);
					} else {
						timeoutForward=getSuccessForward();
					}
				}
				if (timeoutForward!=null) {
					Message resultmsg;
					if (StringUtils.isNotEmpty(getResultOnTimeOut())) {
						resultmsg =new Message(getResultOnTimeOut());
					} else {
						resultmsg=new ErrorMessageFormatter().format(getLogPrefix(session),toe,this,input,session.getMessageId(),0);
					}
					return new PipeRunResult(timeoutForward,resultmsg);
				}
				throw new PipeRunException(this, getLogPrefix(session) + "caught timeout-exception", toe);
	
			} catch (Throwable t) {
				throwEvent(PIPE_EXCEPTION_MONITOR_EVENT);
				PipeForward exceptionForward = findForward(PipeForward.EXCEPTION_FORWARD_NAME);
				if (exceptionForward!=null) {
					log.warn(getLogPrefix(session) + "exception occured, forwarding to exception-forward ["+exceptionForward.getPath()+"], exception:\n", t);
					return new PipeRunResult(exceptionForward, new ErrorMessageFormatter().format(getLogPrefix(session),t,this,input,session.getMessageId(),0));
				}
				throw new PipeRunException(this, getLogPrefix(session) + "caught exception", t);
			}
		}
		
		try {
			if (!validResult(result)) {
				PipeForward illegalResultForward = findForward(ILLEGAL_RESULT_FORWARD);
				return new PipeRunResult(illegalResultForward, result);
			}
		} catch (IOException e) {
			throw new PipeRunException(this, getLogPrefix(session) + "caught exception", e);
		}
		IPipe outputValidator = getOutputValidator();
		if (outputValidator!=null) {
			log.debug(getLogPrefix(session)+"validating response");
			PipeRunResult validationResult;
			validationResult = pipeProcessor.processPipe(getPipeLine(), outputValidator, Message.asMessage(result), session);
			if (validationResult!=null && !validationResult.isSuccessful()) {
				return validationResult;
			}
		}
		if (getOutputWrapper()!=null) {
			log.debug(getLogPrefix(session)+"wrapping response");
			PipeRunResult wrapResult = pipeProcessor.processPipe(getPipeLine(), outputWrapper, Message.asMessage(result), session);
			if (wrapResult==null) {
				throw new PipeRunException(outputWrapper, "retrieved null result from outputWrapper");
			}
			if (!wrapResult.isSuccessful()) {
				return wrapResult;
			} 
			result = wrapResult.getResult();
			log.debug(getLogPrefix(session)+"response after wrapping  ("+ClassUtils.nameOf(result)+") [" + result + "]");
		}

		if (isStreamResultToServlet()) {
			Message mia = Message.asMessage(result);
			
			try {
				InputStream resultStream=new Base64InputStream(mia.asInputStream(),false);
				String contentType = session.getMessage("contentType").asString();
				if (StringUtils.isNotEmpty(contentType)) {
					RestListenerUtils.setResponseContentType(session, contentType);
				}
				RestListenerUtils.writeToResponseOutputStream(session, resultStream);
			} catch (IOException e) {
				throw new PipeRunException(this, getLogPrefix(session) + "caught exception", e);
			}
			return new PipeRunResult(forward, "");
		}
		return new PipeRunResult(forward, result);
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

	protected PipeRunResult sendMessage(Message input, PipeLineSession session, ISender sender, Map<String,Object> threadContext) throws SenderException, TimeoutException, IOException, InterruptedException {
		long startTime = System.currentTimeMillis();
		PipeRunResult sendResult = null;
		String exitState = null;
		try {
			PipeLine pipeline = getPipeLine();
			if  (pipeline!=null) {
				Adapter adapter = pipeline.getAdapter();
				if (adapter!=null) {
					if (getPresumedTimeOutInterval()>=0 && !isConfigurationStubbed) {
						long lastExitIsTimeoutDate = adapter.getLastExitIsTimeoutDate(getName());
						if (lastExitIsTimeoutDate>0) {
							long duration = startTime - lastExitIsTimeoutDate;
							if (duration < (1000L * getPresumedTimeOutInterval())) {
								exitState = PRESUMED_TIMEOUT_FORWARD;
								throw new TimeoutException(getLogPrefix(session)+exitState);
							}
						}
					}
				}
			}
			try {
				if (sender instanceof IStreamingSender && canStreamToNextPipe() && getOutputValidator()==null && getOutputWrapper()==null && !isStreamResultToServlet()) {
					sendResult =  ((IStreamingSender)sender).sendMessage(input, session, getNextPipe());
				} else {
					// sendResult has a messageID for async senders, the result for sync senders
					Message result = sender.sendMessage(input, session);
					sendResult = new PipeRunResult(null,result);
				}
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
					throw new TimeoutException(getLogPrefix(session)+"timeOutOnResult ["+getTimeOutOnResult()+"]");
				}
				if (StringUtils.isNotEmpty(getExceptionOnResult()) && getExceptionOnResult().equals(result)) {
					exitState = PipeForward.EXCEPTION_FORWARD_NAME;
					throw new SenderException(getLogPrefix(session)+"exceptionOnResult ["+getExceptionOnResult()+"]");
				}
			}
		} finally {
			if (exitState==null) {
				exitState = PipeForward.SUCCESS_FORWARD_NAME;
			}
			PipeLine pipeline = getPipeLine();
			if (pipeline!=null) {
				Adapter adapter = pipeline.getAdapter();
				if (adapter!=null) {
					if (getPresumedTimeOutInterval()>=0 && !ConfigurationUtils.isConfigurationStubbed(getConfigurationClassLoader())) {
						if (!PRESUMED_TIMEOUT_FORWARD.equals(exitState)) {
							adapter.setLastExitState(getName(), System.currentTimeMillis(), exitState);
						}
					}

					String duration;
					if(msgLogHumanReadable) {
						duration = Misc.getAge(startTime);
					} else {
						duration = Misc.getDurationInMs(startTime);
					}

					if(msgLog.getLevel().isMoreSpecificThan(MSGLOG_LEVEL_TERSE)) {
						msgLog.log(MSGLOG_LEVEL_TERSE, String.format("Sender [%s] class [%s] duration [%s] got exit-state [%s]", sender.getName(), ClassUtils.nameOf(sender), duration, exitState));
					}
				}
			}
		}
		return sendResult;
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
		log.warn(getLogPrefix(session)+description+", starts waiting for [" + currentInterval + "] seconds");
		while (currentInterval-->0) {
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
				PipeStartException pse = new PipeStartException(getLogPrefix(null)+"could not start", t);
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
				PipeStartException pse = new PipeStartException(getLogPrefix(null)+"could not open messagelog", e);
				pse.setPipeNameInError(getName());
				throw pse;
			}
		}
	}
	@Override
	public void stop() {
		if (StringUtils.isEmpty(getStubFilename())) {
			log.info(getLogPrefix(null) + "is closing");
			try {
				getSender().close();
			} catch (SenderException e) {
				log.warn(getLogPrefix(null) + "exception closing sender", e);
			}
			if (getListener() != null) {
				try {
					log.info(getLogPrefix(null) + "is closing; closing listener");
					getListener().close();
				} catch (ListenerException e) {
					log.warn(getLogPrefix(null) + "Exception closing listener", e);
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
				log.warn(getLogPrefix(null) + "Exception closing messageLog", e);
			}
		}
	}

	@Override
	public void iterateOverStatistics(StatisticsKeeperIterationHandler hski, Object data, int action) throws SenderException {
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
		log.debug("pipe [" + getName() + "] registered sender [" + sender.getName() + "] with properties [" + sender.toString() + "]");
	}

	/** Listener for responses on the request sent */
	protected void setListener(ICorrelatedPullingListener listener) {
		this.listener = listener;
		log.debug("pipe [" + getName() + "] registered listener [" + listener.toString() + "]");
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




	/**
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
	@IbisDoc({"1", "For asynchronous communication, the server side may either use the messageID or the correlationID "
		+ "in the correlationID field of the reply message. Use this property to set the behaviour of the reply-listener.", "CORRELATIONID"})
	public void setLinkMethod(LinkMethod method) {
		linkMethod = method;
	}


	@IbisDoc({"2", "stylesheet to extract correlationid from message", ""})
	public void setCorrelationIDStyleSheet(String string) {
		correlationIDStyleSheet = string;
	}

	@IbisDoc({"3", "xpath expression to extract correlationid from message", ""})
	public void setCorrelationIDXPath(String string) {
		correlationIDXPath = string;
	}

	@IbisDoc({"4", "namespace defintions for correlationidxpath. must be in the form of a comma or space separated list of <code>prefix=namespaceuri</code>-definitions", ""})
	public void setCorrelationIDNamespaceDefs(String correlationIDNamespaceDefs) {
		this.correlationIDNamespaceDefs = correlationIDNamespaceDefs;
	}

	@IbisDoc({"5", "key of a pipelinesession-variable. is specified, the value of the pipelinesession variable is used as input for the xpathexpression or stylesheet, instead of the current input message", ""})
	public void setCorrelationIDSessionKey(String string) {
		correlationIDSessionKey = string;
	}

	
	@IbisDoc({"6", "stylesheet to extract label from message", ""})
	public void setLabelStyleSheet(String string) {
		labelStyleSheet = string;
	}
	
	@IbisDoc({"7", "xpath expression to extract label from message", ""})
	public void setLabelXPath(String string) {
		labelXPath = string;
	}

	@IbisDoc({"8", "namespace defintions for labelxpath. must be in the form of a comma or space separated list of <code>prefix=namespaceuri</code>-definitions", ""})
	public void setLabelNamespaceDefs(String labelXNamespaceDefs) {
		this.labelNamespaceDefs = labelXNamespaceDefs;
	}
	

	@IbisDoc({"9", "xpath expression to extract audit trail from message", ""})
	public void setAuditTrailXPath(String string) {
		auditTrailXPath = string;
	}

	@IbisDoc({"10", "namespace defintions for audittrailxpath. must be in the form of a comma or space separated list of <code>prefix=namespaceuri</code>-definitions", ""})
	public void setAuditTrailNamespaceDefs(String auditTrailNamespaceDefs) {
		this.auditTrailNamespaceDefs = auditTrailNamespaceDefs;
	}

	@IbisDoc({"11", "key of a pipelinesession-variable. if specified, the value of the pipelinesession variable is used as audit trail (instead of the default 'no audit trail)", ""})
	public void setAuditTrailSessionKey(String string) {
		auditTrailSessionKey = string;
	}

	@IbisDoc({"12", "when set <code>true</code>, the input of a pipe is used to extract audit trail, correlationid and label (instead of the wrapped input)", "true"})
	public void setUseInputForExtract(boolean b) {
		useInputForExtract = b;
	}

	@Override
	@IbisDoc({"13", "next to common usage in {@link AbstractPipe}, also strings in the error/logstore are masked", ""})
	public void setHideRegex(String hideRegex) {
		super.setHideRegex(hideRegex);
	}

	@IbisDoc({"14", "(only used when hideregex is not empty and only applies to error/logstore) either <code>all</code> or <code>firsthalf</code>. when <code>firsthalf</code> only the first half of the string is masked, otherwise (<code>all</code>) the entire string is masked", "all"})
	public void setHideMethod(String hideMethod) {
		this.hideMethod = hideMethod;
	}



	@IbisDoc({"15", "when set <code>true</code>, the xml well-formedness of the result is checked", "false"})
	public void setCheckXmlWellFormed(boolean b) {
		checkXmlWellFormed = b;
	}

	@IbisDoc({"16", "when set, besides the xml well-formedness the root element of the result is checked to be equal to the value set", ""})
	public void setCheckRootTag(String s) {
		checkRootTag = s;
	}





	/**
	 * The message that is returned when the time listening for a reply message
	 * exceeds the timeout, or in other situations no reply message is received.
	 */
	@IbisDoc({"17", "result returned when no return-message was received within the timeout limit (e.g. 'receiver timed out').", ""})
	public void setResultOnTimeOut(String newResultOnTimeOut) {
		resultOnTimeOut = newResultOnTimeOut;
	}

	@IbisDoc({"18", "the number of times a processing attempt is retried after a timeout or an exception is caught or after a incorrect reply is received (see also <code>retryxpath</code>)", "0"})
	public void setMaxRetries(int i) {
		maxRetries = i;
	}

	@IbisDoc({"19", "the starting number of seconds waited after an unsuccessful processing attempt before another processing attempt is made. each next retry this interval is doubled with a upper limit of <code>retrymaxinterval</code>", "1"})
	public void setRetryMinInterval(int i) {
		retryMinInterval = i;
	}

	@IbisDoc({"20", "the maximum number of seconds waited after an unsuccessful processing attempt before another processing attempt is made", "600"})
	public void setRetryMaxInterval(int i) {
		retryMaxInterval = i;
	}

	@IbisDoc({"21", "xpath expression evaluated on each technical successful reply. retry is done if condition returns true", ""})
	public void setRetryXPath(String string) {
		retryXPath = string;
	}

	@IbisDoc({"22", "namespace defintions for retryxpath. must be in the form of a comma or space separated list of <code>prefix=namespaceuri</code>-definitions", ""})
	public void setRetryNamespaceDefs(String retryNamespaceDefs) {
		this.retryNamespaceDefs = retryNamespaceDefs;
	}

	@IbisDoc({"23", "when the previous call was a timeout, the maximum time <i>in seconds</i> after this timeout to presume the current call is also a timeout. a value of -1 indicates to never presume timeouts", "10"})
	public void setPresumedTimeOutInterval(int i) {
		presumedTimeOutInterval = i;
	}

	@Deprecated
	@ConfigurationWarning("Please use a base64pipe to decode the message and send the result to the pipeline exit")
	@IbisDoc({"24", "if set, the result is first base64 decoded and then streamed to the httpservletresponse object", "false"})
	public void setStreamResultToServlet(boolean b) {
		streamResultToServlet = b;
	}

	@Deprecated
	@ConfigurationWarning("attribute 'stubFileName' is replaced with 'stubFilename'")
	public void setStubFileName(String fileName) {
		setStubFilename(fileName);
	}

	@IbisDoc({"25", "when set, the pipe returns a message from a file, instead of doing the regular process", ""})
	public void setStubFilename(String filename) {
		stubFilename = filename;
	}

	@IbisDoc({"26", "when not empty, a timeoutexception is thrown when the result equals this value (for testing purposes only)", ""})
	public void setTimeOutOnResult(String string) {
		timeOutOnResult = string;
	}

	@IbisDoc({"27", "when not empty, a piperunexception is thrown when the result equals this value (for testing purposes only)", ""})
	public void setExceptionOnResult(String string) {
		exceptionOnResult = string;
	}

}
