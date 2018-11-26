package nl.nn.adapterframework.pipes;

import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeLineSessionBase;
import nl.nn.adapterframework.core.PipeRunException;
import org.junit.Test;

import static org.junit.Assert.*;

public class CompressPipeTest extends PipeTestBase<CompressPipe>{

    private IPipeLineSession session = new PipeLineSessionBase();

    @Override
    public CompressPipe createPipe() {
        return new CompressPipe();
    }

    @Test
    public void testGetterSetterMessageIsContent() {
        pipe.setMessageIsContent(true);
        boolean checkBoolean = pipe.isMessageIsContent();
        assertEquals(true, checkBoolean);

        pipe.setMessageIsContent(false);
        checkBoolean = pipe.isMessageIsContent();
        assertEquals(false, checkBoolean);
    }

    @Test
    public void testGetterSetterResultIsContent() {
        pipe.setResultIsContent(true);
        boolean checkBoolean = pipe.isResultIsContent();
        assertEquals(true, checkBoolean);

        pipe.setResultIsContent(false);
        checkBoolean = pipe.isResultIsContent();
        assertEquals(false, checkBoolean);
    }

    @Test
    public void testGetterSetterOuputDirectory() {
        String dummyString =  "dummyString";
        pipe.setOutputDirectory(dummyString);
        String otherString = pipe.getOutputDirectory();
        assertEquals(dummyString, otherString);
    }

    @Test
    public void testGetterSetterFilenamePattern() {
        String dummyString = "dummyString";
        pipe.setFilenamePattern(dummyString);
        String otherString = pipe.getFilenamePattern();
        assertEquals(dummyString, otherString);
    }

    @Test
    public void testGetterSetterZipEntryPattern() {
        String dummyString = "dummyString";
        pipe.setZipEntryPattern(dummyString);
        String otherString = pipe.getZipEntryPattern();
        assertEquals(dummyString, otherString);
    }

    @Test
    public void testGetterSetterCompress() {
        pipe.setCompress(true);
        boolean checkBoolean = pipe.isCompress();
        assertEquals(true, checkBoolean);

        pipe.setCompress(false);
        checkBoolean = pipe.isCompress();
        assertEquals(false, checkBoolean);
    }

    @Test
    public void testGetterSetterConvert2String() {
        pipe.setConvert2String(true);
        boolean checkBoolean = pipe.isConvert2String();
        assertEquals(true, checkBoolean);

        pipe.setConvert2String(false);
        checkBoolean = pipe.isConvert2String();
        assertEquals(false, checkBoolean);
    }

    // TODO : There is no getter for file format
//    @Test
//    public void testGetterSetterFileFormat() {
//        String dummyString = "dummyString";
//        pipe.setFileFormat(dummyString);
//        String otherString = pipe.file
//        assertEquals(dummyString, otherString);
//    }

    @Test(expected = PipeRunException.class)
    public void testCaptureFakeFilePath() throws PipeRunException {
        Object input = "dummyString;";
        pipe.setMessageIsContent(false);
        pipe.setCompress(true);

        pipe.doPipe(input, session);
    }

    @Test(expected = PipeRunException.class)
    public void testCaptureUncompressedLegitimateFilePath() throws PipeRunException {
        Object input = "dummyString;";
        pipe.setMessageIsContent(false);
        pipe.setCompress(false);
        pipe.setFileFormat("gz");

        pipe.doPipe(input, session);
    }

    @Test(expected = PipeRunException.class)
    public void testResultIsContent() throws PipeRunException {
        Object input = "dummyString;";
        pipe.setMessageIsContent(true);
        pipe.setResultIsContent(true);
        pipe.doPipe(input, session);
    }

    @Test
    public void testCompressWithLegitimateFileFormat() throws PipeRunException {
        Object input = "dummyString;";
        pipe.setFileFormat("gz");
        pipe.setMessageIsContent(true);
        pipe.setResultIsContent(true);
        pipe.setCompress(true);
        pipe.doPipe(input, session);
    }

    @Test
    public void testCompressWithIllegimitateFileFormat() throws PipeRunException {
        Object input = "dummyString;";
        pipe.setFileFormat("notNull");
        pipe.setMessageIsContent(true);
        pipe.setResultIsContent(true);
        pipe.setCompress(true);
        assertTrue(pipe.doPipe(input, session) !=  null);
    }


    @Test
    public void testUncompressedWithIlligimitateFileFormat() throws PipeRunException {
        Object input = "dummyString;";
        pipe.setFileFormat("notNull");
        pipe.setMessageIsContent(true);
        pipe.setResultIsContent(true);
        pipe.setCompress(false);
        assertTrue(pipe.doPipe(input, session) !=  null);
    }

    @Test
    public void testConvertByteToStringForResultMsg() throws PipeRunException {
        Object input = "dummyString;";
        pipe.setFileFormat("notNull");
        pipe.setMessageIsContent(true);
        pipe.setResultIsContent(true);
        pipe.setCompress(false);
        pipe.setConvert2String(true);
        assertTrue(pipe.doPipe(input, session) !=  null);
    }

    @Test(expected = PipeRunException.class)
    public void testCaptureIllegitimateFilePath() throws PipeRunException {
        Object input = "dummyString";
        pipe.setMessageIsContent(false);
        pipe.setCompress(true);

        pipe.doPipe(input, session);
    }

    @Test(expected = PipeRunException.class)
    public void testCaptureIllegitimateByteArray() throws PipeRunException {
        Object input = "dummyString".getBytes();
        pipe.setMessageIsContent(true);
        pipe.doPipe(input, session);
    }

    @Test(expected = PipeRunException.class)
    public void testCaptureUnconvertableArray() throws PipeRunException {
        Object input = "dummyString";
        pipe.setMessageIsContent(true);
        pipe.doPipe(input, session);
    }

}