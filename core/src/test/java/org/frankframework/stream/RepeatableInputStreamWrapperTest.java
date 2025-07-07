package org.frankframework.stream;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

import jakarta.annotation.Nonnull;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import org.frankframework.util.AppConstants;
import org.frankframework.util.CloseUtils;
import org.frankframework.util.StreamUtil;

class RepeatableInputStreamWrapperTest {

	private static final int MAX_IN_MEMORY = 15;
	// TODO: Move AppConstants to commons and configure this in tests to very small size to fully test the partial buffering
	private static final int STREAM_BUFFER_SIZE = 6;

	private static final String STD_INPUT_SHORT = "ABCDEFGHIJ";
	private static final String STD_INPUT_LONG = "ABCDEFGHIJKLMNOPQRSTUVWXYX";
	private static final String INPUT_SHORT_SPECIAL_CHARS = "håndværkere";
	private static final String INPUT_LONG_SPECIAL_CHARS = "håndværkere værgeløn håndværkere";

	private RepeatableInputStreamWrapper inputStreamWrapper;

	@BeforeAll
	static void beforeAll() {
		AppConstants.removeInstance(); // This property may have already been set by previous tests

		// Set this to a low value between the sizes of "short" and "long" inputs to force the "long"
		// inputs to a size that will overflow to disk and test that path.
		System.setProperty(Message.MESSAGE_MAX_IN_MEMORY_PROPERTY, String.valueOf(MAX_IN_MEMORY));

	}

	@AfterAll
	static void afterAll() {
		System.clearProperty(Message.MESSAGE_MAX_IN_MEMORY_PROPERTY);
	}

	@AfterEach
	void tearDown() {
		CloseUtils.closeSilently(inputStreamWrapper);
	}

	static Stream<Arguments> characterInputs() {
		return Stream.of(arguments(STD_INPUT_SHORT), arguments(STD_INPUT_LONG),
				arguments(INPUT_SHORT_SPECIAL_CHARS), arguments(INPUT_LONG_SPECIAL_CHARS));
	}

	@ParameterizedTest
	@MethodSource("characterInputs")
	void asInputStreamReadOnce(String input) {
		// Arrange
		inputStreamWrapper = new RepeatableInputStreamWrapper(wrapWithInputStream(input));

		assertEquals(Message.MESSAGE_SIZE_UNKNOWN, inputStreamWrapper.size());

		// Act
		String result = assertDoesNotThrow(() -> StreamUtil.streamToString(inputStreamWrapper.asInputStream()));

		// Assert
		assertEquals(input, result);
		assertEquals(input.getBytes().length, inputStreamWrapper.size());
	}

	@Nonnull
	private static ByteArrayInputStream wrapWithInputStream(String input) {
		return new ByteArrayInputStream(input.getBytes());
	}


	@ParameterizedTest
	@MethodSource("characterInputs")
	void asInputStreamReadMultipleTimes(String input) {
		// Arrange
		inputStreamWrapper = new RepeatableInputStreamWrapper(wrapWithInputStream(input));

		// Act
		String result1 = assertDoesNotThrow(() -> StreamUtil.streamToString(inputStreamWrapper.asInputStream()));
		String result2 = assertDoesNotThrow(() -> StreamUtil.streamToString(inputStreamWrapper.asInputStream()));

		// Assert
		assertAll(
				() -> assertEquals(input, result1),
				() -> assertEquals(input, result2)
		);
	}

	@ParameterizedTest
	@MethodSource("characterInputs")
	void asInputStreamReadParallelSingleBytes(String input) throws IOException {
		// Arrange
		inputStreamWrapper = new RepeatableInputStreamWrapper(wrapWithInputStream(input));

		ByteArrayOutputStream boas1 = new ByteArrayOutputStream();
		ByteArrayOutputStream boas2 = new ByteArrayOutputStream();

		// Act
		InputStream inputStream1 = inputStreamWrapper.asInputStream();
		InputStream inputStream2 = inputStreamWrapper.asInputStream();

		boolean done1 = false;
		boolean done2 = false;
		while (!(done1 && done2)) {
			int b1 = inputStream1.read();
			int b2 = inputStream2.read();

			if (b1 != -1) {
				boas1.write(b1);
			} else {
				done1 = true;
			}
			if (b2 != -1) {
				boas2.write(b2);
			}  else {
				done2 = true;
			}
		}

		// Assert
		String result1 = boas1.toString();
		String result2 = boas2.toString();
		assertAll(
				() -> assertEquals(input, result1),
				() -> assertEquals(input, result2),
				() -> assertEquals(input.getBytes().length, inputStreamWrapper.size())
		);
	}

	@ParameterizedTest
	@MethodSource("characterInputs")
	void asInputStreamReadParallelMultipleBytes(String input) throws IOException {
		// Arrange
		inputStreamWrapper = new RepeatableInputStreamWrapper(wrapWithInputStream(input));

		ByteArrayOutputStream boas1 = new ByteArrayOutputStream();
		ByteArrayOutputStream boas2 = new ByteArrayOutputStream();

		byte[] buf1 = new byte[3];
		byte[] buf2 = new byte[3];

		// Act
		InputStream inputStream1 = inputStreamWrapper.asInputStream();
		InputStream inputStream2 = inputStreamWrapper.asInputStream();

		boolean done1 = false;
		boolean done2 = false;
		while (!(done1 && done2)) {
			int b1 = inputStream1.read(buf1);
			int b2 = inputStream2.read(buf2);

			if (b1 != -1) {
				boas1.write(buf1, 0, b1);
			} else {
				done1 = true;
			}
			if (b2 != -1) {
				boas2.write(buf2, 0, b2);
			}  else {
				done2 = true;
			}
		}

		// Assert
		String result1 = boas1.toString();
		String result2 = boas2.toString();
		assertAll(
				() -> assertEquals(input, result1),
				() -> assertEquals(input, result2),
				() -> assertEquals(input.getBytes().length, inputStreamWrapper.size())
		);
	}

	@ParameterizedTest
	@MethodSource("characterInputs")
	void asInputStreamReadParallelMixed(String input) throws IOException {
		// Arrange
		inputStreamWrapper = new RepeatableInputStreamWrapper(wrapWithInputStream(input));

		ByteArrayOutputStream boas1 = new ByteArrayOutputStream();
		ByteArrayOutputStream boas2 = new ByteArrayOutputStream();

		byte[] buf1 = new byte[3];

		// Act
		InputStream inputStream1 = inputStreamWrapper.asInputStream();
		InputStream inputStream2 = inputStreamWrapper.asInputStream();

		boolean done1 = false;
		boolean done2 = false;
		while (!(done1 && done2)) {
			int b1 = inputStream1.read(buf1);
			int b2 = inputStream2.read();

			if (b1 != -1) {
				boas1.write(buf1, 0, b1);
			} else {
				done1 = true;
			}
			if (b2 != -1) {
				boas2.write(b2);
			}  else {
				done2 = true;
			}
		}

		// Assert
		String result1 = boas1.toString();
		String result2 = boas2.toString();
		assertAll(
				() -> assertEquals(input, result1),
				() -> assertEquals(input, result2),
				() -> assertEquals(input.getBytes().length, inputStreamWrapper.size())
		);
	}

	@ParameterizedTest
	@MethodSource("characterInputs")
	void asReader(String input) {
		// Arrange
		inputStreamWrapper = new RepeatableInputStreamWrapper(wrapWithInputStream(input));

		// Act
		String result1 = assertDoesNotThrow(() -> StreamUtil.readerToString(inputStreamWrapper.asReader(), null));
		String result2 = assertDoesNotThrow(() -> StreamUtil.readerToString(inputStreamWrapper.asReader(), null));

		// Assert
		assertAll(
				() -> assertEquals(input, result1),
				() -> assertEquals(input, result2)
		);
	}

	@ParameterizedTest
	@MethodSource("characterInputs")
	void asReaderWithCharset(String input) {
		// Arrange
		inputStreamWrapper = new RepeatableInputStreamWrapper(wrapWithInputStream(input));

		// Act
		String result1 = assertDoesNotThrow(() -> StreamUtil.readerToString(inputStreamWrapper.asReader(StandardCharsets.UTF_8), null));
		String result2 = assertDoesNotThrow(() -> StreamUtil.readerToString(inputStreamWrapper.asReader(StandardCharsets.UTF_8), null));

		// Assert
		assertAll(
				() -> assertEquals(input, result1),
				() -> assertEquals(input, result2)
		);
	}

	@ParameterizedTest
	@MethodSource("characterInputs")
	void preserveNothingYetRead(String input) throws IOException {
		// Arrange
		inputStreamWrapper = new RepeatableInputStreamWrapper(wrapWithInputStream(input));

		// Act
		Object result = inputStreamWrapper.preserve();

		// Assert
		int size = Math.toIntExact(inputStreamWrapper.size());

		if (size < MAX_IN_MEMORY) {
			assertInstanceOf(byte[].class, result);
		} else  {
			assertInstanceOf(SerializableFileReference.class, result);
		}
	}

	@ParameterizedTest
	@MethodSource("characterInputs")
	void preservePartiallyRead(String input) throws IOException {
		// Arrange
		inputStreamWrapper = new RepeatableInputStreamWrapper(wrapWithInputStream(input));

		byte[] destination = new byte[input.length() / 2];
		InputStream inputStream = inputStreamWrapper.asInputStream();
		inputStream.read(destination, 0, destination.length);

		// Act
		Object result = inputStreamWrapper.preserve();

		// Assert
		int size = Math.toIntExact(inputStreamWrapper.size());

		if (size < MAX_IN_MEMORY) {
			assertInstanceOf(byte[].class, result);
		} else  {
			assertInstanceOf(SerializableFileReference.class, result);
		}
	}

	@ParameterizedTest
	@MethodSource("characterInputs")
	void preserveFullyRead(String input) throws IOException {
		// Arrange
		inputStreamWrapper = new RepeatableInputStreamWrapper(wrapWithInputStream(input));

		byte[] destination = new byte[input.length() * 2]; // Ensure we read everything by trying to read more than everything
		InputStream inputStream = inputStreamWrapper.asInputStream();
		inputStream.read(destination, 0, destination.length);

		// Act
		Object result = inputStreamWrapper.preserve();

		// Assert
		int size = Math.toIntExact(inputStreamWrapper.size());

		if (size < MAX_IN_MEMORY) {
			assertInstanceOf(byte[].class, result);
		} else  {
			assertInstanceOf(SerializableFileReference.class, result);
		}
	}
}
