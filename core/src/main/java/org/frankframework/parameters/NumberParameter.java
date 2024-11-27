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

import static org.frankframework.functional.FunctionalUtil.logValue;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;

import jakarta.annotation.Nonnull;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.ParameterException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.stream.Message;

@Log4j2
public class NumberParameter extends AbstractParameter {

	private @Getter String decimalSeparator = null;
	private @Getter String groupingSeparator = null;
	private @Getter String minInclusiveString = null;
	private @Getter String maxInclusiveString = null;
	private Number minInclusive;
	private Number maxInclusive;

	private @Getter DecimalFormatSymbols decimalFormatSymbols = new DecimalFormatSymbols();

	public NumberParameter() {
		setType(ParameterType.INTEGER); // Defaults to integer
	}

	@Override
	public void configure() throws ConfigurationException {
		if (StringUtils.isNotEmpty(getDecimalSeparator()) || StringUtils.isNotEmpty(getGroupingSeparator())) {
			setType(ParameterType.NUMBER);

			if (StringUtils.isNotEmpty(getDecimalSeparator())) {
				decimalFormatSymbols.setDecimalSeparator(getDecimalSeparator().charAt(0));
			}
			if (StringUtils.isNotEmpty(getGroupingSeparator())) {
				decimalFormatSymbols.setGroupingSeparator(getGroupingSeparator().charAt(0));
			}
		}

		if (StringUtils.isNotEmpty(getMinInclusiveString()) || StringUtils.isNotEmpty(getMaxInclusiveString())) {
			setType(ParameterType.NUMBER);
			if (getMinInclusiveString() != null) {
				DecimalFormat df = new DecimalFormat();
				df.setDecimalFormatSymbols(decimalFormatSymbols);
				try {
					minInclusive = df.parse(getMinInclusiveString());
				} catch (ParseException e) {
					throw new ConfigurationException("Attribute [minInclusive] could not parse result ["+getMinInclusiveString()+"] to number; decimalSeparator ["+decimalFormatSymbols.getDecimalSeparator()+"] groupingSeparator ["+decimalFormatSymbols.getGroupingSeparator()+"]",e);
				}
			}
			if (getMaxInclusiveString() != null) {
				DecimalFormat df = new DecimalFormat();
				df.setDecimalFormatSymbols(decimalFormatSymbols);
				try {
					maxInclusive = df.parse(getMaxInclusiveString());
				} catch (ParseException e) {
					throw new ConfigurationException("Attribute [maxInclusive] could not parse result ["+getMaxInclusiveString()+"] to number; decimalSeparator ["+decimalFormatSymbols.getDecimalSeparator()+"] groupingSeparator ["+decimalFormatSymbols.getGroupingSeparator()+"]",e);
				}
			}
		}

		super.configure();
	}

	@Override
	public Object getValue(ParameterValueList alreadyResolvedParameters, Message message, PipeLineSession session, boolean namespaceAware) throws ParameterException {
		Object result = super.getValue(alreadyResolvedParameters, message, session, namespaceAware);

		if (result instanceof Number number) {
			if (getMinInclusiveString() != null && number.floatValue() < minInclusive.floatValue()) {
				log.debug("Replacing parameter [{}] because value [{}] falls short of minInclusive [{}]", this::getName, logValue(result), this::getMinInclusiveString);
				result = minInclusive;
			}
			if (getMaxInclusiveString() != null && number.floatValue() > maxInclusive.floatValue()) {
				log.debug("Replacing parameter [{}] because value [{}] exceeds maxInclusive [{}]", this::getName, logValue(result), this::getMaxInclusiveString);
				result = maxInclusive;
			}
		}

		// This turns the result type into a String
		if (getMinLength() >= 0 && (result+"").length() < getMinLength()) {
			log.debug("Adding leading zeros to parameter [{}]", this::getName);
			result = StringUtils.leftPad(result+"", getMinLength(), '0');
		}

		return result;
	}

	@Override
	protected Number getValueAsType(@Nonnull Message request, boolean namespaceAware) throws ParameterException, IOException {
		if(getType() == ParameterType.NUMBER) {
			if (request.asObject() instanceof Number number) {
				return number;
			}
			log.debug("Parameter [{}] converting result [{}] to number decimalSeparator [{}] groupingSeparator [{}]", this::getName, ()->request, decimalFormatSymbols::getDecimalSeparator, decimalFormatSymbols::getGroupingSeparator);
			DecimalFormat decimalFormat = new DecimalFormat();
			decimalFormat.setDecimalFormatSymbols(decimalFormatSymbols);
			try {
				return decimalFormat.parse(request.asString());
			} catch (ParseException e) {
				throw new ParameterException(getName(), "Parameter [" + getName() + "] could not parse result [" + request + "] to number decimalSeparator [" + decimalFormatSymbols.getDecimalSeparator() + "] groupingSeparator [" + decimalFormatSymbols.getGroupingSeparator() + "]", e);
			}
		}
		if(getType() == ParameterType.INTEGER) {
			if (request.asObject() instanceof Integer integer) {
				return integer;
			}
			log.debug("Parameter [{}] converting result [{}] to integer", this::getName, ()->request);
			try {
				return Integer.parseInt(request.asString());
			} catch (NumberFormatException e) {
				throw new ParameterException(getName(), "Parameter [" + getName() + "] could not parse result [" + request + "] to integer", e);
			}
		}
		throw new ParameterException("unexpected type");
	}

	/**
	 * Separate the integer part from the fractional part of a number.
	 * @ff.default system default
	 */
	public void setDecimalSeparator(String string) {
		decimalSeparator = string;
	}

	/**
	 * In the United States, the comma is typically used for the grouping separator; however, several publication standards follow international standards in using either a space or a thin space character.
	 * @ff.default system default
	 */
	public void setGroupingSeparator(String string) {
		groupingSeparator = string;
	}

	/** Used in combination with type <code>number</code>; if set and the value of the parameter exceeds this maximum value, this maximum value is taken */
	public void setMaxInclusive(String string) {
		maxInclusiveString = string;
	}

	/** Used in combination with type <code>number</code>; if set and the value of the parameter falls short of this minimum value, this minimum value is taken */
	public void setMinInclusive(String string) {
		minInclusiveString = string;
	}
}
