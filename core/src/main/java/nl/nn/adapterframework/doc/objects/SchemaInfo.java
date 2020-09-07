package nl.nn.adapterframework.doc.objects;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import lombok.Getter;
import lombok.Setter;

public class SchemaInfo {
	private @Getter @Setter Map<String, TreeSet<IbisBean>> groups;
	private @Getter @Setter Set<IbisBean> ibisBeans;
	private @Getter @Setter List<IbisMethod> ibisMethods;
}
