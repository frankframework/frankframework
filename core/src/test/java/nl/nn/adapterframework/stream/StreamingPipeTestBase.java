package nl.nn.adapterframework.stream;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assume.assumeNotNull;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.util.Arrays;
import java.util.Collection;

import org.junit.AssumptionViolatedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import nl.nn.adapterframework.core.IForwardTarget;
import nl.nn.adapterframework.core.INamedObject;
import nl.nn.adapterframework.core.IPipe;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.pipes.PipeTestBase;
import nl.nn.adapterframework.testutil.ThrowingAfterCloseInputStream;

@RunWith(Parameterized.class)
public abstract class StreamingPipeTestBase<P extends StreamingPipe> extends PipeTestBase<P> {

	@Parameter(0)
	public String  description=null;
	@Parameter(1)
	public boolean classic=true;
	@Parameter(2)
	public boolean provideStreamForInput=false;
	@Parameter(3)
	public boolean writeOutputToStream=false;

	@Parameters(name = "{index}: {0}: provide [{2}] stream out [{3}]")
	public static Collection<Object[]> data() {
		return Arrays.asList(new Object[][] {
			{ "classic", 			true, false, false },
			{ "new, no stream", 	false, false, false },
			{ "output to stream", 	false, false, true  },
			{ "consume stream", 	false, true,  false },
			{ "stream through",  	false, true,  true  }
		});
	}

	@Override
	public void setUp() throws Exception {
		super.setUp();
		pipe.setStreamingActive(!classic);
	}


	@SuppressWarnings("deprecation")
	@Override
	protected PipeRunResult doPipe(P pipe, Message input, PipeLineSession session) throws PipeRunException {
		PipeRunResult prr=null;
		// TODO: CapProvider should not be provided as argument to provideOutputStream, because that is not used there.
		// Instead, it must be the next pipe in the pipeline. When it is called, the forward of that pipe
		// must be the result of the streaming operation.
//		CapProvider capProvider = writeOutputToStream?new CapProvider(null):null;
		IPipe nextPipe = null; // TODO: must replace with capProvider, to monitor proper pass through
		if (provideStreamForInput) {
			//Object result;
			try (MessageOutputStream target = pipe.provideOutputStream(session, nextPipe)) {
				assumeNotNull(target);
				if(input.isBinary()) {
					try (OutputStream stream = target.asStream()) {
						stream.write(input.asByteArray());
					}
				} else {
					try (Writer writer = target.asWriter()) {
						writer.write(input.asString());
					}
				}
				prr=target.getPipeRunResult();
			} catch (AssumptionViolatedException e) {
				throw e;
			} catch (Exception e) {
				throw new PipeRunException(pipe,"cannot convert input",e);
			}
		} else {
			// Wrap input-stream in a stream that forces IOExceptions after it is closed; close the session
			// (and thus any messages attached) after running the pipe so that reading the result message
			// will verify the original input-stream of the input-message is not used beyond due-date.
			try (PipeLineSession ignored = session) {
				Message messageToSend;
				if (input != null) {
					if (input.asObject() instanceof InputStream) {
						input.unscheduleFromCloseOnExitOf(session);
						messageToSend = new Message(new ThrowingAfterCloseInputStream((InputStream) input.asObject()));
					} else {
						messageToSend = input;
					}
					messageToSend.closeOnCloseOf(session, pipe);
				} else {
					messageToSend = null;
				}
				prr = pipe.doPipe(messageToSend,session);

				// Before session closes, unschedule result from close-on-close.
				if (prr != null && prr.getResult() != null) {
					prr.getResult().unscheduleFromCloseOnExitOf(session);
				}
			}
		}
		assertNotNull(prr);
		assertNotNull(prr.getPipeForward());
		return prr;
	}

	private class CapProvider implements IOutputStreamingSupport {

		private CloseObservableCap cap;

		public CapProvider(INamedObject owner) {
			cap=new CloseObservableCap(owner);
		}

		@Override
		public boolean supportsOutputStreamPassThrough() {
			return false;
		}

		@Override
		public MessageOutputStream provideOutputStream(PipeLineSession session, IForwardTarget next) throws StreamingException {
			return cap;
		}

		public CloseObservableCap getCap() {
			return cap;
		}

	}

	private class CloseObservableCap extends MessageOutputStreamCap {

		private int closeCount=0;

		public CloseObservableCap(INamedObject owner) {
			super(owner, null);
		}

		@Override
		public void afterClose() throws Exception {
			super.afterClose();
			closeCount++;
		}

		public int getCloseCount() {
			return closeCount;
		}
	}

}
