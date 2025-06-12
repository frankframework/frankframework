package org.frankframework.pipes;

import java.io.IOException;

import com.jayway.jsonpath.JsonPath;

import lombok.Getter;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.doc.EnterpriseIntegrationPattern;
import org.frankframework.doc.EnterpriseIntegrationPattern.Type;
import org.frankframework.doc.Mandatory;
import org.frankframework.stream.Message;
import org.frankframework.util.MessageUtils;
import org.frankframework.util.XmlException;

/**
 * Apply a one-liner JSON path expression to the input to extract a value from input data.
 * If the input is in XML format, it will be converted to JSON using the same method as the {@link org.frankframework.align.Xml2Json} pipe.
 *
 */
@EnterpriseIntegrationPattern(Type.TRANSLATOR)
public class JsonPathPipe extends FixedForwardPipe {

	private @Getter String jsonPathExpression;

	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		if (jsonPathExpression == null) {
			throw new ConfigurationException("jsonPathExpression has to be set");
		}
	}

	@Override
	public PipeRunResult doPipe(Message message, PipeLineSession session) throws PipeRunException {

		Object result;
		try {
			message.preserve();
			Message jsonMessage = MessageUtils.convertToJsonMessage(message);
			result = JsonPath.read(jsonMessage.asInputStream(), jsonPathExpression);
		} catch (IOException | XmlException e) {
			throw new PipeRunException(this, "Failed to evaluate json path expression [" + jsonPathExpression + "] on input [" + message + "]", e);
		}

		PipeRunResult prr = new PipeRunResult();
		prr.setResult(result);
		return prr;
	}

	@Mandatory
	public void setJsonPathExpression(String jsonPathExpression) {
		this.jsonPathExpression = jsonPathExpression;
	}
}
