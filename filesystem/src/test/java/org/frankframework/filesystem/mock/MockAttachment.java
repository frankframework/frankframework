package org.frankframework.filesystem.mock;

import java.util.LinkedHashMap;
import java.util.Map;

public class MockAttachment {

	private final String name;
	private final String filename;
	private final String contentType;
	private Map<String,Object> additionalProperties;
	private byte[] contents;

	public MockAttachment(String name, String filename, String contentType) {
		this.name=name;
		this.contentType=contentType;
		this.filename=filename;
	}

	public String getName() {
		return name;
	}
	public String getFilename() {
		return filename;
	}
	public String getContentType() {
		return contentType;
	}

	public byte[] getContents() {
		return contents;
	}

	public void setContents(byte[] contents) {
		this.contents = contents;
	}


	public Map<String, Object> getAdditionalProperties() {
		return additionalProperties;
	}

	public void setAdditionalProperties(String key, Object value) {
		if (additionalProperties==null) {
			additionalProperties=new LinkedHashMap<>();
		}
		additionalProperties.put(key, value);
	}

}
