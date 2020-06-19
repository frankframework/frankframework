/*
   Copyright 2013, 2015-2020 Nationale-Nederlanden

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
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.SystemUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationUtils;
import nl.nn.adapterframework.configuration.ConfigurationWarnings;
import nl.nn.adapterframework.core.Adapter;
import nl.nn.adapterframework.core.HasPhysicalDestination;
import nl.nn.adapterframework.core.HasSender;
import nl.nn.adapterframework.core.IAdapter;
import nl.nn.adapterframework.core.ICorrelatedPullingListener;
import nl.nn.adapterframework.core.IDualModeValidator;
import nl.nn.adapterframework.core.IPipe;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.ISender;
import nl.nn.adapterframework.core.ISenderWithParameters;
import nl.nn.adapterframework.core.ITransactionalStorage;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeLine;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.core.PipeStartException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.errormessageformatters.ErrorMessageFormatter;
import nl.nn.adapterframework.extensions.esb.EsbSoapWrapperPipe;
import nl.nn.adapterframework.http.RestListenerUtils;
import nl.nn.adapterframework.jdbc.DirectQuerySender;
import nl.nn.adapterframework.monitoring.EventThrowing;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.ParameterList;
import nl.nn.adapterframework.processors.ListenerProcessor;
import nl.nn.adapterframework.processors.PipeProcessor;
import nl.nn.adapterframework.senders.ConfigurationAware;
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
import nl.nn.adapterframework.util.XmlUtils;

/**
 * Sends a message using a {@link ISender sender} and optionally receives a reply from the same sender, or
 * from a {@link ICorrelatedPullingListener listener}.
 *
 * <tr><td>{@link #setResultOnTimeOut(String) resultOnTimeOut}</td><td>result returned when no return-message was received within the timeout limit (e.g. "receiver timed out").</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setLinkMethod(String) linkMethod}</td><td>Indicates wether the server uses the correlationID or the messageID in the correlationID field of the reply. This requirers the sender to have set the correlationID at the time of sending.</td><td>CORRELATIONID</td></tr>
 * <tr><td>{@link #setAuditTrailXPath(String) auditTrailXPath}</td><td>xpath expression to extract audit trail from message</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setAuditTrailNamespaceDefs(String) auditTrailNamespaceDefs}</td><td>namespace defintions for auditTrailXPath. Must be in the form of a comma or space separated list of <code>prefix=namespaceuri</code>-definitions</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setAuditTrailSessionKey(String) auditTrailSessionKey}</td><td>Key of a PipeLineSession-variable. If specified, the value of the PipeLineSession variable is used as audit trail (instead of the default "no audit trail")</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setCorrelationIDXPath(String) correlationIDXPath}</td><td>xpath expression to extract correlationID from message</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setCorrelationIDNamespaceDefs(String) correlationIDNamespaceDefs}</td><td>namespace defintions for correlationIDXPath. Must be in the form of a comma or space separated list of <code>prefix=namespaceuri</code>-definitions</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setCorrelationIDStyleSheet(String) correlationIDStyleSheet}</td><td>stylesheet to extract correlationID from message</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setCorrelationIDSessionKey(String) correlationIDSessionKey}</td><td>Key of a PipeLineSession-variable. Is specified, the value of the PipeLineSession variable is used as input for the XpathExpression or StyleSheet, instead of the current input message</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setHideRegex(String) hideRegex}</td><td>Next to common usage in {@link AbstractPipe}, also strings in the error/logstore are masked</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setHideMethod(String) hideMethod}</td><td>(only used when hideRegex is not empty and only applies to error/logstore) either <code>all</code> or <code>firstHalf</code>. When <code>firstHalf</code> only the first half of the string is masked, otherwise (<code>all</code>) the entire string is masked</td><td>"all"</td></tr>
 * <tr><td>{@link #setLabelXPath(String) labelXPath}</td><td>xpath expression to extract label from message</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setLabelNamespaceDefs(String) labelNamespaceDefs}</td><td>namespace defintions for labelXPath. Must be in the form of a comma or space separated list of <code>prefix=namespaceuri</code>-definitions</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setLabelStyleSheet(String) labelStyleSheet}</td><td>stylesheet to extract label from message</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setTimeOutOnResult(String) timeOutOnResult}</td><td>when not empty, a TimeOutException is thrown when the result equals this value (for testing purposes only)</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setExceptionOnResult(String) exceptionOnResult}</td><td>when not empty, a PipeRunException is thrown when the result equals this value (for testing purposes only)</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setMaxRetries(int) maxRetries}</td><td>the number of times a processing attempt is retried after a timeout or an exception is caught or after a incorrect reply is received (see also <code>retryXPath</code>)</td><td>0</td></tr>
 * <tr><td>{@link #setRetryMinInterval(int) retryMinInterval}</td><td>The starting number of seconds waited after an unsuccessful processing attempt before another processing attempt is made. Each next retry this interval is doubled with a upper limit of <code>retryMaxInterval</code></td><td>1</td></tr>
 * <tr><td>{@link #setRetryMaxInterval(int) retryMaxInterval}</td><td>The maximum number of seconds waited after an unsuccessful processing attempt before another processing attempt is made</td><td>600</td></tr>
 * <tr><td>{@link #setRetryXPath(String) retryXPath}</td><td>xpath expression evaluated on each technical successful reply. Retry is done if condition returns true</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setRetryNamespaceDefs(String) retryNamespaceDefs}</td><td>namespace defintions for retryXPath. Must be in the form of a comma or space separated list of <code>prefix=namespaceuri</code>-definitions</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setUseInputForExtract(boolean) useInputForExtract}</td><td>when set <code>true</code>, the input of a pipe is used to extract audit trail, correlationID and label (instead of the wrapped input)</td><td>true</td></tr>
 * <tr><td>{@link #setStreamResultToServlet(boolean) streamResultToServlet}</td><td>if set, the result is first base64 decoded and then streamed to the HttpServletResponse object</td><td>false</td></tr>
 * <tr><td>{@link #setPresumedTimeOutInterval(int) presumedTimeOutInterval}</td><td>when the previous call was a timeout, the maximum time (in seconds) after this timeout to presume the current call is also a timeout. A value of -1 indicates to never presume timeouts</td><td>10 s</td></tr> 
 * <tr><td><code>sender.*</td><td>any attribute of the sender instantiated by descendant classes</td><td>&nbsp;</td></tr>
 * </table>
 * <table border="1">
 * <tr><th>nested elements</th><th>description</th></tr>
 * <tr><td>{@link ISender sender}</td><td>specification of sender to send messages with</td></tr>
 * <tr><td>{@link ICorrelatedPullingListener listener}</td><td>specification of listener to listen to for replies</td></tr>
 * <tr><td>{@link Parameter param}</td><td>any parameters defined on the pipe will be handed to the sender,
 * if this is a {@link ISenderWithParameters ISenderWithParameters}.
 * When a parameter with the name stubFileName is present, it will <u>not</u> be handed to the sender 
 * and it is used at runtime instead of the stubFileName specified by the attribute. A lookup of the 
 * file for this stubFileName will be done at runtime, while the file for the stubFileName specified 
 * as an attribute will be done at configuration time.</td></tr>
 * <tr><td><code>inputValidator</code></td><td>specification of Pipe to validate input messages</td></tr>
 * <tr><td><code>outputValidator</code></td><td>specification of Pipe to validate output messages</td></tr>
 * <tr><td><code>inputWrapper</code></td><td>specification of Pipe to wrap input messages (before validating)</td></tr>
 * <tr><td><code>outputWrapper</code></td><td>specification of Pipe to wrap output messages (after validating)</td></tr>
 * <tr><td>{@link ITransactionalStorage messageLog}</td><td>log of all messages sent</td></tr>
 * </table>
 * </p>
 * <p><b>Exits:</b>
 * <table border="1">
 * <tr><th>state</th><th>condition</th></tr>
 * <tr><td>"success"</td><td>default when a good message was retrieved (synchronous sender), or the message was successfully sent and no listener was specified and the sender was not synchronous</td></tr>
 * <tr><td><i>{@link #setForwardName(String) forwardName}</i></td><td>if specified, and otherwise under same condition as "success"</td></tr>
 * <tr><td>"timeout"</td><td>no data was received (timeout on listening), if the sender was synchronous or a listener was specified. If "timeout" and <code>resultOnTimeOut</code> are not specified, "exception" is used in such a case</td></tr>
 * <tr><td>"exception"</td><td>an exception was thrown by the Sender or its reply-Listener. The result passed to the next pipe is the exception that was caught.</td></tr>
 * <tr><td>"illegalResult"</td><td>the received data does not comply with <code>checkXmlWellFormed</code> or <code>checkRootTag</code>.</td></tr>
 * </table>
 * </p>
 * @author  Gerrit van Brakel
 */

public class MessageSendingPipe extends StreamingPipe implements HasSender, HasStatistics, EventThrowing {
	protected Logger msgLog = LogUtil.getLogger("MSG");
	private Level MSGLOG_LEVEL_TERSE = Level.toLevel("TERSE");

	public static final String PIPE_TIMEOUT_MONITOR_EVENT = "Sender Timeout";
	public static final String PIPE_CLEAR_TIMEOUT_MONITOR_EVENT = "Sender Received Result on Time";
	public static final String PIPE_EXCEPTION_MONITOR_EVENT = "Sender Exception Caught";

	private final static String SUCCESS_FORWARD = "success";
	private final static String TIMEOUT_FORWARD = "timeout";
	private final static String EXCEPTION_FORWARD = "exception";
	private final static String ILLEGAL_RESULT_FORWARD = "illegalResult";
	private final static String PRESUMED_TIMEOUT_FORWARD = "presumedTimeout";
	private final static String INTERRUPT_FORWARD = "interrupt";
	
	private final static String STUBFILENAME = "stubFileName";

	public static final int MIN_RETRY_INTERVAL=1;
	public static final int MAX_RETRY_INTERVAL=600;

	private String linkMethod = "CORRELATIONID";

	private String correlationIDStyleSheet;
	private String correlationIDXPath;
	private String correlationIDNamespaceDefs;
	private String correlationIDSessionKey = null;
	private String labelStyleSheet;
	private String labelXPath;
	private String labelNamespaceDefs;
	private String auditTrailSessionKey = null;
	private String auditTrailXPath;
	private String auditTrailNamespaceDefs;
	private boolean useInputForExtract = true;
	private String hideMethod = "all";

	private boolean checkXmlWellFormed = false;
	private String checkRootTag;

	private String resultOnTimeOut;
	private int maxRetries=0;
	private int retryMinInterval=1;
	private int retryMaxInterval=1;
	private String retryXPath;
	private String retryNamespaceDefs;
	private int presumedTimeOutInterval=10;


	private boolean streamResultToServlet=false;

	private String stubFileName;
	private String timeOutOnResult;
	private String exceptionOnResult;

	private ISender sender = null;
	private ICorrelatedPullingListener listener = null;
	private ITransactionalStorage messageLog=null;

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

	private IPipe inputValidator=null;
	private IPipe outputValidator=null;
	private IPipe inputWrapper=null;
	private IPipe outputWrapper=null;
	
	private boolean timeoutPending=false;

	private boolean checkMessageLog = AppConstants.getInstance(getConfigurationClassLoader()).getBoolean("messageLog.check", false);
	private boolean isConfigurationStubbed = ConfigurationUtils.isConfigurationStubbed(getConfigurationClassLoader());
	private boolean msgLogHumanReadable = AppConstants.getInstance(getConfigurationClassLoader()).getBoolean("msg.log.humanReadable", false);


	private PipeProcessor pipeProcessor;
	private ListenerProcessor listenerProcessor;


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

	@Override
	public void addParameter(Parameter p){
		if (getSender() instanceof ISenderWithParameters && getParameterList()!=null) {
			if (p.getName().equals(STUBFILENAME)) {
				super.addParameter(p);
			} else {
				((ISenderWithParameters)getSender()).addParameter(p);
			}
		}
	}

	/**
	 * Checks whether a sender is defined for this pipe.
	 */
	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		msgLog = LogUtil.getMsgLogger(getAdapter(), this);
		if (StringUtils.isNotEmpty(getStubFileName())) {
			URL stubUrl;
			try {
				stubUrl = ClassUtils.getResourceURL(getConfigurationClassLoader(), getStubFileName());
			} catch (Throwable e) {
				throw new ConfigurationException(getLogPrefix(null)+"got exception finding resource for stubfile ["+getStubFileName()+"]", e);
			}
			if (stubUrl==null) {
				throw new ConfigurationException(getLogPrefix(null)+"could not find resource for stubfile ["+getStubFileName()+"]");
			}
			try {
				returnString = Misc.resourceToString(stubUrl, SystemUtils.LINE_SEPARATOR);
			} catch (Throwable e) {
				throw new ConfigurationException(getLogPrefix(null)+"got exception loading stubfile ["+getStubFileName()+"] from resource ["+stubUrl.toExternalForm()+"]", e);
			}
		} else {
			propagateName();
			if (getSender() == null) {
				throw new ConfigurationException(getLogPrefix(null) + "no sender defined ");
			}
	
			try {
				if (getSender() instanceof ConfigurationAware) {
					IAdapter adapter=getAdapter();
					if (adapter!=null) {
						((ConfigurationAware)getSender()).setConfiguration(getAdapter().getConfiguration());
					} else {
						log.debug("No Adapter to set Configuration from");
					}
				}
				//In order to suppress 'XmlQuerySender is used one or more times' config warnings
				if(sender instanceof DirectQuerySender) {
					String dynamicallyGeneratedKey = "warnings.suppress.sqlInjections."+getAdapter().getName();
					boolean suppressSqlWarning = AppConstants.getInstance().getBoolean(dynamicallyGeneratedKey, false);
					((DirectQuerySender) getSender()).configure(suppressSqlWarning);
				} else {
					getSender().configure();
				}
			} catch (ConfigurationException e) {
				throw new ConfigurationException(getLogPrefix(null)+"while configuring sender",e);
			}
			if (getSender() instanceof HasPhysicalDestination) {
				log.info(getLogPrefix(null)+"has sender on "+((HasPhysicalDestination)getSender()).getPhysicalDestinationName());
			}
			if (getListener() != null) {
				if (getSender().isSynchronous()) {
					throw new ConfigurationException(
						getLogPrefix(null)
							+ "cannot have listener with synchronous sender");
				}
				try {
					getListener().configure();
				} catch (ConfigurationException e) {
					throw new ConfigurationException(getLogPrefix(null)+"while configuring listener",e);
				}
				if (getListener() instanceof HasPhysicalDestination) {
					log.info(getLogPrefix(null)+"has listener on "+((HasPhysicalDestination)getListener()).getPhysicalDestinationName());
				}
			}
			if (!(getLinkMethod().equalsIgnoreCase("MESSAGEID"))
				&& (!(getLinkMethod().equalsIgnoreCase("CORRELATIONID")))) {
				throw new ConfigurationException(getLogPrefix(null)+ "Invalid argument for property LinkMethod ["+getLinkMethod()+ "]. it should be either MESSAGEID or CORRELATIONID");
			}	

			if (!(getHideMethod().equalsIgnoreCase("all"))
					&& (!(getHideMethod().equalsIgnoreCase("firstHalf")))) {
				throw new ConfigurationException(getLogPrefix(null) + "invalid value for hideMethod [" + getHideMethod() + "], must be 'all' or 'firstHalf'");
			}

			if (isCheckXmlWellFormed() || StringUtils.isNotEmpty(getCheckRootTag())) {
				if (findForward(ILLEGAL_RESULT_FORWARD) == null)
					throw new ConfigurationException(getLogPrefix(null) + "has no forward with name [illegalResult]");
			}
			if (!ConfigurationUtils.isConfigurationStubbed(getConfigurationClassLoader())) {
				if (StringUtils.isNotEmpty(getTimeOutOnResult())) {
					throw new ConfigurationException(getLogPrefix(null)+"timeOutOnResult only allowed in stub mode");
				}
				if (StringUtils.isNotEmpty(getExceptionOnResult())) {
					throw new ConfigurationException(getLogPrefix(null)+"exceptionOnResult only allowed in stub mode");
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
		if (checkMessageLog) {
			if (!getSender().isSynchronous() && getListener()==null && !(getSender() instanceof nl.nn.adapterframework.senders.IbisLocalSender)) {
				if (messageLog==null) {
					ConfigurationWarnings.add(this, log, "asynchronous sender [" + getSender().getName() + "] without sibling listener has no messageLog. Integrity check not possible");
				}
			}
		}
		if (messageLog!=null) {
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
				auditTrailTp = TransformerPool.configureTransformer(getLogPrefix(null), getConfigurationClassLoader(), getAuditTrailNamespaceDefs(), getAuditTrailXPath(), null,"text",false,null);
			}
			if (StringUtils.isNotEmpty(getCorrelationIDXPath()) || StringUtils.isNotEmpty(getCorrelationIDStyleSheet())) {
				correlationIDTp=TransformerPool.configureTransformer(getLogPrefix(null), getConfigurationClassLoader(), getCorrelationIDNamespaceDefs(), getCorrelationIDXPath(), getCorrelationIDStyleSheet(),"text",false,null);
			}
			if (StringUtils.isNotEmpty(getLabelXPath()) || StringUtils.isNotEmpty(getLabelStyleSheet())) {
				labelTp=TransformerPool.configureTransformer(getLogPrefix(null), getConfigurationClassLoader(), getLabelNamespaceDefs(), getLabelXPath(), getLabelStyleSheet(),"text",false,null);
			}
		}
		if (StringUtils.isNotEmpty(getRetryXPath())) {
			retryTp = TransformerPool.configureTransformer(getLogPrefix(null), getConfigurationClassLoader(), getRetryNamespaceDefs(), getRetryXPath(), null,"text",false,null);
		}
		IPipe inputValidator = getInputValidator();
		IPipe outputValidator = getOutputValidator();
		if (inputValidator!=null && outputValidator==null && inputValidator instanceof IDualModeValidator) {
			outputValidator=((IDualModeValidator)inputValidator).getResponseValidator();
			setOutputValidator(outputValidator);
		}
		if (inputValidator!=null) {
			PipeForward pf = new PipeForward();
			pf.setName(SUCCESS_FORWARD);
			inputValidator.registerForward(pf);
			//inputValidator.configure(); // configure is handled in PipeLine.configure()
		}
		if (outputValidator!=null) {
			PipeForward pf = new PipeForward();
			pf.setName(SUCCESS_FORWARD);
			outputValidator.registerForward(pf);
			//outputValidator.configure(); // configure is handled in PipeLine.configure()
		}
		if (getInputWrapper()!=null) {
			PipeForward pf = new PipeForward();
			pf.setName(SUCCESS_FORWARD);
			getInputWrapper().registerForward(pf);
			if (getInputWrapper() instanceof EsbSoapWrapperPipe) {
				EsbSoapWrapperPipe eswPipe = (EsbSoapWrapperPipe)getInputWrapper();
				ISender sender = getSender();
				eswPipe.retrievePhysicalDestinationFromSender(sender);
			}
		}
		if (getOutputWrapper()!=null) {
			PipeForward pf = new PipeForward();
			pf.setName(SUCCESS_FORWARD);
			getOutputWrapper().registerForward(pf);
		}

		registerEvent(PIPE_TIMEOUT_MONITOR_EVENT);
		registerEvent(PIPE_CLEAR_TIMEOUT_MONITOR_EVENT);
		registerEvent(PIPE_EXCEPTION_MONITOR_EVENT);
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
//	public boolean canProvideOutputStream() {
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
	public boolean canProvideOutputStream() {
		return super.canProvideOutputStream() && 
				getInputValidator()==null && getInputWrapper()==null && getOutputValidator()==null && getOutputWrapper()==null &&
				!isStreamResultToServlet() && StringUtils.isEmpty(getStubFileName()) && getMessageLog()==null && getListener()==null;
	}

	@Override
	public boolean canStreamToNextPipe() {
		return super.canStreamToNextPipe() && getOutputValidator()==null && getOutputWrapper()==null &&
				!isStreamResultToServlet();
	}

	@Override
	public MessageOutputStream provideOutputStream(IPipeLineSession session) throws StreamingException {
		if (!canProvideOutputStream()) {
			return null;
		}
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

	protected void preserve(Message input, IPipeLineSession session) throws PipeRunException {
		try {
			input.preserve();
		} catch (IOException e) {
			throw new PipeRunException(this,getLogPrefix(session)+"cannot preserve message",e);
		}
	}
	
	@Override
	public PipeRunResult doPipe(Message input, IPipeLineSession session) throws PipeRunException {
		String correlationID = session==null?null:session.getMessageId();
 		Message originalMessage = null;
 		Message result = null;
		PipeForward forward = getForward();

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
			if (!wrapResult.getPipeForward().getName().equals(SUCCESS_FORWARD)) {
				return wrapResult;
			} else {
				input = wrapResult.getResult();
				if (messageLog!=null) {
					preserve(input, session);
				}
			}
			log.debug(getLogPrefix(session)+"input after wrapping ("+ClassUtils.nameOf(input)+") [" + input.toString() + "]");
		}

		if (getInputValidator()!=null) {
			preserve(input, session);
			log.debug(getLogPrefix(session)+"validating input");
			PipeRunResult validationResult = pipeProcessor.processPipe(getPipeLine(), inputValidator, input, session);
			if (validationResult!=null && !validationResult.getPipeForward().getName().equals(SUCCESS_FORWARD)) {
				return validationResult;
			}
		}

		if (StringUtils.isNotEmpty(getStubFileName())) {
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
						result = new Message(Misc.resourceToString(ClassUtils.getResourceURL(getConfigurationClassLoader(), sfn), SystemUtils.LINE_SEPARATOR));
						log.info(getLogPrefix(session)+"returning result from dynamic stub ["+sfn+"]");
					} catch (Throwable e) {
						throw new PipeRunException(this,getLogPrefix(session)+"got exception loading result from stub [" + sfn + "]",e);
					}
				} else {
					log.info(getLogPrefix(session)+"returning result from static stub ["+getStubFileName()+"]");
				}
			} else {
				log.info(getLogPrefix(session)+"returning result from static stub ["+getStubFileName()+"]");
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
					} catch (TimeOutException toe) {
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
					if (getLinkMethod().equalsIgnoreCase("MESSAGEID")) {
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
							messageTrail = (String)(session.get(getAuditTrailSessionKey()));
						}
					}
					String storedMessageID=messageID;
					if (storedMessageID==null) {
						storedMessageID="-";
					}
					if (correlationIDTp!=null) {
						if (StringUtils.isNotEmpty(getCorrelationIDSessionKey())) {
							String sourceString = (String)(session.get(getCorrelationIDSessionKey()));
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
		
			} catch (TimeOutException toe) {
				throwEvent(PIPE_TIMEOUT_MONITOR_EVENT);
				if (!timeoutPending) {
					timeoutPending=true;
				}
				PipeForward timeoutForward = findForward(TIMEOUT_FORWARD);
				log.warn(getLogPrefix(session) + "timeout occured");
				if (timeoutForward==null) {
					if (StringUtils.isEmpty(getResultOnTimeOut())) {
						timeoutForward=findForward(EXCEPTION_FORWARD);
					} else {
						timeoutForward=getForward();
					}
				}
				if (timeoutForward!=null) {
					String resultmsg;
					if (StringUtils.isNotEmpty(getResultOnTimeOut())) {
						resultmsg =getResultOnTimeOut();
					} else {
						resultmsg=new ErrorMessageFormatter().format(getLogPrefix(session),toe,this,input,session.getMessageId(),0);
					}
					return new PipeRunResult(timeoutForward,resultmsg);
				}
				throw new PipeRunException(this, getLogPrefix(session) + "caught timeout-exception", toe);
	
			} catch (Throwable t) {
				throwEvent(PIPE_EXCEPTION_MONITOR_EVENT);
				PipeForward exceptionForward = findForward(EXCEPTION_FORWARD);
				if (exceptionForward!=null) {
					log.warn(getLogPrefix(session) + "exception occured, forwarding to exception-forward ["+exceptionForward.getPath()+"], exception:\n", t);
					String resultmsg;
					resultmsg=new ErrorMessageFormatter().format(getLogPrefix(session),t,this,input,session.getMessageId(),0);
					return new PipeRunResult(exceptionForward,resultmsg);
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
			if (validationResult!=null && !validationResult.getPipeForward().getName().equals(SUCCESS_FORWARD)) {
				return validationResult;
			}
		}
		if (getOutputWrapper()!=null) {
			log.debug(getLogPrefix(session)+"wrapping response");
			PipeRunResult wrapResult = pipeProcessor.processPipe(getPipeLine(), outputWrapper, Message.asMessage(result), session);
			if (wrapResult==null) {
				throw new PipeRunException(outputWrapper, "retrieved null result from outputWrapper");
			}
			if (!wrapResult.getPipeForward().getName().equals(SUCCESS_FORWARD)) {
				return wrapResult;
			} 
			result = wrapResult.getResult();
			log.debug(getLogPrefix(session)+"response after wrapping  ("+ClassUtils.nameOf(result)+") [" + result + "]");
		}

		if (isStreamResultToServlet()) {
			Message mia = Message.asMessage(result);
			
			try {
				InputStream resultStream=new Base64InputStream(mia.asInputStream(),false);
				String contentType = (String) session.get("contentType");
				if (StringUtils.isNotEmpty(contentType)) {
					RestListenerUtils.setResponseContentType(session, contentType);
				}
				RestListenerUtils.writeToResponseOutputStream(session, resultStream);
			} catch (IOException e) {
				throw new PipeRunException(this, getLogPrefix(session) + "caught exception", e);
			}
			return new PipeRunResult(forward, "");
		} else {
			return new PipeRunResult(forward, result);
		}
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

	protected PipeRunResult sendMessage(Message input, IPipeLineSession session, ISender sender, Map<String,Object> threadContext) throws SenderException, TimeOutException, IOException, InterruptedException {
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
								throw new TimeOutException(getLogPrefix(session)+exitState);
							}
						}
					}
				}
			}
			try {
				if (sender instanceof IStreamingSender && getOutputValidator()==null && getOutputWrapper()==null && !isStreamResultToServlet()) {
					sendResult =  ((IStreamingSender)sender).sendMessage(input, session, getNextPipe());
				} else {
					// sendResult has a messageID for async senders, the result for sync senders
					Message result = sender.sendMessage(input, session);
					sendResult = new PipeRunResult(null,result);
				}
			} catch (SenderException se) {
				exitState = EXCEPTION_FORWARD;
				throw se;
			} catch (TimeOutException toe) {
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
					throw new TimeOutException(getLogPrefix(session)+"timeOutOnResult ["+getTimeOutOnResult()+"]");
				}
				if (StringUtils.isNotEmpty(getExceptionOnResult()) && getExceptionOnResult().equals(result)) {
					exitState = EXCEPTION_FORWARD;
					throw new SenderException(getLogPrefix(session)+"exceptionOnResult ["+getExceptionOnResult()+"]");
				}
			}
		} finally {
			if (exitState==null) {
				exitState = SUCCESS_FORWARD;
			}
			PipeLine pipeline = getPipeLine();
			if  (pipeline!=null) {
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
	

	public int increaseRetryIntervalAndWait(IPipeLineSession session, int retryInterval, String description) throws InterruptedException {
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
		if (StringUtils.isEmpty(getStubFileName())) {
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
		ITransactionalStorage messageLog = getMessageLog();
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
		if (StringUtils.isEmpty(getStubFileName())) {
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
		} else {
			return super.hasSizeStatistics();
		}
	}


	/**
	 * Register a {@link ICorrelatedPullingListener} at this Pipe
	 */
	protected void setListener(ICorrelatedPullingListener listener) {
		this.listener = listener;
		log.debug("pipe [" + getName() + "] registered listener [" + listener.toString() + "]");
	}
	public ICorrelatedPullingListener getListener() {
		return listener;
	}

	/**
	 * Sets the messageLog.
	 */
	public void setMessageLog(ITransactionalStorage messageLog) {
		if (messageLog.isActive()) {
			this.messageLog = messageLog;
			messageLog.setName(MESSAGE_LOG_NAME_PREFIX+getName()+MESSAGE_LOG_NAME_SUFFIX);
			if (StringUtils.isEmpty(messageLog.getSlotId())) {
				messageLog.setSlotId(getName());
			}
			if (StringUtils.isEmpty(messageLog.getType())) {
				messageLog.setType(ITransactionalStorage.TYPE_MESSAGELOG_PIPE);
			}
		}
	}
	public ITransactionalStorage getMessageLog() {
		return messageLog;
	}

 

	/**
	 * Register a ISender at this Pipe
	 * @see ISender
	 */
	protected void setSender(ISender sender) {
		this.sender = sender;
		log.debug("pipe [" + getName() + "] registered sender [" + sender.getName() + "] with properties [" + sender.toString() + "]");
	}
	@Override
	public ISender getSender() {
		return sender;
	}

	public void setInputValidator(IPipe inputValidator) {
		inputValidator.setName(INPUT_VALIDATOR_NAME_PREFIX+getName()+INPUT_VALIDATOR_NAME_SUFFIX);
		this.inputValidator = inputValidator;
	}
	public IPipe getInputValidator() {
		return inputValidator;
	}

	public void setOutputValidator(IPipe outputValidator) {
		if (outputValidator!=null) {
			outputValidator.setName(OUTPUT_VALIDATOR_NAME_PREFIX+getName()+OUTPUT_VALIDATOR_NAME_SUFFIX);
		}
		this.outputValidator = outputValidator;
	}
	public IPipe getOutputValidator() {
		return outputValidator;
	}

	public void setInputWrapper(IPipe inputWrapper) {
		inputWrapper.setName(INPUT_WRAPPER_NAME_PREFIX+getName()+INPUT_WRAPPER_NAME_SUFFIX);
		this.inputWrapper = inputWrapper;
	}
	public IPipe getInputWrapper() {
		return inputWrapper;
	}

	public void setOutputWrapper(IPipe outputWrapper) {
		outputWrapper.setName(OUTPUT_WRAPPER_NAME_PREFIX+getName()+OUTPUT_WRAPPER_NAME_SUFFIX);
		this.outputWrapper = outputWrapper;
	}
	public IPipe getOutputWrapper() {
		return outputWrapper;
	}

	public void setPipeProcessor(PipeProcessor pipeProcessor) {
		this.pipeProcessor = pipeProcessor;
	}

	public void setListenerProcessor(ListenerProcessor listenerProcessor) {
		this.listenerProcessor = listenerProcessor;
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
	@IbisDoc({"1", "either MESSAGEID or CORRELATIONID. For asynchronous communication, the server side may either use the messageID or the correlationID "
		+ "in the correlationID field of the reply message. Use this property to set the behaviour of the reply-listener.", "correlationid"})
	public void setLinkMethod(String method) {
		linkMethod = method;
	}
	public String getLinkMethod() {
		return linkMethod;
	}


	@IbisDoc({"2", "stylesheet to extract correlationid from message", ""})
	public void setCorrelationIDStyleSheet(String string) {
		correlationIDStyleSheet = string;
	}
	public String getCorrelationIDStyleSheet() {
		return correlationIDStyleSheet;
	}

	@IbisDoc({"3", "xpath expression to extract correlationid from message", ""})
	public void setCorrelationIDXPath(String string) {
		correlationIDXPath = string;
	}
	public String getCorrelationIDXPath() {
		return correlationIDXPath;
	}

	@IbisDoc({"4", "namespace defintions for correlationidxpath. must be in the form of a comma or space separated list of <code>prefix=namespaceuri</code>-definitions", ""})
	public void setCorrelationIDNamespaceDefs(String correlationIDNamespaceDefs) {
		this.correlationIDNamespaceDefs = correlationIDNamespaceDefs;
	}
	public String getCorrelationIDNamespaceDefs() {
		return correlationIDNamespaceDefs;
	}

	@IbisDoc({"5", "key of a pipelinesession-variable. is specified, the value of the pipelinesession variable is used as input for the xpathexpression or stylesheet, instead of the current input message", ""})
	public void setCorrelationIDSessionKey(String string) {
		correlationIDSessionKey = string;
	}
	public String getCorrelationIDSessionKey() {
		return correlationIDSessionKey;
	}

	
	@IbisDoc({"6", "stylesheet to extract label from message", ""})
	public void setLabelStyleSheet(String string) {
		labelStyleSheet = string;
	}
	public String getLabelStyleSheet() {
		return labelStyleSheet;
	}
	
	@IbisDoc({"7", "xpath expression to extract label from message", ""})
	public void setLabelXPath(String string) {
		labelXPath = string;
	}
	public String getLabelXPath() {
		return labelXPath;
	}

	@IbisDoc({"8", "namespace defintions for labelxpath. must be in the form of a comma or space separated list of <code>prefix=namespaceuri</code>-definitions", ""})
	public void setLabelNamespaceDefs(String labelXNamespaceDefs) {
		this.labelNamespaceDefs = labelXNamespaceDefs;
	}
	public String getLabelNamespaceDefs() {
		return labelNamespaceDefs;
	}
	

	@IbisDoc({"9", "xpath expression to extract audit trail from message", ""})
	public void setAuditTrailXPath(String string) {
		auditTrailXPath = string;
	}
	public String getAuditTrailXPath() {
		return auditTrailXPath;
	}

	@IbisDoc({"10", "namespace defintions for audittrailxpath. must be in the form of a comma or space separated list of <code>prefix=namespaceuri</code>-definitions", ""})
	public void setAuditTrailNamespaceDefs(String auditTrailNamespaceDefs) {
		this.auditTrailNamespaceDefs = auditTrailNamespaceDefs;
	}
	public String getAuditTrailNamespaceDefs() {
		return auditTrailNamespaceDefs;
	}

	@IbisDoc({"11", "key of a pipelinesession-variable. if specified, the value of the pipelinesession variable is used as audit trail (instead of the default 'no audit trail)", ""})
	public void setAuditTrailSessionKey(String string) {
		auditTrailSessionKey = string;
	}
	public String getAuditTrailSessionKey() {
		return auditTrailSessionKey;
	}

	@IbisDoc({"12", "when set <code>true</code>, the input of a pipe is used to extract audit trail, correlationid and label (instead of the wrapped input)", "true"})
	public void setUseInputForExtract(boolean b) {
		useInputForExtract = b;
	}
	public boolean isUseInputForExtract() {
		return useInputForExtract;
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
	public String getHideMethod() {
		return hideMethod;
	}

	
	
	@IbisDoc({"15", "when set <code>true</code>, the xml well-formedness of the result is checked", "false"})
	public void setCheckXmlWellFormed(boolean b) {
		checkXmlWellFormed = b;
	}
	public boolean isCheckXmlWellFormed() {
		return checkXmlWellFormed;
	}

	@IbisDoc({"16", "when set, besides the xml well-formedness the root element of the result is checked to be equal to the value set", ""})
	public void setCheckRootTag(String s) {
		checkRootTag = s;
	}
	public String getCheckRootTag() {
		return checkRootTag;
	}





	/**
	 * The message that is returned when the time listening for a reply message
	 * exceeds the timeout, or in other situations no reply message is received.
	 */
	@IbisDoc({"17", "result returned when no return-message was received within the timeout limit (e.g. 'receiver timed out').", ""})
	public void setResultOnTimeOut(String newResultOnTimeOut) {
		resultOnTimeOut = newResultOnTimeOut;
	}
	public String getResultOnTimeOut() {
		return resultOnTimeOut;
	}

	@IbisDoc({"18", "the number of times a processing attempt is retried after a timeout or an exception is caught or after a incorrect reply is received (see also <code>retryxpath</code>)", "0"})
	public void setMaxRetries(int i) {
		maxRetries = i;
	}
	public int getMaxRetries() {
		return maxRetries;
	}

	@IbisDoc({"19", "the starting number of seconds waited after an unsuccessful processing attempt before another processing attempt is made. each next retry this interval is doubled with a upper limit of <code>retrymaxinterval</code>", "1"})
	public void setRetryMinInterval(int i) {
		retryMinInterval = i;
	}
	public int getRetryMinInterval() {
		return retryMinInterval;
	}

	@IbisDoc({"20", "the maximum number of seconds waited after an unsuccessful processing attempt before another processing attempt is made", "600"})
	public void setRetryMaxInterval(int i) {
		retryMaxInterval = i;
	}
	public int getRetryMaxInterval() {
		return retryMaxInterval;
	}

	@IbisDoc({"21", "xpath expression evaluated on each technical successful reply. retry is done if condition returns true", ""})
	public void setRetryXPath(String string) {
		retryXPath = string;
	}
	public String getRetryXPath() {
		return retryXPath;
	}

	@IbisDoc({"22", "namespace defintions for retryxpath. must be in the form of a comma or space separated list of <code>prefix=namespaceuri</code>-definitions", ""})
	public void setRetryNamespaceDefs(String retryNamespaceDefs) {
		this.retryNamespaceDefs = retryNamespaceDefs;
	}
	public String getRetryNamespaceDefs() {
		return retryNamespaceDefs;
	}

	@IbisDoc({"23", "when the previous call was a timeout, the maximum time (in seconds) after this timeout to presume the current call is also a timeout. a value of -1 indicates to never presume timeouts", "10 s"})
	public void setPresumedTimeOutInterval(int i) {
		presumedTimeOutInterval = i;
	}
	public int getPresumedTimeOutInterval() {
		return presumedTimeOutInterval;
	}

	
	@IbisDoc({"24", "if set, the result is first base64 decoded and then streamed to the httpservletresponse object", "false"})
	public void setStreamResultToServlet(boolean b) {
		streamResultToServlet = b;
	}
	public boolean isStreamResultToServlet() {
		return streamResultToServlet;
	}

	@IbisDoc({"25", "when set, the pipe returns a message from a file, instead of doing the regular process", ""})
	public void setStubFileName(String fileName) {
		stubFileName = fileName;
	}
	public String getStubFileName() {
		return stubFileName;
	}
	
	@IbisDoc({"26", "when not empty, a timeoutexception is thrown when the result equals this value (for testing purposes only)", ""})
	public void setTimeOutOnResult(String string) {
		timeOutOnResult = string;
	}
	public String getTimeOutOnResult() {
		return timeOutOnResult;
	}

	@IbisDoc({"27", "when not empty, a piperunexception is thrown when the result equals this value (for testing purposes only)", ""})
	public void setExceptionOnResult(String string) {
		exceptionOnResult = string;
	}
	public String getExceptionOnResult() {
		return exceptionOnResult;
	}

}
