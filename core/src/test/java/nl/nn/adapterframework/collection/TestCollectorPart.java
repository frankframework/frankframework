package nl.nn.adapterframework.collection;

import java.io.IOException;

import nl.nn.adapterframework.stream.Message;

public class TestCollectorPart {

	private final Message part;
	public TestCollectorPart(Message input) {
		this.part = input;
	}
	public String asString() throws IOException {
		return part.asString();
	}
}
