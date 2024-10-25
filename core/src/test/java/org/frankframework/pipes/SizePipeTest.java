package org.frankframework.pipes;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;

import org.junit.jupiter.api.Test;

import org.frankframework.core.PipeRunResult;

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
	void testDoPipeSuccess() throws Exception {
		PipeRunResult res = doPipe(pipe, "abcsd", session);
		assertEquals("5", res.getResult().asString());
	}

	@Test
	void testDoPipeFail() throws Exception {
		ArrayList<String> arr = new ArrayList<>();
		PipeRunResult res = doPipe(pipe, arr, session);
		assertEquals("-1", res.getResult().asString());
	}

	@Override
	public SizePipe createPipe() {
		return new SizePipe();
	}
}
