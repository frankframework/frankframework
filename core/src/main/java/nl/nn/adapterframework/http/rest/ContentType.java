/*
Copyright 2020 WeAreFrank!

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

import org.apache.commons.lang3.StringUtils;

public class ContentType {
	public static final String CHARSET_PARAMETER = "charset";
	private MediaTypes mediaType;
	private String charset;

	public ContentType(MediaTypes mediaType) {
		this.mediaType = mediaType;
	}

	public void setCharset(String charset) {
		if(StringUtils.isNotEmpty(charset)) {
			this.charset = charset.toUpperCase();
		}
	}
	public String getCharset() {
		return charset;
	}

	public String getContentType() {
		StringBuilder string = new StringBuilder(mediaType.getContentType());
		if(charset != null) {
			string.append(";");
			string.append(CHARSET_PARAMETER);
			string.append("=");
			string.append(charset);
		}

		return string.toString();
	}
}
