package org.frankframework.pipes;

import static org.bson.assertions.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.documentbuilder.DocumentFormat;
import org.frankframework.stream.Message;
import org.frankframework.testutil.LargeStructuredMockData;
import org.frankframework.testutil.TestAssertions;


@Tag("slow")
public class Json2XmlValidatorVeryLargeInputsTest extends PipeTestBase<Json2XmlValidator> {

	private static final long MEGA_BYTE = 1024L * 1024L;

	@BeforeAll
	public static void beforeAll() {
		// These tests collectively take a long time. When running the build locally, it can double
		// the time to run unit tests in the module Core.
		// So by default, the tests only run on the CI environment, and because of the tag "slow", only when running the "slow" tests in CI.
		assumeTrue(TestAssertions.isTestRunningOnCI());
	}

	public static Stream<Arguments> testLargeInputArguments() {
		return Stream.of(
				// Parsing JSON is still very memory-intensive and thus slow. JSON output is also memory intensive but not as slow.
				// So therefore tests parsing JSON have to be disabled for data sizes smaller than tests parsing XML, and to a lesser extent the
				// same goes for tests producing JSON output.
				// Changing the JSON parser to be streaming will be a major undertaking and a major rewrite of the code.
				arguments(DocumentFormat.XML, DocumentFormat.JSON, 1),
				arguments(DocumentFormat.XML, DocumentFormat.XML, 1),
				arguments(DocumentFormat.JSON, DocumentFormat.XML, 1),
				arguments(DocumentFormat.JSON, DocumentFormat.JSON, 1),
				arguments(DocumentFormat.XML, DocumentFormat.JSON, 10),
				arguments(DocumentFormat.XML, DocumentFormat.XML, 10),
				arguments(DocumentFormat.JSON, DocumentFormat.XML, 10), // Doable but already takes a very long time (~18-50 seconds)
				arguments(DocumentFormat.JSON, DocumentFormat.JSON, 10), // Doable but already takes a very long time (~18-50 seconds)
				arguments(DocumentFormat.XML, DocumentFormat.JSON, 100),
				arguments(DocumentFormat.XML, DocumentFormat.XML, 100),
//				arguments(DocumentFormat.JSON, DocumentFormat.XML, 100), // Works but takes too long with current JSON parser
//				arguments(DocumentFormat.JSON, DocumentFormat.JSON, 100), // Works but takes too long with current JSON parser
//				arguments(DocumentFormat.XML, DocumentFormat.JSON, 500), // Doable but takes a very long time (~27-50 seconds)
				arguments(DocumentFormat.XML, DocumentFormat.XML, 500)
//				arguments(DocumentFormat.JSON, DocumentFormat.XML, 500), // Too large to complete in reasonable amount of time with current JSON parser
//				arguments(DocumentFormat.JSON, DocumentFormat.JSON, 500), // Too large to complete in reasonable amount of time with current JSON parser
//				arguments(DocumentFormat.XML, DocumentFormat.JSON, 1023), // Too big for Jenkins CI. Could still fit in memory as single array. Barely testable locally.
//				arguments(DocumentFormat.XML, DocumentFormat.XML, 1023), // Takes around ~24-45 seconds which is pretty good considering it is nearly 1 Gigabyte
//				arguments(DocumentFormat.JSON, DocumentFormat.XML, 1023), // Below are all too large to be practical to test
//				arguments(DocumentFormat.JSON, DocumentFormat.JSON, 1023),
//				arguments(DocumentFormat.XML, DocumentFormat.JSON, 2049), // Larger than array can be. Takes far too long.
//				arguments(DocumentFormat.JSON, DocumentFormat.XML, 2049),
//				arguments(DocumentFormat.JSON, DocumentFormat.JSON, 2049),
//				arguments(DocumentFormat.XML, DocumentFormat.JSON, 2049),
//				arguments(DocumentFormat.XML, DocumentFormat.XML, 2049) // Takes about ~140-160 seconds. But it works now.
		);
	}

	@Override
	public Json2XmlValidator createPipe() {
		return new Json2XmlValidator();
	}

	@ParameterizedTest(name = "[{index}] {argumentsWithNames}MiB")
	@MethodSource("testLargeInputArguments")
	public void testLargeCharInput(DocumentFormat inputFormat, DocumentFormat outputFormat, long minDataSize) throws Exception {
		// Arrange
		log.info("<*> Testing large char input, inputFormat [{}], outputFormat [{}], minDataSize [{}MiB]", inputFormat, outputFormat, minDataSize);
		configurePipe(pipe, inputFormat, outputFormat);

		long actualMinSize = minDataSize * MEGA_BYTE;
		Reader reader;
		if (inputFormat == DocumentFormat.XML) {
			reader = LargeStructuredMockData.getLargeXmlDataReader(actualMinSize);
		} else {
			reader = LargeStructuredMockData.getLargeJsonDataReader(actualMinSize);
		}
		Message input = Message.asMessage(reader);

		// Act / Assert
		runTestAndAssert(inputFormat, outputFormat, actualMinSize, input);
	}

	private void runTestAndAssert(DocumentFormat inputFormat, DocumentFormat outputFormat, long minDataSize, Message input) throws Exception {
		// Act
		try (PipeRunResult prr = pipe.doPipe(input, session)) {
			Message result = prr.getResult();
			System.err.println("Output size: " + result.size() + "; input = " + inputFormat + "; output = " + outputFormat);

			// Assert
			assertResultSize(minDataSize, result);

			// Don't yet know what else to assert.
		} catch (OutOfMemoryError e) {
			fail("Test ran out of memory with inputFormat [" + inputFormat + "], outputFormat [" + outputFormat + "], minDataSize [" + minDataSize + "MiB]");
		}
	}

	@ParameterizedTest(name = "[{index}] {argumentsWithNames}MiB")
	@MethodSource("testLargeInputArguments")
	public void testLargeBinaryInput(DocumentFormat inputFormat, DocumentFormat outputFormat, long minDataSize) throws Exception {
		// Arrange
		log.info("<*> Testing large binary input, inputFormat [{}], outputFormat [{}], minDataSize [{}MiB]", inputFormat, outputFormat, minDataSize);
		configurePipe(pipe, inputFormat, outputFormat);

		long actualMinSize = minDataSize * MEGA_BYTE;
		InputStream inputStream;
		if (inputFormat == DocumentFormat.XML) {
			inputStream = LargeStructuredMockData.getLargeXmlDataInputStream(actualMinSize, StandardCharsets.UTF_8);
		} else {
			inputStream = LargeStructuredMockData.getLargeJsonDataInputStream(actualMinSize, StandardCharsets.UTF_8);
		}
		Message input = Message.asMessage(inputStream);

		// Act / Assert
		runTestAndAssert(inputFormat, outputFormat, actualMinSize, input);
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
