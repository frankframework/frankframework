package nl.nn.adapterframework.pipes;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.springframework.core.annotation.Order;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.core.PipeStartException;


/**
 * IfMultipart Tester.
 *
 * @author <Sina Sen>
 */
public class MoveFilePipeTest extends PipeTestBase<MoveFilePipe>{

    @Rule
    public ExpectedException exception = ExpectedException.none();
	private final String pipeForwardThen = "success";


    @ClassRule
    public static TemporaryFolder testFolderSource = new TemporaryFolder();

    private static String sourceFolderPath;

    @ClassRule
    public static TemporaryFolder testFolderDest = new TemporaryFolder();

    private static String destFolderPath;

    @ClassRule
    public static TemporaryFolder testFolderCantDelete = new TemporaryFolder();

    private static String cantdeleteFolderPath;

    @ClassRule
    public static TemporaryFolder testFolderDelete = new TemporaryFolder();

    private static String deleteFolderPath;

    @Override
    public MoveFilePipe createPipe() { return new MoveFilePipe(); }

    @BeforeClass
    public static void setUpTest() throws IOException {
            sourceFolderPath = testFolderSource.getRoot().getPath();
            destFolderPath = testFolderDest.getRoot().getPath();
            cantdeleteFolderPath = testFolderCantDelete.getRoot().getPath();
            deleteFolderPath = testFolderDelete.getRoot().getPath();
            testFolderSource.newFile("1.txt"); testFolderSource.newFile("2.txt"); testFolderSource.newFile("3.txt"); testFolderSource.newFile("a.md"); testFolderSource.newFile("b.md"); testFolderSource.newFile("sad.lk"); testFolderSource.newFile("notCompatible.txt");
            testFolderSource.newFile("createDirectory.txt"); testFolderSource.newFile("cantmove.sc"); testFolderSource.newFile("prefixsuffix.txt"); testFolderSource.newFile("toAppend1.txt"); testFolderSource.newFile("toAppend2.txt"); testFolderSource.newFile("xx.txt"); testFolderSource.newFile("test.txt");
            testFolderDest.newFile("cantmove.sc"); testFolderDest.newFile("notcompatible.asd"); testFolderDest.newFile("toBeAppended.txt"); testFolderCantDelete.newFile("deletable.sd"); testFolderCantDelete.newFile("deletionInterrupter.mz");
            testFolderDelete.newFile("moveAndDeleteDirectory.txt");
    }


    @Test
    @Order(1)
    public void nonExistingFileWithSourceAndTargetDirectories() throws ConfigurationException, PipeStartException, PipeRunException {

        pipe.setMove2dir(destFolderPath);
        pipe.setDirectory(sourceFolderPath);
        pipe.setFilename(null);
        pipe.configure();
        pipe.start();

        PipeRunResult res = doPipe(pipe, "xx.txt", session);

        assertEquals(pipeForwardThen, res.getPipeForward().getName());

    }

    @Test
    @Order(2)
    public void fileToFolderTransferWithoutWildcardTest() throws ConfigurationException, PipeStartException, PipeRunException {
        pipe.setMove2dir(destFolderPath);
        pipe.setDirectory(sourceFolderPath);
        pipe.setFilename("test.txt");
        pipe.setNumberOfBackups(0);
        pipe.configure();
        pipe.start();


        PipeRunResult res = doPipe(pipe, "xdfgfx", session);

        assertEquals(pipeForwardThen, res.getPipeForward().getName());
    }

    @Test
    @Order(3)
    public void appendFileToFileInAnotherDirectoryWithoutWildcardTest()  throws ConfigurationException, PipeStartException, PipeRunException {
        pipe.setMove2dir(destFolderPath);
        pipe.setDirectory(sourceFolderPath);
        pipe.setFilename("toAppend1.txt");
        pipe.setMove2file("toBeAppended.txt");
        pipe.setAppend(true);
        pipe.configure();
        pipe.start();

        PipeRunResult res = pipe.doPipe(null, session);

        assertEquals(pipeForwardThen, res.getPipeForward().getName());
    }

    @Test
    @Order(4)
    public void appendFileToFileWithSessionKey() throws ConfigurationException, PipeStartException, PipeRunException {
        pipe.setMove2dir(destFolderPath);
        pipe.setDirectory(sourceFolderPath);
        pipe.setFilename("toAppend2.txt");
        pipe.setMove2fileSessionKey("a");
        session.put("a", "toBeAppended.txt");
        pipe.setAppend(true);
        pipe.configure();
        pipe.start();

        PipeRunResult res = pipe.doPipe(null, session);

        assertEquals(pipeForwardThen, res.getPipeForward().getName());
    }


    @Test
    @Order(5)
    public void moveFileAndDeleteDirectory()throws ConfigurationException, PipeStartException, PipeRunException {
        pipe.setMove2dir(destFolderPath);
        pipe.setDirectory(deleteFolderPath);
        pipe.setDeleteEmptyDirectory(true);
        pipe.setFilename("moveAndDeleteDirectory.txt");
        pipe.configure();
        pipe.start();

        PipeRunResult res = doPipe(pipe,"xx", session);

        assertEquals(pipeForwardThen, res.getPipeForward().getName());
    }

    @Test
    @Order(6)
    public void moveToNewlyCreatedDirectory() throws ConfigurationException, PipeStartException, PipeRunException {
        pipe.setCreateDirectory(true);
        pipe.setMove2dir(destFolderPath+"/new");//for MAC, different for Windows
        pipe.setDirectory(sourceFolderPath);
        pipe.setFilename("createDirectory.txt");
        pipe.configure();
        pipe.start();

        PipeRunResult res = doPipe(pipe, "xx", session);

        assertEquals(pipeForwardThen, res.getPipeForward().getName());
    }

    @Test
    @Order(7)
    public void moveFilesWithWildcardTest() throws ConfigurationException, PipeStartException, PipeRunException {
        pipe.setMove2dir(destFolderPath);
        pipe.setDirectory(sourceFolderPath);
        pipe.setWildcard("*.md");
        pipe.configure();
        pipe.start();

        PipeRunResult res = pipe.doPipe(null, session);

        assertEquals(pipeForwardThen, res.getPipeForward().getName());
    }

    @Test
    @Order(8)
    public void moveWithPrefixAndSuffixChange() throws ConfigurationException, PipeStartException, PipeRunException {
        pipe.setMove2dir(destFolderPath);
        pipe.setDirectory(sourceFolderPath);
        pipe.setFilename("prefixsuffix.txt");
        pipe.setSuffix(".md");
        pipe.setPrefix("1");
        pipe.configure();
        pipe.start();

        PipeRunResult res = doPipe(pipe, "xx", session);
        assertEquals(pipeForwardThen, res.getPipeForward().getName());
    }
    @Test
    @Order(9)
    public void testThrowException() throws ConfigurationException, PipeStartException {
        exception.expect(ConfigurationException.class);
        pipe.setThrowException(true);
        pipe.configure();
        pipe.start();

        //PipeRunResult res = pipe.doPipe("xx", session);
        fail("this is expected to fail");
    }

    @Test
    @Order(10)
    public void moveFilesWithWildcardSessionKeyTest() throws ConfigurationException, PipeStartException, PipeRunException {

        pipe.setMove2dir(destFolderPath);
        pipe.setDirectory(sourceFolderPath);
        pipe.setWildcardSessionKey("a");
        session.put("a", "*.txt");
        pipe.configure();
        pipe.start();
        PipeRunResult res = doPipe(pipe, "sd", session);

        assertEquals(pipeForwardThen, res.getPipeForward().getName());
    }




    @Test
    @Order(11)
    public void nonExistingFileWithEverythingNull() throws ConfigurationException, PipeStartException, PipeRunException {
        exception.expect(ConfigurationException.class);
        pipe.configure();
        pipe.start();
        doPipe(pipe, "testdoc", session);

        fail("this is expected to fail");
    }

    @Test
    @Order(12)
    public void everythingNull() throws ConfigurationException, PipeStartException, PipeRunException {
        exception.expect(ConfigurationException.class);
        pipe.setFilename(null);
        pipe.configure();
        pipe.start();

        pipe.doPipe(null, session);

        fail("this is expected to fail");
    }

    @Test
    @Order(13)
    public void cantMoveAsItAlreadyExists() throws ConfigurationException, PipeStartException, PipeRunException {
        exception.expect(PipeRunException.class);
        pipe.setMove2dir(destFolderPath);
        pipe.setDirectory(sourceFolderPath);
        pipe.setMove2file("cantmove.sc");
        pipe.setFilename("cantmove.sc");
        pipe.setNumberOfBackups(0);
        pipe.setThrowException(true);
        pipe.configure();
        pipe.start();

        pipe.doPipe(null, session);

        fail("this is expected to fail");
    }

    @Test
    @Order(14)
    public void cantMoveFileAsItsDirectoryIsFalse() throws ConfigurationException, PipeStartException, PipeRunException {
        exception.expect(PipeRunException.class);
        pipe.setMove2dir(destFolderPath);
        pipe.setDirectory(sourceFolderPath+"/itswrong");
        pipe.setFilename("cantmove.sc");
        pipe.setNumberOfAttempts(1);
        pipe.configure();
        pipe.start();

        doPipe(pipe, "xdfgfx", session);

        fail("this is expected to fail");
    }

    @Test
    @Order(15)
    public void appendFilesNotCompatible() throws ConfigurationException, PipeStartException, PipeRunException {
        pipe.setMove2dir(destFolderPath);
        pipe.setDirectory(sourceFolderPath);
        pipe.setFilename("notCompatible.txt");
        pipe.setMove2file("notCompatible.asd");
        pipe.setOverwrite(false);
        pipe.setAppend(true);
        pipe.configure();
        pipe.start();

        PipeRunResult res = pipe.doPipe(null, session);

        assertEquals(pipeForwardThen, res.getPipeForward().getName());
    }


    @Test
    @Order(16)
    public void cantMoveFilesWithWildcardTest() throws ConfigurationException, PipeStartException, PipeRunException {
        //exception.expect(PipeRunException.class);
        //exception.expectMessage("no files with wildcard [*.xd] found in directory ["+sourceFolderPath+"]");
        pipe.setMove2dir(destFolderPath);
        pipe.setDirectory(sourceFolderPath);
        pipe.setWildcard("*.xd");
        pipe.configure();
        pipe.start();
        PipeRunResult res = pipe.doPipe(null, session);
        assertEquals(pipeForwardThen, res.getPipeForward().getName());
    }



    @Test
    @Order(17)
    public void cantDeleteDirectoryAsWrongName() throws ConfigurationException, PipeStartException, PipeRunException {
        exception.expect(PipeRunException.class);
        pipe.setMove2dir(destFolderPath);
        pipe.setDirectory("/Users/apollo11/Desktop/iaf/core/src/test/java/nl/nn/adapterframework/pipes/cantbedeleteddd");// some random, wrong directory path
        pipe.setDeleteEmptyDirectory(true);
        pipe.setFilename("deletable.sd");
        pipe.configure();
        pipe.setNumberOfAttempts(1);
        pipe.start();

        doPipe(pipe, "xx", session);

        fail("this is expected to fail");
    }
    @Test
    @Order(18)
    public void cantDeleteDirectoryAsItIsNotEmpty() throws ConfigurationException, PipeStartException, PipeRunException {
        //exception.expect(PipeRunException.class);
        //exception.expectMessage("directory ["+cantdeleteFolderPath+"] is not empty");
        pipe.setMove2dir(destFolderPath);//for MAC, different for Windows
        pipe.setDirectory(cantdeleteFolderPath);//for MAC, different for Windows
        pipe.setDeleteEmptyDirectory(true);
        pipe.setFilename("deletable.sd");
        pipe.configure();
        pipe.start();

        PipeRunResult res = doPipe(pipe, "xx", session);

        assertEquals(pipeForwardThen, res.getPipeForward().getName());
    }



    @Test
    @Order(19)
    public void failCreatingNewDirectory() throws ConfigurationException, PipeStartException, PipeRunException {
        exception.expect(PipeRunException.class);
        pipe.setCreateDirectory(false);
        pipe.setMove2dir(destFolderPath+"/newas");
        pipe.setDirectory(sourceFolderPath);
        pipe.setFilename("sad.lk");
        pipe.setNumberOfAttempts(1);
        pipe.configure();
        pipe.start();

        doPipe(pipe, "xx", session);

        fail("this is expected to fail");
    }
}
