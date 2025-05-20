/*
   Copyright 2025 WeAreFrank!

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
package org.frankframework.errormessageformatters;

import java.io.IOException;

import jakarta.annotation.Nonnull;

import lombok.extern.log4j.Log4j2;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.HasName;
import org.frankframework.core.IConfigurable;
import org.frankframework.core.IErrorMessageFormatter;
import org.frankframework.core.IScopeProvider;
import org.frankframework.core.IWithParameters;
import org.frankframework.core.ParameterException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.doc.Protected;
import org.frankframework.documentbuilder.DocumentFormat;
import org.frankframework.json.DataSonnetOutputType;
import org.frankframework.json.JsonMapper;
import org.frankframework.json.JsonUtil;
import org.frankframework.parameters.IParameter;
import org.frankframework.parameters.ParameterList;
import org.frankframework.stream.Message;

/**
 * Create an error message in JSON format.
 * <p>
 *
 * </p>
 */
@Log4j2
public class DataSonnetErrorMessageFormatter extends ErrorMessageFormatter implements IErrorMessageFormatter, IConfigurable, IScopeProvider, IWithParameters {

	private String styleSheetName;
	private boolean computeMimeType = true;
	private DataSonnetOutputType outputType = DataSonnetOutputType.JSON;
	private JsonMapper mapper;
	private final ParameterList parameters = new ParameterList();

	@Override
	public void configure() throws ConfigurationException {
		super.setMessageFormat(DocumentFormat.JSON);
		parameters.setNamesMustBeUnique(true);
		parameters.configure();

		mapper = JsonUtil.buildJsonMapper(this, styleSheetName, outputType, computeMimeType, parameters);
	}

	@Override
	public Message format(String errorMessage, Throwable t, HasName location, Message originalMessage, PipeLineSession session) {

		try (Message defaultMessage = super.format(errorMessage, t, location, originalMessage, session)) {
			return mapper.transform(defaultMessage, parameters.getValues(originalMessage, session));
		} catch (IOException e) {
			throw new FormatterException("Cannot format error message", e);
		} catch (ParameterException e) {
			throw new FormatterException("Cannot extract parameter values", e);
		}
	}

	/**
	 * Set a DataSonnet stylesheet to transform the default JSON error message to a custom format.
	 */
	public void setStyleSheetName(String styleSheetName) {
		this.styleSheetName = styleSheetName;
	}

	/**
	 * Computes the mimetype when it is unknown. It requires more computation but improves
	 * mapping results.
	 *
	 * @ff.default true
	 */
	public void setComputeMimeType(boolean computeMimeType) {
		this.computeMimeType = computeMimeType;
	}

	/**
	 * Output file format. DataSonnet is semi-capable of converting the converted JSON to a different format.
	 *
	 * @ff.default JSON
	 */
	public void setOutputType(DataSonnetOutputType outputType) {
		this.outputType = outputType;
	}

	@Override
	@Protected
	public void setMessageFormat(@Nonnull DocumentFormat messageFormat) {
		throw new UnsupportedOperationException("Not supported for this type");
	}

	@Override
	public void addParameter(IParameter p) {
		parameters.add(p);
	}

	@Nonnull
	@Override
	public ParameterList getParameterList() {
		return parameters;
	}
}
