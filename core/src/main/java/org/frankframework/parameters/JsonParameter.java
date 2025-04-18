package org.frankframework.parameters;

import java.io.IOException;

import jakarta.annotation.Nonnull;

import org.frankframework.core.ParameterException;
import org.frankframework.stream.Message;
import org.frankframework.util.MessageUtils;
import org.frankframework.util.XmlException;

public class JsonParameter extends AbstractParameter {

	public JsonParameter() {
		setType(ParameterType.JSON);
	}

	@Override
	protected Object getValueAsType(@Nonnull Message request, boolean namespaceAware) throws ParameterException, IOException {
		try {
			// Caller closes the message so since we pass a message to `asJsonMessage` we get back the same instance and need to copy it.
			return MessageUtils.asJsonMessage(request, getName()).copyMessage();
		} catch (XmlException e) {
			throw new ParameterException("Cannot convert value to JSON", e);
		}
	}
}
