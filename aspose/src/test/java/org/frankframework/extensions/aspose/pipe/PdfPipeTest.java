/*
   Copyright 2020-2024 WeAreFrank!

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
package org.frankframework.extensions.aspose.pipe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import jakarta.annotation.Nonnull;

import org.apache.commons.io.FileUtils;

import org.frankframework.testutil.TestAssertions;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.util.FileSystemUtils;
import org.springframework.util.MimeType;

import com.testautomationguru.utility.CompareMode;
import com.testautomationguru.utility.ImageUtil;
import com.testautomationguru.utility.PDFUtil;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.PipeForward;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunResult;
import org.frankframework.extensions.aspose.pipe.PdfPipe.DocumentAction;
import org.frankframework.pipes.PipeTestBase;
import org.frankframework.stream.Message;
import org.frankframework.stream.UrlMessage;
import org.frankframework.testutil.MatchUtils;
import org.frankframework.testutil.MessageTestUtils;
import org.frankframework.testutil.MessageTestUtils.MessageType;
import org.frankframework.testutil.TestFileUtils;
import org.frankframework.util.MessageUtils;

/**
 * Executes defined tests against the PdfPipe to ensure the correct working of this pipe.
 *
 * @author Laurens Mäkel
 */
@Tag("slow")
public class PdfPipeTest extends PipeTestBase<PdfPipe> {
	private static final String REGEX_PATH_IGNORE = "(?<=convertedDocument=\").*?(?=\")";
	private static final String REGEX_TIMESTAMP_IGNORE = "(?<=Timestamp:).*(?=\" n)";
	private static final String[] REGEX_IGNORES = {REGEX_PATH_IGNORE, REGEX_TIMESTAMP_IGNORE};

        //note: this test does not work on ARM64 MacOS. No clue why, but we do not really need it
	@BeforeAll
	public static void setup() {
		assumeFalse(TestAssertions.isTestRunningOnARMMacOS());
	}

	@TempDir
	private Path pdfOutputLocation;

	@Override
	public PdfPipe createPipe() {
		PdfPipe pipe = new PdfPipe();
		pipe.setPdfOutputLocation(pdfOutputLocation.toString());
		pipe.setUnpackCommonFontsArchive(true);
		return pipe;
	}

	@Override
	@AfterEach
	public void tearDown() {
		synchronized(pdfOutputLocation) {
			try {
				FileSystemUtils.deleteRecursively(pdfOutputLocation);
			} catch (IOException e) {
				log.warn("Error deleting temporary file", e);
			}
		}

		super.tearDown();
	}

	public void expectSuccessfulConversion(String pipeName, String fileToConvert, String metadataXml, String expectedFile) throws Exception {
		String documentMetadata = executeConversion(pipeName, fileToConvert);
		String expected = TestFileUtils.getTestFile(metadataXml);

		MatchUtils.assertXmlEquals("Conversion XML does not match", applyIgnores(expected), applyIgnores(documentMetadata), true);

		//Get document for path
		Pattern convertedDocumentPattern = Pattern.compile(REGEX_PATH_IGNORE);
		Matcher convertedDocumentMatcher = convertedDocumentPattern.matcher(documentMetadata);

		if(convertedDocumentMatcher.find()) { //Find converted document location
			String convertedFilePath = convertedDocumentMatcher.group();
			log.debug("found converted file [{}]", convertedFilePath);

			URL expectedFileUrl = TestFileUtils.getTestFileURL(expectedFile);
			assertNotNull(expectedFileUrl, "cannot find expected file ["+expectedFile+"]");
			File file = new File(expectedFileUrl.toURI());
			String expectedFilePath = file.getPath();
			log.debug("converted relative path [{}] to absolute file [{}]", expectedFile, expectedFilePath);

			PDFUtil pdfUtil = createPdfUtil(CompareMode.VISUAL_MODE);
			double compare = pdfUtil.compare(convertedFilePath, expectedFilePath);
			updateExpectedSource(convertedFilePath, expectedFilePath, expectedFile);
			assertEquals(0d, compare, pdfUtil.getAllowedRGBDeviation(), "pdf files ["+convertedFilePath+"] and ["+expectedFilePath+"] should match");
		}
		else {
			fail("failed to extract converted file from documentMetadata xml");
		}
	}

	private void updateExpectedSource(final String convertedFilePath, final String expectedFilePath, final String expectedFile) throws IOException {
		if (!"true".equalsIgnoreCase(System.getProperty("pdfPipeTest.updateExpectations", "false"))) {
			return;
		}

		String prefixPath = expectedFilePath.replace(expectedFile, "");
		File targetResourceFile = Paths.get(prefixPath, "/../../src/test/resources", expectedFile).toFile().getCanonicalFile();

		if (!targetResourceFile.exists() || !targetResourceFile.canWrite()) {
			log.warn("Cannot find or write to target resource file at [{}]", targetResourceFile);
			return;
		}
		log.warn("Overwriting expectation at [{}] with actual output from [{}]", targetResourceFile, convertedFilePath);

		FileUtils.copyFile(new File(convertedFilePath), targetResourceFile);
	}

	@Nonnull
	private static PDFUtil createPdfUtil(CompareMode compareMode) throws IOException {
		PDFUtil pdfUtil = new PDFUtil();
		//remove Aspose evaluation copy information
		pdfUtil.excludeText(
				"(Created with an evaluation copy of Aspose.([a-zA-Z]+). To discover the full versions of our APIs please visit: https:\\/\\/products.aspose.com\\/([a-zA-Z]+)\\/)",
				"Evaluation Only. Created with Aspose\\.\\w+\\. Copyright \\d{4}-\\d{4} Aspose Pty Ltd."
		);
		pdfUtil.enableLog();
		pdfUtil.setCompareMode(compareMode);
		if (compareMode == CompareMode.VISUAL_MODE) {
			pdfUtil.setAllowedRGBDeviation(2d); //In percents, diff is between RGB values
			pdfUtil.highlightPdfDifference(true);
			pdfUtil.setImageDestinationPath(getTargetTestDirectory());
		}
		return pdfUtil;
	}

	/*
	 * The diff is 33% because green and blue are swapped. In these colors the RED channel remains 0, and the GREEN and BLUE channels are swapped.
	 * Half the picture remains unchanged, of the remaining 50% only 66% differs. Thus a 33% total change.
	 */
	@Test
	public void testImageDiff() throws IOException {
		URL rbgw = TestFileUtils.getTestFileURL("/PdfPipe/imageDiffTest/rbgw.png");
		URL rgbw = TestFileUtils.getTestFileURL("/PdfPipe/imageDiffTest/rgbw.png");
		assertNotNull(rbgw, "unable to find [rbgw]");
		assertNotNull(rgbw, "unable to find [rgbw]");
		BufferedImage img1 = ImageIO.read(rbgw.openStream());
		BufferedImage img2 = ImageIO.read(rgbw.openStream());
		double deviation = ImageUtil.getDifferencePercent(img1, img2);
		assertEquals(33, deviation, 0.1);
	}

	// Use surefire folder which is preserved by GitHub Actions
	private static String getTargetTestDirectory() throws IOException {
		File targetFolder = new File(".", "target");
		File sftpTestFS = new File(targetFolder.getCanonicalPath(), "surefire-reports");
		sftpTestFS.mkdir();
		assertTrue(sftpTestFS.exists());

		return sftpTestFS.getAbsolutePath();
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

		Message input = MessageTestUtils.getBinaryMessage(fileToConvert, false);
		pipe.doPipe(input, session);

		//returns <main conversionOption="0" mediaType="xxx/xxx" documentName="filename" numberOfPages="1" convertedDocument="xxx.pdf" />
		return session.getString(pipe.getConversionResultDocumentSessionKey());
	}

	public String applyIgnores(String input){
		String result = input;
		for(String ignore : REGEX_IGNORES){
			result = result.replaceAll(ignore, "IGNORE");
		}
		return result;
	}

	@Test
	public void testFileWithMimeTypeAndCharset() throws Exception {
		pipe.setAction(DocumentAction.CONVERT);
		pipe.configure();
		pipe.start();

		PipeLineSession session = new PipeLineSession();
		Message input = MessageTestUtils.getMessage(MessageType.CHARACTER_UTF8);
		MimeType mimeType = MessageUtils.computeMimeType(input);
		assertEquals("UTF-8", mimeType.getParameter("charset")); //ensure we have a charset in the mimetype
		PipeRunResult result = pipe.doPipe(input, session);

		//returns <main conversionOption="0" mediaType="xxx/xxx" documentName="filename" numberOfPages="1" convertedDocument="xxx.pdf" />
		String responseXml = result.getResult().asString();
		assertFalse(responseXml.contains("failureReason"));
	}

	@Test
	public void bmp2Pdf() throws Exception {
		expectSuccessfulConversion("Bmp2Pdf", "/PdfPipe/bmp.bmp", "/PdfPipe/xml-results/bmp.xml", "/PdfPipe/results/bmp.pdf");
		assertTrue(session.containsKey("documents"));
		assertTrue(session.containsKey("pdfConversionResultFiles1"));
		assertFalse(session.containsKey("pdfConversionResultFiles2"));
		assertFalse(session.containsKey("pdfConversionResultFiles3"));
	}

	@Test
	public void docWord2016Macro2Pdf() throws Exception {
		expectSuccessfulConversion("DocWord2016Macro2Pdf", "/PdfPipe/docm-word-2016-macro.docm", "/PdfPipe/xml-results/docm-word-2016-macro.xml", "/PdfPipe/results/docm-word-2016-macro.pdf");
		assertTrue(session.containsKey("documents"));
		assertTrue(session.containsKey("pdfConversionResultFiles1"));
		assertFalse(session.containsKey("pdfConversionResultFiles2"));
		assertFalse(session.containsKey("pdfConversionResultFiles3"));
	}

	@Test
	public void docWord20032Pdf() throws Exception {
		expectSuccessfulConversion("DocWord20032Pdf", "/PdfPipe/doc-word-2003.doc", "/PdfPipe/xml-results/doc-word-2003.xml", "/PdfPipe/results/doc-word-2003.pdf");
		assertTrue(session.containsKey("documents"));
		assertTrue(session.containsKey("pdfConversionResultFiles1"));
		assertFalse(session.containsKey("pdfConversionResultFiles2"));
		assertFalse(session.containsKey("pdfConversionResultFiles3"));
	}

	@Test
	public void dot2Pdf() throws Exception {
		expectSuccessfulConversion("Dot2Pdf", "/PdfPipe/dot.dot", "/PdfPipe/xml-results/dot.xml", "/PdfPipe/results/dot.pdf");
		assertTrue(session.containsKey("documents"));
		assertTrue(session.containsKey("pdfConversionResultFiles1"));
		assertFalse(session.containsKey("pdfConversionResultFiles2"));
		assertFalse(session.containsKey("pdfConversionResultFiles3"));
	}

	@Test
	public void emlFromGroupmailbox2Pdf() throws Exception {
		expectSuccessfulConversion("EmlFromGroupmailbox", "/PdfPipe/eml-from-groupmailbox.eml", "/PdfPipe/xml-results/eml-from-groupmailbox.xml", "/PdfPipe/results/eml-from-groupmailbox.pdf");
		assertTrue(session.containsKey("documents"));
		assertTrue(session.containsKey("pdfConversionResultFiles1"));
		assertFalse(session.containsKey("pdfConversionResultFiles2"));
		assertFalse(session.containsKey("pdfConversionResultFiles3"));
	}

	@Test
	public void gif2Pdf() throws Exception {
		expectSuccessfulConversion("Gif2Pdf", "/PdfPipe/gif.gif", "/PdfPipe/xml-results/gif.xml", "/PdfPipe/results/gif.pdf");
		assertTrue(session.containsKey("documents"));
		assertTrue(session.containsKey("pdfConversionResultFiles1"));
		assertFalse(session.containsKey("pdfConversionResultFiles2"));
		assertFalse(session.containsKey("pdfConversionResultFiles3"));
	}

	@Test
	public void htm2Pdf() throws Exception {
		expectSuccessfulConversion("Htm2Pdf", "/PdfPipe/htm.htm", "/PdfPipe/xml-results/htm.xml", "/PdfPipe/results/htm.pdf");
		assertTrue(session.containsKey("documents"));
		assertTrue(session.containsKey("pdfConversionResultFiles1"));
		assertFalse(session.containsKey("pdfConversionResultFiles2"));
		assertFalse(session.containsKey("pdfConversionResultFiles3"));
	}

	@Test
	public void html2Pdf() throws Exception {
		expectSuccessfulConversion("Html2Pdf", "/PdfPipe/html.html", "/PdfPipe/xml-results/html.xml", "/PdfPipe/results/html.pdf");
		assertTrue(session.containsKey("documents"));
		assertTrue(session.containsKey("pdfConversionResultFiles1"));
		assertFalse(session.containsKey("pdfConversionResultFiles2"));
		assertFalse(session.containsKey("pdfConversionResultFiles3"));
	}

	@Test
	public void jpeg2Pdf() throws Exception {
		expectSuccessfulConversion("Jpeg2Pdf", "/PdfPipe/jpeg.jpeg", "/PdfPipe/xml-results/jpeg.xml", "/PdfPipe/results/jpeg.pdf");
		assertTrue(session.containsKey("documents"));
		assertTrue(session.containsKey("pdfConversionResultFiles1"));
		assertFalse(session.containsKey("pdfConversionResultFiles2"));
		assertFalse(session.containsKey("pdfConversionResultFiles3"));
	}

	@Test
	public void jpg2Pdf() throws Exception {
		expectSuccessfulConversion("Jpg2Pdf", "/PdfPipe/jpg.jpg", "/PdfPipe/xml-results/jpg.xml", "/PdfPipe/results/jpg.pdf");
		assertTrue(session.containsKey("documents"));
		assertTrue(session.containsKey("pdfConversionResultFiles1"));
		assertFalse(session.containsKey("pdfConversionResultFiles2"));
		assertFalse(session.containsKey("pdfConversionResultFiles3"));
	}

	@Test
	public void log2Pdf() throws Exception {
		expectSuccessfulConversion("Log2Pdf", "/PdfPipe/log.log", "/PdfPipe/xml-results/log.xml", "/PdfPipe/results/log.pdf");
		assertTrue(session.containsKey("documents"));
		assertTrue(session.containsKey("pdfConversionResultFiles1"));
		assertFalse(session.containsKey("pdfConversionResultFiles2"));
		assertFalse(session.containsKey("pdfConversionResultFiles3"));
	}

	@Test
	public void png2Pdf() throws Exception {
		expectSuccessfulConversion("Png2Pdf", "/PdfPipe/png.png", "/PdfPipe/xml-results/png.xml", "/PdfPipe/results/png.pdf");
		assertTrue(session.containsKey("documents"));
		assertTrue(session.containsKey("pdfConversionResultFiles1"));
		assertFalse(session.containsKey("pdfConversionResultFiles2"));
		assertFalse(session.containsKey("pdfConversionResultFiles3"));
	}

	@Test
	public void ppt2Pdf() throws Exception {
		expectSuccessfulConversion("Ppt2Pdf", "/PdfPipe/ppt.ppt", "/PdfPipe/xml-results/ppt.xml", "/PdfPipe/results/ppt.pdf");
		assertTrue(session.containsKey("documents"));
		assertTrue(session.containsKey("pdfConversionResultFiles1"));
		assertFalse(session.containsKey("pdfConversionResultFiles2"));
		assertFalse(session.containsKey("pdfConversionResultFiles3"));
	}

	@Test
	public void rtf2Pdf() throws Exception {
		expectSuccessfulConversion("Rtf2Pdf", "/PdfPipe/rtf.rtf", "/PdfPipe/xml-results/rtf.xml", "/PdfPipe/results/rtf.pdf");
		assertTrue(session.containsKey("documents"));
		assertTrue(session.containsKey("pdfConversionResultFiles1"));
		assertFalse(session.containsKey("pdfConversionResultFiles2"));
		assertFalse(session.containsKey("pdfConversionResultFiles3"));
	}

	@Test
	public void tiff2Pdf() throws Exception {
		expectSuccessfulConversion("Tiff2Pdf", "/PdfPipe/tiff.tiff", "/PdfPipe/xml-results/tiff.xml", "/PdfPipe/results/tiff.pdf");
		assertTrue(session.containsKey("documents"));
		assertTrue(session.containsKey("pdfConversionResultFiles1"));
		assertFalse(session.containsKey("pdfConversionResultFiles2"));
		assertFalse(session.containsKey("pdfConversionResultFiles3"));
	}

	@Test
	public void txt2Pdf() throws Exception {
		expectSuccessfulConversion("Txt2Pdf", "/PdfPipe/txt.txt", "/PdfPipe/xml-results/txt.xml", "/PdfPipe/results/txt.pdf");
		assertTrue(session.containsKey("documents"));
		assertTrue(session.containsKey("pdfConversionResultFiles1"));
		assertFalse(session.containsKey("pdfConversionResultFiles2"));
		assertFalse(session.containsKey("pdfConversionResultFiles3"));
	}

	@Test
	public void zip2Pdf() throws Exception {
		expectUnsuccessfullConversion("Zip2Pdf", "/PdfPipe/PdfPipe.zip", "/PdfPipe/xml-results/zip.xml");
		assertTrue(session.containsKey("documents"));
		assertFalse(session.containsKey("pdfConversionResultFiles1"));
		assertFalse(session.containsKey("pdfConversionResultFiles2"));
		assertFalse(session.containsKey("pdfConversionResultFiles3"));
	}

	@Test
	public void emailWithAttachments() throws Exception {
		expectSuccessfulConversion("Txt2Pdf", "/PdfPipe/nestedMail.msg", "/PdfPipe/xml-results/nestedMail.xml", "/PdfPipe/results/nestedMail.pdf");
		assertTrue(session.containsKey("documents"));
		assertTrue(session.containsKey("pdfConversionResultFiles1"));
		assertFalse(session.containsKey("pdfConversionResultFiles2"));
		assertFalse(session.containsKey("pdfConversionResultFiles3"));
	}

	@Test
	public void excel2pdf() throws Exception {
		expectSuccessfulConversion("xls2pdf", "/PdfPipe/excel.xls", "/PdfPipe/xml-results/xls.xml", "/PdfPipe/results/excel.pdf");
		assertTrue(session.containsKey("documents"));
		assertTrue(session.containsKey("pdfConversionResultFiles1"));
		assertFalse(session.containsKey("pdfConversionResultFiles2"));
		assertFalse(session.containsKey("pdfConversionResultFiles3"));
	}

	@Test
	public void xslx2pdf() throws Exception {
		expectSuccessfulConversion("xslx2pdf", "/PdfPipe/fonttest.xlsx", "/PdfPipe/xml-results/xlsx.xml", "/PdfPipe/results/fonttest.pdf");
		assertTrue(session.containsKey("documents"));
		assertTrue(session.containsKey("pdfConversionResultFiles1"));
		assertFalse(session.containsKey("pdfConversionResultFiles2"));
		assertFalse(session.containsKey("pdfConversionResultFiles3"));
	}

	@Test
	public void fontTestEmail() throws Exception {
		expectSuccessfulConversion("fontTestEmail", "/PdfPipe/fonttest/fontTestEmail.msg", "/PdfPipe/xml-results/fontTestEmail.xml", "/PdfPipe/results/fontTestEmail.pdf");
		assertTrue(session.containsKey("documents"));
		assertTrue(session.containsKey("pdfConversionResultFiles1"));
		assertFalse(session.containsKey("pdfConversionResultFiles2"));
		assertFalse(session.containsKey("pdfConversionResultFiles3"));
	}

	@Test
	public void fontTestSlides() throws Exception {
		expectSuccessfulConversion("fontTestSlides", "/PdfPipe/fonttest/fontTestSlides.msg", "/PdfPipe/xml-results/fontTestSlides.xml", "/PdfPipe/results/fontTestSlides.pdf");
		assertTrue(session.containsKey("documents"));
		assertTrue(session.containsKey("pdfConversionResultFiles1"));
		assertFalse(session.containsKey("pdfConversionResultFiles2"));
		assertFalse(session.containsKey("pdfConversionResultFiles3"));
	}

	@Test
	public void fontTestWord() throws Exception {
		expectSuccessfulConversion("fontTestWord", "/PdfPipe/fonttest/fontTestWord.msg", "/PdfPipe/xml-results/fontTestWord.xml", "/PdfPipe/results/fontTestWord.pdf");
		assertTrue(session.containsKey("documents"));
		assertTrue(session.containsKey("pdfConversionResultFiles1"));
		assertFalse(session.containsKey("pdfConversionResultFiles2"));
		assertFalse(session.containsKey("pdfConversionResultFiles3"));
	}

	@Test
	public void mailWithExcelAttachment() throws Exception {
		expectSuccessfulConversion("mailWithExcelAttachment", "/PdfPipe/MailWithAttachments/mailWithExcelAttachment.msg", "/PdfPipe/xml-results/mailWithExcelAttachment.xml", "/PdfPipe/results/mailWithExcelAttachment.pdf");
		assertTrue(session.containsKey("documents"));
		assertTrue(session.containsKey("pdfConversionResultFiles1"));
		assertFalse(session.containsKey("pdfConversionResultFiles2"));
		assertFalse(session.containsKey("pdfConversionResultFiles3"));
	}

	@Test
	public void mailWithImage() throws Exception {
		expectSuccessfulConversion("mailWithImage", "/PdfPipe/MailWithAttachments/mailWithImage.msg", "/PdfPipe/xml-results/mailWithImage.xml", "/PdfPipe/results/mailWithImage.pdf");
		assertTrue(session.containsKey("documents"));
		assertTrue(session.containsKey("pdfConversionResultFiles1"));
		assertFalse(session.containsKey("pdfConversionResultFiles2"));
		assertFalse(session.containsKey("pdfConversionResultFiles3"));
	}

	@Test
	public void mailWithLargeImage() throws Exception {
		expectSuccessfulConversion("mailWithLargeImage", "/PdfPipe/aspect-ratio/aspect-ratio-test.msg", "/PdfPipe/xml-results/mail-with-large-image.xml", "/PdfPipe/results/mailWithLargeImage.pdf");
		assertTrue(session.containsKey("documents"));
		assertTrue(session.containsKey("pdfConversionResultFiles1"));
		assertFalse(session.containsKey("pdfConversionResultFiles2"));
		assertFalse(session.containsKey("pdfConversionResultFiles3"));
	}

	@Test
	public void mailWithSmallImage() throws Exception {
		expectSuccessfulConversion("mailWithSmallImage", "/PdfPipe/aspect-ratio/mailWithSmallImage.msg", "/PdfPipe/xml-results/mailWithSmallImage.xml", "/PdfPipe/results/mailWithSmallImage.pdf");
		assertTrue(session.containsKey("documents"));
		assertTrue(session.containsKey("pdfConversionResultFiles1"));
		assertFalse(session.containsKey("pdfConversionResultFiles2"));
		assertFalse(session.containsKey("pdfConversionResultFiles3"));
	}

	@Test
	public void mailWithPdfAttachment() throws Exception {
		expectSuccessfulConversion("mailWithPdfAttachment", "/PdfPipe/MailWithAttachments/mailWithPdfAttachment.msg", "/PdfPipe/xml-results/mailWithPdfAttachment.xml", "/PdfPipe/results/mailWithPdfAttachment.pdf");
		assertTrue(session.containsKey("documents"));
		assertTrue(session.containsKey("pdfConversionResultFiles1"));
		assertFalse(session.containsKey("pdfConversionResultFiles2"));
		assertFalse(session.containsKey("pdfConversionResultFiles3"));
	}

	@Test
	public void mailWithWordAttachment() throws Exception {
		expectSuccessfulConversion("mailWithWordAttachment", "/PdfPipe/MailWithAttachments/mailWithWordAttachment.msg", "/PdfPipe/xml-results/mailWithWordAttachment.xml", "/PdfPipe/results/mailWithWordAttachment.pdf");
		assertTrue(session.containsKey("documents"));
		assertTrue(session.containsKey("pdfConversionResultFiles1"));
		assertFalse(session.containsKey("pdfConversionResultFiles2"));
		assertFalse(session.containsKey("pdfConversionResultFiles3"));
	}

	@Test
	public void mailWithAttachmentSaveSeparateFiles() throws Exception {
		pipe.setSaveSeparate(true);

		expectSuccessfulConversion("mailWithAttachmentSaveSeparateFiles", "/PdfPipe/MailWithAttachments/mailWithWordAttachment.msg", "/PdfPipe/xml-results/mailWithWordAttachmentSaveSeparate.xml", "/PdfPipe/results/mailWithWordAttachment.pdf");

		assertTrue(session.containsKey("documents"));
		assertTrue(session.containsKey("pdfConversionResultFiles1"));
		assertTrue(session.containsKey("pdfConversionResultFiles2"));
		assertTrue(session.containsKey("pdfConversionResultFiles3"));
		assertFalse(session.containsKey("pdfConversionResultFiles4"));
	}

	@Test
	public void mailWithAttachmentDifferentSessionKeyNames() throws Exception {
		pipe.setSaveSeparate(true);
		pipe.setConversionResultDocumentSessionKey("output");
		pipe.setConversionResultFilesSessionKey("pdf");

		expectSuccessfulConversion("mailWithAttachmentDifferentSessionKeyNames", "/PdfPipe/MailWithAttachments/mailWithWordAttachment.msg", "/PdfPipe/xml-results/mailWithAttachmentDifferentSessionKeys.xml", "/PdfPipe/results/mailWithWordAttachment.pdf");

		assertFalse(session.containsKey("documents"));
		assertFalse(session.containsKey("pdfConversionResultFiles1"));
		assertTrue(session.containsKey("output"));
		assertTrue(session.containsKey("pdf1"));
		assertTrue(session.containsKey("pdf2"));
		assertTrue(session.containsKey("pdf3"));
	}

	@Test
	public void multiThreadedMailWithWordAttachment() throws Exception {
		pipe.setName("multiThreadedmailWithWordAttachment");
		pipe.setAction(DocumentAction.CONVERT);
		pipe.addForward(new PipeForward("success", "dummy"));
		pipe.configure();
		pipe.start();

		List<Message> inputs = new ArrayList<>();
		URL url = TestFileUtils.getTestFileURL("/PdfPipe/MailWithAttachments/mailWithWordAttachment.msg");
		assertNotNull(url, "unable to find test file");
		for(int i = 0; i<5; i++) {
			inputs.add(new UrlMessage(url));
		}
		String expected = MessageTestUtils.getMessage("/PdfPipe/xml-results/mailWithWordAttachment.xml").asString();
		inputs.parallelStream().forEach(item -> {
			try {
				PipeRunResult prr = pipe.doPipe(item, session);
				Message result = prr.getResult();
				MatchUtils.assertXmlEquals("Conversion XML does not match", applyIgnores(expected), applyIgnores(result.asString()), true);
			} catch (Exception e) {
				log.error("failed to execute test", e);
				fail("Failed to execute test ("+e.getClass()+"): " + e.getMessage());
			}
		});
	}

	@Test
	public void wrongPdfOutputLocation() {
		pipe.setPdfOutputLocation("not a valid location");
		assertThrows(ConfigurationException.class, pipe::configure);
	}

	@Test
	public void nullAction() {
		assertThrows(ConfigurationException.class, pipe::configure, "please specify an action for pdf pipe [PdfPipe under test]. possible values: [CONVERT, COMBINE]");
	}

	@Test
	public void emptyLicense() throws Exception {
		pipe.setAction(DocumentAction.CONVERT); //without action the pipe will never reach the license block!
		pipe.setLicense("");
		pipe.configure();

		List<String> warnings = getConfigurationWarnings().getWarnings();
		assertEquals(1, warnings.size());
		assertTrue(warnings.get(0).contains("Aspose License is not configured"));
	}

	@Test
	public void wrongLicense() {
		pipe.setAction(DocumentAction.CONVERT); //without action the pipe will never reach the license block!
		pipe.setLicense("test123");//can't find this 'license' file
		assertThrows(ConfigurationException.class, pipe::configure);
	}

	@Test
	public void attachFileToMainDoc() throws Exception{
		pipe.setAction(DocumentAction.COMBINE);
		pipe.setMainDocumentSessionKey("mainDoc");
		pipe.setFilenameToAttachSessionKey("attachedFilename");

		Message mainDoc = MessageTestUtils.getBinaryMessage("/PdfPipe/combine/maindoc.pdf", false);

		session.put(pipe.getMainDocumentSessionKey(), mainDoc);

		session.put(pipe.getFilenameToAttachSessionKey(), "attachedFile");

		Message fileToAttachMainDoc = MessageTestUtils.getBinaryMessage("/PdfPipe/combine/filetobeattached.pdf", false);
		PipeRunResult prr = doPipe(pipe, fileToAttachMainDoc, session);

		Message result = prr.getResult();
		Path resultingFile = Files.createTempFile("PdfPipeTest", ".pdf");
		Files.copy(result.asInputStream(), resultingFile, StandardCopyOption.REPLACE_EXISTING);
		String expectedFilePath = new File(TestFileUtils.getTestFileURL("/PdfPipe/combine/combined.pdf").toURI()).getCanonicalPath();

		// comparison
		PDFUtil pdfUtil = createPdfUtil(CompareMode.TEXT_MODE);
		assertEquals(0d, pdfUtil.compare(expectedFilePath, resultingFile.toFile().getCanonicalPath()));
	}
}
