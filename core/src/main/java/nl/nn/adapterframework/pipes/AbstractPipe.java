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

import java.lang.reflect.Field;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import nl.nn.adapterframework.doc.IbisDoc;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.logging.log4j.Logger;
import org.springframework.transaction.TransactionDefinition;

import nl.nn.adapterframework.configuration.ClassLoaderManager;
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
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.JtaUtil;
import nl.nn.adapterframework.util.Locker;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.XmlUtils;

/**
 * Base class for {@link IPipe Pipe}.
 * A Pipe represents an action to take in a {@link PipeLine Pipeline}. This class is meant to be extended
 * for defining steps or actions to take to complete a request. <br/>
 * The contract is that a pipe is created (by the digester), {@link #setName(String)} is called and
 * other setters are called, and then {@link IPipe#configure()} is called, optionally
 * throwing a {@link ConfigurationException}. <br/>
 * As much as possible, class instantiating should take place in the
 * {@link IPipe#configure()} method.
 * The object remains alive while the framework is running. When the pipe is to be run,
 * the {@link IPipe#doPipe(Message, IPipeLineSession) doPipe} method is activated.
 * <p>
 * For the duration of the processing of a message by the {@link PipeLine pipeline} has a {@link IPipeLineSession pipeLineSession}.
 * <br/>
 * By this mechanism, pipes may communicate with one another.<br/>
 * However, use this functionality with caution, as it is not desirable to make pipes dependent
 * on each other. If a pipe expects something in a session, it is recommended that
 * the key under which the information is stored is configurable (has a setter for this keyname).
 * Also, the setting of something in the <code>PipeLineSession</code> should be done using
 * this technique (specifying the key under which to store the value by a parameter).
 * </p>
 * <p>Since 4.1 this class also has parameters, so that descendants of this class automatically are parameter-enabled.
 * However, your documentation should say if and how parameters are used!<p>
 * <tr><td>{@link #setWriteToSecLog (boolean) writeToSecLog}</td><td>when set to <code>true</code> a record is written to the security log when the pipe has finished successfully</td><td>false</td></tr>
 * <tr><td>{@link #setSecLogSessionKeys(String) secLogSessionKeys}</td><td>(only used when <code>writeToSecLog=true</code>) comma separated list of keys of session variables that is appended to the security log record</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setLogIntermediaryResults (String) logIntermediaryResults}</td><td>when set, the value in AppConstants is overwritten (for this pipe only)</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setHideRegex(String) hideRegex}</td><td>Regular expression to mask strings in the log. For example, the regular expression <code>(?&lt;=&lt;password&gt;).*?(?=&lt;/password&gt;)</code> will replace every character between keys '&lt;password&gt;' and '&lt;/password&gt;'. <b>Note:</b> this feature is used at adapter level, so one pipe affects all pipes in the pipeline (and multiple values in different pipes are merged)</td><td>&nbsp;</td></tr>
 * </table>
 * </p>
 *
 * <p>
 * <table border="1">
 * <tr><th>nested elements</th><th>description</th></tr>
 * <tr><td>{@link Locker locker}</td><td>optional: the pipe will only be executed if a lock could be set successfully</td></tr>
 * </table>
 * </p>
 *
 * @author     Johan Verrips / Gerrit van Brakel
 *
 * @see IPipeLineSession
 */
public abstract class AbstractPipe implements IExtendedPipe, HasTransactionAttribute, EventThrowing {
	protected Logger log = LogUtil.getLogger(this);
	private ClassLoader configurationClassLoader = Thread.currentThread().getContextClassLoader();

	private String name;
	private String getInputFromSessionKey=null;
	private String getInputFromFixedValue=null;
	private String storeResultInSessionKey=null;
	private boolean preserveInput=false;

	private int maxThreads = 0;
	private long durationThreshold = -1;

	private String chompCharSize = null;
	private String elementToMove = null;
	private String elementToMoveSessionKey = null;
	private String elementToMoveChain = null;
	private boolean removeCompactMsgNamespaces = true;
	private boolean restoreMovedElements=false;
	private boolean namespaceAware=XmlUtils.isNamespaceAwareByDefault();
	
	private int transactionAttribute=TransactionDefinition.PROPAGATION_SUPPORTS;
	private int transactionTimeout=0;
	private boolean sizeStatistics = AppConstants.getInstance(configurationClassLoader).getBoolean("statistics.size", false);
	private Locker locker;
	private String emptyInputReplacement=null;
	private boolean writeToSecLog = false;
	private String secLogSessionKeys = null;
	private boolean recoverAdapter = false;
	private String logIntermediaryResults = null;
	private String hideRegex = null;

	private boolean active=true;

	private Map<String, PipeForward> pipeForwards = new Hashtable<String, PipeForward>();
	private ParameterList parameterList = new ParameterList();
	private EventHandler eventHandler=null;

	private PipeLine pipeline;

	private DummyNamedObject inSizeStatDummyObject=null;
	private DummyNamedObject outSizeStatDummyObject=null;

	public AbstractPipe() {
		inSizeStatDummyObject = new DummyNamedObject();
		outSizeStatDummyObject = new DummyNamedObject();
	}

	/**
	 * <code>configure()</code> is called after the {@link PipeLine Pipeline} is registered
	 * at the {@link Adapter Adapter}. Purpose of this method is to reduce
	 * creating connections to databases etc. in the {@link #doPipe(Message, IPipeLineSession) doPipe()} method.
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
			ConfigurationWarnings.add(this, log, "has no pipe forwards defined");
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
								ConfigurationWarnings.add(this, log, "has a forward of which the pipe to execute ["+path+"] is not defined");
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
	 */
	@Override
	public abstract PipeRunResult doPipe (Message message, IPipeLineSession session) throws PipeRunException;

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
		PipeLine pipeline = getPipeLine();
		if (pipeline==null) {
			return null;
		}
		List<IPipe> pipes = pipeline.getPipes();
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
	 * @see PipeLine
	 * @see PipeForward
	 */
	@Override
	public void registerForward(PipeForward forward) {
		PipeForward current = pipeForwards.get(forward.getName());
		if (current==null){
			pipeForwards.put(forward.getName(), forward);
		} else {
			if (!isRecoverAdapter()) {
				if (forward.getPath()!=null && forward.getPath().equals(current.getPath())) {
					ConfigurationWarnings.add(this, log, "has forward ["+forward.getName()+"] which is already registered");
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
	 * @see ToStringBuilder#reflectionToString
	 *
	 **/
	@Override
	public String toString() {
		try {
			return (new ReflectionToStringBuilder(this) {
				@Override
				protected boolean accept(Field f) {
					//TODO create a blacklist or whitelist
					return super.accept(f) && !f.getName().contains("appConstants");
				}
			}).toString();
		} catch (Throwable t) {
			log.warn("exception getting string representation of pipe ["+getName()+"]", t);
		}
		return null;
	}

	/**
	 * Add a parameter to the list of parameters
	 * @param param the PipeParameter.
	 */
	public void addParameter(Parameter param) {
		log.debug("Pipe ["+getName()+"] added parameter ["+param.toString()+"]");
		parameterList.add(param);
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

	/**
	 * This ClassLoader is set upon creation of the pipe, used to retrieve resources configured by the Ibis application.
	 * @return returns the ClassLoader created by the {@link ClassLoaderManager ClassLoaderManager}.
	 */
	public ClassLoader getConfigurationClassLoader() {
		return configurationClassLoader;
	}

	/**
	 * Indicates the maximum number of treads ;that may call {@link #doPipe(Message, IPipeLineSession)} simultaneously in case
	 *  A value of 0 indicates an unlimited number of threads.
	 */
	@IbisDoc({"maximum number of threads that may call {@link #doPipe(java.lang.Object, nl.nn.adapterframework.core.IPipeLineSession)} simultaneously", "0 (unlimited)"})
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
	@IbisDoc({"1", "name of the pipe", ""})
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

	@IbisDoc({"2", "controls whether pipe is included in configuration. when set <code>false</code> or set to something else as <code>true</code>, (even set to the empty string), the pipe is not included in the configuration", "true"})
	public void setActive(boolean b) {
		active = b;
	}
	@Override
	public boolean isActive() {
		return active;
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
	public void setEmptyInputReplacement(String string) {
		emptyInputReplacement = string;
	}
	@Override
	public String getEmptyInputReplacement() {
		return emptyInputReplacement;
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
	public void setStoreResultInSessionKey(String string) {
		storeResultInSessionKey = string;
	}
	@Override
	public String getStoreResultInSessionKey() {
		return storeResultInSessionKey;
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
	public void setDurationThreshold(long maxDuration) {
		this.durationThreshold = maxDuration;
	}
	@Override
	public long getDurationThreshold() {
		return durationThreshold;
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

	
	@IbisDoc({"controls namespace-awareness of possible xml parsing in descender-classes", "application default"})
	public void setNamespaceAware(boolean b) {
		namespaceAware = b;
	}
	public boolean isNamespaceAware() {
		return namespaceAware;
	}

	@IbisDoc({"3", "Defines transaction and isolation behaviour."
			+ "For developers: it is equal"
			+ "to <a href=\"http://java.sun.com/j2ee/sdk_1.2.1/techdocs/guides/ejb/html/Transaction2.html#10494\">EJB transaction attribute</a>."
			+ "Possible values are:"
			+ "  <table border=\"1\">"
			+ "    <tr><th>transactionAttribute</th><th>callers Transaction</th><th>Pipeline excecuted in Transaction</th></tr>"
			+ "    <tr><td colspan=\"1\" rowspan=\"2\">Required</td>    <td>none</td><td>T2</td></tr>"
			+ "											      <tr><td>T1</td>  <td>T1</td></tr>"
			+ "    <tr><td colspan=\"1\" rowspan=\"2\">RequiresNew</td> <td>none</td><td>T2</td></tr>"
			+ "											      <tr><td>T1</td>  <td>T2</td></tr>"
			+ "    <tr><td colspan=\"1\" rowspan=\"2\">Mandatory</td>   <td>none</td><td>error</td></tr>"
			+ "											      <tr><td>T1</td>  <td>T1</td></tr>"
			+ "    <tr><td colspan=\"1\" rowspan=\"2\">NotSupported</td><td>none</td><td>none</td></tr>"
			+ "											      <tr><td>T1</td>  <td>none</td></tr>"
			+ "    <tr><td colspan=\"1\" rowspan=\"2\">Supports</td>    <td>none</td><td>none</td></tr>"
			+ " 										      <tr><td>T1</td>  <td>T1</td></tr>"
			+ "    <tr><td colspan=\"1\" rowspan=\"2\">Never</td>       <td>none</td><td>none</td></tr>"
			+ "											      <tr><td>T1</td>  <td>error</td></tr>"
			+ "  </table>", "Supports"})
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

	@IbisDoc({"4", "Like <code>transactionAttribute</code>, but the chosen "
			+ "option is represented with a number. The numbers mean:"
			+ "<table>"
			+ "<tr><td>0</td><td>Required</td></tr>"
			+ "<tr><td>1</td><td>Supports</td></tr>"
			+ "<tr><td>2</td><td>Mandatory</td></tr>"
			+ "<tr><td>3</td><td>RequiresNew</td></tr>"
			+ "<tr><td>4</td><td>NotSupported</td></tr>"
			+ "<tr><td>5</td><td>Never</td></tr>"
			+ "</table>", "1"})
	public void setTransactionAttributeNum(int i) {
		transactionAttribute = i;
	}
	@Override
	public int getTransactionAttributeNum() {
		return transactionAttribute;
	}

	@IbisDoc({"5", "timeout (in seconds) of transaction started to process a message.", "<code>0</code> (use system default)"})
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

	@IbisDoc({"Regular expression to mask strings in the log. For example, the regular expression <code>(?&lt;=&lt;password&gt;).*?(?=&lt;/password&gt;)</code> will replace every character between keys '&lt;password&gt;' and '&lt;/password&gt;'. <b>note:</b> this feature is used at adapter level, so one pipe affects all pipes in the pipeline (and multiple values in different pipes are merged)", ""})
	public void setHideRegex(String hideRegex) {
		this.hideRegex = hideRegex;
	}
	public String getHideRegex() {
		return hideRegex;
	}
}
