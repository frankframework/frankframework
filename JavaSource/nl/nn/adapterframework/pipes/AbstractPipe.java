/*
 * $Log: AbstractPipe.java,v $
 * Revision 1.28  2007-06-08 07:58:14  europe\L190409
 * set default transactionAttribute to Supports
 *
 * Revision 1.27  2007/05/02 11:34:34  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added attribute 'active'
 * added attribute getInputFromFixedValue
 *
 * Revision 1.26  2007/05/01 14:09:39  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * introduction of PipeLine-exithandlers
 *
 * Revision 1.25  2007/02/12 14:02:19  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * Logger from LogUtil
 *
 * Revision 1.24  2006/12/28 14:21:23  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * updated javadoc
 *
 * Revision 1.23  2006/10/13 08:17:24  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * cache UserTransaction at startup
 *
 * Revision 1.22  2006/09/14 11:59:09  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * corrected javadoc
 *
 * Revision 1.21  2006/08/24 07:12:42  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * documented METT tracing event numbers
 *
 * Revision 1.20  2006/08/22 12:52:36  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added preserveInput attribute
 *
 * Revision 1.19  2006/08/21 15:21:23  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * introduction of transaction attribute handling
 *
 * Revision 1.18  2006/02/20 15:42:41  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * moved METT-support to single entry point for tracing
 *
 * Revision 1.17  2006/02/09 08:01:48  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * METT tracing support
 *
 * Revision 1.16  2006/01/05 14:34:48  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * updated javadoc
 *
 * Revision 1.15  2005/10/24 09:20:18  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * made namespaceAware an attribute of AbstractPipe
 *
 * Revision 1.14  2005/09/08 15:53:01  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * moved extra functionality to IExtendedPipe
 *
 * Revision 1.13  2005/09/07 15:26:16  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added attributes getInputFromSessionKey and storeResultInSessionKey
 *
 * Revision 1.12  2005/09/05 07:00:06  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * changed maxDuration into durationThreshold
 *
 * Revision 1.11  2005/09/01 08:51:10  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added maxDuration attribute, to be used to log messages that take long time
 *
 * Revision 1.10  2005/08/24 15:52:16  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * improved error message for configuration exception
 *
 * Revision 1.9  2005/06/13 10:00:15  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * modified handling of empty forwards
 *
 * Revision 1.8  2004/10/19 13:51:58  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * parameter-configure in configure()
 *
 * Revision 1.7  2004/10/05 10:47:03  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * retyped ParameterList
 *
 * Revision 1.6  2004/05/21 07:37:08  unknown <unknown@ibissource.org>
 * Moved PipeParameter to core
 *
 * Revision 1.5  2004/04/06 10:16:16  Johan Verrips <johan.verrips@ibissource.org>
 * Added PipeParameter and implemented it. Added XsltParamPipe also
 *
 * Revision 1.4  2004/03/30 07:30:05  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * updated javadoc
 *
 */
package nl.nn.adapterframework.pipes;

import java.util.Hashtable;

import javax.naming.NamingException;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.HasTransactionAttribute;
import nl.nn.adapterframework.core.IExtendedPipe;
import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeLine;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.core.PipeStartException;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.ParameterList;
import nl.nn.adapterframework.util.JtaUtil;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.TracingEventNumbers;
import nl.nn.adapterframework.util.XmlUtils;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.log4j.Logger;

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
 * the {@link nl.nn.adapterframework.core.IPipe#doPipe(Object, PipeLineSession) doPipe} method is activated.
 * <p>
 * For the duration of the processing of a message by the {@link nl.nn.adapterframework.core.PipeLine pipeline} has a {@link nl.nn.adapterframework.core.PipeLineSession session}.
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
 * <tr><td>{@link #setMaxThreads(int) maxThreads}</td><td>maximum number of threads that may call {@link #doPipe(Object, PipeLineSession)} simultaneously</td><td>0 (unlimited)</td></tr>
 * <tr><td>{@link #setActive(boolean) active}</td><td>controls whether Pipe is included in configuration. When set <code>false</code> or set to something else as "true", (even set to the empty string), the Pipe is not included in the configuration</td><td>true</td></tr>
 * <tr><td>{@link #setDurationThreshold(long) durationThreshold}</td><td>if durationThreshold >=0 and the duration (in milliseconds) of the message processing exceeded the value specified, then the message is logged informatory</td><td>-1</td></tr>
 * <tr><td>{@link #setGetInputFromSessionKey(String) getInputFromSessionKey}</td><td>when set, input is taken from this session key, instead of regular input</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setGetInputFromFixedValue(String) getInputFromFixedValue}</td><td>when set, this fixed value is taken as input, instead of regular input</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setStoreResultInSessionKey(String) storeResultInSessionKey}</td><td>when set, the result is stored under this session key</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setPreserveInput(boolean) preserveInput}</td><td>when set <code>true</code>, the input of a pipe is restored before processing the next one</td><td>false</td></tr>
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
 * <tr><td>{@link #setBeforeEvent(int) beforeEvent}</td>      <td>METT eventnumber, fired just before a message is processed by this Pipe</td><td>-1 (disabled)</td></tr>
 * <tr><td>{@link #setAfterEvent(int) afterEvent}</td>        <td>METT eventnumber, fired just after message processing by this Pipe is finished</td><td>-1 (disabled)</td></tr>
 * <tr><td>{@link #setExceptionEvent(int) exceptionEvent}</td><td>METT eventnumber, fired when message processing by this Pipe resulted in an exception</td><td>-1 (disabled)</td></tr>
 * </table>
 * </p>
 * @version Id
 * @author     Johan Verrips / Gerrit van Brakel
 *
 * @see nl.nn.adapterframework.core.PipeLineSession
 */
public abstract class AbstractPipe implements IExtendedPipe, HasTransactionAttribute, TracingEventNumbers {
	public static final String version="$RCSfile: AbstractPipe.java,v $ $Revision: 1.28 $ $Date: 2007-06-08 07:58:14 $";
	protected Logger log = LogUtil.getLogger(this);

	private String name;
	
	private Hashtable pipeForwards=new Hashtable();
	private int maxThreads = 0;
	private ParameterList parameterList = new ParameterList();
	private long durationThreshold = -1;
	private String getInputFromSessionKey=null;
	private String getInputFromFixedValue=null;
	private String storeResultInSessionKey=null;
	private boolean preserveInput=false;
	private boolean namespaceAware=XmlUtils.isNamespaceAwareByDefault();
	private int transactionAttribute=JtaUtil.TRANSACTION_ATTRIBUTE_SUPPORTS;
 
	// METT event numbers
	private int beforeEvent=-1;
	private int afterEvent=-1;
	private int exceptionEvent=-1;

	private boolean active=true;
 
 
	/**
	 * <code>configure()</code> is called after the {@link nl.nn.adapterframework.core.PipeLine Pipeline} is registered
	 * at the {@link nl.nn.adapterframework.core.Adapter Adapter}. Purpose of this method is to reduce
	 * creating connections to databases etc. in the {@link #doPipe(Object) doPipe()} method.
	 * As much as possible class-instantiating should take place in the
	 * <code>configure()</code> method, to improve performance.
	 */ 
	public void configure() throws ConfigurationException {
		ParameterList params = getParameterList();
		if (params!=null) {
			try {
				params.configure();
			} catch (ConfigurationException e) {
				throw new ConfigurationException(getLogPrefix(null)+"while configuring parameters",e);
			}
		}
	}

	/**
	 * Extension for IExtendedPipe that calls configure(void) in its implementation. 
	 */
	public void configure(PipeLine pipeline) throws ConfigurationException {
		configure();
	}


	/**
	 * This is where the action takes place. Pipes may only throw a PipeRunException,
	 * to be handled by the caller of this object.
	 * @deprecated use {@link #doPipe(Object,PipeLineSession)} instead
	 */
	public PipeRunResult doPipe (Object input) throws PipeRunException {
		throw new PipeRunException(this, "Pipe should implement method doPipe()");
	}
	 
	/**
	 * This is where the action takes place. Pipes may only throw a PipeRunException,
	 * to be handled by the caller of this object.
	 */
	public PipeRunResult doPipe (Object input, PipeLineSession session) throws PipeRunException {
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
     * may come in handy for switcher-pipes.<br/.<br/>
     * @param forward   Name of the forward
     * @return PipeForward
     */
    public final PipeForward findForward(String forward){
    	if (StringUtils.isEmpty(forward)) {
    		return null;
    	}
        return (PipeForward)pipeForwards.get(forward);
    }
    
	/**
	 * Convenience method for building up log statements.
	 * This method may be called from within the <code>doPipe()</code> method with the current <code>PipeLineSession</code>
	 * as a parameter. Then it will use this parameter to retrieve the messageId. The method can be called with a <code>null</code> parameter
	 * from the <code>configure()</code>, <code>start()</code> and <code>stop()</code> methods.
	 * @return String with the name of the pipe and the message id of the current message.
	 */
	protected String getLogPrefix(PipeLineSession session){
		  StringBuffer sb=new StringBuffer();
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
	public void registerForward(PipeForward forward){
		if (pipeForwards.get(forward.getName())==null){
			pipeForwards.put(forward.getName(), forward);
		}
		else
			log.warn("PipeForward ["+forward.getName()+"] already registered for pipe ["+name+"] ignoring this one");
 	  
	}

	
	/**
	  * Perform necessary action to start the pipe. This method is executed
	  * after the {@link #configure()} method, for eacht start and stop command of the
	  * adapter.
	  */
	public void start() throws PipeStartException{
		if (getTransactionAttributeNum()>0 && getTransactionAttributeNum()!=JtaUtil.TRANSACTION_ATTRIBUTE_SUPPORTS) {
			try {
				// getUserTransaction, to make sure its available
				JtaUtil.getUserTransaction();
			} catch (NamingException e) {
				throw new PipeStartException(getLogPrefix(null)+"cannot obtain UserTransaction",e);
			}
		}
	}
	 /**
	  * Perform necessary actions to stop the <code>Pipe</code>.<br/>
	  * For instance, closing JMS connections, dbms connections etc.
	  */
	 public void stop() {}
    
	 /**
	  * The <code>toString()</code> method retrieves its value
	  * by reflection, so overriding this method is mostly not
	  * usefull.
	  * @see org.apache.commons.lang.builder.ToStringBuilder#reflectionToString
	  *
	  **/

  

    public String toString() {
		return ToStringBuilder.reflectionToString(this);
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

	/**
	 * Indicates the maximum number of treads ;that may call {@link #doPipe(Object, PipeLineSession)} simultaneously in case
	 *  A value of 0 indicates an unlimited number of threads.
	 */
	public void setMaxThreads(int newMaxThreads) {
	  maxThreads = newMaxThreads;
	}
	public int getMaxThreads() {
		return maxThreads;
	}

	/**
	 * The functional name of this pipe
	 */
	public void setName(String name) {
		this.name=name;
	}
	public String getName() {
	  return this.name;
	}

	/**
	 * Sets a threshold for the duration of message execution; 
	 * If the threshold is exceeded, the message is logged to be analyzed.
	 */
	public void setDurationThreshold(long maxDuration) {
		this.durationThreshold = maxDuration;
	}
	public long getDurationThreshold() {
		return durationThreshold;
	}





	public void setGetInputFromSessionKey(String string) {
		getInputFromSessionKey = string;
	}
	public String getGetInputFromSessionKey() {
		return getInputFromSessionKey;
	}

	public void setGetInputFromFixedValue(String string) {
		getInputFromFixedValue = string;
	}
	public String getGetInputFromFixedValue() {
		return getInputFromFixedValue;
	}

	public void setStoreResultInSessionKey(String string) {
		storeResultInSessionKey = string;
	}
	public String getStoreResultInSessionKey() {
		return storeResultInSessionKey;
	}

	public void setPreserveInput(boolean preserveInput) {
		this.preserveInput = preserveInput;
	}
	public boolean isPreserveInput() {
		return preserveInput;
	}

	
	public void setNamespaceAware(boolean b) {
		namespaceAware = b;
	}
	public boolean isNamespaceAware() {
		return namespaceAware;
	}


	// event numbers for tracing

	public int getAfterEvent() {
		return afterEvent;
	}

	public int getBeforeEvent() {
		return beforeEvent;
	}

	public int getExceptionEvent() {
		return exceptionEvent;
	}

	public void setAfterEvent(int i) {
		afterEvent = i;
	}

	public void setBeforeEvent(int i) {
		beforeEvent = i;
	}

	public void setExceptionEvent(int i) {
		exceptionEvent = i;
	}


	public void setTransactionAttribute(String attribute) throws ConfigurationException {
		transactionAttribute = JtaUtil.getTransactionAttributeNum(attribute);
		if (transactionAttribute<0) {
			throw new ConfigurationException("illegal value for transactionAttribute ["+attribute+"]");
		}
	}
	public String getTransactionAttribute() {
		return JtaUtil.getTransactionAttributeString(transactionAttribute);
	}

	public void setTransactionAttributeNum(int i) {
		transactionAttribute = i;
	}
	public int getTransactionAttributeNum() {
		return transactionAttribute;
	}

	public void setActive(boolean b) {
		active = b;
	}
	public boolean isActive() {
		return active;
	}


}
