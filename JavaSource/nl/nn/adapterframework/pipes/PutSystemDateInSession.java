/*
   Copyright 2013 Nationale-Nederlanden

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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationUtils;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.util.DateUtils;

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
 * @version $Id$
 * @author  Johan Verrips
 * @author  Jaco de Groot (***@dynasol.nl)
 * @since   4.2c
 */
public class PutSystemDateInSession extends FixedForwardPipe {
	public static final String version="$RCSfile: PutSystemDateInSession.java,v $  $Revision: 1.14 $ $Date: 2012-06-01 10:52:50 $";

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

	public PipeRunResult doPipe(Object input, IPipeLineSession session)
		throws PipeRunException {

		String formattedDate;
		if (isReturnFixedDate()) {
			SimpleDateFormat formatterFrom = new SimpleDateFormat(FORMAT_FIXEDDATETIME);
			String fixedDateTime = (String)session.get(FIXEDDATE_STUB4TESTTOOL_KEY);
			if (StringUtils.isEmpty(fixedDateTime)) {
				fixedDateTime = FIXEDDATETIME;
			}
			Date d;
			try {
				d = formatterFrom.parse(fixedDateTime);
			} catch (ParseException e) {
				throw new PipeRunException(this,"cannot parse fixed date ["+fixedDateTime+"] with format ["+FORMAT_FIXEDDATETIME+"]",e);
			}
			formattedDate = formatter.format(d);
		} else{
			if (sleepWhenEqualToPrevious > -1) {
				// Synchronize on a static value to generate unique value's for the
				// whole virtual machine.
				synchronized (version) {
					formattedDate = formatter.format(new Date());
					while (formattedDate.equals(previousFormattedDate)) {
						try {
							Thread.sleep(sleepWhenEqualToPrevious);
						} catch(InterruptedException e) {
						}
						formattedDate = formatter.format(new Date());
					}
					previousFormattedDate = formattedDate;
				}
			} else {
				formattedDate = formatter.format(new Date());
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

