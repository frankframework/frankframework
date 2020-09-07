package nl.nn.adapterframework.doc.objects;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import lombok.Getter;
import lombok.Setter;

public class SchemaInfo {
	@Getter
	@Setter
	private Map<String, TreeSet<IbisBean>> groups;
	
	@Getter
	@Setter
	private Set<IbisBean> ibisBeans;
	
	@Getter
	@Setter
	private List<IbisMethod> ibisMethods;
}
