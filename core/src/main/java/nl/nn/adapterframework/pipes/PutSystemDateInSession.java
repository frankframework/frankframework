/*
   Copyright 2013, 2019 Nationale-Nederlanden, 2020 WeAreFrank!

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
import nl.nn.adapterframework.stream.Message;
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
	private boolean getCurrentTimeStampInMillis = false;

	/**
	 * Checks whether the proper forward is defined, a dateFormat is specified and the dateFormat is valid.
	 */
	@Override
	public void configure() throws ConfigurationException {
		super.configure();

		// check the presence of a sessionKey
		if (getSessionKey() == null) {
			throw new ConfigurationException("has a null value for sessionKey");
		}
		// check the presence of a dateformat
		if (getDateFormat() == null) {
			throw new ConfigurationException("has a null value for dateFormat");
		}

		if (isReturnFixedDate()) {
			if (!ConfigurationUtils.isConfigurationStubbed(getConfigurationClassLoader())) {
				throw new ConfigurationException("returnFixedDate only allowed in stub mode");
			}
		}
		
		if(isGetCurrentTimeStampInMillis() && isReturnFixedDate()) {
			throw new ConfigurationException("returnFixedDate cannot be used to get current time stamp in millis");
		}
		// check the dateformat
		try {
			formatter = new SimpleDateFormat(getDateFormat());
		} catch (IllegalArgumentException ex){
			throw new ConfigurationException("has an illegal value for dateFormat", ex);
		}
		
		if (timeZone!=null) {
			formatter.setTimeZone(timeZone);
		}

	}

	@Override
	public PipeRunResult doPipe(Message message, IPipeLineSession session) throws PipeRunException {

		String formattedDate;
		if(isGetCurrentTimeStampInMillis()) {
			formattedDate = new Date().getTime()+"";
		}
		else {
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
			}
			else {
				if (sleepWhenEqualToPrevious > -1) {
					// Synchronize on a static value to generate unique value's for the
					// whole virtual machine.
					synchronized(OBJECT) {
						formattedDate = formatter.format(new Date());
						while (formattedDate.equals(previousFormattedDate)) {
							try {
								Thread.sleep(sleepWhenEqualToPrevious);
							} catch(InterruptedException e) {
								log.debug("interrupted");
							}
							formattedDate = formatter.format(new Date());
						}
						previousFormattedDate = formattedDate;
					}
				}
				else {
					formattedDate = formatter.format(new Date());
				}
			}
		}

		session.put(this.getSessionKey(), formattedDate);
	
		if (log.isDebugEnabled()) {
			log.debug(getLogPrefix(session) + "stored ["+ formattedDate	+ "] in pipeLineSession under key [" + getSessionKey() + "]");
		}

		return new PipeRunResult(getForward(), message);
	}
	
	@IbisDoc({"Key of session variable to store systemdate in", "systemdate"})
	public void setSessionKey(String newSessionKey) {
		sessionKey = newSessionKey;
	}
	public String getSessionKey() {
		return sessionKey;
	}
	
	@IbisDoc({"Format to store date in", "full ISO format: "+DateUtils.fullIsoFormat})
	public void setDateFormat(String rhs) {
		dateFormat = rhs;
	}
	public String getDateFormat() {
		return dateFormat;
	}
	
	@IbisDoc({"Time zone to use for the formatter", "the default time zone for the JVM"})
	public void setTimeZone(String timeZone) {
		this.timeZone = TimeZone.getTimeZone(timeZone);
	}

	@IbisDoc({"Set to a time in millisecond to create a value that is different to the previous returned value by a PutSystemDateInSession pipe in this virtual machine. The thread will sleep for the specified time before recalculating a new value. Set the timezone to a value without Daylight Saving Time (like GMT+1) to prevent this pipe to generate two equal value's when the clock is set back. <b>note:</b> When you're looking for a GUID parameter for your XSLT it might be better to use &lt;param name=&quot;guid&quot; pattern=&quot;{hostname}_{uid}&quot;/&gt;, see {@link nl.nn.adapterframework.parameters.Parameter}", "-1 (disabled)"})
	public void setSleepWhenEqualToPrevious(long sleepWhenEqualToPrevious) {
		this.sleepWhenEqualToPrevious = sleepWhenEqualToPrevious;
	}
	
	@IbisDoc({"If <code>true</code>, the date/time returned will always be "+FIXEDDATETIME+" (for testing purposes only). It is overridden by the value of the pipelinesession key <code>stub4testtool.fixeddate</code> when it exists", "<code>false</code>"})
	public void setReturnFixedDate(boolean b) {
		returnFixedDate = b;
	}
	public boolean isReturnFixedDate() {
		return returnFixedDate;
	}

	@IbisDoc({"If set to 'true' then current time stamp in millisecond will be stored in the sessionKey", "false"})
	public void setGetCurrentTimeStampInMillis(boolean getCurrentTimeStampInMillis) {
		this.getCurrentTimeStampInMillis = getCurrentTimeStampInMillis;
	}
	public boolean isGetCurrentTimeStampInMillis() {
		return getCurrentTimeStampInMillis;
	}
}

