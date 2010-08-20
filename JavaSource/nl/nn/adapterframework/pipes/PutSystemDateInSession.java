/*
 * $Log: PutSystemDateInSession.java,v $
 * Revision 1.9  2010-08-20 07:45:40  m168309
 * returnFixedDate attribute only available in stub mode
 *
 * Revision 1.8  2009/11/20 10:18:01  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * facility to override fixeddate
 *
 * Revision 1.7  2009/06/09 09:16:16  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * updated javadoc
 *
 * Revision 1.6  2009/06/09 08:35:15  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * added returnFixedDate attribute
 *
 * Revision 1.5  2006/08/30 12:33:15  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * set timezone only when set
 *
 * Revision 1.4  2006/08/22 12:55:06  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added timeZone and sleepWhenEqualToPrevious attributes
 *
 * Revision 1.3  2005/07/28 07:40:28  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * improved logging
 *
 * Revision 1.2  2004/11/10 12:58:17  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added default values for attributes + cosmetic changes
 *
 * Revision 1.1  2004/09/01 11:05:06  Johan Verrips <johan.verrips@ibissource.org>
 * Initial version
 *
 */
package nl.nn.adapterframework.pipes;

import nl.nn.adapterframework.util.DateUtils;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationUtils;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;

import java.util.Date;
import java.util.TimeZone;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import org.apache.commons.lang.StringUtils;

/**
 * Puts the system date/time under a key in the {@link nl.nn.adapterframework.core.PipeLineSession pipeLineSession}.
 *
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>{@link #setName(String) name}</td><td>name of the Pipe</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setSessionKey(String) sessionKey}</td><td>key of session variable to store result in</td><td>systemDate</td></tr>
 * <tr><td>{@link #setDateFormat(String) dateFormat}</td><td>format to store date in</td><td>fullIsoFormat: yyyy-MM-dd'T'hh:mm:sszzz</td></tr>
 * <tr><td>{@link #setTimeZone(String) timeZone}</td><td>the time zone to use for the formatter</td><td>the default time zone for the JVM</td></tr>
 * <tr><td>{@link #setSleepWhenEqualToPrevious(long) sleepWhenEqualToPrevious}</td><td>set to a time in millisecond to create a value that is different to the previous returned value by a PutSystemDateInSession pipe in this virtual machine. The thread will sleep for the specified time before recalculating a new value. Set the timezone to a value without daylight saving time (like GMT+1) to prevent this pipe to generate two equal value's when the clock is set back. <b>Note:</b> When you're looking for a guid parameter for you XSLT it might be better to use &lt;param name=&quot;guid&quot; pattern=&quot;{hostname}_{uid}&quot;/&gt;, see {@link nl.nn.adapterframework.parameters.Parameter}</a></td><td>-1 (disabled)</td></tr>
 * <tr><td>{@link #setReturnFixedDate(boolean) returnFixedDate}</td><td>If <code>true</code>, the date/time returned will always be December 17, 2001, 09:30:47 (for testing purposes only). It is overridden by the value of the pipeLineSession key <code>stub4testtool.fixeddate</code> when it exists</td><td><code>false</code></td></tr>
 * <tr><td>{@link #setMaxThreads(int) maxThreads}</td><td>maximum number of threads that may call {@link #doPipe(Object, nl.nn.adapterframework.core.PipeLineSession)} simultaneously</td><td>0 (unlimited)</td></tr>
 * <tr><td>{@link #setForwardName(String) forwardName}</td>  <td>name of forward returned upon completion</td><td>"success"</td></tr>
 * </table>
 * </p>
 * @version Id
 * @author  Johan Verrips
 * @author  Jaco de Groot (***@dynasol.nl)
 * @since   4.2c
 */
public class PutSystemDateInSession extends FixedForwardPipe {
	public static final String version="$RCSfile: PutSystemDateInSession.java,v $  $Revision: 1.9 $ $Date: 2010-08-20 07:45:40 $";

	public final static String FIXEDDATETIME  ="2001-12-17 09:30:47";
	public final static String FORMAT_FIXEDDATETIME  ="yyyy-MM-dd HH:mm:ss";
	public final static String FIXEDDATE_STUB4TESTTOOL_KEY  ="stub4testtool.fixeddate";

	private String sessionKey="systemDate";
	private String dateFormat=DateUtils.fullIsoFormat;
	private SimpleDateFormat formatter;
	private boolean returnFixedDate=false;
	private long sleepWhenEqualToPrevious = -1;
	private TimeZone timeZone=null;
	private String previousFormattedDate;

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

		if (isReturnFixedDate()) {
			if (!ConfigurationUtils.stubConfiguration()) {
				throw new ConfigurationException(getLogPrefix(null)+"returnFixedDate only allowed in stub mode");
			}
		}

		// check the dateformat
		try {
			Date currentDate = new Date();
			SimpleDateFormat formatter = new SimpleDateFormat(getDateFormat());
			
		} catch (IllegalArgumentException ex){
			throw new ConfigurationException(getLogPrefix(null)+"has an illegal value for dateFormat", ex);
		}

		formatter = new SimpleDateFormat(getDateFormat());
		if (timeZone!=null) {
			formatter.setTimeZone(timeZone);
		}

	}

	public PipeRunResult doPipe(Object input, PipeLineSession session)
		throws PipeRunException {

		Date d;
		if (isReturnFixedDate()) {
			SimpleDateFormat formatterFrom = new SimpleDateFormat(FORMAT_FIXEDDATETIME);
			String fixedDateTime = (String)session.get(FIXEDDATE_STUB4TESTTOOL_KEY);
			if (StringUtils.isEmpty(fixedDateTime)) {
				fixedDateTime = FIXEDDATETIME;
			}
			try {
				d = formatterFrom.parse(fixedDateTime);
			} catch (ParseException e) {
				throw new PipeRunException(this,"cannot parse fixed date ["+fixedDateTime+"] with format ["+FORMAT_FIXEDDATETIME+"]",e);
			}
		} else{
			d = new Date();
		}
		String formattedDate = formatter.format(d);

		if (sleepWhenEqualToPrevious > -1) {
			// Synchronize on a static value to generate unique value's for the
			// whole virtual machine.
			synchronized (version) {
				while (formattedDate.equals(previousFormattedDate)) {
					try {
						Thread.sleep(sleepWhenEqualToPrevious);
					} catch(InterruptedException e) {
					}
					formattedDate = formatter.format(new Date());
				}
				previousFormattedDate = formattedDate;
			}
		}

		session.put(this.getSessionKey(), formattedDate);		
		
		if (log.isDebugEnabled()) {
			log.debug(getLogPrefix(session) + "stored ["+ formattedDate	+ "] in pipeLineSession under key [" + getSessionKey() + "]");
		}

		return new PipeRunResult(getForward(), input);
	}
	
	/**
	 * The name of the key in the <code>PipeLineSession</code> to store the systemdate in
	 * @see nl.nn.adapterframework.core.PipeLineSession
	 */
	public String getSessionKey() {
		return sessionKey;
	}
	/**
	 * The name of the key in the <code>PipeLineSession</code> to store the systemdate in
	 * @see nl.nn.adapterframework.core.PipeLineSession
	 */
	public void setSessionKey(String newSessionKey) {
		sessionKey = newSessionKey;
	}

	
	/**
	 * The String for the DateFormat.
	 * @see java.text.SimpleDateFormat
	 */
	public void setDateFormat(String rhs) {
		dateFormat = rhs;
	}
	public String getDateFormat() {
		return dateFormat;
	}
	
	public void setTimeZone(String timeZone) {
		this.timeZone = TimeZone.getTimeZone(timeZone);
	}

	public void setSleepWhenEqualToPrevious(long sleepWhenEqualToPrevious) {
		this.sleepWhenEqualToPrevious = sleepWhenEqualToPrevious;
	}
	
	public void setReturnFixedDate(boolean b) {
		returnFixedDate = b;
	}

	public boolean isReturnFixedDate() {
		return returnFixedDate;
	}
}

