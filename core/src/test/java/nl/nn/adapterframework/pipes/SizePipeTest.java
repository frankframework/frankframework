package nl.nn.adapterframework.pipes;

import nl.nn.adapterframework.core.PipeRunResult;
import org.junit.Test;
import java.util.ArrayList;

import static org.junit.Assert.assertEquals;

/** 
* SizePipe Tester. 
* 
* @author <Sina Sen>
*/ 
public class SizePipeTest extends PipeTestBase<SizePipe>{


/** 
* 
* Method: doPipe(Object input, IPipeLineSession session) 
* 
*/ 
@Test
public void testDoPipeSuccess() throws Exception {
    PipeRunResult res = doPipe(pipe, "abcsd", session);
    assertEquals(res.getResult().toString(), "5");
}

    @Test
    public void testDoPipeFail() throws Exception {
    ArrayList<String> arr = new ArrayList<>();
        PipeRunResult res = doPipe(pipe, arr, session);
        assertEquals(res.getResult().toString(), "-1");
    }


    @Override
    public SizePipe createPipe() {
        return new SizePipe();
    }
}
