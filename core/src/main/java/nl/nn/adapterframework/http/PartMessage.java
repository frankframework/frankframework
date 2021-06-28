/*
   Copyright 2021 WeAreFrank!

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
package nl.nn.adapterframework.http;

import java.nio.charset.UnsupportedCharsetException;

import javax.mail.MessagingException;
import javax.mail.Part;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.ParseException;
import org.apache.http.entity.ContentType;

import nl.nn.adapterframework.stream.Message;

public class PartMessage extends Message {
	
	private Part part;
	
	public PartMessage(Part part, String charset) {
		super(() -> part.getInputStream(), charset, part.getClass());
		this.part = part;
		if (StringUtils.isEmpty(charset)) {
			try {
				ContentType contentType = ContentType.parse(part.getContentType());
				if(contentType.getCharset() != null) {
					this.setCharset(contentType.getCharset().name());
				}
			} catch (UnsupportedCharsetException | ParseException | MessagingException e) {
				log.warn("Could not determine charset", e);
			}
		}
	}
	public PartMessage(Part part) {
		this(part, null);
	}

	@Override
	public long size() {
		try {
			return part.getSize();
		} catch (MessagingException e) {
			log.warn("Cannot get size", e);
			return -1;
		}
	}

}
