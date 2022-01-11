package nl.nn.adapterframework.pipes;

import static org.junit.Assert.assertNotNull;

import java.security.NoSuchAlgorithmException;

import org.junit.Test;

import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.pipes.ChecksumPipe.ChecksumGenerator;
import nl.nn.adapterframework.pipes.ChecksumPipe.ChecksumType;
import nl.nn.adapterframework.stream.Message;



public class ChecksumPipeTest extends PipeTestBase<ChecksumPipe> {

	@Override
	public ChecksumPipe createPipe() {
		return new ChecksumPipe();
	}

	@Test
	public void rightChecksumGeneratedMD5() throws Exception {
		pipe.setType(ChecksumType.MD5);
		configureAndStartPipe();
		ChecksumGenerator result = pipe.createChecksumGenerator();

		assertNotNull(result);
	}
	
	@Test
	public void rightChecksumGeneratedSHA() throws Exception {
		pipe.setType(ChecksumType.SHA);
		configureAndStartPipe();
		ChecksumGenerator result = pipe.createChecksumGenerator();

		assertNotNull(result);
	}

	@Test
	public void rightChecksumGeneratedCRC32() throws Exception {
		pipe.setType(ChecksumType.CRC32);
		configureAndStartPipe();
		ChecksumGenerator result = pipe.createChecksumGenerator();

		assertNotNull(result);
	}

	@Test
	public void rightChecksumGeneratedADLER32() throws Exception {
		pipe.setType(ChecksumType.ADLER32);
		ChecksumGenerator result = pipe.createChecksumGenerator();

		assertNotNull(result);
	}
	
	@Test(expected = PipeRunException.class)
	public void cantCalculate() throws Exception {
		doPipe(pipe, new Message((String)null), session);
	}

	@Test(expected = PipeRunException.class)
	public void wrongPathToFile() throws Exception {
		pipe.setInputIsFile(true);
		configureAndStartPipe();
		doPipe(pipe,"dummyPathToFile", session);
	}


	@Test(expected = PipeRunException.class)
	public void badCharset() throws Exception {
		pipe.setInputIsFile(false);
		pipe.setCharset("dummy");
		configureAndStartPipe();
		doPipe(pipe,"anotherDummy", session);
	}

	@Test
	public void emptyCharset() throws Exception {
		pipe.setInputIsFile(false);
		pipe.setCharset("");
		configureAndStartPipe();
		assertNotNull(doPipe(pipe,"anotherDummy", session));
	}

}
