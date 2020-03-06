package nl.nn.adapterframework.pipes;

import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeLineSessionBase;
import org.junit.Test;
import org.junit.Before; 
import org.junit.After;
import org.mockito.Mock;

import static org.junit.Assert.*;

/** 
* FilePipe Tester. 
* 
* @author <Sina Sen>
* @since <pre>Feb 28, 2020</pre> 
* @version 1.0 
*/ 
public class FilePipeTest extends PipeTestBase<FilePipe>{

        @Mock
        private IPipeLineSession session = new PipeLineSessionBase();

        @Override
        public FilePipe createPipe() {
            return new FilePipe();
        }


    @Before
    public void before() throws Exception {
    }

    @After
    public void after() throws Exception {
    }

} 
