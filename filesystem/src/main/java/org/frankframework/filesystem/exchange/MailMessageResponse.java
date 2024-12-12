/*
   Copyright 2024 WeAreFrank!

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

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.HttpGet;

import com.fasterxml.jackson.annotation.JsonProperty;

import org.frankframework.filesystem.MsalClientAdapter.GraphClient;

public class MailMessageResponse {
	private static final int MAX_ENTRIES_PER_CALL = 20;
	private static final String MESSAGES = "%s/messages?$top=%d&$skip=0";
	private static final String MESSAGE = "%s/messages/%s";

	public static List<MailMessage> get(GraphClient client, MailFolder folder, int limit) throws IOException {
		URI uri = URI.create(MESSAGES.formatted(folder.getUrl(), MAX_ENTRIES_PER_CALL));
		List<MailMessage> folders = new ArrayList<>();
		getRecursive(client, uri, folder, folders, limit - MAX_ENTRIES_PER_CALL);
		return folders;
	}

	private static void getRecursive(GraphClient client, URI uri, MailFolder parentFolder, List<MailMessage> folders, int limit) throws IOException {
		MailMessageResponse response = client.execute(new HttpGet(uri), MailMessageResponse.class);
		if (response.messages != null && !response.messages.isEmpty()) {
			response.messages.forEach(e -> e.setMailFolder(parentFolder));
			folders.addAll(response.messages);
		}

		if (StringUtils.isNotBlank(response.nextLink) && limit > 0) {
			getRecursive(client, URI.create(response.nextLink), parentFolder, folders, limit - MAX_ENTRIES_PER_CALL);
		}
	}

	public static MailMessage get(GraphClient client, MailMessage file) throws IOException {
		URI uri = URI.create(MESSAGE.formatted(file.getMailFolder().getUrl(), file.getId()));
		MailMessage response = client.execute(new HttpGet(uri), MailMessage.class);
		response.setMailFolder(file.getMailFolder());
		return response;
	}

	/**
	 * Pagination in an API.
	 * @see <a href="https://learn.microsoft.com/en-us/graph/api/user-list-mailfolders?view=graph-rest-1.0&tabs=http">MS Graph API</a>
	 */
	@JsonProperty("@odata.nextLink")
	String nextLink;

	@JsonProperty("value")
	List<MailMessage> messages;
}
