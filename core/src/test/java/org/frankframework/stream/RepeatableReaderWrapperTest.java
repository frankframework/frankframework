package org.frankframework.stream;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Stream;

import jakarta.annotation.Nonnull;

import org.apache.commons.io.input.NullReader;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import org.frankframework.util.AppConstants;
import org.frankframework.util.CloseUtils;
import org.frankframework.util.StreamUtil;

class RepeatableReaderWrapperTest {

	private static final int MAX_IN_MEMORY = AppConstants.getInstance().getInt(Message.MESSAGE_MAX_IN_MEMORY_PROPERTY, (int) Message.MESSAGE_MAX_IN_MEMORY_DEFAULT);

	// These different input-sizes are chosen to get full coverage of all critical code paths
	private static final int SHORT_INPUT_KB = 1; // Shorter than standard stream buffer
	private static final int MEDIUM_INPUT_KB = (StreamUtil.BUFFER_SIZE / 1024) + 1; // Longer than standard stream buffer
	private static final int LONG_INPUT_KB = 3 + (MAX_IN_MEMORY / 1024); // Longer than the in-memory limit
	private static final int VERY_LONG_INPUT_KB = (2 * MEDIUM_INPUT_KB) + (MAX_IN_MEMORY / 1024); // Longer than the in-memory limit

	private static final String STD_INPUT = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
	private static final String INPUT_SPECIAL_CHARS = "håndværkere værgeløn håndværkere";

	RepeatableReaderWrapper readerWrapper;

	@AfterEach
	void tearDown() {
		CloseUtils.closeSilently(readerWrapper);
	}

	static Stream<Arguments> expandedCharacterInputsWithEncoding() {
		List<String> inputs = List.of(STD_INPUT, INPUT_SPECIAL_CHARS);
		List<Integer> sizes = List.of(SHORT_INPUT_KB, MEDIUM_INPUT_KB, LONG_INPUT_KB);
		List<Charset> charsets = List.of(StandardCharsets.UTF_8, StandardCharsets.UTF_16LE, StandardCharsets.ISO_8859_1);

		// Create a stream with all combinations of inputs, sizes, and charsets
		return inputs.stream()
				.flatMap(input -> sizes.stream()
						.flatMap(size -> charsets.stream().map(cs -> {
							String data = expandToSize(input, size);
							Reader source = new StringReader(data);
							return argumentSet("[" + input + "] " + size + "KiB", cs, data, source);
						})));
	}

	static Stream<Arguments> expandedCharacterInputsWithoutEncoding() {
		List<String> inputs = List.of(STD_INPUT, INPUT_SPECIAL_CHARS);
		List<Integer> sizes = List.of(SHORT_INPUT_KB, MEDIUM_INPUT_KB, LONG_INPUT_KB, VERY_LONG_INPUT_KB);

		// Create a stream with all combinations of inputs and sizes
		return inputs.stream()
				.flatMap(input -> sizes.stream()
						.map(size -> {
							String data = expandToSize(input, size);
							Reader source = new StringReader(data);
							return argumentSet("[" + input + "] " + size + "KiB", data, source);
						}));
	}

	@Nonnull
	private static String expandToSize(String input, int sizeInKb) {
		int nrRepetitions = 1 + (sizeInKb * 1024 / input.length());
		return StringUtils.repeat(input, nrRepetitions);
	}

	@ParameterizedTest
	@MethodSource("expandedCharacterInputsWithoutEncoding")
	void asInputStreamReadOnceDefaultEncoding(String expected, Reader source) throws IOException {
		// Arrange
		readerWrapper = new RepeatableReaderWrapper(source);

		assertEquals(Message.MESSAGE_SIZE_UNKNOWN, readerWrapper.size());

		// Act
		String result = assertDoesNotThrow(() -> StreamUtil.streamToString(readerWrapper.asInputStream()));

		// Assert
		assertEquals(expected, result);
		assertEquals(expected.length(), readerWrapper.size());
		assertFalse(readerWrapper.isEmpty());
	}

	@ParameterizedTest
	@MethodSource("expandedCharacterInputsWithEncoding")
	void asInputStreamReadOnceWithEncoding(Charset charset, String expected, Reader source) throws IOException {
		// Arrange
		readerWrapper = new RepeatableReaderWrapper(source);

		assertEquals(Message.MESSAGE_SIZE_UNKNOWN, readerWrapper.size());

		// Act
		String result = assertDoesNotThrow(() -> StreamUtil.streamToString(readerWrapper.asInputStream(charset), charset.name()));

		// Assert
		assertEquals(expected, result);
		assertEquals(expected.length(), readerWrapper.size());
		assertFalse(readerWrapper.isEmpty());
	}

	@ParameterizedTest
	@MethodSource("expandedCharacterInputsWithoutEncoding")
	void asReaderReadParallelSingleChars(String expected, Reader source) throws IOException {
		// Arrange
		readerWrapper = new RepeatableReaderWrapper(source);

		StringWriter sw1 = new StringWriter();
		StringWriter sw2 = new StringWriter();

		// Act
		Reader reader1 = readerWrapper.asReader();
		Reader reader2 = readerWrapper.asReader();

		boolean done1 = false;
		boolean done2 = false;
		while (!(done1 && done2)) {
			int b1 = reader1.read();
			int b2 = reader2.read();

			if (b1 != -1) {
				sw1.write(b1);
			} else {
				done1 = true;
			}
			if (b2 != -1) {
				sw2.write(b2);
			}  else {
				done2 = true;
			}
		}

		// Assert
		String result1 = sw1.toString();
		String result2 = sw2.toString();
		assertAll(
				() -> assertEquals(expected, result1),
				() -> assertEquals(expected, result2),
				() -> assertEquals(expected.length(), readerWrapper.size())
		);
	}

	@ParameterizedTest
	@MethodSource("expandedCharacterInputsWithoutEncoding")
	void asInputStreamReadParallelMultipleBytes(String expected, Reader source) throws IOException {
		// Arrange
		readerWrapper = new RepeatableReaderWrapper(source);

		StringWriter sw1 = new StringWriter();
		StringWriter sw2 = new StringWriter();

		char[] buf1 = new char[3];
		char[] buf2 = new char[3];

		// Act
		Reader reader1 = readerWrapper.asReader();
		Reader reader2 = readerWrapper.asReader();

		boolean done1 = false;
		boolean done2 = false;
		while (!(done1 && done2)) {
			int b1 = reader1.read(buf1);
			int b2 = reader2.read(buf2);

			if (b1 != -1) {
				sw1.write(buf1, 0, b1);
			} else {
				done1 = true;
			}
			if (b2 != -1) {
				sw2.write(buf2, 0, b2);
			}  else {
				done2 = true;
			}
		}

		// Assert
		String result1 = sw1.toString();
		String result2 = sw2.toString();
		assertAll(
				() -> assertEquals(expected, result1),
				() -> assertEquals(expected, result2),
				() -> assertEquals(expected.length(), readerWrapper.size())
		);
	}

	@ParameterizedTest
	@MethodSource("expandedCharacterInputsWithoutEncoding")
	void asInputStreamReadParallelMixed(String expected, Reader source) throws IOException {
		// Arrange
		readerWrapper = new RepeatableReaderWrapper(source);

		StringWriter sw1 = new StringWriter();
		StringWriter sw2 = new StringWriter();

		char[] buf1 = new char[3];

		// Act
		Reader reader1 = readerWrapper.asReader();
		Reader reader2 = readerWrapper.asReader();

		boolean done1 = false;
		boolean done2 = false;
		while (!(done1 && done2)) {
			int b1 = reader1.read(buf1);
			int b2 = reader2.read();

			if (b1 != -1) {
				sw1.write(buf1, 0, b1);
			} else {
				done1 = true;
			}
			if (b2 != -1) {
				sw2.write(b2);
			}  else {
				done2 = true;
			}
		}

		// Assert
		String result1 = sw1.toString();
		String result2 = sw2.toString();
		assertAll(
				() -> assertEquals(expected, result1),
				() -> assertEquals(expected, result2),
				() -> assertEquals(expected.length(), readerWrapper.size())
		);
	}

	@ParameterizedTest
	@MethodSource("expandedCharacterInputsWithoutEncoding")
	void asReaderReadMultipleTimes(String expected, Reader source) {
		// Arrange
		readerWrapper = new RepeatableReaderWrapper(source);

		// Act
		String result1 = assertDoesNotThrow(() -> StreamUtil.readerToString(readerWrapper.asReader(), null));
		String result2 = assertDoesNotThrow(() -> StreamUtil.readerToString(readerWrapper.asReader(), null));

		// Assert
		assertAll(
				() -> assertEquals(expected, result1),
				() -> assertEquals(expected, result2)
		);
	}

	@ParameterizedTest
	@MethodSource("expandedCharacterInputsWithoutEncoding")
	void asReaderReadOnce(String expected, Reader source) {
		// Arrange
		readerWrapper = new RepeatableReaderWrapper(source);

		// Act
		String result1 = assertDoesNotThrow(() -> StreamUtil.readerToString(readerWrapper.asReader(StandardCharsets.UTF_8), null));

		// Assert
		assertEquals(expected, result1);
	}

	@ParameterizedTest
	@MethodSource("expandedCharacterInputsWithoutEncoding")
	void asSerializableNothingYetRead(String expected, Reader source) throws IOException {
		// Arrange
		readerWrapper = new RepeatableReaderWrapper(source);

		// Act
		Object result = readerWrapper.asSerializable();

		// Close readerWrapper, and data should still be there in the serializable result.
		readerWrapper.close();

		// Assert
		int size = Math.toIntExact(readerWrapper.size());

		if (size < MAX_IN_MEMORY) {
			String output = assertInstanceOf(String.class, result);
			assertEquals(expected, output);
		} else  {
			SerializableFileReference output = assertInstanceOf(SerializableFileReference.class, result);
			assertEquals(expected, StreamUtil.readerToString(output.getReader(), null));
		}
	}

	@ParameterizedTest
	@MethodSource("expandedCharacterInputsWithoutEncoding")
	void asSerializablePartiallyRead(String expected, Reader source) throws IOException {
		// Arrange
		readerWrapper = new RepeatableReaderWrapper(source);

		byte[] destination = new byte[expected.length() / 2];
		InputStream inputStream = readerWrapper.asInputStream();
		inputStream.read(destination, 0, destination.length);

		// Act
		Object result = readerWrapper.asSerializable();

		// Assert
		int size = Math.toIntExact(readerWrapper.size());

		if (size < MAX_IN_MEMORY) {
			assertInstanceOf(String.class, result);
		} else  {
			assertInstanceOf(SerializableFileReference.class, result);
		}
	}

	@ParameterizedTest
	@MethodSource("expandedCharacterInputsWithoutEncoding")
	void asSerializableFullyRead(String expected, Reader source) throws IOException {
		// Arrange
		readerWrapper = new RepeatableReaderWrapper(source);

		byte[] destination = new byte[expected.length() + 100]; // Ensure we read everything by trying to read more than everything
		InputStream inputStream = readerWrapper.asInputStream();
		inputStream.read(destination, 0, destination.length);

		// Act
		Object result = readerWrapper.asSerializable();

		// Assert
		int size = Math.toIntExact(readerWrapper.size());

		if (size < MAX_IN_MEMORY) {
			assertInstanceOf(String.class, result);
		} else  {
			assertInstanceOf(SerializableFileReference.class, result);
		}
	}

	@Test
	void isEmpty() throws IOException {
		// Arrange
		readerWrapper = new RepeatableReaderWrapper(new NullReader());

		// Act / Assert

		// Before we read the message or check empty, size should be unknown
		assertEquals(Message.MESSAGE_SIZE_UNKNOWN, readerWrapper.size());

		assertTrue(readerWrapper.isEmpty());

		// After checking size, we should know it to be 0
		assertEquals(0L, readerWrapper.size());
	}
}
