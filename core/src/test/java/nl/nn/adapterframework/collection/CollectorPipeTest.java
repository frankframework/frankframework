package nl.nn.adapterframework.collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import nl.nn.adapterframework.collection.CollectionActor.Action;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.parameters.ParameterValueList;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.stream.StreamingPipeTestBase;

public class CollectorPipeTest extends StreamingPipeTestBase<CollectorPipe> {

	@Override
	public CollectorPipe createPipe() throws ConfigurationException {
		return new CollectorPipe() {

			@Override
			public ICollector openCollection(Message input, PipeLineSession session, ParameterValueList pvl) throws CollectionException {
				return new TestCollector(input, session, pvl);
			}

		};
	}


	@Test
	public void testOpen() throws Exception {
		pipe.setAction(Action.OPEN);
		configureAndStartPipe();

		String input = "testOpen";
		PipeRunResult prr = doPipe(input);

		assertEquals("success", prr.getPipeForward().getName());

		TestCollector collector = (TestCollector)session.get("collection");

		assertEquals(true, collector.open);
		assertEquals(input, collector.input.toString());
		assertEquals(session, collector.session);
	}

	@Test
	public void testClose() throws Exception {
		pipe.setAction(Action.CLOSE);
		configureAndStartPipe();

		TestCollector collector = new TestCollector();
		session.put("collection", collector);

		String input = "testClose";
		PipeRunResult prr = doPipe(input);

		assertEquals("success", prr.getPipeForward().getName());

		assertEquals(false, collector.open);
	}

	@Test
	public void testWrite() throws Exception {
		pipe.setAction(Action.WRITE);
		configureAndStartPipe();

		TestCollector collector = new TestCollector();
		session.put("collection", collector);

		String input = "testWrite";
		PipeRunResult prr = doPipe(input);

		assertEquals("success", prr.getPipeForward().getName());
		assertEquals("writeItem", prr.getResult().asString());

		assertEquals(true, collector.open);
		assertEquals(input, collector.input.toString());
		assertEquals(session, collector.session);
		assertEquals(pipe, collector.writingElement);
	}

	@Test
	public void testStream() throws Exception {
		pipe.setAction(Action.STREAM);
		configureAndStartPipe();

		TestCollector collector = new TestCollector();
		session.put("collection", collector);

		String input = "testStream";
		PipeRunResult prr = doPipe(input);

		assertEquals("success", prr.getPipeForward().getName());
		assertNotNull(prr.getResult().asObject());
		assertEquals(collector.outputStream, prr.getResult().asObject());

		assertEquals(true, collector.open);
		assertEquals(input, collector.input.toString());
		assertEquals(session, collector.session);
		assertEquals(pipe, collector.writingElement);
	}

}
