/*
Copyright 2019-2021 WeAreFrank!

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
package org.frankframework.http.rest;

import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.util.function.Predicate;

import jakarta.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.springframework.util.InvalidMimeTypeException;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;

import org.frankframework.util.EnumUtils;
import org.frankframework.util.StreamUtil;

public enum MediaTypes {

	// common types
	ANY("*/*", null),
	/* (Only for produces) Attempts to detect the MimeType as well as charset when not known */
	DETECT("*/*"),
	TEXT("text/plain"),
	XML("application/xml"),
	JSON("application/json"),
	PDF("application/pdf", null), // raw binary formats do not have a charset
	OCTET("application/octet-stream", null), // raw binary formats do not have a charset
	MULTIPART_RELATED("multipart/related"),
	MULTIPART_FORMDATA("multipart/form-data"),
	MULTIPART("multipart/*"),

	// less but still common types
	TXT("text/plain"),
	CSS("text/css"),
	CSV("text/csv"),
	MJS("text/javascript"),
	HTML("text/html"),
	JAR("application/java-archive"),

	DOC("application/msword"),
	EPUB("application/epub+zip"),
	VSD("application/vnd.visio"),
	AZW("application/vnd.amazon.ebook"),
	PPT("application/vnd.ms-powerpoint"),
	XLS("application/vnd.ms-excel"),
	RAR("application/vnd.rar"),
	RTF("application/rtf"),
	ICO("image/vnd.microsoft.icon"),
	ICS("text/calendar"),

	BMP("image/bmp"),
	GIF("image/gif"),
	PNG("image/png"),
	SVG("image/svg+xml"),
	TIFF("image/tiff"),
	JPG("image/jpeg"),

	OTF("font/otf"),
	TTF("font/ttf"),
	WOFF("font/woff"),
	WOFF2("font/woff2"),

	MPEG("video/mpeg"),
	MP3("audio/mpeg"),
	MP4("video/mp4"),
	MIDI("audio/midi"),
	AAC("audio/aac"),
	AVI("video/x-msvideo"),
	TS("video/mp2t"),
	WAV("audio/wav"),
	WEBA("audio/webm"),
	WEBM("video/webm"),
	WEBP("image/webp"),

	GZ("application/gzip"),
	TAR("application/x-tar"),
	BZ("application/x-bzip"),
	BZ2("application/x-bzip2"),
	ZIP("application/zip");

	private final MimeType mimeType;
	private final Charset defaultCharset;

	/**
	 * Creates a new MediaType with the default charset (UTF-8)
	 */
	private MediaTypes(String mediaType) {
		this(mediaType, Charset.forName(StreamUtil.DEFAULT_INPUT_STREAM_ENCODING));
	}

	/**
	 * Creates a new MediaType with the given charset. `null` means no charset!
	 */
	private MediaTypes(String mediaType, Charset charset) {
		String[] type = mediaType.split("/");
		this.mimeType = new MimeType(type[0], type[1]);
		this.defaultCharset = charset;
	}

	/**
	 * returns the default charset for the given mediatype or null when non is allowed
	 */
	public Charset getDefaultCharset() {
		return defaultCharset;
	}

	/**
	 * Matches the provided 'Content-Type' to this enum, should always be valid, is not weighted
	 */
	public boolean includes(@Nullable String contentTypeHeader) {
		if (this == ANY) {
			return true;
		}
		if (StringUtils.isBlank(contentTypeHeader)) {
			return false;
		}
		return parseAndEvaluate(contentTypeHeader, parsedType->mimeType.includes(parsedType) && parsedType.getParameter("q") == null);
	}

	/**
	 * Checks if this enum matches a value in the provided 'Accept' header.
	 */
	public boolean accepts(@Nullable String acceptHeader) {
		if (this == ANY || StringUtils.isBlank(acceptHeader)) {
			return true;
		}
		//The Accept header may consist out of multiple parts.
		String[] headerParts = acceptHeader.split(",");
		for(String headerPart : headerParts) {
			if(parseAndEvaluate(headerPart, mimeType::isCompatibleWith)) {
				return true;
			}
		}
		return false;
	}

	private boolean parseAndEvaluate(String mimeType, Predicate<MimeType> predicate) {
		try {
			MimeType type = MimeTypeUtils.parseMimeType(mimeType);
			return predicate.test(type);
		} catch (InvalidMimeTypeException e) {
			return false;
		}
	}

	public static MediaTypes fromValue(String contentType) {
		return EnumUtils.parseFromField(MediaTypes.class, "content-type", contentType, e -> e.mimeType.toString());
	}

	/**
	 * Returns the MimeType without any parameters (such as charset)
	 */
	public MimeType getMimeType() {
		return getMimeType(null);
	}

	public MimeType getMimeType(String charset) {
		Charset withCharset = defaultCharset;
		if(StringUtils.isNotEmpty(charset)) {
			if(defaultCharset == null) {
				throw new UnsupportedCharsetException("provided mediatype does not support setting charset");
			}
			withCharset = Charset.forName(charset);
		}

		if(withCharset == null) {
			return mimeType;
		}

		return new MimeType(mimeType, withCharset);
	}
}
