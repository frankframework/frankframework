package org.frankframework.testutil;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.Charset;

import jakarta.annotation.Nonnull;

/**
 * Test utility class to produce large amounts of valid structured data (such as JSON or XML) from fixed blocks: opening block for start of document,
 * closing block for end of document, and a repeating block for all the repeated data in between.
 * <br/>
 * This data can be used to test JSON and XML parsers with large volumes of data.
 * <br/>
 * The default test data for JSON and XML formats matches the XSD at: src/test/resources/Validation/Json2Xml/ParameterSubstitution/Main.xsd
 */
public class LargeStructuredMockData {

	public static final String DEFAULT_JSON_OPENING_BLOCK = "[\n";
	public static final String DEFAULT_JSON_CLOSING_BLOCK = """
			{
				"type": "One Two Buckle my Shoe",
				"title": "Three Four Knock at the Door",
				"status": "Five Six Pick up the Sticks",
				"detail": "Seven Eight Lay them Straight",
				"instance": "Nine Ten a Big Fat Hen"
			}
			]
			""";
	public static final String DEFAULT_JSON_REPEATED_BLOCK = """
			{
				"type": "/errors/",
				"title": "There Is Joy In Repetition",
				"status": "DATA_ERROR",
				"detail": "The Devil's In The Details",
				"instance": "/archiving/documents"
			},
			""";
	public static final String DEFAULT_XML_OPENING_BLOCK = """
			<?xml version="1.0" encoding="UTF-8"?>
			<GetDocumentAttributes_Error>
			""";
	public static final String DEFAULT_XML_CLOSING_BLOCK = """
			</GetDocumentAttributes_Error>
			""";
	public static final String DEFAULT_XML_REPEATED_BLOCK = """
			<errors>
				<error>
					<type>/errors/</type>
					<title>There Is Joy In Repetition</title>
					<status>One Two Buckle my Shoe</status>
					<detail>The Devil's In The Details</detail>
					<instance>/archiving/documents</instance>
				</error>
			</errors>
			""";

	/**
	 * Get a Reader that produces at least {@code minDataToProduce} characters of JSON data using the default JSON message.
	 * @param minDataToProduce Minimum number of characters to produce from the Reader.
	 * @return Custom Reader producing JSON data.
	 */
	@Nonnull
	public static Reader getLargeJsonDataReader(long minDataToProduce) {
		return new LargeStructuredMockDataReader(
				minDataToProduce, // Larger than array can be
				DEFAULT_JSON_OPENING_BLOCK,
				DEFAULT_JSON_CLOSING_BLOCK,
				DEFAULT_JSON_REPEATED_BLOCK
		);
	}

	/**
	 * Get a Reader that produces at least {@code minDataToProduce} characters of XML data using the default XML message.
	 * @param minDataToProduce Minimum number of characters to produce from the Reader.
	 * @return Custom Reader producing XML data.
	 */
	@Nonnull
	public static Reader getLargeXmlDataReader(long minDataToProduce) {
		return new LargeStructuredMockDataReader(
				minDataToProduce, // Larger than array can be
				DEFAULT_XML_OPENING_BLOCK,
				DEFAULT_XML_CLOSING_BLOCK,
				DEFAULT_XML_REPEATED_BLOCK
		);
	}

	/**
	 * Get an InputStream that produces at least {@code minDataToProduce} bytes of JSON data using the default JSON message.
	 * @param minDataToProduce Minimum number of bytes to produce from the InputStream.
	 * @return Custom InputStream producing JSON data.
	 */
	@Nonnull
	public static InputStream getLargeJsonDataInputStream(long minDataToProduce, Charset charset) {
		return new LargeStructuredMockDataInputStream(
				minDataToProduce, charset,
				DEFAULT_JSON_OPENING_BLOCK,
				DEFAULT_JSON_CLOSING_BLOCK,
				DEFAULT_JSON_REPEATED_BLOCK
		);
	}

	/**
	 * Get an InputStream that produces at least {@code minDataToProduce} bytes of XML data using the default XML message.
	 * @param minDataToProduce Minimum number of bytes to produce from the InputStream.
	 * @return Custom InputStream producing XML data.
	 */
	@Nonnull
	public static InputStream getLargeXmlDataInputStream(long minDataToProduce, Charset charset) {
		return new LargeStructuredMockDataInputStream(
				minDataToProduce, charset,
				DEFAULT_XML_OPENING_BLOCK,
				DEFAULT_XML_CLOSING_BLOCK,
				DEFAULT_XML_REPEATED_BLOCK
		);
	}

	static class LargeStructuredMockDataReader extends Reader {
		final long minDataToProduce;
		final char[] openingBlock;
		final char[] closingBlock;
		final char[] repeatedBlock;

		boolean isClosed = false;
		long currentAmountProduced;
		char[] currentBuffer;
		int currentIndex;

		public LargeStructuredMockDataReader(long minDataToProduce, String openingBlock, String closingBlock, String repeatedBlock) {
			this.minDataToProduce = minDataToProduce;
			this.openingBlock = openingBlock.toCharArray();
			this.closingBlock = closingBlock.toCharArray();
			this.repeatedBlock = repeatedBlock.toCharArray();

			this.currentBuffer = this.openingBlock;
		}

		@Override
		public int read(@Nonnull char[] cbuf, int off, int len) throws IOException {
			if (isClosed) {
				throw new EOFException("Reader is closed");
			}
			if (currentBuffer != null && currentIndex >= currentBuffer.length) {
				currentBuffer = findNextBuffer();
				currentIndex = 0;
			}
			if (currentBuffer == null) {
				return -1;
			}
			int dataToCopy = Math.min(len, currentBuffer.length - currentIndex);
			System.arraycopy(currentBuffer, currentIndex, cbuf, off, dataToCopy);
			currentIndex += dataToCopy;
			currentAmountProduced += dataToCopy;
			return dataToCopy;
		}

		private char[] findNextBuffer() {
			// Intentionally comparing with the '==' operator!
			if (currentBuffer == null || currentBuffer == closingBlock) {
				return null;
			}
			if (currentAmountProduced > minDataToProduce) {
				return closingBlock;
			}
			return repeatedBlock;
		}

		@Override
		public void close() throws IOException {
			isClosed = true;
		}
	}

	static class LargeStructuredMockDataInputStream extends InputStream {
		final long minDataToProduce;
		final byte[] openingBlock;
		final byte[] closingBlock;
		final byte[] repeatedBlock;

		boolean isClosed = false;
		long currentAmountProduced;
		byte[] currentBuffer;
		int currentIndex;

		public LargeStructuredMockDataInputStream(long minDataToProduce, Charset charset, String openingBlock, String closingBlock, String repeatedBlock) {
			this.minDataToProduce = minDataToProduce;
			this.openingBlock = openingBlock.getBytes(charset);
			this.closingBlock = closingBlock.getBytes(charset);
			this.repeatedBlock = repeatedBlock.getBytes(charset);

			this.currentBuffer = this.openingBlock;
		}

		@Override
		public int read() throws IOException {
			byte[] buf = new byte[1];
			int r = read(buf, 0, 1);
			if (r == -1) {
				return -1;
			}
			return buf[0] & 0xFF;
		}

		@Override
		public int read(@Nonnull byte[] buf, int off, int len) throws IOException {
			if (isClosed) {
				throw new EOFException("Reader is closed");
			}
			if (currentBuffer != null && currentIndex >= currentBuffer.length) {
				currentBuffer = findNextBuffer();
				currentIndex = 0;
			}
			if (currentBuffer == null) {
				return -1;
			}
			int dataToCopy = Math.min(len, currentBuffer.length - currentIndex);
			System.arraycopy(currentBuffer, currentIndex, buf, off, dataToCopy);
			currentIndex += dataToCopy;
			currentAmountProduced += dataToCopy;
			return dataToCopy;
		}

		private byte[] findNextBuffer() {
			// Intentionally comparing with the '==' operator!
			if (currentBuffer == null || currentBuffer == closingBlock) {
				return null;
			}
			if (currentAmountProduced > minDataToProduce) {
				return closingBlock;
			}
			return repeatedBlock;
		}

		@Override
		public void close() throws IOException {
			isClosed = true;
		}
	}
}
