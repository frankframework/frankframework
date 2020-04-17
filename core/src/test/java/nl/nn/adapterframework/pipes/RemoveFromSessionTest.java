package nl.nn.adapterframework.pipes;

import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeLineSessionBase;
import nl.nn.adapterframework.core.PipeRunResult;
import org.junit.Test;
import org.mockito.Mock;

import static org.junit.Assert.assertEquals;

/** 
* RemoveFromSession Tester. 
* 
* @author <Sina Sen>
*/ 
public class RemoveFromSessionTest extends PipeTestBase<RemoveFromSession> {

    @Mock
    private IPipeLineSession session = new PipeLineSessionBase();
    @Override
    public RemoveFromSession createPipe() {
        return new RemoveFromSession();
    }


/** 
* 
* Method: configure() 
* 
*/ 
@Test
public void testEmptySessionKeyNonEmptyInput() throws Exception {
        pipe.setSessionKey(null);
        session.put("a", "123");
        PipeRunResult res = doPipe(pipe, "a", session);
        assertEquals(res.getResult().toString(), "123");

}

    @Test
    public void testNonEmptySessionKeyNonEmptyInput() throws Exception {
        pipe.setSessionKey("a");
        session.put("a", "123");
        PipeRunResult res = doPipe(pipe, "a", session);
        assertEquals(res.getResult().toString(), "123");    }

    @Test
    public void testNonEmptySessionKeyEmptyInput() throws Exception {
            pipe.setSessionKey("a");
            session.put("a", "123");
            PipeRunResult res = pipe.doPipe(null, session);
            assertEquals(res.getResult().toString(), "123");

    }
    @Test
    public void testEmptySessionKeyEmptyInput() throws Exception {
        exception.expect(NullPointerException.class);
        pipe.setSessionKey(null);
        session.put("a", "123");
        PipeRunResult res = doPipe(pipe, null, session);
        assertEquals(res.getResult().toString(), "[null]");    }

    @Test
    public void testFailAsKeyIsWrong() throws Exception {
        pipe.setSessionKey("ab");
        session.put("a", "123");
        PipeRunResult res = doPipe(pipe, "ab", session);
        assertEquals(res.getResult().toString(), "[null]");

    }





}
