/*
 * $Log: AbstractPipe.java,v $
 * Revision 1.10  2005-08-24 15:52:16  europe\L190409
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

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IPipe;
import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.core.PipeStartException;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.ParameterList;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.log4j.Logger;

import java.util.Hashtable;

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
 * <tr><td>{@link #setName(String) name}</td><td>name of the Pipe</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setMaxThreads(int) maxThreads}</td><td>maximum number of threads that may call {@link #doPipe(Object, PipeLineSession)} simultaneously</td><td>0 (unlimited)</td></tr>
 * </table>
 * </p>
 * @version Id
 * @author     Johan Verrips
 *
 * @see nl.nn.adapterframework.core.PipeLineSession
 */
public abstract class AbstractPipe implements IPipe {
  public static final String version="$RCSfile: AbstractPipe.java,v $ $Revision: 1.10 $ $Date: 2005-08-24 15:52:16 $";
  private String name;
  protected Logger log = Logger.getLogger(this.getClass());
  private Hashtable pipeForwards=new Hashtable();
  private int maxThreads = 0;
  private ParameterList parameterList = new ParameterList();
  
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


}
