package nl.nn.adapterframework.management.bus;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.Response;

import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;

public class ResponseMessage {

//	private static final long serialVersionUID = 4268801052358035098L;

	public ResponseMessage(Response payload, Map<String, Object> headers) {
//		super(payload, headers);
	}

	public static GenericMessage create(String payload) {
		Map<String, Object> headers = new HashMap<>();
		Response reponse = Response.status(Response.Status.OK).entity(payload).build();
		return new GenericMessage(reponse);
	}

	public static Message create(Response build) {
		return new GenericMessage(build);
	}
}
