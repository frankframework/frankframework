/*
   Copyright 2020-2021 WeAreFrank!

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package nl.nn.adapterframework.extensions.aspose.pipe;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeFalse;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Assert;
import org.junit.Test;

import com.testautomationguru.utility.PDFUtil;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.extensions.aspose.pipe.PdfPipe.DocumentAction;
import nl.nn.adapterframework.pipes.PipeTestBase;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.testutil.MatchUtils;
import nl.nn.adapterframework.testutil.TestAssertions;
import nl.nn.adapterframework.testutil.TestFileUtils;

/**
 * Executes defined tests against the PdfPipe to ensure the correct working of this pipe.
 * 
 * @author Laurens Mäkel
 */

public class PdfPipeTest extends PipeTestBase<PdfPipe> {
	private static final String REGEX_PATH_IGNORE = "(?<=convertedDocument=\").*(?=\")";
	private static final String REGEX_TIJDSTIP_IGNORE = "(?<=Tijdstip:).*(?=\" n)";
	private static final String[] REGEX_IGNORES = {REGEX_PATH_IGNORE, REGEX_TIJDSTIP_IGNORE};
	private Path pdfOutputLocation;

	@Override
	public PdfPipe createPipe() {
		return new PdfPipe();
	}

	@Override
	public void setup() throws Exception {
		super.setup();
		pdfOutputLocation = Files.createTempDirectory("Pdf");
		pipe.setPdfOutputLocation(pdfOutputLocation.toString());
		pipe.setUnpackCommonFontsArchive(true);
	}

	@Override
	public void tearDown() throws Exception {
		synchronized(pdfOutputLocation) {
			Files.walk(pdfOutputLocation)
			.forEach(t -> {
				if(Files.isRegularFile(t)) {
					try {
						Files.delete(t);
					} catch (IOException e) {
						e.printStackTrace();
						Assert.fail("unable to delete: "+ e.getMessage());
					}
				}
			});

			Files.deleteIfExists(pdfOutputLocation);
		}

		super.tearDown();
	}

	public void expectSuccessfullConversion(String pipeName, String fileToConvert, String metadataXml, String expectedFile) throws Exception {
		String documentMetadata = executeConversion(pipeName, fileToConvert);
		String expected = TestFileUtils.getTestFile(metadataXml);

		MatchUtils.assertXmlEquals("Conversion XML does not match", applyIgnores(expected), applyIgnores(documentMetadata), true);

		//Get document for path
		Pattern convertedDocumentPattern = Pattern.compile(REGEX_PATH_IGNORE);
		Matcher convertedDocumentMatcher = convertedDocumentPattern.matcher(documentMetadata);

		if(convertedDocumentMatcher.find()) { //Find converted document location
			String convertedFilePath = convertedDocumentMatcher.group();
			System.out.println("found converted file ["+convertedFilePath+"]");

			URL expectedFileUrl = TestFileUtils.getTestFileURL(expectedFile);
			assertNotNull("cannot find expected file ["+expectedFile+"]", expectedFileUrl);
			File file = new File(expectedFileUrl.toURI());
			String expectedFilePath = file.getPath();
			System.out.println("converted relative path ["+expectedFile+"] to absolute file ["+expectedFilePath+"]");

			PDFUtil pdfUtil = new PDFUtil();
			//remove Aspose evaluation copy information
			pdfUtil.excludeText("(Created with an evaluation copy of Aspose.([a-zA-Z]+). To discover the full versions of our APIs please visit: https:\\/\\/products.aspose.com\\/([a-zA-Z]+)\\/)");
//			pdfUtil.enableLog();
			boolean compare = pdfUtil.compare(convertedFilePath, file.getPath());
			assertTrue("pdf files ["+convertedFilePath+"] and ["+expectedFilePath+"] should match", compare);
		}
		else {
			fail("failed to extract converted file from documentMetadata xml");
		}
	}

	public void expectUnsuccessfullConversion(String name, String fileToConvert, String fileContaingExpectedXml) throws Exception {
		String actualXml = executeConversion(name, fileToConvert);
		String expected = TestFileUtils.getTestFile(fileContaingExpectedXml);

		MatchUtils.assertXmlEquals("Conversion XML does not match", applyIgnores(expected), applyIgnores(actualXml), true);
	}

	public String executeConversion(String pipeName, String fileToConvert) throws Exception {
		pipe.setName(pipeName);
		pipe.setAction(DocumentAction.CONVERT);
		pipe.configure();
		pipe.start();

		PipeLineSession session = new PipeLineSession();
		URL input = TestFileUtils.getTestFileURL(fileToConvert);
		pipe.doPipe(Message.asMessage(new File(input.toURI())), session);

		//returns <main conversionOption="0" mediaType="xxx/xxx" documentName="filename" numberOfPages="1" convertedDocument="xxx.pdf" />
		return session.getMessage("documents").asString();
	}

	public String applyIgnores(String input){
		String result = input;
		for(String ignore : REGEX_IGNORES){
			result = result.replaceAll(ignore, "IGNORE");
		}
		return result;
	}

	@Test
	public void bmp2Pdf() throws Exception {
		expectSuccessfullConversion("Bmp2Pdf", "/PdfPipe/bmp.bmp", "/PdfPipe/xml-results/bmp.xml", "/PdfPipe/results/bmp.pdf");
	}

	@Test
	public void docWord2016Macro2Pdf() throws Exception {
		expectSuccessfullConversion("DocWord2016Macro2Pdf", "/PdfPipe/docm-word-2016-macro.docm", "/PdfPipe/xml-results/docm-word-2016-macro.xml", "/PdfPipe/results/docm-word-2016-macro.pdf");
	}

	@Test
	public void docWord20032Pdf() throws Exception {
		expectSuccessfullConversion("DocWord20032Pdf", "/PdfPipe/doc-word-2003.doc", "/PdfPipe/xml-results/doc-word-2003.xml", "/PdfPipe/results/doc-word-2003.pdf");
	}

	@Test
	public void dot2Pdf() throws Exception {
		expectSuccessfullConversion("Dot2Pdf", "/PdfPipe/dot.dot", "/PdfPipe/xml-results/dot.xml", "/PdfPipe/results/dot.pdf");
	}

	@Test
	public void emlFromGroupmailbox2Pdf() throws Exception {
		assumeFalse("This test does not run on Travis-CI / GitHub Actions", TestAssertions.isTestRunningOnCI());
		expectSuccessfullConversion("EmlFromGroupmailbox", "/PdfPipe/eml-from-groupmailbox.eml", "/PdfPipe/xml-results/eml-from-groupmailbox.xml", "/PdfPipe/results/eml-from-groupmailbox.pdf");
	}

	@Test
	public void gif2Pdf() throws Exception {
		expectSuccessfullConversion("Gif2Pdf", "/PdfPipe/gif.gif", "/PdfPipe/xml-results/gif.xml", "/PdfPipe/results/gif.pdf");
	}

	@Test
	public void htm2Pdf() throws Exception {
		expectSuccessfullConversion("Htm2Pdf", "/PdfPipe/htm.htm", "/PdfPipe/xml-results/htm.xml", "/PdfPipe/results/htm.pdf");
	}

	@Test
	public void html2Pdf() throws Exception {
		expectSuccessfullConversion("Html2Pdf", "/PdfPipe/html.html", "/PdfPipe/xml-results/html.xml", "/PdfPipe/results/html.pdf");
	}

	@Test
	public void jpeg2Pdf() throws Exception {
		expectSuccessfullConversion("Jpeg2Pdf", "/PdfPipe/jpeg.jpeg", "/PdfPipe/xml-results/jpeg.xml", "/PdfPipe/results/jpeg.pdf");
	}

	@Test
	public void jpg2Pdf() throws Exception {
		expectSuccessfullConversion("Jpg2Pdf", "/PdfPipe/jpg.jpg", "/PdfPipe/xml-results/jpg.xml", "/PdfPipe/results/jpg.pdf");
	}

	@Test
	public void log2Pdf() throws Exception {
		expectSuccessfullConversion("Log2Pdf", "/PdfPipe/log.log", "/PdfPipe/xml-results/log.xml", "/PdfPipe/results/log.pdf");
	}

	@Test
	public void png2Pdf() throws Exception {
		expectSuccessfullConversion("Png2Pdf", "/PdfPipe/png.png", "/PdfPipe/xml-results/png.xml", "/PdfPipe/results/png.pdf");
	}

	@Test
	public void ppt2Pdf() throws Exception {
		assumeFalse("This test does not run on Travis-CI / GitHub Actions", TestAssertions.isTestRunningOnCI());
		expectSuccessfullConversion("Ppt2Pdf", "/PdfPipe/ppt.ppt", "/PdfPipe/xml-results/ppt.xml", "/PdfPipe/results/ppt.pdf");
	}

	@Test
	public void rtf2Pdf() throws Exception {
		expectSuccessfullConversion("Rtf2Pdf", "/PdfPipe/rtf.rtf", "/PdfPipe/xml-results/rtf.xml", "/PdfPipe/results/rtf.pdf");
	}

	@Test
	public void tiff2Pdf() throws Exception {
		expectSuccessfullConversion("Tiff2Pdf", "/PdfPipe/tiff.tiff", "/PdfPipe/xml-results/tiff.xml", "/PdfPipe/results/tiff.pdf");
	}

	@Test
	public void txt2Pdf() throws Exception {
		expectSuccessfullConversion("Txt2Pdf", "/PdfPipe/txt.txt", "/PdfPipe/xml-results/txt.xml", "/PdfPipe/results/txt.pdf");
	}

	@Test
	public void zip2Pdf() throws Exception {
		expectUnsuccessfullConversion("Zip2Pdf", "/PdfPipe/PdfPipe.zip", "/PdfPipe/xml-results/zip.xml");
	}

	@Test
	public void emailWithAttachments() throws Exception {
		expectSuccessfullConversion("Txt2Pdf", "/PdfPipe/nestedMail.msg", "/PdfPipe/xml-results/nestedMail.xml", "/PdfPipe/results/nestedMail.pdf");
	}
	
	@Test
	public void excel2pdf() throws Exception {
		expectSuccessfullConversion("xls2pdf", "/PdfPipe/excel.xls", "/PdfPipe/xml-results/xls.xml", "/PdfPipe/results/excel.pdf");
	}

	@Test
	public void xslx2pdf() throws Exception {
		expectSuccessfullConversion("xslx2pdf", "/PdfPipe/fonttest.xlsx", "/PdfPipe/xml-results/xlsx.xml", "/PdfPipe/results/fonttest.pdf");
	}

	@Test
	public void fontTestEmail() throws Exception {
		expectSuccessfullConversion("fontTestEmail", "/PdfPipe/fonttest/fontTestEmail.msg", "/PdfPipe/xml-results/fontTestEmail.xml", "/PdfPipe/results/fontTestEmail.pdf");
	}
	
	@Test
	public void fontTestSlides() throws Exception {
		expectSuccessfullConversion("fontTestSlides", "/PdfPipe/fonttest/fontTestSlides.msg", "/PdfPipe/xml-results/fontTestSlides.xml", "/PdfPipe/results/fontTestSlides.pdf");
	}
	
	@Test
	public void fontTestWord() throws Exception {
		expectSuccessfullConversion("fontTestWord", "/PdfPipe/fonttest/fontTestWord.msg", "/PdfPipe/xml-results/fontTestWord.xml", "/PdfPipe/results/fontTestWord.pdf");
	}
	
	@Test
	public void mailWithExcelAttachment() throws Exception {
		expectSuccessfullConversion("mailWithExcelAttachment", "/PdfPipe/MailWithAttachments/mailWithExcelAttachment.msg", "/PdfPipe/xml-results/mailWithExcelAttachment.xml", "/PdfPipe/results/mailWithExcelAttachment.pdf");
	}
	
	@Test
	public void mailWithImage() throws Exception {
		expectSuccessfullConversion("mailWithImage", "/PdfPipe/MailWithAttachments/mailWithImage.msg", "/PdfPipe/xml-results/mailWithImage.xml", "/PdfPipe/results/mailWithImage.pdf");
	}
	
	@Test
	public void mailWithPdfAttachment() throws Exception {
		expectSuccessfullConversion("mailWithPdfAttachment", "/PdfPipe/MailWithAttachments/mailWithPdfAttachment.msg", "/PdfPipe/xml-results/mailWithPdfAttachment.xml", "/PdfPipe/results/mailWithPdfAttachment.pdf");
	}

	@Test
	public void mailWithWordAttachment() throws Exception {
		expectSuccessfullConversion("mailWithWordAttachment", "/PdfPipe/MailWithAttachments/mailWithWordAttachment.msg", "/PdfPipe/xml-results/mailWithWordAttachment.xml", "/PdfPipe/results/mailWithWordAttachment.pdf");
	}
	
	@Test
	public void multiThreadedMailWithWordAttachment() throws Exception {
		pipe.setName("multiThreadedmailWithWordAttachment");
		pipe.setAction(DocumentAction.CONVERT);
		pipe.registerForward(new PipeForward("success", "dummy"));
		pipe.configure();
		pipe.start();

		PipeLineSession session = new PipeLineSession();
		List<URL> inputs = new ArrayList<URL>();
		for(int i = 0; i<5; i++) {
			inputs.add(TestFileUtils.getTestFileURL("/PdfPipe/MailWithAttachments/mailWithWordAttachment.msg"));
		}
		String expected = TestFileUtils.getTestFileMessage("/PdfPipe/xml-results/mailWithWordAttachment.xml").asString();
		inputs.parallelStream().forEach(item -> {
			try {
				PipeRunResult prr = pipe.doPipe(Message.asMessage(new File(item.toURI())), session);
				Message result = prr.getResult();
				MatchUtils.assertXmlEquals("Conversion XML does not match", applyIgnores(result.asString()), applyIgnores(expected), true);
			} catch (Exception e) {
				fail("Failed to execute test " + e.getMessage());
			}
		});
	}

	@Test(expected = ConfigurationException.class)
	public void wrongPdfOutputLocation() throws Exception {
		pipe.setPdfOutputLocation("not a valid location");
		pipe.configure();
	}

	@Test
	public void nullAction() throws Exception {
		assertThrows("please specify an action for pdf pipe [PdfPipe under test]. possible values: [CONVERT, COMBINE]", ConfigurationException.class, () -> pipe.configure());
	}

	@Test
	public void emptyLicense() throws Exception {
		pipe.setAction(DocumentAction.CONVERT); //without action the pipe will never reach the license block!
		pipe.setLicense("");
		pipe.configure();
	}

	@Test(expected = ConfigurationException.class)
	public void wrongLicense() throws Exception {
		pipe.setAction(DocumentAction.CONVERT); //without action the pipe will never reach the license block!
		pipe.setLicense("test123");//can't find this 'license' file
		pipe.configure();
	}

	@Test
	public void attachFileToMainDoc() throws Exception{
		pipe.setAction(DocumentAction.COMBINE);
		pipe.setMainDocumentSessionKey("mainDoc");
		pipe.setFilenameToAttachSessionKey("attachedFilename");

		Message mainDoc = TestFileUtils.getNonRepeatableTestFileMessage("/PdfPipe/combine/maindoc.pdf");

		session.put(pipe.getMainDocumentSessionKey(), mainDoc);

		session.put(pipe.getFilenameToAttachSessionKey(), "attachedFile");

		Message fileToAttachMainDoc = TestFileUtils.getNonRepeatableTestFileMessage("/PdfPipe/combine/filetobeattached.pdf");
		PipeRunResult prr = doPipe(pipe, fileToAttachMainDoc, session);

		Message result = prr.getResult();
		Path resultingFile = Files.createTempFile("PdfPipeTest", ".pdf");
		Files.copy(result.asInputStream(), resultingFile, StandardCopyOption.REPLACE_EXISTING);
		String expectedFilePath = new File(TestFileUtils.getTestFileURL("/PdfPipe/combine/combined.pdf").toURI()).getCanonicalPath();

		// comparison
		PDFUtil pdfUtil = new PDFUtil();
		assertTrue(pdfUtil.compare(expectedFilePath, resultingFile.toFile().getCanonicalPath()));
	}
}