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
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.springframework.beans.BeanUtils;

import com.fasterxml.jackson.annotation.JsonProperty;

import org.frankframework.filesystem.MsalClientAdapter.GraphClient;

public class MailMessageResponse {
	private static final int MAX_ENTRIES_PER_CALL = 20;
	private static final String TRUSTED_URL_PREFIX = "https://graph.microsoft.com/v1.0/";
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
			getRecursive(client, validateNextLink(response.nextLink), parentFolder, folders, limit - MAX_ENTRIES_PER_CALL);
		}
	}

	private static URI validateNextLink(String nextLink) throws IOException {
		if (!nextLink.startsWith(TRUSTED_URL_PREFIX)) {
			throw new IOException("Untrusted URL: " + nextLink);
		}
		return URI.create(nextLink);
	}

	/**
	 * Resolves mail message
	 */
	public static MailMessage get(GraphClient client, MailMessage filePointer) throws IOException {
		String composedUrl = MESSAGE.formatted(filePointer.getMailFolder().getUrl(), filePointer.getId());
		String generatedUrl = filePointer.getUrl();
		if (!composedUrl.equals(generatedUrl)) {
			throw new IOException("url mismatch");
		}
		URI uri = URI.create(generatedUrl);
		MailMessage response = client.execute(new HttpGet(uri), MailMessage.class);

		response.setMailFolder(filePointer.getMailFolder());
		BeanUtils.copyProperties(response, filePointer, "url"); // should ignore the url as that's compiled already when setting the MailFolder
		return response;
	}

	public static MailMessage move(GraphClient client, MailMessage filePointer, MailFolder destinationFolder) throws IOException {
		String composedUrl = MESSAGE.formatted(filePointer.getMailFolder().getUrl(), filePointer.getId());
		String generatedUrl = filePointer.getUrl();
		if (!composedUrl.equals(generatedUrl)) {
			throw new IOException("url mismatch");
		}
		URI uri = URI.create(generatedUrl + "/move");

		String content = "{\"destinationId\":\"%s\"}".formatted(destinationFolder.getId());
		HttpPost post = new HttpPost(uri);
		HttpEntity entity = new StringEntity(content, ContentType.APPLICATION_JSON);
		post.setEntity(entity);

		MailMessage response = client.execute(post, MailMessage.class);
		response.setMailFolder(filePointer.getMailFolder());
		return response;
	}

	public static MailMessage copy(GraphClient client, MailMessage filePointer, MailFolder destinationFolder) throws IOException {
		String composedUrl = MESSAGE.formatted(filePointer.getMailFolder().getUrl(), filePointer.getId());
		String generatedUrl = filePointer.getUrl();
		if (!composedUrl.equals(generatedUrl)) {
			throw new IOException("url mismatch");
		}
		URI uri = URI.create(generatedUrl + "/copy");

		String content = "{\"destinationId\":\"%s\"}".formatted(destinationFolder.getId());
		HttpPost post = new HttpPost(uri);
		HttpEntity entity = new StringEntity(content, ContentType.APPLICATION_JSON);
		post.setEntity(entity);

		MailMessage response = client.execute(post, MailMessage.class);
		response.setMailFolder(filePointer.getMailFolder());
		return response;
	}

	public static void delete(GraphClient client, MailMessage filePointer) throws IOException {
		String composedUrl = MESSAGE.formatted(filePointer.getMailFolder().getUrl(), filePointer.getId());
		String generatedUrl = filePointer.getUrl();
		if (!composedUrl.equals(generatedUrl)) {
			throw new IOException("url mismatch");
		}
		URI uri = URI.create(generatedUrl);
		client.execute(new HttpDelete(uri));
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
