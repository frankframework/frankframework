/*
   Copyright 2013, 2016 Nationale-Nederlanden

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

import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.log4j.Logger;
import org.springframework.transaction.TransactionDefinition;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarnings;
import nl.nn.adapterframework.core.Adapter;
import nl.nn.adapterframework.core.DummyNamedObject;
import nl.nn.adapterframework.core.HasTransactionAttribute;
import nl.nn.adapterframework.core.IAdapter;
import nl.nn.adapterframework.core.IExtendedPipe;
import nl.nn.adapterframework.core.IPipe;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeLine;
import nl.nn.adapterframework.core.PipeLineExit;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.core.PipeStartException;
import nl.nn.adapterframework.monitoring.EventHandler;
import nl.nn.adapterframework.monitoring.EventThrowing;
import nl.nn.adapterframework.monitoring.MonitorManager;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.ParameterList;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.JtaUtil;
import nl.nn.adapterframework.util.Locker;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.XmlUtils;

/**
 * Base class for {@link nl.nn.adapterframework.core.IPipe Pipe}.
 * A Pipe represents an action to take in a {@link nl.nn.adapterframework.core.PipeLine Pipeline}. This class is ment to be extended
 * for defining steps or actions to take to complete a request. <br/>
 * The contract is that a pipe is created (by the digester), {@link #setName(String)} is called and
 * other setters are called, and then {@link nl.nn.adapterframework.core.IPipe#configure()} is called, optionally
 * throwing a {@link nl.nn.adapterframework.configuration.ConfigurationException}. <br/>
 * As much as possible, class instantiating should take place in the
 * {@link nl.nn.adapterframework.core.IPipe#configure()} method.
 * The object remains alive while the framework is running. When the pipe is to be run,
 * the {@link nl.nn.adapterframework.core.IPipe#doPipe(Object, IPipeLineSession) doPipe} method is activated.
 * <p>
 * For the duration of the processing of a message by the {@link nl.nn.adapterframework.core.PipeLine pipeline} has a {@link nl.nn.adapterframework.core.IPipeLineSession pipeLineSession}.
 * <br/>
 * By this mechanism, pipes may communicate with one another.<br/>
 * However, use this functionality with caution, as it is not desirable to make pipes dependend
 * on each other. If a pipe expects something in a session, it is recommended that
 * the key under which the information is stored is configurable (has a setter for this keyname).
 * Also, the setting of something in the <code>PipeLineSession</code> should be done using
 * this technique (specifying the key under which to store the value by a parameter).
 * </p>
 * <p>Since 4.1 this class also has parameters, so that decendants of this class automatically are parameter-enabled.
 * However, your documentation should say if and how parameters are used!<p>
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>className</td><td>nl.nn.adapterframework.pipes.AbstractPipe</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setName(String) name}</td><td>name of the Pipe</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setMaxThreads(int) maxThreads}</td><td>maximum number of threads that may call {@link #doPipe(java.lang.Object, nl.nn.adapterframework.core.IPipeLineSession)} simultaneously</td><td>0 (unlimited)</td></tr>
 * <tr><td>{@link #setActive(boolean) active}</td><td>controls whether Pipe is included in configuration. When set <code>false</code> or set to something else as "true", (even set to the empty string), the Pipe is not included in the configuration</td><td>true</td></tr>
 * <tr><td>{@link #setDurationThreshold(long) durationThreshold}</td><td>if durationThreshold >=0 and the duration (in milliseconds) of the message processing exceeded the value specified, then the message is logged informatory</td><td>-1</td></tr>
 * <tr><td>{@link #setGetInputFromSessionKey(String) getInputFromSessionKey}</td><td>when set, input is taken from this session key, instead of regular input</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setGetInputFromFixedValue(String) getInputFromFixedValue}</td><td>when set, this fixed value is taken as input, instead of regular input</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setEmptyInputReplacement(String) emptyInputReplacement}</td><td>when set and the regular input is empty, this fixed value is taken as input</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setStoreResultInSessionKey(String) storeResultInSessionKey}</td><td>when set, the result is stored under this session key</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setPreserveInput(boolean) preserveInput}</td><td>when set <code>true</code>, the input of a pipe is restored before processing the next one</td><td>false</td></tr>
 * <tr><td>{@link #setRestoreMovedElements(boolean) restoreMovedElements}</td><td>when set <code>true</code>, compacted messages in the result are restored to their original format (see also  {@link nl.nn.adapterframework.receivers.ReceiverBase#setElementToMove(java.lang.String)})</td><td>false</td></tr>
 * <tr><td>{@link #setChompCharSize(String) chompCharSize}</td><td>if set (>=0) and the character data length inside a xml element exceeds this size, the character data is chomped (with a clear comment)</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setElementToMove(String) elementToMove}</td><td>if set, the character data in this element is stored under a session key and in the message replaced by a reference to this session key: "{sessionKey:" + <code>elementToMoveSessionKey</code> + "}"</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setElementToMoveSessionKey(String) elementToMoveSessionKey}</td><td>(only used when <code>elementToMove</code> is set) name of the session key under which the character data is stored</td><td>"ref_" + the name of the element</td></tr>
 * <tr><td>{@link #setElementToMoveChain(String) elementToMoveChain}</td><td>like <code>elementToMove</code> but element is preceded with all ancestor elements and separated by semicolons (e.g. "adapter;pipeline;pipe")</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setRemoveCompactMsgNamespaces (boolean) removeCompactMsgNamespaces}</td><td>when set <code>true</code> namespaces (and prefixes) in the compacted message are removed</td><td>true</td></tr>
 * <tr><td>{@link #setNamespaceAware(boolean) namespaceAware}</td><td>controls namespace-awareness of possible XML parsing in descender-classes</td><td>application default</td></tr>
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
 *  </table></td><td>Supports</td></tr>
 * <tr><td>{@link #setTransactionTimeout(int) transactionTimeout}</td><td>Timeout (in seconds) of transaction started to process a message.</td><td><code>0</code> (use system default)</code></td></tr>
 * <tr><td>{@link #setWriteToSecLog (boolean) writeToSecLog}</td><td>when set to <code>true</code> a record is written to the security log when the pipe has finished successfully</td><td>false</td></tr>
 * <tr><td>{@link #setSecLogSessionKeys(String) secLogSessionKeys}</td><td>(only used when <code>writeToSecLog=true</code>) comma separated list of keys of session variables that is appended to the security log record</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setLogIntermediaryResults (boolean) logIntermediaryResults}</td><td>when set, the value in AppConstants is overwritten (for this pipe only)</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setHideRegex(String) hideRegex}</td><td>Regular expression to mask strings in the log. For example, the regular expression <code>(?&lt;=&lt;password&gt;).*?(?=&lt;/password&gt;)</code> will replace every character between keys '&lt;password&gt;' and '&lt;/password&gt;'. <b>Note:</b> this feature is used at adapter level, so one pipe affects all pipes in the pipeline (and multiple values in different pipes are merged)</td><td>&nbsp;</td></tr>
 * </table>
 * </p>
 * 
 * <p>
 * <table border="1">
 * <tr><th>nested elements</th><th>description</th></tr>
 * <tr><td>{@link nl.nn.adapterframework.util.Locker locker}</td><td>optional: the pipe will only be executed if a lock could be set successfully</td></tr>
 * </table>
 * </p>
 * 
 * @author     Johan Verrips / Gerrit van Brakel
 *
 * @see nl.nn.adapterframework.core.IPipeLineSession
 */
public abstract class AbstractPipe implements IExtendedPipe, HasTransactionAttribute, EventThrowing {
	protected Logger log = LogUtil.getLogger(this);
	protected ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

	private String name;

	private Map<String, PipeForward> pipeForwards = new Hashtable<String, PipeForward>();
	private int maxThreads = 0;
	private ParameterList parameterList = new ParameterList();
	private long durationThreshold = -1;
	private String getInputFromSessionKey=null;
	private String getInputFromFixedValue=null;
	private String storeResultInSessionKey=null;
	private boolean preserveInput=false;
	private String chompCharSize = null;
	private String elementToMove = null;
	private String elementToMoveSessionKey = null;
	private String elementToMoveChain = null;
	private boolean removeCompactMsgNamespaces = true;
	private boolean restoreMovedElements=false;
	private boolean namespaceAware=XmlUtils.isNamespaceAwareByDefault();
	private int transactionAttribute=TransactionDefinition.PROPAGATION_SUPPORTS;
	private int transactionTimeout=0;
	private boolean sizeStatistics = AppConstants.getInstance().getBoolean("statistics.size", false);
	private Locker locker;
	private String emptyInputReplacement=null;
	private boolean writeToSecLog = false;
	private String secLogSessionKeys = null;
	private boolean recoverAdapter = false;
	private String logIntermediaryResults = null;
	private String hideRegex = null;

	private boolean active=true;

	private EventHandler eventHandler=null;

	private PipeLine pipeline;

	private DummyNamedObject inSizeStatDummyObject=null;
	private DummyNamedObject outSizeStatDummyObject=null;

	public AbstractPipe() {
		inSizeStatDummyObject = new DummyNamedObject();
		outSizeStatDummyObject = new DummyNamedObject();
	}

	/**
	 * <code>configure()</code> is called after the {@link nl.nn.adapterframework.core.PipeLine Pipeline} is registered
	 * at the {@link nl.nn.adapterframework.core.Adapter Adapter}. Purpose of this method is to reduce
	 * creating connections to databases etc. in the {@link #doPipe(Object) doPipe()} method.
	 * As much as possible class-instantiating should take place in the
	 * <code>configure()</code> method, to improve performance.
	 */
	@Override
	public void configure() throws ConfigurationException {
		ParameterList params = getParameterList();

		if (params!=null) {
			try {
				params.configure();
			} catch (ConfigurationException e) {
				throw new ConfigurationException(getLogPrefix(null)+"while configuring parameters",e);
			}
		}

		if (!StringUtils.isEmpty(getElementToMove()) && !StringUtils.isEmpty(getElementToMoveChain())) {
			throw new ConfigurationException(getLogPrefix(null)+"cannot have both an elementToMove and an elementToMoveChain specified");
		}

		if (pipeForwards.isEmpty()) {
			ConfigurationWarnings configWarnings = ConfigurationWarnings.getInstance();
			String msg = getLogPrefix(null)+"has no forwards defined.";
			configWarnings.add(log, msg);
		} else {
			for (Iterator<String> it = pipeForwards.keySet().iterator(); it.hasNext();) {
				String forwardName = it.next();
				PipeForward forward= pipeForwards.get(forwardName);
				if (forward!=null) {
					String path=forward.getPath();
					if (path!=null) {
						PipeLineExit plExit= pipeline.getPipeLineExits().get(path);
						if (plExit==null){
							if (pipeline.getPipe(path)==null){
								ConfigurationWarnings configWarnings = ConfigurationWarnings.getInstance();
								String msg = getLogPrefix(null)+"has a forward of which the pipe to execute ["+path+"] is not defined.";
								configWarnings.add(log, msg);
							}
						}
					}
				}
			}
		}

		if (getLocker() != null) {
			getLocker().configure();
		}

		eventHandler = MonitorManager.getEventHandler();
	}

	/**
	 * Extension for IExtendedPipe that calls configure(void) in its implementation.
	 */
	@Override
	public void configure(PipeLine pipeline) throws ConfigurationException {
		this.pipeline=pipeline;
		configure();
	}


	/**
	 * This is where the action takes place. Pipes may only throw a PipeRunException,
	 * to be handled by the caller of this object.
	 * @deprecated use {@link #doPipe(Object,IPipeLineSession)} instead
	 */
	@Deprecated
	public PipeRunResult doPipe (Object input) throws PipeRunException {
		throw new PipeRunException(this, "Pipe should implement method doPipe()");
	}

	/**
	 * This is where the action takes place. Pipes may only throw a PipeRunException,
	 * to be handled by the caller of this object.
	 */
	@Override
	public PipeRunResult doPipe (Object input, IPipeLineSession session) throws PipeRunException {
		return doPipe(input);
	}

	/**
	 * looks up a key in the pipeForward hashtable. <br/>
	 * A typical use would be on return from a Pipe: <br/>
	 * <code><pre>
	 * return new PipeRunResult(findForward("success"), result);
	 * </pre></code>
	 * In the pipeForward hashtable are available:
	 * <ul><li>All forwards defined in xml under the pipe element of this pipe</li>
	 * <li> All global forwards defined in xml under the PipeLine element</li>
	 * <li> All pipenames with their (identical) path</li>
	 * </ul>
	 * Therefore, you can directly jump to another pipe, although this is not recommended
	 * as the pipe should not know the existence of other pipes. Nevertheless, this feature
	 * may come in handy for switcher-pipes.<br/><br/>
	 * @param forward   Name of the forward
	 * @return PipeForward
	 */
	//TODO: Create a 2nd findForwards method without all pipes in the hashtable and make the first one deprecated.
	public PipeForward findForward(String forward){
		if (StringUtils.isEmpty(forward)) {
			return null;
		}
		return pipeForwards.get(forward);
	}

	@Override
	public Map<String, PipeForward> getForwards(){
		Map<String, PipeForward> forwards = new Hashtable<String, PipeForward>(pipeForwards);
		List<IPipe> pipes = getPipeLine().getPipes();
		for (int i=0; i<pipes.size(); i++) {
			String pipeName = pipes.get(i).getName();
			if(forwards.containsKey(pipeName))
				forwards.remove(pipeName);
		}
		return forwards;
	}


	/**
	 * Convenience method for building up log statements.
	 * This method may be called from within the <code>doPipe()</code> method with the current <code>PipeLineSession</code>
	 * as a parameter. Then it will use this parameter to retrieve the messageId. The method can be called with a <code>null</code> parameter
	 * from the <code>configure()</code>, <code>start()</code> and <code>stop()</code> methods.
	 * @return String with the name of the pipe and the message id of the current message.
	 */
	protected String getLogPrefix(IPipeLineSession session) {
		StringBuilder sb = new StringBuilder();
		sb.append("Pipe ["+getName()+"] ");
		if (session!=null) {
			sb.append("msgId ["+session.getMessageId()+"] ");
		}
		return sb.toString();
	}

	/**
	 * Register a PipeForward object to this Pipe. Global Forwards are added
	 * by the PipeLine. If a forward is already registered, it logs a warning.
	 * @param forward
	 * @see nl.nn.adapterframework.core.PipeLine
	 * @see PipeForward
	 */
	@Override
	public void registerForward(PipeForward forward) {
		PipeForward current = pipeForwards.get(forward.getName());
		if (current==null){
			pipeForwards.put(forward.getName(), forward);
		} else {
			if (!isRecoverAdapter()) {
				if (forward.getPath().equals(current.getPath())) {
					ConfigurationWarnings configWarnings = ConfigurationWarnings.getInstance();
					String msg = getLogPrefix(null)+"PipeForward ["+forward.getName()+"] pointing to ["+forward.getPath()+"] already registered";
					configWarnings.add(log, msg);
				} else {
					log.info(getLogPrefix(null)+"PipeForward ["+forward.getName()+"] already registered, pointing to ["+current.getPath()+"]. Ignoring new one, that points to ["+forward.getPath()+"]");
				}
			}
		}
	}

	protected boolean isRecoverAdapter() {
		boolean recover = false;
		IAdapter iAdapter = getAdapter();
		if (iAdapter == null) {
			recover = recoverAdapter;
		} else {
			if (iAdapter instanceof Adapter) {
				Adapter adapter = (Adapter) iAdapter;
				recover = adapter.isRecover();
			}
		}
		return recover;
	}	

	/**
	 * Perform necessary action to start the pipe. This method is executed
	 * after the {@link #configure()} method, for each start and stop command of the
	 * adapter.
	 */
	@Override
	public void start() throws PipeStartException {
//		if (getTransactionAttributeNum()>0 && getTransactionAttributeNum()!=JtaUtil.TRANSACTION_ATTRIBUTE_SUPPORTS) {
//			try {
//				// getUserTransaction, to make sure its available
//				JtaUtil.getUserTransaction();
//			} catch (NamingException e) {
//				throw new PipeStartException(getLogPrefix(null)+"cannot obtain UserTransaction",e);
//			}
//		}
	}

	/**
	 * Perform necessary actions to stop the <code>Pipe</code>.<br/>
	 * For instance, closing JMS connections, dbms connections etc.
	 */
	@Override
	public void stop() {}

	/**
	 * The <code>toString()</code> method retrieves its value
	 * by reflection, so overriding this method is mostly not
	 * usefull.
	 * @see org.apache.commons.lang.builder.ToStringBuilder#reflectionToString
	 *
	**/
	@Override
	public String toString() {
		try {
			return ToStringBuilder.reflectionToString(this);
		} catch (Throwable t) {
			log.warn("exception getting string representation of pipe ["+getName()+"]", t);
		}
		return null;
	}

	/**
	 * Add a parameter to the list of parameters
	 * @param rhs the PipeParameter.
	 */
	public void addParameter(Parameter rhs) {
		log.debug("Pipe ["+getName()+"] added parameter ["+rhs.toString()+"]");
		parameterList.add(rhs);
	}

	/**
	 * return the Parameters
	 */
	public ParameterList getParameterList() {
		return parameterList;
	}

	@Override
	public String getEventSourceName() {
		return getLogPrefix(null).trim();
	}
	@Override
	public void registerEvent(String description) {
		if (eventHandler!=null) {
			eventHandler.registerEvent(this,description);
		}
	}
	@Override
	public void throwEvent(String event) {
		if (eventHandler!=null) {
			eventHandler.fireEvent(this,event);
		}
	}

	public PipeLine getPipeLine() {
		return pipeline;
	}

	@Override
	public IAdapter getAdapter() {
		if (getPipeLine()!=null) {
			return getPipeLine().getAdapter();
		}
		return null;
	}

	@Override
	public String getType() {
		return this.getClass().getSimpleName();
	}

	/**
	 * Indicates the maximum number of treads ;that may call {@link #doPipe(java.lang.Object, nl.nn.adapterframework.core.IPipeLineSession)} simultaneously in case
	 *  A value of 0 indicates an unlimited number of threads.
	 */
	public void setMaxThreads(int newMaxThreads) {
		maxThreads = newMaxThreads;
	}
	@Override
	public int getMaxThreads() {
		return maxThreads;
	}

	/**
	 * The functional name of this pipe
	 */
	@Override
	public void setName(String name) {
		this.name=name;
		inSizeStatDummyObject.setName(getName() + " (in)");
		outSizeStatDummyObject.setName(getName() + " (out)");
	}
	@Override
	public String getName() {
		return this.name;
	}

	/**
	 * Sets a threshold for the duration of message execution;
	 * If the threshold is exceeded, the message is logged to be analyzed.
	 */
	@Override
	public void setDurationThreshold(long maxDuration) {
		this.durationThreshold = maxDuration;
	}
	@Override
	public long getDurationThreshold() {
		return durationThreshold;
	}

	@Override
	public void setGetInputFromSessionKey(String string) {
		getInputFromSessionKey = string;
	}
	@Override
	public String getGetInputFromSessionKey() {
		return getInputFromSessionKey;
	}

	@Override
	public void setGetInputFromFixedValue(String string) {
		getInputFromFixedValue = string;
	}
	@Override
	public String getGetInputFromFixedValue() {
		return getInputFromFixedValue;
	}

	@Override
	public void setStoreResultInSessionKey(String string) {
		storeResultInSessionKey = string;
	}
	@Override
	public String getStoreResultInSessionKey() {
		return storeResultInSessionKey;
	}

	@Override
	public void setPreserveInput(boolean preserveInput) {
		this.preserveInput = preserveInput;
	}
	@Override
	public boolean isPreserveInput() {
		return preserveInput;
	}

	@Override
	public void setChompCharSize(String string) {
		chompCharSize = string;
	}

	@Override
	public String getChompCharSize() {
		return chompCharSize;
	}

	@Override
	public void setElementToMove(String string) {
		elementToMove = string;
	}

	@Override
	public String getElementToMove() {
		return elementToMove;
	}

	@Override
	public void setElementToMoveSessionKey(String string) {
		elementToMoveSessionKey = string;
	}

	@Override
	public String getElementToMoveSessionKey() {
		return elementToMoveSessionKey;
	}

	@Override
	public void setElementToMoveChain(String string) {
		elementToMoveChain = string;
	}

	@Override
	public String getElementToMoveChain() {
		return elementToMoveChain;
	}

	@Override
	public void setRemoveCompactMsgNamespaces(boolean b) {
		removeCompactMsgNamespaces = b;
	}

	@Override
	public boolean isRemoveCompactMsgNamespaces() {
		return removeCompactMsgNamespaces;
	}
	
	@Override
	public void setRestoreMovedElements(boolean restoreMovedElements) {
		this.restoreMovedElements = restoreMovedElements;
	}
	@Override
	public boolean isRestoreMovedElements() {
		return restoreMovedElements;
	}

	public void setNamespaceAware(boolean b) {
		namespaceAware = b;
	}
	public boolean isNamespaceAware() {
		return namespaceAware;
	}

	public void setTransactionAttribute(String attribute) throws ConfigurationException {
		transactionAttribute = JtaUtil.getTransactionAttributeNum(attribute);
		if (transactionAttribute<0) {
			throw new ConfigurationException("illegal value for transactionAttribute ["+attribute+"]");
		}
	}
	@Override
	public String getTransactionAttribute() {
		return JtaUtil.getTransactionAttributeString(transactionAttribute);
	}

	public void setTransactionAttributeNum(int i) {
		transactionAttribute = i;
	}
	@Override
	public int getTransactionAttributeNum() {
		return transactionAttribute;
	}

	public void setActive(boolean b) {
		active = b;
	}
	@Override
	public boolean isActive() {
		return active;
	}

	public void setTransactionTimeout(int i) {
		transactionTimeout = i;
	}
	@Override
	public int getTransactionTimeout() {
		return transactionTimeout;
	}

	@Override
	public boolean hasSizeStatistics() {
		return sizeStatistics;
	}
	public void setSizeStatistics(boolean sizeStatistics) {
		this.sizeStatistics = sizeStatistics;
	}

	@Override
	public void setLocker(Locker locker) {
		this.locker = locker;
	}
	@Override
	public Locker getLocker() {
		return locker;
	}

	@Override
	public void setEmptyInputReplacement(String string) {
		emptyInputReplacement = string;
	}

	@Override
	public String getEmptyInputReplacement() {
		return emptyInputReplacement;
	}

	public DummyNamedObject getInSizeStatDummyObject() {
		return inSizeStatDummyObject;
	}

	public DummyNamedObject getOutSizeStatDummyObject() {
		return outSizeStatDummyObject;
	}

	@Override
	public void setWriteToSecLog(boolean b) {
		writeToSecLog = b;
	}
	@Override
	public boolean isWriteToSecLog() {
		return writeToSecLog;
	}

	@Override
	public void setSecLogSessionKeys(String string) {
		secLogSessionKeys = string;
	}
	@Override
	public String getSecLogSessionKeys() {
		return secLogSessionKeys;
	}

	public void setRecoverAdapter(boolean b) {
		recoverAdapter = b;
	}

	public void setLogIntermediaryResults(String string) {
		logIntermediaryResults = string;
	}
	public String getLogIntermediaryResults() {
		return logIntermediaryResults;
	}

	public void setHideRegex(String hideRegex) {
		this.hideRegex = hideRegex;
	}

	public String getHideRegex() {
		return hideRegex;
	}
}
