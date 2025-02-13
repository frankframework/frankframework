package org.frankframework.pipes;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.documentbuilder.DocumentFormat;
import org.frankframework.stream.Message;
import org.frankframework.testutil.LargeStructuredMockData;


@Tag("slow")
public class Json2XmlValidatorVeryLargeInputsTest extends PipeTestBase<Json2XmlValidator> {

	public static Stream<Arguments> testLargeInputArguments() {
		return Stream.of(
				arguments(DocumentFormat.XML, DocumentFormat.JSON, 1_000_000),
				arguments(DocumentFormat.XML, DocumentFormat.XML, 1_000_000),
				arguments(DocumentFormat.JSON, DocumentFormat.XML, 1_000_000),
				arguments(DocumentFormat.JSON, DocumentFormat.JSON, 1_000_000),
				arguments(DocumentFormat.XML, DocumentFormat.JSON, 10_000_000),
				arguments(DocumentFormat.XML, DocumentFormat.XML, 10_000_000),
				arguments(DocumentFormat.JSON, DocumentFormat.XML, 10_000_000),
				arguments(DocumentFormat.JSON, DocumentFormat.JSON, 10_000_000),
				arguments(DocumentFormat.XML, DocumentFormat.JSON, 100_000_000),
				arguments(DocumentFormat.XML, DocumentFormat.XML, 100_000_000),
				arguments(DocumentFormat.JSON, DocumentFormat.XML, 100_000_000),
				arguments(DocumentFormat.JSON, DocumentFormat.JSON, 100_000_000),
				arguments(DocumentFormat.XML, DocumentFormat.JSON, 500_000_000),
				arguments(DocumentFormat.XML, DocumentFormat.XML, 500_000_000),
//				arguments(DocumentFormat.JSON, DocumentFormat.XML, 500_000_000), // Too large to complete in reasonable amount of time with current JSON parser
//				arguments(DocumentFormat.JSON, DocumentFormat.JSON, 500_000_000), // Too large to complete in reasonable amount of time with current JSON parser
//				arguments(DocumentFormat.XML, DocumentFormat.JSON, Integer.MAX_VALUE / 2 - 100), // Too big for Jenkins CI. Could still fit in memory as single array. Barely testable.
				arguments(DocumentFormat.XML, DocumentFormat.XML, Integer.MAX_VALUE / 2 - 100)
//				Arguments.arguments(DocumentFormat.JSON, DocumentFormat.XML, Integer.MAX_VALUE / 2 - 100), // Below are all too large to be practical to test
//				Arguments.arguments(DocumentFormat.JSON, DocumentFormat.JSON, Integer.MAX_VALUE / 2 - 100),
//				Arguments.arguments(DocumentFormat.XML, DocumentFormat.JSON, Integer.MAX_VALUE + 100L), // Larger than array can be
//				Arguments.arguments(DocumentFormat.JSON, DocumentFormat.JSON, Integer.MAX_VALUE + 100L)
		);
	}

	@Override
	public Json2XmlValidator createPipe() {
		return new Json2XmlValidator();
	}

	@ParameterizedTest
	@MethodSource("testLargeInputArguments")
	public void testLargeCharInput(DocumentFormat inputFormat, DocumentFormat outputFormat, long minDataSize) throws Exception {
		// Arrange
		log.info("<*> Testing large char input, inputFormat [{}], outputFormat [{}], minDataSize [{}]", inputFormat, outputFormat, minDataSize);
		configurePipe(pipe, inputFormat, outputFormat);

		Reader reader;
		if (inputFormat == DocumentFormat.XML) {
			reader = LargeStructuredMockData.getLargeXmlDataReader(minDataSize);
		} else {
			reader = LargeStructuredMockData.getLargeJsonDataReader(minDataSize);
		}
		Message input = Message.asMessage(reader);

		// Act
		PipeRunResult prr = pipe.doPipe(input, session);
		Message result = prr.getResult();
		System.err.println("Output size: " + result.size() + "; input = " + inputFormat + "; output = " + outputFormat);

		// Assert
		assertResultSize(minDataSize, result);

		// Don't yet know what else to assert.
	}

	@Disabled("Something weird still happens that causes the binary data not to be read correctly")
	@ParameterizedTest
	@MethodSource("testLargeInputArguments")
	public void testLargeBinaryInput(DocumentFormat inputFormat, DocumentFormat outputFormat, long minDataSize) throws Exception {
		// Arrange
		log.info("<*> Testing large binary input, inputFormat [{}], outputFormat [{}], minDataSize [{}]", inputFormat, outputFormat, minDataSize);
		configurePipe(pipe, inputFormat, outputFormat);

		InputStream inputStream;
		if (inputFormat == DocumentFormat.XML) {
			inputStream = LargeStructuredMockData.getLargeXmlDataInputStream(minDataSize, StandardCharsets.UTF_8);
		} else {
			inputStream = LargeStructuredMockData.getLargeJsonDataInputStream(minDataSize, StandardCharsets.UTF_8);
		}
		Message input = Message.asMessage(inputStream);

		// Act
		PipeRunResult prr = pipe.doPipe(input, session);
		Message result = prr.getResult();
		System.err.println("Output size: " + result.size() + "; input = " + inputFormat + "; output = " + outputFormat);

		// Assert
		assertResultSize(minDataSize, result);

		// Don't yet know what else to assert.
	}

	private void configurePipe(Json2XmlValidator json2XmlValidator, DocumentFormat inputFormat, DocumentFormat outputFormat) throws ConfigurationException {
		json2XmlValidator.setName("testLargeData");
		json2XmlValidator.setSchema("/Validation/Json2Xml/ParameterSubstitution/Main.xsd");
		json2XmlValidator.setAcceptNamespacelessXml(true);
		json2XmlValidator.setThrowException(true);
		json2XmlValidator.setOutputFormat(outputFormat);
		json2XmlValidator.setRoot("GetDocumentAttributes_Error");
		if (inputFormat == DocumentFormat.JSON) {
			json2XmlValidator.setDeepSearch(true);
		}
		json2XmlValidator.configure();
		json2XmlValidator.start();
	}

	private static void assertResultSize(long minDataSize, Message result) {
		assertFalse(result.isEmpty());

		// JSON and XML size can vary a lot in size so allow a large margin; mostly this guards against errors where the output is really unrealistically small.
		long maxAllowedDelta = minDataSize / 3; // Actual result may be at most 30 pct smaller or larger than minimum input size
		long actualDelta = Math.abs(minDataSize - result.size());
		assertTrue(actualDelta <= maxAllowedDelta, "Difference between output size and minDataSize should be no larger than " + maxAllowedDelta + " but was " + actualDelta + "; minDataSize: " + minDataSize + "; result size: " + result.size());
	}
}
