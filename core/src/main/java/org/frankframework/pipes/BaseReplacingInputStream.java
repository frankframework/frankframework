package org.frankframework.pipes;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

import jakarta.annotation.Nonnull;

/**
 * Since {@link ReplacingVariablesInputStream} and {@link ReplacingInputStream} share some base overrides,
 * these methods were extracted to this base class.
 *
 * @author Erik van Dongen
 */
public abstract class BaseReplacingInputStream extends FilterInputStream {
	static final byte BYTE_VALUE_END_OF_STREAM = -1;

	protected BaseReplacingInputStream(InputStream in) {
		super(in);
	}

	/**
	 * Make sure to return false here, because we don't support it.
	 * See org.frankframework.stream.Message#readBytesFromInputStream(int)
	 */
	@Override
	public boolean markSupported() {
		return false;
	}

	@Override
	public int read(byte[] b) throws IOException {
		return read(b, 0, b.length);
	}

	/**
	 * copied straight from InputStream implementation, just needed to use {@link #read()} from this class
	 *
	 * @see InputStream#read(byte[], int, int)
	 */
	@Override
	public int read(@Nonnull byte[] b, int off, int len) throws IOException {
		if (off < 0 || len < 0 || len > b.length - off) {
			throw new IndexOutOfBoundsException();
		} else if (len == 0) {
			return 0;
		}

		int c = read();
		if (c == BYTE_VALUE_END_OF_STREAM) {
			return BYTE_VALUE_END_OF_STREAM;
		}
		b[off] = (byte) c;

		int i = 1;
		try {
			for (; i < len; i++) {
				c = read();
				if (c == BYTE_VALUE_END_OF_STREAM) {
					break;
				}
				b[off + i] = (byte) c;
			}
		} catch (IOException ee) {
		}
		return i;
	}
}
