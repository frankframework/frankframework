package nl.nn.adapterframework.pipes;

import static nl.nn.adapterframework.testutil.MatchUtils.assertXmlEquals;

import java.io.File;
import java.net.URL;

import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.core.PipeStartException;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.testutil.TestFileUtils;

public class UnzipPipeTest extends PipeTestBase<UnzipPipe> {

	private TemporaryFolder folder;
	private String fileSeparator = File.separator;

	@Override
	public void setup() throws Exception {
		folder = new TemporaryFolder();
		folder.create();
		super.setup();
	}

	@Override
	public UnzipPipe createPipe() {
		UnzipPipe result = new UnzipPipe();
		result.setDirectory(folder.getRoot().toString());
		return result;
	}

	@Test
	public void testConfigureAndStart() throws ConfigurationException, PipeStartException {
		configureAndStartPipe();
	}

	@Test
	public void testUnzipFromStream() throws Exception {
		pipe.setKeepOriginalFileName(true);
		configureAndStartPipe();
		
		URL zip = TestFileUtils.getTestFileURL("/Unzip/ab.zip");
		
		String expected = 	"<results count=\"2\">"+
								"<result item=\"1\"><zipEntry>fileaa.txt</zipEntry><fileName>"+folder.getRoot().toString()+fileSeparator+"fileaa.txt</fileName></result>" + 
								"<result item=\"2\"><zipEntry>filebb.log</zipEntry><fileName>"+folder.getRoot().toString()+fileSeparator+"filebb.log</fileName></result>" + 
							"</results>";
		
		PipeRunResult prr = doPipe(new Message(zip));
		
		assertXmlEquals(expected, prr.getResult().asString());
	}


	@Test
	public void testUnzipNoCollectResults() throws Exception {
		pipe.setKeepOriginalFileName(true);
		pipe.setCollectResults(false);
		configureAndStartPipe();
		
		URL zip = TestFileUtils.getTestFileURL("/Unzip/ab.zip");
		
		String expected = 	"<results count=\"2\">"+
							"</results>";
		
		PipeRunResult prr = doPipe(new Message(zip));
		
		assertXmlEquals(expected, prr.getResult().asString());
	}

	@Test
	public void testUnzipCollectFileContents() throws Exception {
		pipe.setKeepOriginalFileName(true);
		pipe.setCollectFileContents(true);
		configureAndStartPipe();
		
		URL zip = TestFileUtils.getTestFileURL("/Unzip/ab.zip");
		
		String expected = 	"<results count=\"2\">" +
				"<result item=\"1\"><zipEntry>fileaa.txt</zipEntry><fileName>"+folder.getRoot().toString()+fileSeparator+"fileaa.txt</fileName>" + 
					"<fileContent>aaa</fileContent>"+
				"</result>" + 
				"<result item=\"2\"><zipEntry>filebb.log</zipEntry><fileName>"+folder.getRoot().toString()+fileSeparator+"filebb.log</fileName>" +
					"<fileContent>bbb</fileContent>"+
				"</result>" + 
			"</results>";
		
		PipeRunResult prr = doPipe(new Message(zip));
		
		assertXmlEquals(expected, prr.getResult().asString());
	}
	
	@Test
	public void testUnzipCollectFileContentsBase64() throws Exception {
		pipe.setKeepOriginalFileName(true);
		pipe.setCollectFileContentsBase64Encoded(".log");
		pipe.setCollectFileContents(true);
		configureAndStartPipe();
		
		URL zip = TestFileUtils.getTestFileURL("/Unzip/ab.zip");
		
		String expected = 	"<results count=\"2\">" +
				"<result item=\"1\"><zipEntry>fileaa.txt</zipEntry><fileName>"+folder.getRoot().toString()+fileSeparator+"fileaa.txt</fileName>" + 
					"<fileContent>aaa</fileContent>"+
				"</result>" + 
				"<result item=\"2\"><zipEntry>filebb.log</zipEntry><fileName>"+folder.getRoot().toString()+fileSeparator+"filebb.log</fileName>" +
					"<fileContent>YmJi\n</fileContent>"+
				"</result>" + 
			"</results>";
		
		PipeRunResult prr = doPipe(new Message(zip));
		
		assertXmlEquals(expected, prr.getResult().asString());
	}
	

}
