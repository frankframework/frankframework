package org.frankframework.collection;

import java.io.IOException;

import org.frankframework.stream.Message;

public class TestCollectorPart {

	private final Message part;
	public TestCollectorPart(Message input) {
		this.part = input;
	}
	public String asString() throws IOException {
		return part.asString();
	}
}
