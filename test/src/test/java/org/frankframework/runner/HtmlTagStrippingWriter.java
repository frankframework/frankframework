package org.frankframework.runner;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import jakarta.annotation.Nonnull;

import org.apache.commons.text.StringEscapeUtils;

class HtmlTagStrippingWriter extends Writer {
	private final OutputStream out;
	private boolean writingTag = false;
	private boolean writingTagName = false;
	private boolean lastCharWasNewLine = false;
	private boolean writingHtmlEntity = false;
	private final StringBuffer htmlEntityBuffer = new StringBuffer();
	private final StringBuffer currentTagBuffer = new StringBuffer();
	private @Nonnull Set<String> tagsToStripWithContents = Set.of();

	private String closingTagToFind = null;
	private boolean skipAllUntilTagClosed = false;

	/**
	 * Create a new filtered writer.
	 *
	 * @param out a Writer object to provide the underlying stream.
	 * @throws NullPointerException if {@code out} is {@code null}
	 */
	protected HtmlTagStrippingWriter(@Nonnull OutputStream out) {
		this.out = out;
	}

	protected HtmlTagStrippingWriter(@Nonnull OutputStream out, @Nonnull Set<String> tagsToStripWithContents) {
		this.out = out;
		this.tagsToStripWithContents = tagsToStripWithContents;
	}

	@Override
	public void write(int c) throws IOException {
		if (c == '<') {
			writingTag = true;
			writingTagName = true;
			currentTagBuffer.setLength(0);
		} else if (c == '>' && writingTag) {
			writingTag = false;
			writingTagName = false;
			String tagName = currentTagBuffer.toString();
			if (tagName.equals(closingTagToFind)) {
				skipAllUntilTagClosed = false;
			}
			currentTagBuffer.setLength(0);
		} else if (writingHtmlEntity && !skipAllUntilTagClosed) {
			htmlEntityBuffer.append((char) c);
			if (c == ';') {
				writingHtmlEntity = false;
				writeHtmlEntity();
			}
		} else if (c == '&' && !skipAllUntilTagClosed) {
			writingHtmlEntity = true;
			htmlEntityBuffer.append((char) c);
		} else if (!writingTag && !skipAllUntilTagClosed) {
			boolean isNewLine = (c == '\n' || c == '\r');
			if (isNewLine) {
				if (!lastCharWasNewLine) {
					out.write(c); // Since we strip tags we might end up with lots of empty lines, try to avoid that.
				}
			} else {
				out.write(c);
			}
			lastCharWasNewLine = isNewLine;
		} else if (writingTagName) {
			if (!Character.isWhitespace(c)) {
				currentTagBuffer.append((char) c);
			} else {
				writingTagName = false;
				String tagName = currentTagBuffer.toString();
				if (tagsToStripWithContents.contains(tagName)) {
					closingTagToFind = "/" + tagName;
					skipAllUntilTagClosed = true;
				}
			}
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
