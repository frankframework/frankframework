package nl.nn.adapterframework.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.net.URL;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.filesystem.IFileHandler;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.ParameterList;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.testutil.ParameterBuilder;
import nl.nn.adapterframework.testutil.TestFileUtils;

public abstract class FileHandlerTestBase {

	public static String BASEDIR="/FileHandler/";

	public String TEST_TEMP_DIR="target";
	
	private IFileHandler handler;
	private PipeLineSession session=new PipeLineSession();


    protected abstract IFileHandler createFileHandler() throws IllegalAccessException, InstantiationException;
    
	public String CHARSET_UTF8="UTF-8";
	public String charset=CHARSET_UTF8;

	  @Rule
	  public final ExpectedException exception = ExpectedException.none();
	  
	@Before
	public void setup() throws ConfigurationException, IllegalAccessException, InstantiationException {
		handler = createFileHandler();
	}
	
	public URL getURL(String file) {
		return FileHandlerTestBase.class.getResource(BASEDIR+file);
	}
 
	public String removeNewlines(String contents) {
		return contents.replaceAll("[\n\r]", "");
	}

	public void testIllegalAction(String action) throws Exception {
		handler.setActions(action);
		exception.expect(ConfigurationException.class);
		handler.configure();
	}
		
	public void testRead(String filename, String charset, boolean decode, boolean encode) throws Exception {
		testRead(filename, charset, decode, encode, false, "String");
	}
	
	public void testRead(String filename, String charset, boolean decode, boolean encode, boolean skipBom, String outputType) throws Exception {
		URL fileURL=getURL(filename);
		String inFilepath=fileURL.getPath();

		String compareFile=filename;
		String actions="read";

		if (decode) {
			inFilepath+=".b64";
			actions+=",decode";
		}
		if (encode) {
			actions+=",encode";
			compareFile+=".b64";
		}
		handler.setActions(actions);
		handler.setCharset(charset);
		handler.setFileName(inFilepath);
		handler.setSkipBOM(skipBom);
		handler.setOutputType(outputType);
		handler.configure();
		
		String expectedContents=TestFileUtils.getTestFile(BASEDIR+compareFile, charset);
		if (outputType==null || outputType.equalsIgnoreCase("string")) {
			String actualContents = (String) handler.handle(null,session,null);
			assertEquals("file contents", removeNewlines(expectedContents), removeNewlines(actualContents));
		} else {
			byte[] actualContents = (byte[]) handler.handle(null,session,null);
			assertEquals("file contents", expectedContents, new String(actualContents,"utf-8"));
			assertEquals(expectedContents.getBytes().length, actualContents.length);
		}
	}

	
	public void testWrite(String filename, String charset, boolean decode, boolean encode, String baseAction, String fileContentsAtStart, boolean truncate, boolean write, boolean writeSeparator) throws Exception {
		String contentFile=filename;
		testWrite(null, filename, null, false, contentFile, charset, decode, encode, baseAction, fileContentsAtStart, truncate, write, writeSeparator);
	}
	public void testWrite(String directory, String filename, String suffix, boolean suffixViaParam, String contentFile, String charset, boolean decode, boolean encode, String baseAction, String fileContentsAtStart, boolean truncate, boolean write, boolean writeSeparator) throws Exception {

		ParameterList paramList=null;
		String compareFile=directory==null?contentFile:directory+"/"+contentFile;
		String filepath=null;
		if (filename!=null) {
			URL fileURL=getURL(filename);
			filepath=fileURL.getPath()+".tmp";
			
			File f0 = new File(filepath);
			f0.delete();
			if (fileContentsAtStart!=null) {
				FileWriter fw = new FileWriter(filepath);
				fw.write(fileContentsAtStart);
				fw.close();
			}
		}
		
		String actions=baseAction;

		if (decode) {
			contentFile+=".b64";
			actions="decode,"+actions;
		}
		if (encode) {
			actions="encode,"+actions;
			compareFile+=".b64";
		}
		handler.setActions(actions);
		handler.setCharset(charset);
		if (directory!=null) {
			handler.setDirectory(directory);
		}
		if (suffix!=null) {
			if (suffixViaParam) {
				paramList=new ParameterList();
				paramList.add(new Parameter("writeSuffix", suffix));
				paramList.configure();
				handler.setWriteSuffix(".wsx");
			} else {
				handler.setWriteSuffix(suffix);
				session.remove("writeSuffix");
			}
 		}
		handler.setFileName(filepath);
		handler.setWriteLineSeparator(writeSeparator);
		handler.configure();

		Message contents=TestFileUtils.getTestFileMessage(BASEDIR+contentFile, charset);
		String actFilename=(String)handler.handle(contents,session,paramList);
		if (filename==null) {
			assertNotNull(actFilename);
		} else {
			File f = new File(filepath);
			assertEquals(f.getAbsolutePath(), actFilename);
		}

		String expectedContents;
		if (fileContentsAtStart!=null && !truncate) {
			expectedContents=fileContentsAtStart;
		} else {
			expectedContents="";
		}
		if (write) {
			expectedContents+=TestFileUtils.getTestFile(BASEDIR+compareFile, charset);
			if (writeSeparator) {
				expectedContents+=System.getProperty("line.separator");
			}
		}
		
		System.out.println("act filename ["+actFilename+"] suffix ["+suffix+"]");
		if (suffix!=null) {
			assertThat(actFilename,endsWith(suffix));
		}
		File fa = new File(actFilename);
		
		String actualContents = TestFileUtils.getTestFile(fa.toURL(), charset);
		assertEquals("appended file contents", expectedContents.trim(), actualContents);
	}

	public void testList(String filename, String charset) throws Exception {

		URL fileURL=getURL(filename);
		String filePath=fileURL.getPath();
		String directoryPath=filePath.substring(0,filePath.length()-filename.length());

		String actions="list";

		handler.setActions(actions);
		handler.setDirectory(directoryPath);
		handler.setCharset(charset);
		handler.configure();

		handler.handle(null,session,null);
		
		String expectedContents="<list>";
		String actualContents = (String) handler.handle(null,session,null);
		assertThat(actualContents,startsWith("<directory name"));
		assertThat(actualContents,containsString(filename));
	}

	public void testInfo(String filename, String charset) throws Exception {

		URL fileURL=getURL(filename);
		String filePath=fileURL.getPath();
		String directoryPath=filePath.substring(0,filePath.length()-filename.length());

		String actions="info";

		handler.setActions(actions);
		handler.setFileName(filePath);
		handler.setCharset(charset);
		handler.configure();

		handler.handle(null,session,null);
		
		String expectedContents="<list>";
		String actualContents = (String) handler.handle(null,session,null);
		System.out.println("actual ["+actualContents+"]");
		assertThat(actualContents,startsWith("<file>"));
		assertThat(actualContents,containsString(filename+"</fullName>"));
		assertThat(actualContents,containsString("<name>"+filename+"</name>"));
		assertThat(actualContents,containsString("<modificationDate>"));
		assertThat(actualContents,containsString("<modificationTime>"));
	}

	public void testDelete(String filename, boolean read) throws Exception {

		String expectedContents="contents of read delete file";

		URL fileURL=getURL(filename);
		String filepath=fileURL.getPath()+".tmp";
		
		FileOutputStream fout =new FileOutputStream(filepath);
		fout.write(expectedContents.getBytes());
		fout.close();
		File f=new File(filepath);
		assertTrue(f.exists());
		
		String contentFile=filename;
		String actions=read?"read_delete":"delete";

		handler.setActions(actions);
		handler.setFileName(filepath);
		handler.setCharset(charset);
		handler.configure();

		//String contents=TestFileUtils.getTestFile(contentFile, charset);

		String actualContents = (String) handler.handle(null,session,null);
		if (read) assertEquals("file contents", expectedContents, actualContents);
		File f2=new File(filepath);
		assertFalse("file ["+filepath+"] should have been deleted",f2.exists());
	}

	@Test
	public void testIllegalAction1() throws Exception {
		testIllegalAction("lees");
	}
	@Test
	public void testIllegalAction2() throws Exception {
		testIllegalAction("write,schrijf");
	}

	@Test
	public void testNullAction() throws Exception {
		testIllegalAction(null);
	}
	@Test
	public void testEmptyAction1() throws Exception {
		testIllegalAction("");
	}
	@Test
	public void testEmptyAction2() throws Exception {
		testIllegalAction(",");
	}
	
	
	@Test
	public void testReadXml() throws Exception {
		testRead("smiley.xml",charset,false,false);
		//TODO: fix the below tests. On Azure, filesize is based on CRLF line endings, instead of LF
//		testRead("smiley.xml",charset,false,false,true,"bytes");
//		testRead("smiley.xml",charset,false,false,false,"bytes");
	}

	@Test
	public void testReadJson() throws Exception {
		testRead("smiley.json",charset,false,false);
	}

	@Test
	public void testReadTxt() throws Exception {
		testRead("smiley.txt",charset,false,false);
		//TODO: fix the below tests. On Azure, filesize is based on CRLF line endings, instead of LF
//		testRead("smiley.txt",charset,false,false,true,"bytes");
//		testRead("smiley.txt",charset,false,false,false,"bytes");
	}

	@Test
	public void testReadJsonDecode() throws Exception {
		testRead("smiley.json",charset,true,false);
	}

	@Test
	public void testWriteJsonEncodeCreateFresh() throws Exception {
		testWrite("smiley.json",charset,false,false,"create",null,true,false,false);
		testWrite("smiley.json",charset,false,false,"create",null,true,false,true);
		testWrite(null,null,null,false,"smiley.json",charset,false,false,"create",null, true, false, false);
		testWrite(null,null,null,false,"smiley.json",charset,false,false,"create",null, true, false, true);

		testWrite(TEST_TEMP_DIR+"/sub",null,null,false,"smiley.json",charset,false,false,"create",null, true, false, false);
		testWrite(TEST_TEMP_DIR+"/sub",null,null,false,"smiley.json",charset,false,false,"create",null, true, false, true);

		testWrite(TEST_TEMP_DIR+"/sub1/sub2",null,null,false,"smiley.json",charset,false,false,"create",null, true, false, false);
		testWrite(TEST_TEMP_DIR+"/sub1/sub2",null,null,false,"smiley.json",charset,false,false,"create",null, true, false, true);

		testWrite(TEST_TEMP_DIR+"/sub",null,".sfx",false,"smiley.json",charset,false,false,"create",null, true, false, false);
		testWrite(TEST_TEMP_DIR+"/sub",null,".sfx",true,"smiley.json",charset,false,false,"create",null, true, false, true);
	}
	@Test
	public void testWriteJsonEncodeCreateTruncate() throws Exception {
		testWrite("smiley.json",charset,false,false,"create","content at start1",true,false,false);
		testWrite("smiley.json",charset,false,false,"create","content at start1",true,false,true);
		testWrite(null,null,null,false,"smiley.json",charset,false,false,"create","content at start1", true, false, false);
		testWrite(null,null,null,false,"smiley.json",charset,false,false,"create","content at start1", true, false, true);
	}

	@Test
	public void testWriteJsonEncodeWriteFresh() throws Exception {
		testWrite("smiley.json",charset,false,false,"write",null,true,true,false);
		testWrite("smiley.json",charset,true,false,"write",null,true,true,true);
		testWrite("smiley.json",charset,false,true,"write",null,true,true,false);
		testWrite("smiley.json",charset,false,false,"write","content at start2",true,true,false);
		testWrite("smiley.json",charset,true,false,"write","content at start2",true,true,true);
		testWrite("smiley.json",charset,false,true,"write","content at start2",true,true,false);
		testWrite(null,null,null,false,"smiley.json",charset,false,false,"write",null, true, true, false);
		testWrite(null,null,null,false,"smiley.json",charset,true,false,"write",null, true, true, true);
		testWrite(null,null,null,false,"smiley.json",charset,false,true,"write",null, true, true, false);
	}
	@Test
	public void testWriteJsonEncodeWriteAppend() throws Exception {
//		testWrite("smiley.json",charset,false,false,"write_append",null,false,true);
//		testWrite("smiley.json",charset,true,false,"write_append",null,false,true);
//		testWrite("smiley.json",charset,false,true,"write_append",null,false,true);
		testWrite("smiley.json",charset,false,false,"write_append","content at start2",false,true,false);
		testWrite("smiley.json",charset,true,false,"write_append","content at start3",false,true,true);
		testWrite("smiley.json",charset,false,true,"write_append","content at start4",false,true,true);
	}


	@Test
	public void testList() throws Exception {
		testList("smiley.json",charset);
	}

	@Test
	public void testInfo() throws Exception {
		testInfo("smiley.json",charset);
	}

	@Test
	public void testReadDelete() throws Exception {
		testDelete("smiley.xml",true);
	}
	@Test
	public void testDelete() throws Exception {
		testDelete("smiley.txt",false);
	}
	
//	Nog te testen:
//		read_delete

}
