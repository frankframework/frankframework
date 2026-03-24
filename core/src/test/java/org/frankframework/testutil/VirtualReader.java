package org.frankframework.testutil;

import java.io.IOException;
import java.io.Reader;
import java.util.concurrent.atomic.LongAdder;

import org.jspecify.annotations.NonNull;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class VirtualReader extends Reader {
	final LongAdder charsRead;
	private final long streamSize;

	public VirtualReader(final long streamSize) {
		this.streamSize = streamSize;
		charsRead = new LongAdder();
	}

	@Override
	public int read(@NonNull final char[] cbuf, final int off, final int len) throws IOException {
		if (charsRead.longValue() >= streamSize) {
			log.info("{}: VirtualReader EOF after {} characters", Thread.currentThread().getName(), charsRead.longValue());
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
