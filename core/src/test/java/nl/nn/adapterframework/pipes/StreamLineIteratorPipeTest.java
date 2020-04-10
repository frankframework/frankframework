package nl.nn.adapterframework.pipes;

import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeLineSessionBase;
import nl.nn.adapterframework.core.SenderException;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.Before; 
import org.junit.After;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import static org.junit.Assert.assertEquals;

/** 
* StreamLineIteratorPipe Tester. 
* 
* @author <Sina>
* @since <pre>Mar 27, 2020</pre> 
* @version 1.0 
*/ 
public class StreamLineIteratorPipeTest extends PipeTestBase<StreamLineIteratorPipe> {

    @Mock
    private IPipeLineSession session1 = new PipeLineSessionBase();

    @ClassRule
    public static TemporaryFolder testFolderSource = new TemporaryFolder();

    private static String sourceFolderPath;

    private static FileInputStream fis1;

    private static File newFile;

    @Override
    public StreamLineIteratorPipe createPipe() {
        return new StreamLineIteratorPipe();
    }

@BeforeClass
public static void before() throws Exception {
    sourceFolderPath = testFolderSource.getRoot().getPath();
    newFile = testFolderSource.newFile("1.zip");
    FileWriter fw = new FileWriter(newFile);
    fw.write("asdfdf");
    fis1 = new FileInputStream(newFile.getPath());
} 



@Test
public void testGetReaderSuccess() throws Exception {
    Map<String, Object> map = new HashMap<>();
    map.put("key", fis1);
    Reader res = pipe.getReader(fis1, session1, "correlationID", map);
    assertEquals(res.read(), -1);
}

    @Test
    public void testGetReaderFailAsNullInput() throws Exception {
        exception.expect(SenderException.class);
        exception.expectMessage("input is null. Must supply stream as input");
        Map<String, Object> map = new HashMap<>();
        map.put("key", fis1);
        Reader res = pipe.getReader(null, session1, "correlationID", map);
    }
    @Test
    public void testGetReaderFailAsWrongInputType() throws Exception {
        exception.expect(SenderException.class);
        exception.expectMessage("input must be of type InputStream");
        Map<String, Object> map = new HashMap<>();
        map.put("key", fis1);
        Reader res = pipe.getReader(12, session1, "correlationID", map);

    }

}
