package nl.nn.adapterframework.pipes;

import static org.junit.Assert.assertNotNull;

import java.security.NoSuchAlgorithmException;

import org.junit.Test;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.pipes.ChecksumPipe.ChecksumGenerator;
import nl.nn.adapterframework.pipes.ChecksumPipe.ChecksumType;
import nl.nn.adapterframework.stream.Message;



public class ChecksumPipeTest extends PipeTestBase<ChecksumPipe> {

	@Override
	public ChecksumPipe createPipe() {
		return new ChecksumPipe();
	}
	
	
	@Test(expected = ConfigurationException.class)
	public void checkNullType() throws ConfigurationException {
		pipe.setType(null);
		pipe.configure();
	}
		

	@Test
	public void rightChecksumGeneratedMD5() throws NoSuchAlgorithmException {
		pipe.setType(ChecksumType.MD5);
		ChecksumGenerator result = pipe.createChecksumGenerator();

		assertNotNull(result);
	}
	
	@Test
	public void rightChecksumGeneratedSHA() throws NoSuchAlgorithmException {
		pipe.setType(ChecksumType.SHA);
		ChecksumGenerator result = pipe.createChecksumGenerator();

		assertNotNull(result);
	}

	@Test
	public void rightChecksumGeneratedCRC32() throws NoSuchAlgorithmException {
		pipe.setType(ChecksumType.CRC32);
		ChecksumGenerator result = pipe.createChecksumGenerator();

		assertNotNull(result);
	}

	@Test
	public void rightChecksumGeneratedADLER32() throws NoSuchAlgorithmException {
		pipe.setType(ChecksumType.ADLER32);
		ChecksumGenerator result = pipe.createChecksumGenerator();

		assertNotNull(result);
	}
	
	@Test(expected = NoSuchAlgorithmException.class)
	public void emptyChecksumGenerated () throws NoSuchAlgorithmException {
		pipe.setType(null);
		pipe.createChecksumGenerator();
	}
	
	@Test(expected = PipeRunException.class)
	public void cantCalculate() throws PipeRunException {
		doPipe(pipe, new Message((String)null), session);
	}

	@Test(expected = PipeRunException.class)
	public void wrongPathToFile() throws PipeRunException {
		pipe.setInputIsFile(true);
		doPipe(pipe,"dummyPathToFile", session);
	}


	@Test(expected = PipeRunException.class)
	public void badCharset() throws PipeRunException {
		pipe.setInputIsFile(false);
		pipe.setCharset("dummy");
		doPipe(pipe,"anotherDummy", session);
	}

	@Test
	public void emptyCharset() throws PipeRunException {
		pipe.setInputIsFile(false);
		pipe.setCharset("");
		assertNotNull(doPipe(pipe,"anotherDummy", session));
	}

}
