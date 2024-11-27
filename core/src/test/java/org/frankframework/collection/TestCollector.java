package org.frankframework.collection;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;

import org.frankframework.core.PipeLineSession;
import org.frankframework.parameters.ParameterValueList;
import org.frankframework.stream.Message;

public class TestCollector implements ICollector<TestCollectorPart> {

	boolean open=true;
	private final StringWriter input = new StringWriter();

	public String getInput() {
		return input.toString();
	}

	@Override
	public TestCollectorPart createPart(Message input, PipeLineSession session, ParameterValueList pvl) throws CollectionException {
		try {
			this.input.write(input.asString());
			if ("exception".equals(input.asString())) {
				throw new CollectionException("TestCollector exception");
			}
		} catch (IOException e) {
			throw new CollectionException(e);
		}
		return new TestCollectorPart(input);
	}

	@Override
	public Message build(List<TestCollectorPart> parts) throws IOException {
		StringWriter input = new StringWriter();
		for(TestCollectorPart part : parts) {
			input.append(part.asString());
		}

		return new Message(parts.size() + ":" + input.toString());
	}

	@Override
	public void close() throws Exception {
		open=false;
	}
}
