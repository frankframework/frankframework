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




    /**
     * Method: setOutputType(String outputType)
     */
    @Test
    public void testSetOutputType() throws Exception {
//TODO: Test goes here... 
    }

    /**
     * Method: setActions(String actions)
     */
    @Test
    public void testSetActions() throws Exception {
//TODO: Test goes here... 
    }

    /**
     * Method: setFileSource(String fileSource)
     */
    @Test
    public void testSetFileSource() throws Exception {
//TODO: Test goes here... 
    }

    /**
     * Method: setDirectory(String directory)
     */
    @Test
    public void testSetDirectory() throws Exception {
//TODO: Test goes here... 
    }

    /**
     * Method: setWriteSuffix(String suffix)
     */
    @Test
    public void testSetWriteSuffix() throws Exception {
//TODO: Test goes here... 
    }

    /**
     * Method: setFileName(String filename)
     */
    @Test
    public void testSetFileName() throws Exception {
//TODO: Test goes here... 
    }

    /**
     * Method: setFileNameSessionKey(String filenameSessionKey)
     */
    @Test
    public void testSetFileNameSessionKey() throws Exception {
//TODO: Test goes here... 
    }

    /**
     * Method: setCreateDirectory(boolean b)
     */
    @Test
    public void testSetCreateDirectory() throws Exception {
//TODO: Test goes here... 
    }

    /**
     * Method: setWriteLineSeparator(boolean b)
     */
    @Test
    public void testSetWriteLineSeparator() throws Exception {
//TODO: Test goes here... 
    }

    /**
     * Method: setTestCanWrite(boolean b)
     */
    @Test
    public void testSetTestCanWrite() throws Exception {
//TODO: Test goes here... 
    }

    /**
     * Method: setSkipBOM(boolean b)
     */
    @Test
    public void testSetSkipBOM() throws Exception {
//TODO: Test goes here... 
    }

    /**
     * Method: setDeleteEmptyDirectory(boolean b)
     */
    @Test
    public void testSetDeleteEmptyDirectory() throws Exception {
//TODO: Test goes here... 
    }

    /**
     * Method: setStreamResultToServlet(boolean b)
     */
    @Test
    public void testSetStreamResultToServlet() throws Exception {
//TODO: Test goes here... 
    }

    /**
     * Method: configure()
     */
    @Test
    public void testConfigure() throws Exception {
    }

    /**
     * Method: doPipe(Object input, IPipeLineSession session)
     */
    @Test
    public void testDoPipe() throws Exception {
//TODO: Test goes here...
    }


} 
