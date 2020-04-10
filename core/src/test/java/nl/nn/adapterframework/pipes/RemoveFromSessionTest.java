package nl.nn.adapterframework.pipes;

import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeLineSessionBase;
import nl.nn.adapterframework.core.PipeRunResult;
import org.junit.Test;
import org.junit.Before; 
import org.junit.After;
import org.mockito.Mock;

import static org.junit.Assert.assertEquals;

/** 
* RemoveFromSession Tester. 
* 
* @author <Sina Sen>
* @since <pre>Mar 12, 2020</pre> 
* @version 1.0 
*/ 
public class RemoveFromSessionTest extends PipeTestBase<RemoveFromSession> {

    @Mock
    private IPipeLineSession session = new PipeLineSessionBase();
    @Override
    public RemoveFromSession createPipe() {
        return new RemoveFromSession();
    }
@Before
public void before() throws Exception { 
} 

@After
public void after() throws Exception { 
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
        PipeRunResult res = pipe.doPipe("a", session);
        assertEquals(res.getResult().toString(), "123");

}

    @Test
    public void testNonEmptySessionKeyNonEmptyInput() throws Exception {
        pipe.setSessionKey("a");
        session.put("a", "123");
        PipeRunResult res = pipe.doPipe("a", session);
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
        pipe.setSessionKey(null);
        session.put("a", "123");
        PipeRunResult res = pipe.doPipe(null, session);
        assertEquals(res.getResult().toString(), "[null]");    }

    @Test
    public void testFailAsKeyIsWrong() throws Exception {
        pipe.setSessionKey("ab");
        session.put("a", "123");
        PipeRunResult res = pipe.doPipe("ab", session);
        assertEquals(res.getResult().toString(), "[null]");

    }





}
