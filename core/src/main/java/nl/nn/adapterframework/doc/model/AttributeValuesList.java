package nl.nn.adapterframework.doc.model;

import java.util.List;

import lombok.Getter;

public class AttributeValuesList {
	private @Getter String fullName;
	private String simpleName;
	private @Getter List<String> values;
	private int seq;

	AttributeValuesList(String fullName, String simpleName, List<String> values, int seq) {
		this.fullName = fullName;
		this.values = values;
		this.simpleName = simpleName;
		this.seq = seq;
	}

	public String getUniqueName(String groupWord) {
		if(seq == 1) {
			return String.format("%s%s", simpleName, groupWord);
		} else {
			return String.format("%s%s_%d", simpleName, groupWord, seq);
		}
	}
}
