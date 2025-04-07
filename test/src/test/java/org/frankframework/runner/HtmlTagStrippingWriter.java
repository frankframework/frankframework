package org.frankframework.runner;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

import jakarta.annotation.Nonnull;

import org.apache.commons.text.StringEscapeUtils;

class HtmlTagStrippingWriter extends Writer {
	private final OutputStream out;
	private boolean writingTag = false;
	private boolean lastCharWasNewLine = false;
	private boolean writingHtmlEntity = false;
	private final StringBuffer htmlEntityBuffer = new StringBuffer();

	/**
	 * Create a new filtered writer.
	 *
	 * @param out a Writer object to provide the underlying stream.
	 * @throws NullPointerException if {@code out} is {@code null}
	 */
	protected HtmlTagStrippingWriter(@Nonnull OutputStream out) {
		this.out = out;
	}

	@Override
	public void write(int c) throws IOException {
		if (c == '<') {
			writingTag = true;
		} else if (c == '>' && writingTag) {
			writingTag = false;
		} else if (writingHtmlEntity) {
			htmlEntityBuffer.append((char) c);
			if (c == ';') {
				writingHtmlEntity = false;
				writeHtmlEntity();
			}
		} else if (c == '&') {
			writingHtmlEntity = true;
			htmlEntityBuffer.append((char) c);
		} else if (!writingTag) {
			boolean isNewLine = (c == '\n' || c == '\r');
			if (isNewLine) {
				if (!lastCharWasNewLine) {
					out.write(c); // Since we strip tags we might end up with lots of empty lines, try to avoid that.
				}
			} else {
				out.write(c);
			}
			lastCharWasNewLine = isNewLine;
		}
	}

	private void writeHtmlEntity() throws IOException {
		String entity = StringEscapeUtils.unescapeHtml4(htmlEntityBuffer.toString());
		out.write(entity.getBytes(StandardCharsets.UTF_8));
		htmlEntityBuffer.setLength(0);
	}

	@Override
	public void write(@Nonnull char[] cbuf, int off, int len) throws IOException {
		for (int i = off; i < off + len; i++) {
			write(cbuf[i]);
		}
	}

	@Override
	public void write(@Nonnull char[] cbuf) throws IOException {
		for (int i = 0; i < cbuf.length; i++) {
			write(cbuf[i]);
		}
	}

	@Override
	public void write(@Nonnull String str) throws IOException {
		for (int i = 0; i < str.length(); i++) {
			write(str.charAt(i));
		}
	}

	@Override
	public void write(@Nonnull String str, int off, int len) throws IOException {
		for (int i = off; i < off + len; i++) {
			write(str.charAt(i));
		}
	}

	@Override
	public void flush() {
		// No-op
	}

	@Override
	public void close() {
		// No-op
	}
}
