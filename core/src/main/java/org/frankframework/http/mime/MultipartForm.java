/*
   Copyright 2018 Nationale-Nederlanden, 2025 WeAreFrank!

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
package org.frankframework.http.mime;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.List;

import org.apache.http.entity.mime.FormBodyPart;
import org.apache.http.entity.mime.Header;
import org.apache.http.entity.mime.MIME;
import org.apache.http.entity.mime.MinimalField;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.util.Args;
import org.apache.http.util.ByteArrayBuffer;

/**
 * HttpMultipart represents a collection of MIME multipart encoded content bodies.
 */
public class MultipartForm {

	private final List<FormBodyPart> parts;

	private static final ByteArrayBuffer FIELD_SEP = encode(MIME.DEFAULT_CHARSET, ": ");
	private static final ByteArrayBuffer CR_LF = encode(MIME.DEFAULT_CHARSET, "\r\n");
	private static final ByteArrayBuffer TWO_DASHES = encode(MIME.DEFAULT_CHARSET, "--");

	private final Charset charset;
	private final String boundary;

	private static ByteArrayBuffer encode(final Charset charset, final String string) {
		final ByteBuffer encoded = charset.encode(CharBuffer.wrap(string));
		final ByteArrayBuffer bab = new ByteArrayBuffer(encoded.remaining());
		bab.append(encoded.array(), encoded.position(), encoded.remaining());
		return bab;
	}

	private static void writeBytes(final ByteArrayBuffer b, final OutputStream out) throws IOException {
		out.write(b.buffer(), 0, b.length());
	}

	private static void writeBytes(final String s, final Charset charset, final OutputStream out) throws IOException {
		final ByteArrayBuffer b = encode(charset, s);
		writeBytes(b, out);
	}

	private static void writeBytes(final String s, final OutputStream out) throws IOException {
		final ByteArrayBuffer b = encode(MIME.DEFAULT_CHARSET, s);
		writeBytes(b, out);
	}

	protected static void writeField(final MinimalField field, final OutputStream out) throws IOException {
		writeBytes(field.getName(), out);
		writeBytes(FIELD_SEP, out);
		writeBytes(field.getBody(), out);
		writeBytes(CR_LF, out);
	}

	protected static void writeField(final MinimalField field, final Charset charset, final OutputStream out) throws IOException {
		writeBytes(field.getName(), charset, out);
		writeBytes(FIELD_SEP, out);
		writeBytes(field.getBody(), charset, out);
		writeBytes(CR_LF, out);
	}

	/**
	 * Creates an instance with the specified settings.
	 *
	 * @param charset the character set to use. May be {@code null}, in which case {@link MIME#DEFAULT_CHARSET} - i.e. US-ASCII - is used.
	 * @param boundary to use  - must not be {@code null}
	 * @throws IllegalArgumentException if charset is null or boundary is null
	 */
	public MultipartForm(final Charset charset, final String boundary, final List<FormBodyPart> parts) {
		Args.notNull(boundary, "Multipart boundary");
		this.charset = charset != null ? charset : MIME.DEFAULT_CHARSET;
		this.boundary = boundary;
		this.parts = parts;
	}

	public List<FormBodyPart> getBodyParts() {
		return this.parts;
	}

	void doWriteTo(final OutputStream out, final boolean writeContent) throws IOException {
		final ByteArrayBuffer boundaryEncoded = encode(this.charset, this.boundary);
		for (final FormBodyPart part: getBodyParts()) {
			writeBytes(TWO_DASHES, out);
			writeBytes(boundaryEncoded, out);
			writeBytes(CR_LF, out);

			formatMultipartHeader(part, out);

			writeBytes(CR_LF, out);

			if (writeContent) {
				part.getBody().writeTo(out);
			}
			writeBytes(CR_LF, out);
		}
		writeBytes(TWO_DASHES, out);
		writeBytes(boundaryEncoded, out);
		writeBytes(TWO_DASHES, out);
		writeBytes(CR_LF, out);
	}

	/**
	  * Write the multipart header fields; depends on the style.
	  */
	protected void formatMultipartHeader(final FormBodyPart part, final OutputStream out) throws IOException {
		// For strict, we output all fields with MIME-standard encoding.
		final Header header = part.getHeader();
		for (final MinimalField field: header) {
			writeField(field, out);
		}
	}

	/**
	 * Writes out the content in the specified multipart encoding. This method
	 * produces slightly different formatting depending on its compatibility
	 * mode.
	 */
	public void writeTo(final OutputStream out) throws IOException {
		doWriteTo(out, true);
	}

	/**
	 * Determines the total length of the multipart content (content length of
	 * individual parts plus that of extra elements required to delimit the parts
	 * from one another). If any of the {@link FormBodyPart BodyParts} contained in this object
	 * is of a streaming entity of unknown length the total length is also unknown.
	 * <p>
	 * This method buffers only a small amount of data in order to determine the
	 * total length of the entire entity. The content of individual parts is not
	 * buffered.
	 * </p>
	 *
	 * @return total length of the multipart entity if known, {@code -1} otherwise.
	 */
	public long getTotalLength() {
		long contentLen = 0;
		for (final FormBodyPart part: getBodyParts()) {
			final ContentBody body = part.getBody();
			final long len = body.getContentLength();
			if (len >= 0) {
				contentLen += len;
			} else {
				return -1;
			}
		}
		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			doWriteTo(out, false);
			final byte[] extra = out.toByteArray();
			return contentLen + extra.length;
		} catch (final IOException ex) {
			// Should never happen
			return -1;
		}
	}
}
