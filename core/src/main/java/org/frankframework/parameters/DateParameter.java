/*
   Copyright 2024 WeAreFrank!

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
package org.frankframework.parameters;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import jakarta.annotation.Nonnull;

import org.apache.commons.lang3.StringUtils;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.configuration.ConfigurationWarning;
import org.frankframework.core.ParameterException;
import org.frankframework.stream.Message;
import org.frankframework.util.DateFormatUtils;
import org.frankframework.util.EnumUtils;
import org.frankframework.util.XmlUtils;

@Log4j2
public class DateParameter extends AbstractParameter {
	public static final String TYPE_DATE_PATTERN="yyyy-MM-dd";
	public static final String TYPE_TIME_PATTERN="HH:mm:ss";
	public static final String TYPE_DATETIME_PATTERN="yyyy-MM-dd HH:mm:ss";
	public static final String TYPE_TIMESTAMP_PATTERN= DateFormatUtils.FORMAT_FULL_GENERIC;

	private @Getter String formatString = null;
	private DateFormatType formatType;

	public DateParameter() {
		setFormatType(DateFormatType.DATE); // Defaults to Date
	}

	public enum DateFormatType {
		DATE, DATETIME, TIMESTAMP, TIME, UNIX, XMLDATETIME
	}

	@Override
	public void configure() throws ConfigurationException {
		if(StringUtils.isEmpty(getFormatString())) {
			switch(formatType) {
				case DATE:
					setFormatString(TYPE_DATE_PATTERN);
					break;
				case DATETIME:
					setFormatString(TYPE_DATETIME_PATTERN);
					break;
				case TIMESTAMP:
					setFormatString(TYPE_TIMESTAMP_PATTERN);
					break;
				case TIME:
					setFormatString(TYPE_TIME_PATTERN);
					break;
				default:
					break;
			}
		}

		if(formatType != DateFormatType.XMLDATETIME) {
			try {
				new SimpleDateFormat(getFormatString());
			} catch (IllegalArgumentException e) {
				throw new ConfigurationException("invalid formatString [" + getFormatString() + "]", e);
			}
		}

		super.configure();
	}

	@Override
	protected Date getValueAsType(@Nonnull Message request, boolean namespaceAware) throws ParameterException, IOException {
		if (request.asObject() instanceof Date date) {
			return date;
		}

		if(formatType == DateFormatType.XMLDATETIME) {
			log.debug("Parameter [{}] converting result [{}] from XML dateTime to Date", this::getName, () -> request);
			return XmlUtils.parseXmlDateTime(request.asString());
		}

		if (formatType == DateFormatType.UNIX) {
			log.debug("Parameter [{}] interpreting result [{}] as UNIX timestamp", this::getName, () -> request);
			try {
				String value = request.asString().trim();
				long epoch = Long.parseLong(value);

				// Heuristic: if the number is less than 10^12, treat as seconds
				if (epoch < 1_000_000_000_000L) {
					epoch *= 1000; // convert seconds to milliseconds
				}

				return new Date(epoch);
			} catch (NumberFormatException e) {
				throw new ParameterException(getName(), "Parameter [" + getName() + "] could not parse UNIX timestamp from [" + request + "]", e);
			}
		}

		log.debug("Parameter [{}] converting result [{}] to Date using formatString [{}]", this::getName, () -> request, this::getFormatString);
		DateFormat df = new SimpleDateFormat(getFormatString());
		try {
			return df.parse(request.asString());
		} catch (ParseException e) {
			throw new ParameterException(getName(), "Parameter [" + getName() + "] could not parse result [" + request + "] to Date using formatString [" + getFormatString() + "]", e);
		}
	}

	@Override
	@Deprecated
	@ConfigurationWarning("use element DateParameter with attribute formatType instead")
	public void setType(ParameterType type) {
		this.formatType = EnumUtils.parse(DateFormatType.class, type.name());
		super.setType(type);
	}

	/**
	 * Used in combination with types <code>DATE</code>, <code>TIME</code>, <code>DATETIME</code> and <code>TIMESTAMP</code> to parse the raw parameter string data into an object of the respective type
	 * @ff.default depends on type
	 */
	public void setFormatString(String string) {
		formatString = string;
	}

	public void setFormatType(DateFormatType formatType) {
		this.formatType = formatType;
		super.setType(EnumUtils.parse(ParameterType.class, formatType.name()));
	}
}
