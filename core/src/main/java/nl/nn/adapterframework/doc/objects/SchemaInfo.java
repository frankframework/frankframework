package nl.nn.adapterframework.doc.objects;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import lombok.Getter;
import lombok.Setter;

public class SchemaInfo {
	private @Getter @Setter Map<String, TreeSet<IbisBean>> groups;
	private @Getter @Setter List<MethodNameToChildIbisBeanNameMapping> ibisMethods;
	private @Getter @Setter Set<IbisBean> ibisBeansExtra;
	private @Getter @Setter List<AFolder> folders;
	private @Getter @Setter Map<String, String> ignores;
	private @Getter @Setter Set<String> excludeFilters;
}
