package org.frankframework;

import java.io.EOFException;
import java.io.IOException;
import java.io.Reader;

import jakarta.annotation.Nonnull;

public class LargeStructuredMockDataReader extends Reader {
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

		currentBuffer = this.openingBlock;
	}

	@Override
	public int read(@Nonnull char[] cbuf, int off, int len) throws IOException {
		if (isClosed) {
			throw new EOFException("Reader is closed");
		}
		if (currentIndex >= currentBuffer.length) {
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
