package nl.nn.adapterframework.stream;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.core.PipeStartException;
import nl.nn.adapterframework.pipes.PipeTestBase;
import nl.nn.adapterframework.util.Locker;

/*
 * Test for StreamingPipes own functionalities
 */
public class StreamingPipeTest extends PipeTestBase<StreamingPipe> {

	@Override
	public StreamingPipe createPipe() {
		StreamingPipe pipe = new StreamingPipe() {

			@Override
			public PipeRunResult doPipe(Message message, PipeLineSession session) throws PipeRunException {
				return null;
			}
		};
		return pipe;
	}

	@Test
	public void testCanProvideOutputStreamDefault() throws ConfigurationException, PipeStartException {
		configureAndStartPipe();

		assertTrue(pipe.canProvideOutputStream());
	}

	@Test
	public void testCanProvideOutputStreamWithInputFromSessionKey() throws ConfigurationException, PipeStartException {
		pipe.setGetInputFromSessionKey("fakeSessionKey");
		configureAndStartPipe();

		assertFalse(pipe.canProvideOutputStream());
	}

	@Test
	public void testCanProvideOutputStreamWithStoreResultInSessionKey() throws ConfigurationException, PipeStartException {
		pipe.setStoreResultInSessionKey("fakeSessionKey");
		configureAndStartPipe();

		assertFalse(pipe.canProvideOutputStream());
	}

	@Test
	public void testCanProvideOutputStreamWithInputFromFixedValue() throws ConfigurationException, PipeStartException {
		pipe.setGetInputFromFixedValue("dummyInput");
		configureAndStartPipe();

		assertFalse(pipe.canProvideOutputStream());
	}

	@Test
	public void testCanProvideOutputStreamWithEmptyInputReplacement() throws ConfigurationException, PipeStartException {
		pipe.setEmptyInputReplacement("dummyEmptyInput");
		configureAndStartPipe();

		assertFalse(pipe.canProvideOutputStream());
	}

	@Test
	public void testCanProvideOutputStreamWithSkipOnEmptyInput() throws ConfigurationException, PipeStartException {
		pipe.setSkipOnEmptyInput(true);
		configureAndStartPipe();

		assertFalse(pipe.canProvideOutputStream());
	}

	@Test
	public void testCanProvideOutputStreamWithIfParam() throws ConfigurationException, PipeStartException {
		pipe.setIfParam("dummyIfParam");
		configureAndStartPipe();

		assertFalse(pipe.canProvideOutputStream());
	}

	@Test
	public void testCanProvideOutputStreamWithPreserveInput() throws ConfigurationException, PipeStartException {
		pipe.setPreserveInput(true);
		configureAndStartPipe();

		assertFalse(pipe.canProvideOutputStream());
	}

	@Test
	public void testCanProvideOutputStreamWithElementToMove() throws ConfigurationException, PipeStartException {
		pipe.setElementToMove("fakeElementToMove");
		configureAndStartPipe();

		assertFalse(pipe.canProvideOutputStream());
	}

	@Test
	public void testCanProvideOutputStreamWithRestoreMovedElements() throws ConfigurationException, PipeStartException {
		pipe.setRestoreMovedElements(true);
		configureAndStartPipe();

		assertFalse(pipe.canProvideOutputStream());
	}

	@Test
	public void testCanProvideOutputStreamWithLocker() throws ConfigurationException, PipeStartException {
		Locker locker = new Locker() {
			@Override
			public void configure() throws ConfigurationException {
				// skip configure, only need locker object to be present
			}
		};
		pipe.setLocker(locker);
		configureAndStartPipe();

		assertFalse(pipe.canProvideOutputStream());
	}



	@Test
	public void testStreamToNextPipeDefault() throws ConfigurationException, PipeStartException {
		configureAndStartPipe();

		assertTrue(pipe.canStreamToNextPipe());
	}

	@Test
	public void testStreamToNextPipeWithStoreResultInSessionKey() throws ConfigurationException, PipeStartException {
		pipe.setStoreResultInSessionKey("fakeSessionKey");
		configureAndStartPipe();

		assertFalse(pipe.canStreamToNextPipe());
	}

	@Test
	public void testStreamToNextPipeWithPreserveInput() throws ConfigurationException, PipeStartException {
		pipe.setPreserveInput(true);
		configureAndStartPipe();

		assertFalse(pipe.canStreamToNextPipe());
	}

	@Test
	public void testStreamToNextPipeWithRestoreMovedElements() throws ConfigurationException, PipeStartException {
		pipe.setRestoreMovedElements(true);
		configureAndStartPipe();

		assertFalse(pipe.canStreamToNextPipe());
	}

	@Test
	public void testStreamToNextPipeWithChompCharSize() throws ConfigurationException, PipeStartException {
		pipe.setChompCharSize("size");
		configureAndStartPipe();

		assertFalse(pipe.canStreamToNextPipe());
	}

	@Test
	public void testStreamToNextPipeWithElementToMove() throws ConfigurationException, PipeStartException {
		pipe.setElementToMove("element");
		configureAndStartPipe();

		assertFalse(pipe.canStreamToNextPipe());
	}

	@Test
	public void testStreamToNextPipeWithElementToMoveChain() throws ConfigurationException, PipeStartException {
		pipe.setElementToMoveChain("elementChain");
		configureAndStartPipe();

		assertFalse(pipe.canStreamToNextPipe());
	}

	@Test
	public void testStreamToNextPipeWithWriteToSecLoc() throws ConfigurationException, PipeStartException {
		pipe.setWriteToSecLog(true);
		configureAndStartPipe();

		assertFalse(pipe.canStreamToNextPipe());
	}
}
