/*
   Copyright 2022 WeAreFrank!

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
package nl.nn.adapterframework.stream;

import java.nio.charset.Charset;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import nl.nn.adapterframework.util.CalendarParserException;
import nl.nn.adapterframework.util.DateUtils;

public class MessageContext extends LinkedHashMap<String,Object> {

	public MessageContext() {
		super();
	}
	public MessageContext(Map<? extends String, ? extends Object> base) {
		super(base);
	}

	public MessageContext withCharset(String charset) {
		if (StringUtils.isNotEmpty(charset)) {
			put(Message.METADATA_CHARSET, charset);
		}
		return this;
	}
	public MessageContext withCharset(Charset charset) {
		if (charset!=null) {
			put(Message.METADATA_CHARSET, charset.name());
		}
		return this;
	}
	public MessageContext withSize(long size) {
		put(Message.METADATA_SIZE, size);
		return this;
	}
	public MessageContext withoutSize() {
		remove(Message.METADATA_SIZE);
		return this;
	}
	public MessageContext withModificationTime(long time) {
		return withModificationTime(new Date(time));
	}
	public MessageContext withModificationTime(Date time) {
		put(Message.METADATA_MODIFICATIONTIME, DateUtils.format(time));
		return this;
	}
	public MessageContext withModificationTime(String time) throws CalendarParserException {
		return withModificationTime(DateUtils.parseAnyDate(time));
	}
	public MessageContext withName(String name) {
		put(Message.METADATA_NAME, name);
		return this;
	}
	public MessageContext withLocation(String location) {
		put(Message.METADATA_LOCATION, location);
		return this;
	}
	public MessageContext with(String name, String value) {
		put(name, value);
		return this;
	}

}
