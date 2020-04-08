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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.File;
import java.nio.file.Files;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.net.URISyntaxException;

import org.junit.Test;
import org.mockito.Mock;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.core.PipeStartException;
import nl.nn.adapterframework.util.Misc;
import nl.nn.adapterframework.testutil.MatchUtils;
import nl.nn.adapterframework.testutil.TestFileUtils;
import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeLineSessionBase;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.testutil.MatchUtils;
import nl.nn.adapterframework.testutil.TestFileUtils;

import nl.nn.adapterframework.pipes.PipeTestBase;
import nl.nn.adapterframework.extensions.aspose.pipe.PdfPipe;

/**
 * Executes defined tests against the PdfPipe to ensure the correct working of this pipe.
 * 
 * @since 7.6
 * @author Laurens Mäkel
 */

public class PdfPipeTest extends PipeTestBase<PdfPipe> {
	public final String PATH_REGEX_IGNORE = "(?<=convertedDocument=\").*(?=\")";
	public final String PATH_REGEX_EXTRACT = "convertedDocument=\"(.*)\"";
	
	@Mock
	private IPipeLineSession session;

	@Override
	public PdfPipe createPipe() {
		return new PdfPipe();
	}

	public void expectSuccessfullConversion(String name, String fileToConvert, String fileContaingExpectedXml, String fileContaingExpectedConversion) throws ConfigurationException, PipeStartException, IOException, PipeRunException {
		String actualXml = executeConversion(name, fileToConvert);
		String expected = TestFileUtils.getTestFile(fileContaingExpectedXml);

		//MatchUtils.assertXmlEquals("Conversion XML does not match", applyIgnores(expected), applyIgnores(actualXml), true);

		Pattern pattern = Pattern.compile(PATH_REGEX_IGNORE);
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
		String input = TestFileUtils.getTestFile(fileToConvert);

		PipeRunResult prr = pipe.doPipe(Message.asMessage(input),session);
		String actualXml = session.get("documents").toString();

		return actualXml;
	}

	public String applyIgnores(String input){
		return input.replaceAll(PATH_REGEX_IGNORE, "IGNORE");
	}

	@Test()
	public void Txt2Pdf() throws ConfigurationException, PipeStartException, IOException, PipeRunException {
		expectSuccessfullConversion("Txt2Pdf", "/PdfPipe/txt.txt", "/PdfPipe/txt-out.xml", "/PdfPipe/txt.pdf");
	}

	@Test()
	public void Xml2Pdf() throws ConfigurationException, PipeStartException, IOException, PipeRunException {
		expectSuccessfullConversion("Xml2Pdf", "/PdfPipe/txt-out.xml", "/PdfPipe/xml-out.xml", "/PdfPipe/xml.pdf");
	}

	@Test()
	public void Zip2Pdf() throws ConfigurationException, PipeStartException, IOException, PipeRunException {
		expectUnsuccessfullConversion("Zip2Pdf", "/PdfPipe/PdfPipe.zip", "/PdfPipe/zip-out.xml");
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