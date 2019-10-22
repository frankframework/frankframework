/*
Copyright 2019 Integration Partners B.V.

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

public enum MediaTypes {

	ANY("*/*"),
	TEXT("text/plain"),
	XML("application/xml"),
	JSON("application/json"),
	PDF("application/pdf"),
	MULTIPART_RELATED("multipart/related"),
	MULTIPART_FORMDATA("multipart/form-data"),
	MULTIPART("multipart/*");

	private final String mediaType;

	private MediaTypes(String mediaType) {
		this.mediaType = mediaType;
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

	public static MediaTypes fromValue(String v) {
		for (MediaTypes c : MediaTypes.values()) {
			if (c.mediaType.equalsIgnoreCase(v)) {
				return c;
			}
		}
		throw new IllegalArgumentException(v);
	}
}