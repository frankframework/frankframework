package org.frankframework.testutil;

import java.io.IOException;
import java.io.Reader;
import java.nio.CharBuffer;

import jakarta.annotation.Nonnull;

/**
 * For some readers, "close()" is a no-op (for instance, StringReader, which a lot of tests use). We want to
 * ensure that after closing of a reader, all operations fail. This is to make sure that we do not, in tests, read
 * from readers in messages that have been closed.
 */
public class ThrowingAfterCloseReader extends Reader {
	private boolean closed = false;
	private final Reader delegate;

	public ThrowingAfterCloseReader(final Reader delegate) {
		this.delegate = delegate;
	}

	private void assertNotClosed() throws IOException {
		if (closed) {
			throw new IOException("Cannot read after close");
		}
	}

	@Override
	public int read(@Nonnull final CharBuffer target) throws IOException {
		assertNotClosed();
		return delegate.read(target);
	}

	@Override
	public int read() throws IOException {
		assertNotClosed();
		return delegate.read();
	}

	@Override
	public int read(@Nonnull final char[] cbuf) throws IOException {
		assertNotClosed();
		return delegate.read(cbuf);
	}

	@Override
	public long skip(final long n) throws IOException {
		assertNotClosed();
		return delegate.skip(n);
	}

	@Override
	public boolean ready() throws IOException {
		assertNotClosed();
		return delegate.ready();
	}

	@Override
	public boolean markSupported() {
		return delegate.markSupported();
	}

	@Override
	public void mark(final int readAheadLimit) throws IOException {
		assertNotClosed();
		delegate.mark(readAheadLimit);
	}

	@Override
	public void reset() throws IOException {
		assertNotClosed();
		delegate.reset();
	}

	@Override
	public int read(@Nonnull final char[] cbuf, final int off, final int len) throws IOException {
		assertNotClosed();
		return delegate.read(cbuf, off, len);
	}

	@Override
	public void close() throws IOException {
		closed = true;
		delegate.close();
	}
}
