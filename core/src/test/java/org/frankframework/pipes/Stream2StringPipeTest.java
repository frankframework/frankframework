package org.frankframework.pipes;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.junit.jupiter.api.Test;

import org.frankframework.core.PipeRunResult;
import org.frankframework.stream.Message;

/**
 * Stream2StringPipe Tester.
 *
 * @author <Sina Sen>
 */
public class Stream2StringPipeTest extends PipeTestBase<Stream2StringPipe> {
	@Test
	void testDoPipeSuccess() throws Exception {
        String myString = "testString";
        InputStream is = new ByteArrayInputStream(myString.getBytes());
        Message m = new Message(is);
        PipeRunResult res = doPipe(pipe, m, session);
        assertEquals("testString", res.getResult().asString());
    }
	@Test
	void testDoPipeFail() throws Exception {
        String myString = "testString";
        Message m = new Message(myString);
        PipeRunResult res = doPipe(pipe, m, session);
        assertEquals("testString", res.getResult().asString());
    }


    @Override
    public Stream2StringPipe createPipe() {
        return new Stream2StringPipe();
    }
}
