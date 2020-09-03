package nl.nn.adapterframework.doc.objects;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SchemaInfo {
	private Map<String, TreeSet<IbisBean>> groups;
	private Set<IbisBean> ibisBeans;
	private List<IbisMethod> ibisMethods;
	private Set<IbisBeanExtra> ibisBeansExtra;
}
