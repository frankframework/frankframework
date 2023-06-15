package nl.nn.adapterframework.pipes;

import java.io.IOException;
import java.io.InputStream;

/**
 * For some streams, "close()" is a no-op (for instance, ByteArrayInputStream, which a lot of tests use). We want to
 * ensure that after closing of stream, all operations fail. This is to make sure that we do not, in tests, read
 * from streams in messages that have been closed.
 */
class ThrowingAfterCloseInputStream extends InputStream {
	private boolean closed = false;
	private final InputStream delegate;

	public ThrowingAfterCloseInputStream(InputStream delegate) {
		this.delegate = delegate;
	}

	private void assertNotClosed() throws IOException {
		if (closed) {
			throw new IOException("Cannot read after close");
		}
	}

	@Override
	public int read() throws IOException {
		assertNotClosed();
		return delegate.read();
	}

	@Override
	public int read(byte[] b) throws IOException {
		assertNotClosed();
		return delegate.read(b);
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		assertNotClosed();
		return delegate.read(b, off, len);
	}

	@Override
	public long skip(long n) throws IOException {
		assertNotClosed();
		return delegate.skip(n);
	}

	@Override
	public int available() throws IOException {
		assertNotClosed();
		return delegate.available();
	}

	@Override
	public synchronized void mark(int readlimit) {
		delegate.mark(readlimit);
	}

	@Override
	public synchronized void reset() throws IOException {
		assertNotClosed();
		delegate.reset();
	}

	@Override
	public boolean markSupported() {
		return delegate.markSupported();
	}

	@Override
	public void close() throws IOException {
		closed = true;
		delegate.close();
		super.close();
	}
}
