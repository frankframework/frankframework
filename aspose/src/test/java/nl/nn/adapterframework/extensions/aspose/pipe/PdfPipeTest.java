/*
   Copyright 2018, 2020 Nationale-Nederlanden

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

import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mock;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeLineSessionBase;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeStartException;
import nl.nn.adapterframework.pipes.PipeTestBase;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.testutil.MatchUtils;
import nl.nn.adapterframework.testutil.TestFileUtils;

/**
 * Executes defined tests against the PdfPipe to ensure the correct working of this pipe.
 * 
 * @author Laurens Mäkel
 */

@Ignore
public class PdfPipeTest extends PipeTestBase<PdfPipe> {
	public final String REGEX_PATH_IGNORE = "(?<=convertedDocument=\").*(?=\")";
	public final String REGEX_TIJDSTIP_IGNORE = "(?<=Tijdstip:).*(?=\" n)";
	public final String[] REGEX_IGNORES = {REGEX_PATH_IGNORE, REGEX_TIJDSTIP_IGNORE};
	@Mock
	private IPipeLineSession session;

	@Override
	public PdfPipe createPipe() {
		return new PdfPipe();
	}

	public void expectSuccessfullConversion(String name, String fileToConvert, String fileContaingExpectedXml, String fileContaingExpectedConversion) throws ConfigurationException, PipeStartException, IOException, PipeRunException {
		String actualXml = executeConversion(name, fileToConvert);
		String expected = TestFileUtils.getTestFile(fileContaingExpectedXml);

		MatchUtils.assertXmlEquals("Conversion XML does not match", applyIgnores(expected), applyIgnores(actualXml), true);

		Pattern pattern = Pattern.compile(REGEX_PATH_IGNORE);
		Matcher matcher = pattern.matcher(actualXml);
		
		if(matcher.find()){
			try {
				String convertedFilePath = matcher.group();
		
				byte[] fileContaingExpectedXmlBytes = Files.readAllBytes(new File(convertedFilePath).toPath());
				byte[] actualFileBytes = Files.readAllBytes(new File(TestFileUtils.getTestFileURL(fileContaingExpectedConversion).toURI()).toPath());
				System.out.println("Comparing files. Converted File: ["+convertedFilePath+"], Expected File: ["+fileContaingExpectedConversion+"]");
				
				int maxDiff = 5;
				int maxDiffCount = 100;
				System.out.println("Performing size comparison. Max Difference: ["+maxDiff+"]");
				int diff = fileContaingExpectedXmlBytes.length - actualFileBytes.length;
				if (diff > 0 && diff < maxDiff) {
					if (diff >= maxDiff) {
						fail("Difference in size is more than " + maxDiff 
								+ " (Expected length: " + fileContaingExpectedXmlBytes.length + " actual length: " + actualFileBytes.length + ")");
					} else {
						// LOGGER.warn("There is a length difference: " + diff + " bytes (but less than: " + maxDiff 
						// 		+ " Expected length: " + fileContaingExpectedXmlBytes.length + " actual length: " + actualFileBytes.length + ")");
					}
				}
		
				// Check only the first part which are in common and allow max 10 difference
				int minLength = Math.min(fileContaingExpectedXmlBytes.length, actualFileBytes.length);
				int diffCount = 0;
				System.out.println("Performing byte comparison. Max Difference: ["+maxDiffCount+"], Min Length: ["+minLength+"]");
				for (int i=0; i < minLength; i++) {
					if (fileContaingExpectedXmlBytes[i] != actualFileBytes[i]) {
						diffCount++;
					}
				}
				if (diffCount > 0) {
					if (diffCount > maxDiffCount) {
						fail("Difference count more than " + maxDiffCount 
								+ " (Number of bytes different: " + diffCount + ")");
					} else {
						//LOGGER.warn("There are more than " + diffCount + " bytes different");
					}
				} 
			} catch (URISyntaxException e) {
				fail("Failed to do byte comparison.");
			}
		}
		else {
			fail("Failed to extract converted file path.");
		}
		
	
	}

	public void expectUnsuccessfullConversion(String name, String fileToConvert, String fileContaingExpectedXml) throws ConfigurationException, PipeStartException, IOException, PipeRunException {
		String actualXml = executeConversion(name, fileToConvert);
		String expected = TestFileUtils.getTestFile(fileContaingExpectedXml);

		MatchUtils.assertXmlEquals("Conversion XML does not match", applyIgnores(expected), applyIgnores(actualXml), true);
	}

	public String executeConversion(String name, String fileToConvert) throws ConfigurationException, PipeStartException, IOException, PipeRunException {
		pipe.setName(name);
		pipe.setAction("convert");
		pipe.configure();
		pipe.start();

		PipeLineSessionBase session = new PipeLineSessionBase();
		String actualXml = null;
		// String input = TestFileUtils.getTestFile(fileToConvert);
		try {
			pipe.doPipe(Message.asMessage(new File(TestFileUtils.getTestFileURL(fileToConvert).toURI())),session);
			actualXml = session.get("documents").toString();
		} catch (URISyntaxException e) {
			fail("Could not convert document due "  + e);
		}

		return actualXml;
	}

	public String applyIgnores(String input){
		String result = input;
		for(String ignore : REGEX_IGNORES){
			result = result.replaceAll(ignore, "IGNORE");
		}
		return result;
	}

	@Test()
	public void Bmp2Pdf() throws ConfigurationException, PipeStartException, IOException, PipeRunException {
		expectSuccessfullConversion("Bmp2Pdf", "/PdfPipe/bmp.bmp", "/PdfPipe/xml-results/bmp.xml", "/PdfPipe/results/bmp.pdf");
	}

	@Test()
	public void DocWord2016Macro2Pdf() throws ConfigurationException, PipeStartException, IOException, PipeRunException {
		expectSuccessfullConversion("DocWord2016Macro2Pdf", "/PdfPipe/docm-word-2016-macro.docm", "/PdfPipe/xml-results/docm-word-2016-macro.xml", "/PdfPipe/results/docm-word-2016-macro.pdf");
	}

	@Test()
	public void DocWord20032Pdf() throws ConfigurationException, PipeStartException, IOException, PipeRunException {
		expectSuccessfullConversion("DocWord20032Pdf", "/PdfPipe/doc-word-2003.doc", "/PdfPipe/xml-results/doc-word-2003.xml", "/PdfPipe/results/doc-word-2003.pdf");
	}

	@Test()
	public void Dot2Pdf() throws ConfigurationException, PipeStartException, IOException, PipeRunException {
		expectSuccessfullConversion("Dot2Pdf", "/PdfPipe/dot.dot", "/PdfPipe/xml-results/dot.xml", "/PdfPipe/results/dot.pdf");
	}

	@Test()
	public void EmlFromGroupmailbox2Pdf() throws ConfigurationException, PipeStartException, IOException, PipeRunException {
		expectSuccessfullConversion("EmlFromGroupmailbox", "/PdfPipe/eml-from-groupmailbox.eml", "/PdfPipe/xml-results/eml-from-groupmailbox.xml", "/PdfPipe/results/eml-from-groupmailbox.pdf");
	}

	@Test()
	public void Gif2Pdf() throws ConfigurationException, PipeStartException, IOException, PipeRunException {
		expectSuccessfullConversion("Gif2Pdf", "/PdfPipe/gif.gif", "/PdfPipe/xml-results/gif.xml", "/PdfPipe/results/gif.pdf");
	}

	@Test()
	public void Htm2Pdf() throws ConfigurationException, PipeStartException, IOException, PipeRunException {
		expectSuccessfullConversion("Htm2Pdf", "/PdfPipe/htm.htm", "/PdfPipe/xml-results/htm.xml", "/PdfPipe/results/htm.pdf");
	}

	@Test()
	public void Html2Pdf() throws ConfigurationException, PipeStartException, IOException, PipeRunException {
		expectSuccessfullConversion("Html2Pdf", "/PdfPipe/html.html", "/PdfPipe/xml-results/html.xml", "/PdfPipe/results/html.pdf");
	}

	@Test()
	public void Jpeg2Pdf() throws ConfigurationException, PipeStartException, IOException, PipeRunException {
		expectSuccessfullConversion("Jpeg2Pdf", "/PdfPipe/jpeg.jpeg", "/PdfPipe/xml-results/jpeg.xml", "/PdfPipe/results/jpeg.pdf");
	}

	@Test()
	public void Jpg2Pdf() throws ConfigurationException, PipeStartException, IOException, PipeRunException {
		expectSuccessfullConversion("Jpg2Pdf", "/PdfPipe/jpg.jpg", "/PdfPipe/xml-results/jpg.xml", "/PdfPipe/results/jpg.pdf");
	}

	@Test()
	public void Log2Pdf() throws ConfigurationException, PipeStartException, IOException, PipeRunException {
		expectSuccessfullConversion("Log2Pdf", "/PdfPipe/log.log", "/PdfPipe/xml-results/log.xml", "/PdfPipe/results/log.pdf");
	}

	@Test()
	public void Png2Pdf() throws ConfigurationException, PipeStartException, IOException, PipeRunException {
		expectSuccessfullConversion("Png2Pdf", "/PdfPipe/png.png", "/PdfPipe/xml-results/png.xml", "/PdfPipe/results/png.pdf");
	}

	@Test()
	public void Ppt2Pdf() throws ConfigurationException, PipeStartException, IOException, PipeRunException {
		expectSuccessfullConversion("Ppt2Pdf", "/PdfPipe/ppt.ppt", "/PdfPipe/xml-results/ppt.xml", "/PdfPipe/results/ppt.pdf");
	}

	@Test()
	public void Rtf2Pdf() throws ConfigurationException, PipeStartException, IOException, PipeRunException {
		expectSuccessfullConversion("Rtf2Pdf", "/PdfPipe/rtf.rtf", "/PdfPipe/xml-results/rtf.xml", "/PdfPipe/results/rtf.pdf");
	}

	@Test()
	public void Tiff2Pdf() throws ConfigurationException, PipeStartException, IOException, PipeRunException {
		expectSuccessfullConversion("Tiff2Pdf", "/PdfPipe/tiff.tiff", "/PdfPipe/xml-results/tiff.xml", "/PdfPipe/results/tiff.pdf");
	}

	@Test()
	public void Txt2Pdf() throws ConfigurationException, PipeStartException, IOException, PipeRunException {
		expectSuccessfullConversion("Txt2Pdf", "/PdfPipe/txt.txt", "/PdfPipe/xml-results/txt.xml", "/PdfPipe/results/txt.pdf");
	}

	@Test()
	public void Zip2Pdf() throws ConfigurationException, PipeStartException, IOException, PipeRunException {
		expectUnsuccessfullConversion("Zip2Pdf", "/PdfPipe/PdfPipe.zip", "/PdfPipe/xml-results/zip.xml");
	}

	@Test(expected = ConfigurationException.class)
	public void emptyPdfOutputLocation() throws ConfigurationException, PipeStartException, IOException, PipeRunException {
		pipe.setPdfOutputLocation("");
		pipe.configure();
	}

	@Test(expected = ConfigurationException.class)
	public void wrongPdfOutputLocation() throws ConfigurationException, PipeStartException, IOException, PipeRunException {
		pipe.setPdfOutputLocation("not a valid location");
		pipe.configure();
	}

	@Test(expected = ConfigurationException.class)
	public void emptyAction() throws ConfigurationException, PipeStartException, IOException, PipeRunException {
		pipe.setAction("");
		pipe.configure();
	}

	@Test(expected = ConfigurationException.class)
	public void wrongAction() throws ConfigurationException, PipeStartException, IOException, PipeRunException {
		pipe.setAction("test123");
		pipe.configure();
	}

	@Test(expected = ConfigurationException.class)
	public void emptyLicense() throws ConfigurationException, PipeStartException, IOException, PipeRunException {
		pipe.setLicense("");
		pipe.configure();
	}

	@Test(expected = ConfigurationException.class)
	public void wrongLicense() throws ConfigurationException, PipeStartException, IOException, PipeRunException {
		pipe.setLicense("test123");
		pipe.configure();
	}

}