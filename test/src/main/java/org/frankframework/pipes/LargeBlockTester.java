/*
   Copyright 2022 WeAreFrank!

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package org.frankframework.pipes;

import static java.lang.Math.min;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.annotation.Nonnull;

import lombok.Setter;

import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.stream.Message;

public class LargeBlockTester extends FixedForwardPipe {

	private @Setter int blockSize = 10000;
	private @Setter int blockCount = 20;
	private @Setter int sleepBetweenServedBlocks = 0;
	private @Setter Direction direction=Direction.PRODUCE;

	private static final AtomicInteger totalBlocksServed = new AtomicInteger();

	public enum Direction {
		PRODUCE,
		CONSUME
	}

	@Override
	public PipeRunResult doPipe(Message message, PipeLineSession session) throws PipeRunException {
		Message result;
		if (direction==Direction.PRODUCE) {

			final byte[] filler = buildDataBuffer();
			final long bytesToServe = blockCount * (long) blockSize;
			result = new Message(new InputStream() {
				int i;
				long bytesLeftToServe = bytesToServe;

				@Override
				public int read(byte[] buf, int off, int len) throws IOException {
					if (bytesLeftToServe <= 0L) {
						return -1;
					}
					final int servedSize = (int) min(len, bytesLeftToServe);
					log.debug("serve block [{}] of size [{}]", i, servedSize);

					copyToOutputBuffer(buf, off, servedSize);
					bytesLeftToServe -= servedSize;
					totalBlocksServed.incrementAndGet();

					if (sleepBetweenServedBlocks > 0) {
						try {
							Thread.sleep(sleepBetweenServedBlocks);
						} catch (InterruptedException e) {
							Thread.currentThread().interrupt();
							throw new IOException(e);
						}
					}
					return servedSize;
				}

				private void copyToOutputBuffer(byte[] buf, int off, int servedSize) {
					byte[] blockStart = ("[" + i + "]").getBytes(Charset.defaultCharset());
					System.arraycopy(blockStart, 0, buf, off, min(blockStart.length, servedSize));
					int bytesLeft = servedSize - blockStart.length;
					int offset = off + blockStart.length;
					while (bytesLeft > 0) {
						int blockCopySize = min(filler.length, bytesLeft);
						System.arraycopy(filler, 0, buf, offset, blockCopySize);
						offset += blockCopySize;
						bytesLeft -= blockCopySize;
					}
				}

				@Override
				public int read() throws IOException {
					if (bytesLeftToServe <= 0) {
						return -1;
					}
					log.debug("serve byte");
					--bytesLeftToServe;
					return 'x';
				}

			});
		} else {
			try (Reader reader=message.asReader()) {
				int blocksServedAfterFirstBlockRead=Integer.MAX_VALUE;
				int buflen=blockSize;
				int displaylen=40;
				int bytesRead=0;
				char[] buf = new char[buflen];
				int block=0;

				while (true) {
					int len = reader.read(buf,0,buflen);
					if (block==0) {
						blocksServedAfterFirstBlockRead = totalBlocksServed.get();
					}
					if (len < 0) {
						break;
					}
					bytesRead += len;
					log.debug("read block [{}] of size [{}]: {}", block++, len, new String(buf, 0, len < displaylen ? len : displaylen));
				}
				int blocksServedAtEndOfReading = totalBlocksServed.get();

				int blocksServedWhileReading = blocksServedAtEndOfReading - blocksServedAfterFirstBlockRead;
				boolean moreThanHalfOfBlocksProducedWhileReading = blocksServedWhileReading * 2 > blockCount;

				result = new Message("bytesRead [" + bytesRead + "], more than half of blocks produced while reading [" + moreThanHalfOfBlocksProducedWhileReading + "]");
			} catch (IOException e) {
				throw new PipeRunException(this, "Cannot consume blocks", e);
			}

		}
		return new PipeRunResult(getSuccessForward(), result);
	}

	@Nonnull
	private byte[] buildDataBuffer() {
		final String filler;
		StringBuilder fillerTmp = new StringBuilder();
		for (int i = 0; i < blockSize / 10; i++) {
			fillerTmp.append(" 123456789");
		}
		filler = fillerTmp.toString();
		return filler.getBytes(Charset.defaultCharset());
	}

}
