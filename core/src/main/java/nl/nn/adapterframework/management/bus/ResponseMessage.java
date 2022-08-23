package nl.nn.adapterframework.management.bus;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.util.MimeType;

public class ResponseMessage {

	public static GenericMessage create(String payload) {
		Map<String, Object> headers = new HashMap<>();
		headers.put("status", 200);
		return new GenericMessage(payload, headers);
	}

	public static Message ok(InputStream configFlow, MimeType mediaType) {
		Map<String, Object> headers = new HashMap<>();
		headers.put("status", 200);
		headers.put("mimeType", mediaType.toString());
		return new GenericMessage(configFlow, headers);
	}

	public static Message noContent() {
		Map<String, Object> headers = new HashMap<>();
		headers.put("status", 204);
		return new GenericMessage("no-content", headers);
	}
}
