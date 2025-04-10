package org.frankframework.parameters;

import java.io.IOException;
import java.nio.charset.Charset;

import javax.xml.transform.TransformerException;

import jakarta.annotation.Nonnull;

import org.springframework.util.MimeType;
import org.xml.sax.SAXException;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.ParameterException;
import org.frankframework.stream.Message;
import org.frankframework.util.MessageUtils;
import org.frankframework.util.TransformerPool;

public class JsonParameter extends AbstractParameter {
	private static final MimeType JSON_MIME_TYPE = new MimeType("application/json");
	private static final MimeType XML_MIME_TYPE = new MimeType("application/xml");

	public JsonParameter() {
		setType(ParameterType.JSON);
	}

	@Override
	protected Object getValueAsType(@Nonnull Message request, boolean namespaceAware) throws ParameterException, IOException {
		MessageUtils.computeMimeType(request);
		if (JSON_MIME_TYPE.isCompatibleWith(request.getContext().getMimeType())) {
			return request;
		}

		if (XML_MIME_TYPE.isCompatibleWith(request.getContext().getMimeType())) {
			return convertXmlToJson(request);
		}

		Message result = new Message("{\"value\": " + request.asString() + "}");
		result.getContext().withMimeType(JSON_MIME_TYPE).withCharset(Charset.defaultCharset());
		return result;
	}

	private Message convertXmlToJson(@Nonnull Message request) throws ParameterException, IOException {
		try {
			// TODO: Replace with new utility-transformer for this path after merging other PR
			TransformerPool tpXml2Json = TransformerPool.configureStyleSheetTransformer(this, "/xml/xsl/xml2json.xsl", 2); // shouldn't this be a utility transformer?
			return tpXml2Json.transform(request);
		} catch (ConfigurationException | TransformerException | SAXException e) {
			throw new ParameterException("Cannot convert parameter value from XML to JSON", e);
		}
	}
}
