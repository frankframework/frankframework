package nl.nn.adapterframework.pipes;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.security.NoSuchAlgorithmException;

import org.junit.Test;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.pipes.ChecksumPipe.ChecksumGenerator;
import nl.nn.adapterframework.stream.Message;



public class ChecksumPipeTest extends PipeTestBase<ChecksumPipe> {

	public static final String CHECKSUM_MD5="MD5";
	public static final String CHECKSUM_SHA="SHA";
	public static final String CHECKSUM_CRC32="CRC32";
	public static final String CHECKSUM_ADLER32="Adler32";

	@Override
	public ChecksumPipe createPipe() {
		return new ChecksumPipe();
	}
	
	
	@Test(expected = ConfigurationException.class)
	public void checkEmptyType() throws ConfigurationException {
		String type = "";
		pipe.setType(type);
		pipe.configure();
	}
	
	@Test(expected = ConfigurationException.class)
	public void checkNullType() throws ConfigurationException {
		pipe.setType(null);
		pipe.configure();
	}
	
	@Test(expected = ConfigurationException.class)
	public void checkWrongChecksumType() throws ConfigurationException {
		pipe.setType("dummy");
		pipe.configure();	
	}
	
	@Test
	public void checkRightChecksumTypeMD5() throws ConfigurationException {
		pipe.setType(CHECKSUM_MD5);
		pipe.configure();
		String check = "MD5";
		assertEquals(CHECKSUM_MD5, check);		
	}
	
	
	@Test
	public void checkRightChecksumTypeSHA() throws ConfigurationException {		
		pipe.setType(CHECKSUM_SHA);
		pipe.configure();
		String check = "SHA";
		assertEquals(CHECKSUM_SHA, check);
		
	}
	
	@Test
	public void checkRightChecksumTypeCRC32() throws ConfigurationException {
		pipe.setType(CHECKSUM_CRC32);
		pipe.configure();
		String check = "CRC32";
		assertEquals(CHECKSUM_CRC32, check);
	
	}
	
	@Test
	public void checkRightChecksumTypeADLER32() throws ConfigurationException {
		pipe.setType(CHECKSUM_ADLER32);
		pipe.configure();
		String check = "Adler32";
		assertEquals(CHECKSUM_ADLER32, check);
		
	}

	@Test
	public void rightChecksumGeneratedMD5() throws NoSuchAlgorithmException {
		pipe.setType(CHECKSUM_MD5);
		ChecksumGenerator result = pipe.createChecksumGenerator();

		assertNotNull(result);
	}
	
	@Test
	public void rightChecksumGeneratedSHA() throws NoSuchAlgorithmException {
		pipe.setType(CHECKSUM_SHA);
		ChecksumGenerator result = pipe.createChecksumGenerator();

		assertNotNull(result);
	}

	@Test
	public void rightChecksumGeneratedCRC32() throws NoSuchAlgorithmException {
		pipe.setType(CHECKSUM_CRC32);
		ChecksumGenerator result = pipe.createChecksumGenerator();

		assertNotNull(result);
	}

	@Test
	public void rightChecksumGeneratedADLER32() throws NoSuchAlgorithmException {
		pipe.setType(CHECKSUM_ADLER32);
		ChecksumGenerator result = pipe.createChecksumGenerator();

		assertNotNull(result);
	}
	
	@Test(expected = NoSuchAlgorithmException.class)
	public void emptyChecksumGenerated () throws NoSuchAlgorithmException {
		pipe.setType("");
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
