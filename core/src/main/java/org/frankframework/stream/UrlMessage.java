/*
   Copyright 2021, 2022-2025 WeAreFrank!

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
package org.frankframework.stream;

import java.io.Serial;
import java.io.Serializable;
import java.net.URL;
import java.util.Collections;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;

public class UrlMessage extends Message {

	@Serial
	private static final long serialVersionUID = -8984775227930282095L;

	public UrlMessage(URL url) {
		this(url, Collections.emptyMap());
	}

	public UrlMessage(URL url, Map<String, Serializable> context) {
		super(url::openStream, new MessageContext(context).withName(FilenameUtils.getName(url.toString())).withLocation(url.toString()), url.getClass());
	}

	public UrlMessage(URL url, String charset) {
		super(url::openStream, new MessageContext().withCharset(charset), url.getClass());
	}
}
