package nl.nn.adapterframework.doc;

import static nl.nn.adapterframework.doc.model.ElementChild.DEPRECATED;
import static nl.nn.adapterframework.doc.model.ElementChild.IN_XSD;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.logging.log4j.Logger;

import nl.nn.adapterframework.doc.model.ElementRole;
import nl.nn.adapterframework.doc.model.ElementType;
import nl.nn.adapterframework.doc.model.FrankDocModel;
import nl.nn.adapterframework.util.LogUtil;

class MemberChildrenCalculator {
	private static Logger log = LogUtil.getLogger(MemberChildrenCalculator.class);

	private ElementRole subject;
	private FrankDocModel model;

	MemberChildrenCalculator(ElementRole subject, FrankDocModel model) {
		this.subject = subject;
		this.model = model;
	}

	List<ElementRole> getMemberChildOptions() {
		Map<String, List<ElementRole>> childRolesBySyntax1Name = groupElementRolesBySyntax1Name(
				model.getElementTypeMemberChildRoles(
						subject.getElementType(), IN_XSD, DEPRECATED, f -> ! f.isDeprecated()));
		return disambiguateElementRoles(childRolesBySyntax1Name);
	}

	private Map<String, List<ElementRole>> groupElementRolesBySyntax1Name(List<ElementRole> elementRoles) {
		// This is implemented such that the sort order remains deterministic
		Map<String, List<ElementRole>> result = new TreeMap<>();
		for(ElementRole role: elementRoles) {
			result.merge(role.getSyntax1Name(), Arrays.asList(role),
					(l1, l2) -> Stream.concat(l1.stream(), l2.stream()).collect(Collectors.toList()));
		}
		return result;
	}

	private List<ElementRole> disambiguateElementRoles(Map<String, List<ElementRole>> bySyntax1Name) {
		List<ElementRole> result = new ArrayList<>();
		for(List<ElementRole> bucket: bySyntax1Name.values()) {
			if(bucket.size() >= 2) {
				List<ElementType> highestElementTypes = bucket.stream()
						.map(er -> er.getElementType().getHighestCommonInterface())
						.distinct()
						.collect(Collectors.toList());
				if(highestElementTypes.size() >= 2) {
					String elementTypesAsSingleString = highestElementTypes.stream().map(ElementType::getFullName).collect(Collectors.joining(", "));
					log.warn(String.format("No common ElementType for element types [%s], omitting them from %s [%s]",
							elementTypesAsSingleString, DocWriterNew.MEMBER_CHILD_GROUP, subject.toString()));
				} else {
					ElementRole.Key key = new ElementRole.Key(highestElementTypes.get(0).getFullName(), bucket.get(0).getSyntax1Name());
					ElementRole candidate = model.findElementRole(key);
					if(candidate == null) {
						log.warn(String.format("No element role available for ElementType [%s] and syntax 1 name [%s], omitting from %s [%s]",
								key.getElementTypeName(), key.getSyntax1Name(), DocWriterNew.MEMBER_CHILD_GROUP, subject.toString()));
					} else {
						result.add(candidate);
					}
				}
			}
			else {
				result.add(bucket.get(0));					
			}
		}
		return result;
	}
}
