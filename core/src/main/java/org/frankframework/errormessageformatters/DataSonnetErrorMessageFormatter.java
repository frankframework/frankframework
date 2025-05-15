package org.frankframework.errormessageformatters;

import jakarta.annotation.Nonnull;

import com.datasonnet.Mapper;

import lombok.extern.log4j.Log4j2;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.HasName;
import org.frankframework.core.IConfigurable;
import org.frankframework.core.IErrorMessageFormatter;
import org.frankframework.doc.Protected;
import org.frankframework.documentbuilder.DocumentFormat;
import org.frankframework.stream.Message;

/**
 * Create an error message in JSON format.
 * <p>
 *
 * </p>
 */
@Log4j2
public class DataSonnetErrorMessageFormatter extends ErrorMessageFormatter implements IErrorMessageFormatter, IConfigurable {

	private String styleSheetName;
	private Mapper mapper;

	@Override
	public void configure() throws ConfigurationException {
		setMessageFormat(DocumentFormat.JSON);


	}

	@Override
	public Message format(String errorMessage, Throwable t, HasName location, Message originalMessage, String messageId, long receivedTime) {
		Message defaultMessage = super.format(errorMessage, t, location, originalMessage, messageId, receivedTime);
		return null;
	}

	/**
	 * Set a DataSonnet stylesheet to transform the default JSON error message to a custom format.
	 */
	public void setStyleSheetName(String styleSheetName) {
		this.styleSheetName = styleSheetName;
	}

	@Override
	@Protected
	public void setMessageFormat(@Nonnull DocumentFormat messageFormat) {
		super.setMessageFormat(messageFormat);
	}
}
