package org.frankframework.stream;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Stream;

import jakarta.annotation.Nonnull;

import org.apache.commons.io.input.NullInputStream;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import org.frankframework.util.AppConstants;
import org.frankframework.util.CloseUtils;
import org.frankframework.util.StreamUtil;

class RepeatableInputStreamWrapperTest {

	private static final int MAX_IN_MEMORY = AppConstants.getInstance().getInt(Message.MESSAGE_MAX_IN_MEMORY_PROPERTY, (int) Message.MESSAGE_MAX_IN_MEMORY_DEFAULT);

	// These different input-sizes are chosen to get full coverage of all critical code paths
	private static final int SHORT_INPUT_KB = 1; // Shorter than standard stream buffer
	private static final int MEDIUM_INPUT_KB = (StreamUtil.BUFFER_SIZE / 1024) + 1; // Longer than standard stream buffer
	private static final int LONG_INPUT_KB = MEDIUM_INPUT_KB + (MAX_IN_MEMORY / 1024); // Longer than the in-memory limit

	private static final String STD_INPUT = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
	private static final String INPUT_SPECIAL_CHARS = "håndværkere værgeløn håndværkere";

	private RepeatableInputStreamWrapper inputStreamWrapper;

	@AfterEach
	void tearDown() {
		CloseUtils.closeSilently(inputStreamWrapper);
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
							byte[] bytes = data.getBytes(cs);
							InputStream source = new ByteArrayInputStream(bytes);
							return argumentSet("[" + input + "] " + size + "KiB", cs, data, source);
						})));
	}

	static Stream<Arguments> expandedCharacterInputsWithoutEncoding() {
		List<String> inputs = List.of(STD_INPUT, INPUT_SPECIAL_CHARS);
		List<Integer> sizes = List.of(SHORT_INPUT_KB, MEDIUM_INPUT_KB, LONG_INPUT_KB);

		// Create a stream with all combinations of inputs and sizes
		return inputs.stream()
				.flatMap(input -> sizes.stream()
						.map(size -> {
							String data = expandToSize(input, size);
							byte[] bytes = data.getBytes();
							InputStream source = new ByteArrayInputStream(bytes);
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
	void asInputStreamReadOnce(String expected, InputStream source) throws IOException {
		// Arrange
		inputStreamWrapper = new RepeatableInputStreamWrapper(source);
		assertEquals(Message.MESSAGE_SIZE_UNKNOWN, inputStreamWrapper.size());

		// Act
		String result = assertDoesNotThrow(() -> StreamUtil.streamToString(inputStreamWrapper.asInputStream()));

		// Assert
		assertEquals(expected, result);
		assertEquals(expected.getBytes().length, inputStreamWrapper.size());
		assertFalse(inputStreamWrapper.isEmpty());
	}

	@ParameterizedTest
	@MethodSource("expandedCharacterInputsWithoutEncoding")
	void asInputStreamReadMultipleTimes(String expected, InputStream source) {
		// Arrange
		inputStreamWrapper = new RepeatableInputStreamWrapper(source);

		// Act
		String result1 = assertDoesNotThrow(() -> StreamUtil.streamToString(inputStreamWrapper.asInputStream()));
		String result2 = assertDoesNotThrow(() -> StreamUtil.streamToString(inputStreamWrapper.asInputStream()));

		// Assert
		assertAll(
				() -> assertEquals(expected, result1),
				() -> assertEquals(expected, result2)
		);
	}

	@ParameterizedTest
	@MethodSource("expandedCharacterInputsWithoutEncoding")
	void asInputStreamReadParallelSingleBytes(String expected, InputStream source) throws IOException {
		// Arrange
		inputStreamWrapper = new RepeatableInputStreamWrapper(source);

		// Arrange
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
				() -> assertEquals(expected, result1),
				() -> assertEquals(expected, result2),
				() -> assertEquals(expected.getBytes().length, inputStreamWrapper.size())
		);
	}

	@ParameterizedTest
	@MethodSource("expandedCharacterInputsWithoutEncoding")
	void asInputStreamReadParallelMultipleBytes(String expected, InputStream source) throws IOException {
		// Arrange
		inputStreamWrapper = new RepeatableInputStreamWrapper(source);
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
				() -> assertEquals(expected, result1),
				() -> assertEquals(expected, result2),
				() -> assertEquals(expected.getBytes().length, inputStreamWrapper.size())
		);
	}

	@ParameterizedTest
	@MethodSource("expandedCharacterInputsWithoutEncoding")
	void asInputStreamReadParallelMixed(String expected, InputStream source) throws IOException {
		// Arrange
		inputStreamWrapper = new RepeatableInputStreamWrapper(source);
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
				() -> assertEquals(expected, result1),
				() -> assertEquals(expected, result2),
				() -> assertEquals(expected.getBytes().length, inputStreamWrapper.size())
		);
	}

	@ParameterizedTest
	@MethodSource("expandedCharacterInputsWithEncoding")
	void asReaderWithEncoding(Charset charset, String expected, InputStream source) {
		// Arrange
		inputStreamWrapper = new RepeatableInputStreamWrapper(source);

		// Act
		String result1 = assertDoesNotThrow(() -> StreamUtil.readerToString(inputStreamWrapper.asReader(charset), null));
		String result2 = assertDoesNotThrow(() -> StreamUtil.readerToString(inputStreamWrapper.asReader(charset), null));

		// Assert
		assertAll(
				() -> assertEquals(expected, result1),
				() -> assertEquals(expected, result2)
		);
	}
	@ParameterizedTest
	@MethodSource("expandedCharacterInputsWithoutEncoding")
	void asReaderWithoutEncoding(String expected, InputStream source) {
		// Arrange
		inputStreamWrapper = new RepeatableInputStreamWrapper(source);

		// Act
		String result1 = assertDoesNotThrow(() -> StreamUtil.readerToString(inputStreamWrapper.asReader(), null));
		String result2 = assertDoesNotThrow(() -> StreamUtil.readerToString(inputStreamWrapper.asReader(), null));

		// Assert
		assertAll(
				() -> assertEquals(expected, result1),
				() -> assertEquals(expected, result2)
		);
	}

	@ParameterizedTest
	@MethodSource("expandedCharacterInputsWithoutEncoding")
	void asSerializableNothingYetRead(String expected, InputStream source) throws IOException {
		// Arrange
		inputStreamWrapper = new RepeatableInputStreamWrapper(source);

		// Act
		Object result = inputStreamWrapper.asSerializable();

		// Close the inputStreamWrapper, and the serializable result should still have all data available
		inputStreamWrapper.close();

		// Assert
		int size = Math.toIntExact(inputStreamWrapper.size());

		if (size < MAX_IN_MEMORY) {
			byte[] bytes = assertInstanceOf(byte[].class, result);
			String output = new  String(bytes);
			assertEquals(expected, output);
		} else  {
			SerializableFileReference output = assertInstanceOf(SerializableFileReference.class, result);
			assertEquals(expected, StreamUtil.streamToString(output.getInputStream()));
		}
	}

	@ParameterizedTest
	@MethodSource("expandedCharacterInputsWithoutEncoding")
	void asSerializablePartiallyRead(String expected, InputStream source) throws IOException {
		// Arrange
		inputStreamWrapper = new RepeatableInputStreamWrapper(source);
		byte[] destination = new byte[expected.length() / 2];
		InputStream inputStream = inputStreamWrapper.asInputStream();
		inputStream.read(destination, 0, destination.length);

		// Act
		Object result = inputStreamWrapper.asSerializable();

		// Assert
		int size = Math.toIntExact(inputStreamWrapper.size());

		if (size < MAX_IN_MEMORY) {
			assertInstanceOf(byte[].class, result);
		} else  {
			assertInstanceOf(SerializableFileReference.class, result);
		}
	}

	@ParameterizedTest
	@MethodSource("expandedCharacterInputsWithoutEncoding")
	void asSerializableFullyRead(String expected, InputStream source) throws IOException {
		// Arrange
		inputStreamWrapper = new RepeatableInputStreamWrapper(source);
		byte[] destination = new byte[expected.length() + 100]; // Ensure we read everything by trying to read more than everything
		InputStream inputStream = inputStreamWrapper.asInputStream();
		inputStream.read(destination, 0, destination.length);

		// Act
		Object result = inputStreamWrapper.asSerializable();

		// Assert
		int size = Math.toIntExact(inputStreamWrapper.size());

		if (size < MAX_IN_MEMORY) {
			assertInstanceOf(byte[].class, result);
		} else  {
			assertInstanceOf(SerializableFileReference.class, result);
		}
	}

	@Test
	void isEmpty() throws IOException {
		// Arrange
		inputStreamWrapper = new RepeatableInputStreamWrapper(new NullInputStream());

		// Act / Assert
		assertTrue(inputStreamWrapper.isEmpty());
	}
}
