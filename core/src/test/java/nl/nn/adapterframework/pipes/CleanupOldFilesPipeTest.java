package nl.nn.adapterframework.pipes;

import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeLineSessionBase;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import org.junit.Test;

import java.io.File;
import java.nio.channels.Pipe;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CleanupOldFilesPipeTest extends PipeTestBase<CleanupOldFilesPipe> {

    private IPipeLineSession session = new PipeLineSessionBase();

    @Override
    public CleanupOldFilesPipe createPipe() {
        return new CleanupOldFilesPipe();
    }

    @Test(expected = PipeRunException.class)
    public void emptyInput() throws PipeRunException {
        pipe.doPipe("", session);
    }

    @Test
    public void getterSetterFilePattern() {
        String dummyString = "dummyString";
        pipe.setFilePattern(dummyString);
        String otherString = pipe.getFilePattern();

        assertEquals(dummyString, otherString);
    }

    @Test
    public void getterSetterFilePatternSessionKey() {
        String dummyString = "dummyString";
        pipe.setFilePatternSessionKey(dummyString);
        String otherString = pipe.getFilePatternSessionKey();

        assertEquals(dummyString, otherString);
    }

    @Test
    public void getterSetterWildCard(){
        String dummyString = "dummyString";
        pipe.setWildcard(dummyString);
        String otherString = pipe.getWildcard();

        assertEquals(dummyString, otherString);
    }

    @Test
    public void getterSetterExcludeWildcard() {
        String dummyString = "dummyString";
        pipe.setExcludeWildcard(dummyString);
        String otherString = pipe.getExcludeWildcard();

        assertEquals(dummyString, otherString);
    }

    @Test
    public void getterSetterMinStableTime() {
        long dummyVal = 3000;
        pipe.setMinStableTime(dummyVal);
        long otherVal = pipe.getMinStableTime();

        assertEquals(dummyVal, otherVal);
    }

    @Test
    public void getterSetterDeleteEmptySubdirectories() {
        pipe.setDeleteEmptySubdirectories(true);
        boolean otherBool = pipe.isDeleteEmptySubdirectories();
        assertEquals(true, otherBool);

        pipe.setDeleteEmptySubdirectories(false);
        otherBool = pipe.isDeleteEmptySubdirectories();
        assertEquals(false, otherBool);
    }

    @Test
    public void getterSetterSubdirectories() {
        pipe.setSubdirectories(true);
        boolean otherBool = pipe.isSubdirectories();
        assertEquals(true, otherBool);

        pipe.setSubdirectories(false);
        otherBool = pipe.isSubdirectories();
        assertEquals(false, otherBool);
    }

    @Test
    public void getterSetterLastModifiedDelta() {
        long dummyVal = 3000;
        pipe.setLastModifiedDelta(dummyVal);
        long otherVal = pipe.getLastModifiedDelta();

        assertEquals(dummyVal, otherVal);
    }

    @Test
    public void testCreateFileFromInput() throws PipeRunException {
        Object dummy = "dummyFile.txt";
        pipe.setFilePattern("");
        pipe.setFilePatternSessionKey("");
        assertTrue(pipe.doPipe(dummy, session) != null);
    }

    @Test
    public void testFilePattern() throws PipeRunException {
        Object dummy = "dummy";
        pipe.setFilePattern("dummyPattern");
        assertTrue(pipe.doPipe(dummy, session) != null);
    }

    @Test
    public void testFilePatternSessionKey() throws PipeRunException {
        Object dummy  = "dummy";
        pipe.setFilePattern("");
        pipe.setFilePatternSessionKey("dummyPattern");
        assertTrue(pipe.doPipe(dummy, session) != null);
    }

}