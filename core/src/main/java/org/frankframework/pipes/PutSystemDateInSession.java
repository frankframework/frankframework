/*
   Copyright 2013, 2019 Nationale-Nederlanden, 2020, 2022-2023 WeAreFrank!

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
package org.frankframework.pipes;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.TimeZone;

import org.apache.commons.lang3.StringUtils;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.configuration.ConfigurationUtils;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.doc.EnterpriseIntegrationPattern;
import org.frankframework.parameters.Parameter;
import org.frankframework.stream.Message;
import org.frankframework.util.DateFormatUtils;

/**
 * Puts the system date/time under a key in the {@link PipeLineSession pipeLineSession}.
 *
 * @author  Johan Verrips
 * @author  Jaco de Groot (***@dynasol.nl)
 * @since   4.2c
 */
@EnterpriseIntegrationPattern(EnterpriseIntegrationPattern.Type.SESSION)
public class PutSystemDateInSession extends FixedForwardPipe {
	public static final Object OBJECT = new Object();
	public static final String FIXEDDATETIME  ="2001-12-17 09:30:47";
	public static final String FIXEDDATE_STUB4TESTTOOL_KEY  ="stub4testtool.fixeddate";

	private String sessionKey="systemDate";
	private String dateFormat= DateFormatUtils.FORMAT_FULL_ISO;
	private DateTimeFormatter formatter;
	private boolean returnFixedDate=false;
	private long sleepWhenEqualToPrevious = -1;
	private TimeZone timeZone=null;
	private ZoneId zoneId=null;
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

		if (isReturnFixedDate() && (!ConfigurationUtils.isConfigurationStubbed(getConfigurationClassLoader()))) {
			throw new ConfigurationException("returnFixedDate only allowed in stub mode");
		}

		if(isGetCurrentTimeStampInMillis() && isReturnFixedDate()) {
			throw new ConfigurationException("returnFixedDate cannot be used to get current time stamp in millis");
		}
		if (timeZone == null) {
			zoneId = ZoneId.systemDefault();
		} else {
			zoneId = timeZone.toZoneId();
		}
		// check the dateformat
		try {
			formatter = DateTimeFormatter.ofPattern(getDateFormat()).withZone(zoneId);
		} catch (IllegalArgumentException ex){
			throw new ConfigurationException("has an illegal value for dateFormat", ex);
		}
	}

	@Override
	public PipeRunResult doPipe(Message message, PipeLineSession session) throws PipeRunException {

		String formattedDate;
		if(isGetCurrentTimeStampInMillis()) {
			formattedDate = System.currentTimeMillis()+"";
		} else {
			if (isReturnFixedDate()) {
				DateTimeFormatter formatterFrom = DateFormatUtils.GENERIC_DATETIME_FORMATTER.withZone(zoneId);
				String fixedDateTime = session.getString(FIXEDDATE_STUB4TESTTOOL_KEY);
				if (StringUtils.isEmpty(fixedDateTime)) {
					fixedDateTime = FIXEDDATETIME;
				}
				Instant instant;
				try {
					instant = Instant.from(formatterFrom.parse(fixedDateTime));
				} catch (Exception e) {
					throw new PipeRunException(this,"cannot parse fixed date ["+fixedDateTime+"] with format ["+ DateFormatUtils.FORMAT_DATETIME_GENERIC +"]",e);
				}
				formattedDate = formatter.format(instant);
			} else {
				if (sleepWhenEqualToPrevious > -1) {
					// Synchronize on a static value to generate unique value's for the
					// whole virtual machine.
					synchronized(OBJECT) {
						formattedDate = formatter.format(Instant.now());
						while (formattedDate.equals(previousFormattedDate)) {
							try {
								OBJECT.wait(sleepWhenEqualToPrevious);
							} catch(InterruptedException e) {
								log.debug("interrupted");
								Thread.currentThread().interrupt();
							}
							formattedDate = formatter.format(Instant.now());
						}
						previousFormattedDate = formattedDate;
					}
				} else {
					formattedDate = formatter.format(Instant.now());
				}
			}
		}

		session.put(this.getSessionKey(), formattedDate);
		log.debug("stored [{}] in pipeLineSession under key [{}]", formattedDate, getSessionKey());
		return new PipeRunResult(getSuccessForward(), message);
	}

	/**
	 * Key of session variable to store systemdate in
	 * @ff.default systemDate
	 */
	public void setSessionKey(String newSessionKey) {
		sessionKey = newSessionKey;
	}
	public String getSessionKey() {
		return sessionKey;
	}

	/**
	 * Format to store date in
	 * @ff.default full ISO format: DateUtils.fullIsoFormat
	 */
	public void setDateFormat(String rhs) {
		dateFormat = rhs;
	}
	public String getDateFormat() {
		return dateFormat;
	}

	/**
	 * Time zone to use for the formatter
	 * @ff.default the default time zone for the JVM
	 */
	public void setTimeZone(String timeZone) {
		this.timeZone = TimeZone.getTimeZone(timeZone);
	}

	/**
	 * Set to a time <i>in milliseconds</i> to create a value that is different to the previous returned value by a PutSystemDateInSession pipe in
	 * this virtual machine or {@code -1} to disable. The thread will sleep for the specified time before recalculating a new value. Set the
	 * timezone to a value without Daylight Saving Time (like GMT+1) to prevent this pipe to generate two equal value's when the clock is set back.
	 * <b>note:</b> When you're looking for a GUID parameter for your XSLT it might be better to use
	 * &lt;param name=&quot;guid&quot; pattern=&quot;{hostname}_{uid}&quot;/&gt;, see {@link Parameter}.
	 * @ff.default -1
	 */
	public void setSleepWhenEqualToPrevious(long sleepWhenEqualToPrevious) {
		this.sleepWhenEqualToPrevious = sleepWhenEqualToPrevious;
	}

	/**
	 * If {@code true}, the date/time returned will always be {@value #FIXEDDATETIME} (for testing purposes only). It is overridden by the value of the pipelinesession key <code>stub4testtool.fixeddate</code> when it exists
	 * @ff.default false
	 */
	public void setReturnFixedDate(boolean b) {
		returnFixedDate = b;
	}
	public boolean isReturnFixedDate() {
		return returnFixedDate;
	}

	/**
	 * If set to 'true' then current time stamp in millisecond will be stored in the sessionKey
	 * @ff.default false
	 */
	public void setGetCurrentTimeStampInMillis(boolean getCurrentTimeStampInMillis) {
		this.getCurrentTimeStampInMillis = getCurrentTimeStampInMillis;
	}
	public boolean isGetCurrentTimeStampInMillis() {
		return getCurrentTimeStampInMillis;
	}
}
