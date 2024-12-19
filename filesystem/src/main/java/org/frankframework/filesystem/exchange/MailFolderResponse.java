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
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;

import com.fasterxml.jackson.annotation.JsonProperty;

import org.frankframework.filesystem.MsalClientAdapter.GraphClient;

public class MailFolderResponse {
	private static final int MAX_ENTRIES_PER_CALL = 20;
	private static final String TRUSTED_URL_PREFIX = "https://graph.microsoft.com/v1.0/";
	private static final String MS_GRAPH_MAIL_BASE_URL = TRUSTED_URL_PREFIX + "users/%s/mailFolders?$top=%d&$skip=0";
	private static final String CHILD_MAIL_FOLDERS_SEARCH = "%s/childFolders?$top=%d&$skip=0";
	private static final String CHILD_MAIL_FOLDER_BASE = "%s/childFolders";

	public static List<MailFolder> get(GraphClient client, String email) throws IOException {
		return get(client, email, 200); // default limit is 20, and with 20 entries per API call, only 1 call will be made.
	}

	public static List<MailFolder> get(GraphClient client, String email, int limit) throws IOException {
		URI uri = URI.create(MS_GRAPH_MAIL_BASE_URL.formatted(email, MAX_ENTRIES_PER_CALL));
		List<MailFolder> folders = new ArrayList<>();
		getRecursive(client, uri, folders, limit - MAX_ENTRIES_PER_CALL);
		return folders;
	}

	public static List<MailFolder> get(GraphClient client, MailFolder folder) throws IOException {
		return get(client, folder, 200);
	}

	public static List<MailFolder> get(GraphClient client, MailFolder folder, int limit) throws IOException {
		URI uri = URI.create(CHILD_MAIL_FOLDERS_SEARCH.formatted(folder.getUrl(), MAX_ENTRIES_PER_CALL));
		List<MailFolder> folders = new ArrayList<>();
		getRecursive(client, uri, folders, limit - MAX_ENTRIES_PER_CALL);
		return folders;
	}

	private static void getRecursive(GraphClient client, URI uri, List<MailFolder> folders, int limit) throws IOException {
		MailFolderResponse response = client.execute(new HttpGet(uri), MailFolderResponse.class);

		String urlWithoutQueryParameters = new URIBuilder(uri).removeQuery().toString();
		response.folders.forEach(e -> e.setUrl(urlWithoutQueryParameters));
		folders.addAll(response.folders);

		if (StringUtils.isNotBlank(response.nextLink) && limit > 0) {
			getRecursive(client, validateNextLink(response.nextLink), folders, limit - MAX_ENTRIES_PER_CALL);
		}
	}

	private static URI validateNextLink(String nextLink) throws IOException {
		if (!nextLink.startsWith(TRUSTED_URL_PREFIX)) {
			throw new IOException("Untrusted URL: " + nextLink);
		}
		return URI.create(nextLink);
	}

	public static void create(GraphClient client, MailFolder folder, String folderName) throws IOException {
		URI uri = URI.create(CHILD_MAIL_FOLDER_BASE.formatted(folder.getUrl()));

		String content = "{\"displayName\":\"%s\"}".formatted(folderName);
		HttpPost post = new HttpPost(uri);
		HttpEntity entity = new StringEntity(content, ContentType.APPLICATION_JSON);
		post.setEntity(entity);
		client.execute(post);
	}

	public static void delete(GraphClient client, MailFolder folder) throws IOException {
		URI uri = URI.create(folder.getUrl());
		client.execute(new HttpDelete(uri));
	}

	/**
	 * Pagination in an API.
	 * @see <a href="https://learn.microsoft.com/en-us/graph/api/user-list-mailfolders?view=graph-rest-1.0&tabs=http">MS Graph API</a>
	 */
	@JsonProperty("@odata.nextLink")
	String nextLink;

	@JsonProperty("value")
	List<MailFolder> folders;
}
