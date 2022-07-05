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
package nl.nn.adapterframework.pipes;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.concurrent.atomic.AtomicInteger;

import lombok.Setter;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.stream.Message;

public class LargeBlockTester extends FixedForwardPipe {

	private @Setter int blockSize = 10000;
	private @Setter int blockCount = 20;
	private @Setter int sleepBetweenServedBlocks = 0;
	private @Setter Direction direction=Direction.PRODUCE;

	private static AtomicInteger totalBlocksServed = new AtomicInteger();

	public enum Direction {
		PRODUCE,
		CONSUME
	}

	@Override
	public PipeRunResult doPipe(Message message, PipeLineSession session) throws PipeRunException {
		Message result;
		if (direction==Direction.PRODUCE) {
			final String filler;

			{
				String fillerTmp ="";
				for (int i=0; i<blockSize/10; i++) {
					fillerTmp += " 123456789";
				}
				filler = fillerTmp;
			}
			result = new Message(new InputStream() {
						int i;

						@Override
						public int read(byte[] buf, int off, int len) throws IOException {
							if (i++>blockCount) {
								return -1;
							}
							int servedSize=len<blockSize?len:blockSize;
							log.debug("serve block ["+i+"] of size ["+servedSize+"]");
							byte[] block = ("["+i+"]"+filler).getBytes();
							totalBlocksServed.incrementAndGet();
							System.arraycopy(block, 0, buf, off, servedSize);
							if (sleepBetweenServedBlocks>0) {
								try {
									Thread.sleep(sleepBetweenServedBlocks);
								} catch (InterruptedException e) {
									throw new IOException(e);
								}
							}
							return len;
						}

						@Override
						public int read() throws IOException {
							if (i<blockCount) {
								return -1;
							}
							log.debug("serve byte");
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
					if (len<0) {
						break;
					}
					bytesRead+=len;
					log.debug("read block ["+(block++)+"] of size ["+len+"]: "+new String(buf,0,len<displaylen?len:displaylen));
				}
				int blocksServedAtEndOfReading = totalBlocksServed.get();

				int blocksServedWhileReading = blocksServedAtEndOfReading-blocksServedAfterFirstBlockRead;
				boolean moreThanHalfOfBlocksProducedWhileReading = blocksServedWhileReading*2 > blockCount;

				result = new Message("bytesRead ["+bytesRead+"], more than half of blocks produced while reading ["+moreThanHalfOfBlocksProducedWhileReading+"]");
			} catch (IOException e) {
				throw new PipeRunException(this, "Cannot consume blocks", e);
			}

		}
		return new PipeRunResult(getSuccessForward(), result);
	}

}
