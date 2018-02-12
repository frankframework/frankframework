package nl.nn.adapterframework.http.mime;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


public class MultipartEntity extends BasicHttpEntity implements HttpEntity {

	private final MultipartForm multipart;
	private final Header contentType;
	private final long contentLength;

	MultipartEntity(MultipartForm multipart, final ContentType contentType,final long contentLength) {
		super();
		this.multipart = multipart;
		this.contentType = new BasicHeader(HTTP.CONTENT_TYPE, contentType.toString());
		this.contentLength = contentLength;
	}

	public MultipartForm getMultipart() {
		return this.multipart;
	}

	@Override
	public boolean isRepeatable() {
		return this.contentLength != -1;
	}

	@Override
	public boolean isChunked() {
		return !isRepeatable();
	}

	@Override
	public boolean isStreaming() {
		return !isRepeatable();
	}

	@Override
	public long getContentLength() {
		return this.contentLength;
	}

	@Override
	public Header getContentType() {
		return this.contentType;
	}

	@Override
	public Header getContentEncoding() {
		return null;
	}

	@Override
	public void consumeContent()
		throws IOException, UnsupportedOperationException{
		if (isStreaming()) {
			throw new UnsupportedOperationException(
					"Streaming entity does not implement #consumeContent()");
		}
	}

	@Override
	public InputStream getContent() {
		throw new UnsupportedOperationException(
					"Multipart form entity does not implement #getContent()");
	}

	@Override
	public void writeTo(final OutputStream outstream) throws IOException {
		this.multipart.writeTo(outstream);
	}

}