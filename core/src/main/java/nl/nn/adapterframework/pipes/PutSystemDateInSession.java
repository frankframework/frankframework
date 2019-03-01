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
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.util.DateUtils;

import org.apache.commons.lang.StringUtils;

/**
 * Puts the system date/time under a key in the {@link IPipeLineSession pipeLineSession}.
 *
 * @author  Johan Verrips
 * @author  Jaco de Groot (***@dynasol.nl)
 * @since   4.2c
 */
public class PutSystemDateInSession extends FixedForwardPipe {
	public final static Object OBJECT = new Object();
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
	 * Checks whether the proper forward is defined, a dateFormat is specified and the dateFormat is valid.
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
				synchronized(OBJECT) {
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
	 * This method gets the name of the key in the <code>PipeLineSession</code> to store the systemdate in
	 * @see IPipeLineSession
	 */
	public String getSessionKey() {
		return sessionKey;
	}
	/**
	 * This method sets the name of the key in the <code>PipeLineSession</code> to store the systemdate in
	 * @see IPipeLineSession
	 */
	@IbisDoc({"key of session variable to store result in", "systemdate"})
	public void setSessionKey(String newSessionKey) {
		sessionKey = newSessionKey;
	}
	
	/**
	 * The String for the DateFormat.
	 * @see SimpleDateFormat
	 */
	@IbisDoc({"format to store date in", "fullisoformat: yyyy-mm-dd't'hh:mm:sszzz"})
	public void setDateFormat(String rhs) {
		dateFormat = rhs;
	}
	public String getDateFormat() {
		return dateFormat;
	}
	
	@IbisDoc({"the time zone to use for the formatter", "the default time zone for the jvm"})
	public void setTimeZone(String timeZone) {
		this.timeZone = TimeZone.getTimeZone(timeZone);
	}

	@IbisDoc({"set to a time in millisecond to create a value that is different to the previous returned value by a putsystemdateinsession pipe in this virtual machine. the thread will sleep for the specified time before recalculating a new value. set the timezone to a value without daylight saving time (like gmt+1) to prevent this pipe to generate two equal value's when the clock is set back. <b>note:</b> when you're looking for a guid parameter for you xslt it might be better to use &lt;param name=&quot;guid&quot; pattern=&quot;{hostname}_{uid}&quot;/&gt;, see {@link nl.nn.adapterframework.parameters.parameter}</a>", "-1 (disabled)"})
	public void setSleepWhenEqualToPrevious(long sleepWhenEqualToPrevious) {
		this.sleepWhenEqualToPrevious = sleepWhenEqualToPrevious;
	}
	
	@IbisDoc({"if <code>true</code>, the date/time returned will always be december 17, 2001, 09:30:47 (for testing purposes only). it is overridden by the value of the pipelinesession key <code>stub4testtool.fixeddate</code> when it exists", "<code>false</code>"})
	public void setReturnFixedDate(boolean b) {
		returnFixedDate = b;
	}

	public boolean isReturnFixedDate() {
		return returnFixedDate;
	}
}

