package nl.nn.adapterframework.http;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

import org.junit.Rule;
import org.junit.Test;

import com.github.tomakehurst.wiremock.common.FileSource;
import com.github.tomakehurst.wiremock.common.InputStreamSource;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.extension.ResponseTransformer;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.Response;
import com.github.tomakehurst.wiremock.junit.WireMockRule;

import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.stream.Message;

public class HttpSenderStreamingMessageTest {

	int serverBlockSize = 10000;
	int serverBlockCount = 20;
	int sleepBetweenServedBlocks = 10;

	int clientBlockSize = 10000;

	private ResponseTransformer transformer = new SlowBlockServingResponseTransformer();
	@Rule
	public WireMockRule server = new Server();

	public int totalBlocksServed=0;


	@Test
	public void testStreamingResponseHandlingOfHttpSender() throws Exception {
		HttpSender sender = new HttpSender();

		sender.setUrl(server.baseUrl()+"/file");

		sender.configure();
		sender.open();

		Message input = new Message("");
		PipeLineSession session = new PipeLineSession();


		Message result = sender.sendMessage(input, session);

		try (Reader reader=result.asReader()) {
			int blocksServedAfterFirstBlockRead=Integer.MAX_VALUE;
			int buflen=clientBlockSize;
			int displaylen=40;
			char[] buf = new char[buflen];
			int block=0;

			while (true) {
				int len = reader.read(buf,0,buflen);
				if (block==0) {
					blocksServedAfterFirstBlockRead = totalBlocksServed;
				}
				if (len<0) {
					break;
				}
				System.out.println("read block "+(block++)+": "+new String(buf,0,len<displaylen?len:displaylen));
			}
			int blocksServedAtEndOfReading = totalBlocksServed;

			int blocksServedWhileReading = blocksServedAtEndOfReading-blocksServedAfterFirstBlockRead;
			assertTrue("at least half of total blocks is served after first block is read", blocksServedWhileReading*2 > serverBlockCount);
		}

	}


	private class SlowBlockServingResponseTransformer extends ResponseTransformer {
		private static final String TRANSFORMER_NAME = "file-generator";
		private static final String BLOCK_SIZE_PARAM_NAME = "blockSize";
		private static final String BLOCK_COUNT_PARAM_NAME = "blockCount";


		@Override
		public Response transform(Request request, Response response, FileSource fileSource, Parameters parameters) {
			int blockSize = parameters.getInt(BLOCK_SIZE_PARAM_NAME);
			int blockCount = parameters.getInt(BLOCK_COUNT_PARAM_NAME);
			final String filler;

			{
				String fillerTmp ="";
				for (int i=0; i<blockSize/10; i++) {
					fillerTmp += " 123456789";
				}
				filler = fillerTmp;
			}
			InputStreamSource source = () -> {
				return new InputStream() {
					int i;

					@Override
					public int read(byte[] buf, int off, int len) throws IOException {
						if (i++>blockCount) {
							return -1;
						}
						totalBlocksServed++;
						System.out.println("serve block "+i);
						byte[] block = ("["+i+"]"+filler).getBytes();

						System.arraycopy(block, 0, buf, off, len<blockSize?len:blockSize);
						try {
							Thread.sleep(sleepBetweenServedBlocks);
						} catch (InterruptedException e) {
							throw new IOException(e);
						}
						return len;
					}

					@Override
					public int read() throws IOException {
						if (i<blockCount) {
							return -1;
						}
						System.out.println("read byte");
						return 'x';
					}

				};
			};
			return Response.Builder.like(response).but()
					.body(source).build();
		}

		@Override
		public String getName() {
			return TRANSFORMER_NAME;
		}

		@Override
		public boolean applyGlobally() {
			return false;
		}
	}

	private class Server extends WireMockRule {
		public Server() {
			super(wireMockConfig()
					.dynamicPort()
					.gzipDisabled(true)
					.extensions(transformer));
		}
		public Server(int port) {
			super(port);
		}

		@Override
		public void start() {
			stubFor(any(urlEqualTo("/file"))
						.willReturn(aResponse()
							.withStatus(200)
							.withHeader("Content-Type", "text/plain")
							.withTransformers("file-generator")
							.withTransformerParameter("blockSize", serverBlockSize)
							.withTransformerParameter("blockCount", serverBlockCount)));
			super.start();
		}

	}
}
