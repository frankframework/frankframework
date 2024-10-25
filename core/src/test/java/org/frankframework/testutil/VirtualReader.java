package org.frankframework.testutil;

import java.io.IOException;
import java.io.Reader;
import java.util.concurrent.atomic.LongAdder;

import jakarta.annotation.Nonnull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class VirtualReader extends Reader {
	private final static Logger LOG = LogManager.getLogger(VirtualReader.class);
	final LongAdder charsRead;
	private final long streamSize;

	public VirtualReader(final long streamSize) {
		this.streamSize = streamSize;
		charsRead = new LongAdder();
	}

	@Override
	public int read(@Nonnull final char[] cbuf, final int off, final int len) throws IOException {
		if (charsRead.longValue() >= streamSize) {
			LOG.info("{}: VirtualReader EOF after {} characters", Thread.currentThread().getName(), charsRead.longValue());
			return -1;
		}
		long toRead = Math.min(len, streamSize - charsRead.longValue());
		charsRead.add(toRead);
		Thread.yield();
		for (int i = off; i < toRead + off; i++) {
			cbuf[i] = 65;
		}
		return (int) toRead;
	}

	@Override
	public void close() throws IOException {

	}
}
