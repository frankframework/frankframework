package org.frankframework.testutil;

import java.io.InputStream;
import java.util.concurrent.atomic.LongAdder;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class VirtualInputStream extends InputStream {
	final LongAdder bytesRead;
	private final long streamSize;

	public VirtualInputStream(final long streamSize) {
		this.streamSize = streamSize;
		bytesRead = new LongAdder();
	}

	@Override
	public int read() {
		if (bytesRead.longValue() >= streamSize) {
			log.info("{}: VirtualInputStream EOF after {} bytes", Thread.currentThread().getName(), bytesRead.longValue());
			return -1;
		}
		bytesRead.increment();
		Thread.yield();
		return 1;
	}
}
