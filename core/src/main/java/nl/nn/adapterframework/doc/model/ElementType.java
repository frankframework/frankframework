package nl.nn.adapterframework.doc.model;

import java.util.HashMap;
import java.util.Map;

import lombok.Getter;

public class ElementType {
	private @Getter String fullName;
	private @Getter String simpleName;
	private @Getter Map<String, FrankElement> members;
	private @Getter boolean fromJavaInterface;

	ElementType(Class<?> clazz) {
		fullName = clazz.getName();
		simpleName = clazz.getSimpleName();
		members = new HashMap<>();
		this.fromJavaInterface = clazz.isInterface();
	}

	void addMember(FrankElement member) {
		members.put(member.getFullName(), member);
	}
}
