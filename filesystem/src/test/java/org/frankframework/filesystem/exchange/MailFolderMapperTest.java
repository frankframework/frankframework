/*
   Copyright 2026 WeAreFrank!

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
package org.frankframework.filesystem.exchange;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

public class MailFolderMapperTest {

	private static final ObjectMapper MAPPER = new ObjectMapper()
			.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

	/**
	 * In issue "https://github.com/frankframework/frankframework/issues/11380" we had an issue that mapping failed with a value larger than
	 * Integer.MAX_VALUE. This test ensures that all long fields in MailFolder can exceed Integer.MAX_VALUE.
	 */
	@Test
	void testAllLongFieldsCanExceedIntegerMaxValue() throws Exception {
		long longValue = (long) Integer.MAX_VALUE + 1;
		String json = """
				{
				  "childFolderCount": %d,
				  "unreadItemCount": %d,
				  "totalItemCount": %d,
				  "sizeInBytes": %d
				}
				""".formatted(longValue, longValue, longValue, longValue);

		MailFolder folder = MAPPER.readValue(json, MailFolder.class);

		assertEquals(longValue, folder.getChildFolderCount());
		assertEquals(longValue, folder.getUnreadItemCount());
		assertEquals(longValue, folder.getTotalItemCount());
		assertEquals(longValue, folder.getSizeInBytes());
	}
}
