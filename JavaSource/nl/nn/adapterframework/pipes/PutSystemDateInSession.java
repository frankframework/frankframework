/*
 * $Log: PutSystemDateInSession.java,v $
 * Revision 1.2  2004-11-10 12:58:17  L190409
 * added default values for attributes + cosmetic changes
 *
 * Revision 1.1  2004/09/01 11:05:06  Johan Verrips <johan.verrips@ibissource.org>
 * Initial version
 *
 */
package nl.nn.adapterframework.pipes;
import nl.nn.adapterframework.util.DateUtils;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;

import java.util.Date;
import java.text.SimpleDateFormat;

/**
 * Puts the system date/time under a key in the {@link nl.nn.adapterframework.core.PipeLineSession pipeLineSession}.
 *
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>{@link #setName(String) name}</td><td>name of the Pipe</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setSessionKey(String) sessionKey}</td><td>key of session variable to store result in</td><td>systemDate</td></tr>
 * <tr><td>{@link #setDateFormat(String) dateFormat}</td><td>format to store date in</td><td>fullIsoFormat: yyyy-MM-dd'T'hh:mm:sszzz</td></tr>
 * <tr><td>{@link #setMaxThreads(int) maxThreads}</td><td>maximum number of threads that may call {@link #doPipe(Object, nl.nn.adapterframework.core.PipeLineSession)} simultaneously</td><td>0 (unlimited)</td></tr>
 * <tr><td>{@link #setForwardName(String) forwardName}</td>  <td>name of forward returned upon completion</td><td>"success"</td></tr>
 * </table>
 * </p>
 * @version Id
 * @author Johan Verrips
 * @since 4.2c
 */
public class PutSystemDateInSession extends FixedForwardPipe {
	public static final String version = "$Id: PutSystemDateInSession.java,v 1.2 2004-11-10 12:58:17 L190409 Exp $";
	private String sessionKey="systemDate";
	private String dateFormat=DateUtils.fullIsoFormat;

	/**
	 * checks wether the proper forward is defined, a dateformat is specified and the dateformat is valid.
	 * @throws ConfigurationException
	 */
	public void configure() throws ConfigurationException {
		super.configure();

		// check the presence of a sessionKey
		if (getSessionKey() == null) {
			throw new ConfigurationException(getLogPrefix(null)+"has a null value for sessionKey");
		}
		// check the presence of a dateformat
		if (getDateFormat() == null) {
			throw new ConfigurationException(getLogPrefix(null)+"has a null value for dateFormat");
		}
		
		// check the dateformat
		try {
			Date currentDate = new Date();
			SimpleDateFormat formatter = new SimpleDateFormat(getDateFormat());
			
		} catch (IllegalArgumentException ex){
			throw new ConfigurationException(getLogPrefix(null)+"has an illegal value for dateFormat", ex);
		}

	}

	public PipeRunResult doPipe(Object input, PipeLineSession session)
		throws PipeRunException {

		Date currentDate = new Date();
		SimpleDateFormat formatter = new SimpleDateFormat(getDateFormat());
		session.put(this.getSessionKey(), formatter.format(currentDate));		
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

