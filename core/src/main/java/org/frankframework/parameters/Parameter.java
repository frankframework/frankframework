/*
   Copyright 2024-2025 WeAreFrank!

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

import org.apache.commons.lang3.StringUtils;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;

import org.frankframework.configuration.ConfigurationWarnings;
import org.frankframework.core.ParameterException;
import org.frankframework.doc.Default;
import org.frankframework.stream.Message;
import org.frankframework.util.TransformerPool.OutputType;

/**
 * Placeholder class to allow legacy configuration notations <code>&lt;param type='number' /&gt;</code> in the new Frank!Config XSD.
 * <p>
 * The attribute <code>type</code> has been removed in favor of explicit ParameterTypes such as: <code>NumberParameter</code>, <code>DateParameter</code> and <code>BooleanParameter</code>.
 * Using the new elements enables the use of auto-completion for the specified type.
 *
 * @author Niels Meijer
 */
// See {@link ParameterFactory} and {@link ParameterType} for more information.
@Log4j2
public class Parameter extends AbstractParameter<Message> {

	private @Getter int minLength = -1;

	private @Getter ParameterType type;

	public Parameter() {
		// Default constructor
	}

	/** utility constructor, useful for unit testing */
	public Parameter(String name, String value) {
		this();
		setName(name);
		setValue(value);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder(super.toString());
		if(type != null) builder.append(" type [").append(type).append("]");
		return builder.toString();
	}

	@Override
	@Default("STRING")
	/** The target data type of the parameter, related to the database or XSLT stylesheet to which the parameter is applied. */
	public void setType(ParameterType type) {
		this.type = type;

		if (type == ParameterType.XML) {
			ConfigurationWarnings.add(this, log, "use attribute outputType with value [XML] instead");
			setXpathResult(OutputType.XML);
		}

		super.setType(type);
	}

	@Override
	protected Message getValueAsType(Message request, boolean namespaceAware) throws ParameterException, IOException {
		if (getMinLength() >= 0 || getMaxLength() >= 0) {
			return applyMinLength(request);
		}

		return request;
	}

	/**
	 * Only valid for xPathExpression.
	 * If outputType is {@link OutputType#XML} then the resulting stylesheet will use the {@code copy-of} method instead of {@code value-of}.
	 * This results in an xml-string including the XML tags, if you want the contents of the element (as scalar value), use TEXT.
	 * </p>
	 * This field controls how to read the input and does not determine the output.
	 * @ff.default TEXT
	 */
	@Override
	public void setXpathResult(OutputType outputType) {
		super.setXpathResult(outputType);
	}

	// Someday this will probably belong in a StringParameter...
	private Message applyMinLength(final Message request) {
		if (request.isRequestOfType(String.class)) { // Used by getMinLength and getMaxLength
			try {
				return new Message(applyMinLength(request.asString())); // WARNING this removes the MessageContext
			} catch (IOException e) {
				// Already checked for String, so this should never happen
			}
		}

		// All other types
		log.warn("not applying min or max length. Parameter type does not apply");
		return request;
	}

	// Maybe we want to create a StringParameter someday which might use this method?
	private String applyMinLength(String stringResult) {
		if (getMinLength() >= 0 && stringResult.length() < getMinLength()) {
			log.debug("Padding parameter [{}] because length [{}] falls short of minLength [{}]", this::getName, stringResult::length, this::getMinLength);
			return StringUtils.rightPad(stringResult, getMinLength());
		}

		return stringResult;
	}

	/**
	 * If set (>=0) and the length of the value of the parameter falls short of this minimum length, the value is padded.
	 * This only works for character (input) data.
	 * @ff.default -1
	 */
	public void setMinLength(int i) {
		minLength = i;
	}
}
