/*
 * $Log: PutSystemDateInSession.java,v $
 * Revision 1.1  2004-09-01 11:05:06  NNVZNL01#L180564
 * Initial version
 *
 *
 */
package nl.nn.adapterframework.pipes;
import nl.nn.adapterframework.pipes.FixedForwardPipe;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;

import java.util.Date;
import java.text.SimpleDateFormat;

/**
 * Puts the system date/time under a key in the {@link nl.nn.adapterframework.core.PipeLineSession pipeLineSession}.
 * Initial version from Richard Punt.
 * @version Id
 * @author Johan Verrips
 * @since 4.2c
 */
public class PutSystemDateInSession extends FixedForwardPipe {
	public static final String version = "$Id: PutSystemDateInSession.java,v 1.1 2004-09-01 11:05:06 NNVZNL01#L180564 Exp $";
	private String sessionKey;
	private String dateFormat;

	/**
	 * checks wether the proper forward is defined, a dateformat is specified and the dateformat is valid.
	 * @throws ConfigurationException
	 */
	public void configure() throws ConfigurationException {
		super.configure();

		// check the presence of a sessionKey
		if (getSessionKey() == null) {
			throw new ConfigurationException(
				"Pipe ["
					+ getName()
					+ "]"
					+ " has a null value for sessionKey");
		}
		// check the presence of a dateformat
		if (getDateFormat() == null) {
			throw new ConfigurationException(
				"Pipe ["
					+ getName()
					+ "]"
					+ " has a null value for dateFormat");
		}
		
		// check the dateformat
		try {
			Date currentDate = new Date();
			SimpleDateFormat formatter = new SimpleDateFormat(getDateFormat());
			
		} catch (IllegalArgumentException ex){
			throw new ConfigurationException(
			"Pipe ["
					+ getName()
					+ "]"
					+ " has an illegal value for dateFormat", ex);
		}

	}

	public PipeRunResult doPipe(Object input, PipeLineSession session)
		throws PipeRunException {

		Date currentDate = new Date();
		SimpleDateFormat formatter = new SimpleDateFormat(getDateFormat());
		session.put(this.getSessionKey(), formatter.format(currentDate));		

		log.debug(
			getLogPrefix(session)
				+ "stored ["
				+ input.toString()
				+ "] in pipeLineSession under key ["
				+ getSessionKey()
				+ "]");

		return new PipeRunResult(getForward(), input);
	}
/**
 * The name of the key in the <code>PipeLineSession</code> to store the input in
 * @see nl.nn.adapterframework.core.PipeLineSession
 */
public String getSessionKey() {
	return sessionKey;
}
/**
 * The name of the key in the <code>PipeLineSession</code> to store the input in
 * @see nl.nn.adapterframework.core.PipeLineSession
 */
public void setSessionKey(String newSessionKey) {
	sessionKey = newSessionKey;
}

/**
 * The String for the DateFormat
 * @see java.text.SimpleDateFormat
 * @return
 */
public String getDateFormat() {
	return dateFormat;
}

/**
 * The String for the DateFormat
 * @see java.text.SimpleDateFormat
 * @return
 */
public void setDateFormat(String rhs) {
	dateFormat = rhs;
}


	
}

