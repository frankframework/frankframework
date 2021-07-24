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
package nl.nn.adapterframework.http.rest;

import java.nio.charset.Charset;

import nl.nn.adapterframework.util.StreamUtil;
import nl.nn.adapterframework.util.EnumUtils;

public enum MediaTypes {

	ANY("*/*", null),
	TEXT("text/plain"),
	XML("application/xml"),
	JSON("application/json"),
	PDF("application/pdf", null), //raw binary formats do not have a charset
	OCTET("application/octet-stream", null), //raw binary formats do not have a charset
	MULTIPART_RELATED("multipart/related"),
	MULTIPART_FORMDATA("multipart/form-data"),
	MULTIPART("multipart/*");

	private final String mediaType;
	private final Charset defaultCharset;

	/**
	 * Creates a new MediaType with the default charset (UTF-8)
	 */
	private MediaTypes(String mediaType) {
		this(mediaType, Charset.forName(StreamUtil.DEFAULT_INPUT_STREAM_ENCODING));
	}

	/**
	 * Creates a new MediaType with the given charset.
	 * `null` means no charset!
	 */
	private MediaTypes(String mediaType, Charset charset) {
		this.mediaType = mediaType;
		this.defaultCharset = charset;
	}

	/**
	 * returns the default charset for the given mediatype or null when non is allowed
	 */
	public Charset getDefaultCharset() {
		return defaultCharset;
	}

	public String getContentType() {
		return mediaType;
	}

	public boolean isConsumable(String contentType) {
		switch (this) {
			case ANY:
				return true;

			case MULTIPART:
			case MULTIPART_RELATED:
			case MULTIPART_FORMDATA:
				return (contentType.contains("multipart/"));

			default:
				return (contentType.contains(mediaType));
		}
	}

	public static MediaTypes fromValue(String contentType) {
		return EnumUtils.parseFromField(MediaTypes.class, "content-type", contentType, e -> e.mediaType);
	}
}