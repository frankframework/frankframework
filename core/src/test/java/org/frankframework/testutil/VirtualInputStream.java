package org.frankframework.testutil;

import java.io.InputStream;
import java.util.concurrent.atomic.LongAdder;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class VirtualInputStream extends InputStream {
	private final static Logger LOG = LogManager.getLogger(VirtualInputStream.class);
	final LongAdder bytesRead;
	private final long streamSize;

	public VirtualInputStream(final long streamSize) {
		this.streamSize = streamSize;
		bytesRead = new LongAdder();
	}

	@Override
	public int read() {
		if (bytesRead.longValue() >= streamSize) {
			LOG.info("{}: VirtualInputStream EOF after {} bytes", Thread.currentThread().getName(), bytesRead.longValue());
			return -1;
		}
		bytesRead.increment();
		Thread.yield();
		return 1;
	}
}
