package nl.nn.adapterframework.doc.model;

import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;

public class FrankDocModel {
	private @Getter @Setter List<FrankDocGroup> groups;
	private @Getter @Setter Map<String, FrankElement> allElements;
}
