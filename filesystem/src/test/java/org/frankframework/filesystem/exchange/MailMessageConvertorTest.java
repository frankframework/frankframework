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

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import org.frankframework.util.JacksonUtils;

/**
 * Proves that mapping with Jackson to an object works as expected.
 */
class MailMessageConvertorTest {

	@Test
	void proveThatMailMessageMappingWorksAsIntended() {
		String exampleResponse = """
				{
				    "@odata.context": "https://graph.microsoft.com/v1.0/$metadata#users('bb8775a4-4d8c-42cf-a1d4-4d58c2bb668f')/messages(sender,subject)",
				    "value": [
				        {
				            "@odata.etag": "W/\\"CQAAABYAAADHcgC8Hl9tRZ/hc1wEUs1TAAAwR4Hg\\"",
				            "id": "AAMkAGUAAAwTW09AAA=",
				            "subject": "You have late tasks!",
				            "sender": {
				                "emailAddress": {
				                    "name": "Microsoft Planner",
				                    "address": "noreply@Planner.Office365.com"
				                }
				            }
				        }
				    ]
				}
				""";

		MailMessage mailMessage = JacksonUtils.convertToDTO(exampleResponse, MailMessage.class);
		assertNotNull(mailMessage);
	}
}
