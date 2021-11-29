package nl.nn.adapterframework.pipes;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;

import org.junit.Test;

import nl.nn.adapterframework.core.PipeRunResult;

/** 
* SizePipe Tester. 
* 
* @author <Sina Sen>
*/ 
public class SizePipeTest extends PipeTestBase<SizePipe>{


	/**
	 * 
	 * Method: doPipe(Object input, PipeLineSession session)
	 * 
	 */
	@Test
	public void testDoPipeSuccess() throws Exception {
		PipeRunResult res = doPipe(pipe, "abcsd", session);
		assertEquals("5", res.getResult().asString());
	}

	@Test
	public void testDoPipeFail() throws Exception {
		ArrayList<String> arr = new ArrayList<>();
		PipeRunResult res = doPipe(pipe, arr, session);
		assertEquals("-1", res.getResult().asString());
	}

	@Override
	public SizePipe createPipe() {
		return new SizePipe();
	}
}
