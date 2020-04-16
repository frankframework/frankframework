package nl.nn.adapterframework.pipes;

import nl.nn.adapterframework.core.PipeRunResult;
import org.junit.Test;
import org.junit.Before; 
import org.junit.After;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;

import static org.junit.Assert.assertEquals;

/** 
* SizePipe Tester. 
* 
* @author <Sina Sen>
* @since <pre>Mar 27, 2020</pre> 
* @version 1.0 
*/ 
public class SizePipeTest extends PipeTestBase<SizePipe>{


/** 
* 
* Method: doPipe(Object input, IPipeLineSession session) 
* 
*/ 
@Test
public void testDoPipeSuccess() throws Exception {
    PipeRunResult res = pipe.doPipe("abcsd", session);
    assertEquals(res.getResult().toString(), "5");
}

    @Test
    public void testDoPipeFail() throws Exception {
    ArrayList<String> arr = new ArrayList<>();
        PipeRunResult res = pipe.doPipe(arr, session);
        assertEquals(res.getResult().toString(), "-1");
    }


    @Override
    public SizePipe createPipe() {
        return new SizePipe();
    }
}
