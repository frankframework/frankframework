/*
 * $Log: LogSender.java,v $
 * Revision 1.1  2005-06-20 09:05:38  europe\L190409
 * introduction of LogSender
 *
 */
package nl.nn.adapterframework.pipes;

import java.util.Iterator;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.SenderWithParametersBase;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;

/**
 * Sender that just logs its message.
 * 
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>{@link #setLogLevel(String) logLevel}</td><td>level on which messages are logged</td><td>info</td></tr>
 * <tr><td>{@link #setLogCategory(String) logCategory}</td><td>category under which messages are logged</td><td>name of the sender</td></tr>
 * </table>
 * 
 * @author Gerrit van Brakel
 * @since  4.3
 * @version Id
 */
public class LogSender extends SenderWithParametersBase {
	public static final String version="$RCSfile: LogSender.java,v $ $Revision: 1.1 $ $Date: 2005-06-20 09:05:38 $";
	
	private String logLevel="info";
	private String logCategory=null;

	protected Level level;
	protected Logger log;

	public void configure() throws ConfigurationException {
		super.configure();
		log=Logger.getLogger(getLogCategory());
		level=Level.toLevel(getLogLevel());
	}

	public boolean isSynchronous() {
		return false;
	}

	public String sendMessage(String correlationID, String message, ParameterResolutionContext prc) throws SenderException, TimeOutException {
		log.log(level,message);
		if (paramList!=null) {
			for (Iterator it=paramList.iterator(); it.hasNext(); ) {
				Parameter p = (Parameter)it.next();
				try {
					log.log(level,"parameter ["+p.getName()+"] value ["+p.getValue(prc)+"]");
				} catch (ParameterException e) {
					throw new SenderException("exception determining value of parameter ["+p.getName()+"]");
				}
			}
		}
		
		return correlationID;
	}


	public String getLogCategory() {
		if (StringUtils.isNotEmpty(logCategory)) {
			return logCategory;
		}
		if (StringUtils.isNotEmpty(getName())) {
			return getName();
		}
		return this.getClass().getName();
	}

	public void setLogCategory(String string) {
		logCategory = string;
	}

	public String getLogLevel() {
		return logLevel;
	}

	public void setLogLlevel(String string) {
		logLevel = string;
	}

	public String toString() {
		return "LogSender ["+getName()+"] logLevel ["+getLogLevel()+"] logCategory ["+logCategory+"]";
	}


}
