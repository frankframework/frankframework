/*
 * $Log: LogSender.java,v $
 * Revision 1.6  2007-09-13 09:09:43  europe\L190409
 * return message instead of correlationid
 *
 * Revision 1.5  2007/02/12 14:02:19  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * Logger from LogUtil
 *
 * Revision 1.4  2006/06/14 09:50:25  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * avoid null pointer exception when prc==null
 *
 * Revision 1.3  2005/12/28 08:38:15  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * corrected typo in attributename
 *
 * Revision 1.2  2005/10/24 09:59:24  John Dekker <john.dekker@ibissource.org>
 * Add support for pattern parameters, and include them into several listeners,
 * senders and pipes that are file related
 *
 * Revision 1.1  2005/06/20 09:05:38  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * introduction of LogSender
 *
 */
package nl.nn.adapterframework.pipes;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.SenderWithParametersBase;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.parameters.IParameterHandler;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.util.LogUtil;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

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
public class LogSender extends SenderWithParametersBase implements IParameterHandler {
	public static final String version="$RCSfile: LogSender.java,v $ $Revision: 1.6 $ $Date: 2007-09-13 09:09:43 $";
	
	private String logLevel="info";
	private String logCategory=null;

	protected Level level;
	protected Logger log;

	public void configure() throws ConfigurationException {
		super.configure();
		log=LogUtil.getLogger(getLogCategory());
		level=Level.toLevel(getLogLevel());
	}

	public boolean isSynchronous() {
		return true;
	}

	public String sendMessage(String correlationID, String message, ParameterResolutionContext prc) throws SenderException, TimeOutException {
		log.log(level,message);
		
		if (prc != null) {
			try {
				prc.forAllParameters(paramList, this);
			} catch (ParameterException e) {
				throw new SenderException("exception determining value of parameters", e);
			}
		}
		
		return message;
	}

	public void handleParam(String paramName, Object value) {
		log.log(level,"parameter [" + paramName + "] value [" + value + "]");
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

	public void setLogLevel(String string) {
		logLevel = string;
	}

	public String toString() {
		return "LogSender ["+getName()+"] logLevel ["+getLogLevel()+"] logCategory ["+logCategory+"]";
	}


}
