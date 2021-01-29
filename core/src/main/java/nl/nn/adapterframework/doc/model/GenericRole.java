package nl.nn.adapterframework.doc.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import nl.nn.adapterframework.doc.Utils;
import nl.nn.adapterframework.util.LogUtil;

public class GenericRole {
	private static Logger log = LogUtil.getLogger(GenericRole.class);
	private final @Getter XsdVersion xsdVersion;
	private final List<ElementRole> roles;
	private final @Getter String syntax1Name;
	private final @Getter FrankElement conflictingFrankElement;
	private final int seq;

	private GenericRole(final XsdVersion xsdVersion, List<ElementRole> roles, int seq) {
		this.xsdVersion = xsdVersion;
		this.roles = roles;
		this.syntax1Name = roles.get(0).getSyntax1Name();
		this.seq = seq;
		Set<FrankElement> conflictCandidates = roles.stream()
				.map(role -> role.getConflictingElement(xsdVersion.getElementFilter()))
				.filter(f -> f != null)
				.collect(Collectors.toSet());
		if(conflictCandidates.size() == 0) {
			conflictingFrankElement = null;
		} else if(conflictCandidates.size() == 1) {
			conflictingFrankElement = conflictCandidates.iterator().next();
		} else {
			log.warn(String.format("Multiple conflict candidates for GenericRole with syntax 1 name [%s] and seq [%d]: [%s]",
					syntax1Name, seq, FrankElement.describe(conflictCandidates)));
			conflictingFrankElement = null;
		}
	}

	public int getNumRoles() {
		return roles.size();
	}

	public List<ElementRole> getRoles() {
		return Collections.unmodifiableList(roles);
	}

	public String getXsdGroupName(String singleRoleWord, String multiRoleWord) {
		if(getNumRoles() == 1) {
			return Utils.toUpperCamelCase(syntax1Name) + singleRoleWord + disambiguation();
		} else {
			return Utils.toUpperCamelCase(syntax1Name) + multiRoleWord + disambiguation();
		}
	}

	String disambiguation() {
		if(seq == 1) {
			return "";
		} else {
			return String.format("_%d", seq);
		}
	}

	public Key getKey() {
		return new Key(xsdVersion, getRoles());
	}

	@EqualsAndHashCode
	public static class Key {
		private XsdVersion xsdVersion;
		private Set<ElementRole.Key> elementRoles;

		/**
		 * 
		 * @throws IllegalArgumentException if the input {@link ElementRole} do not have a common syntax 1 name
		 * or if the list of {@link ElementRole} is empty.
		 */
		Key(XsdVersion xsdVersion, List<ElementRole> elementRoles) {
			Set<String> syntax1Names = elementRoles.stream().map(ElementRole::getSyntax1Name).collect(Collectors.toSet());
			if(syntax1Names.size() != 1) {
				throw new IllegalArgumentException("Roles of a GenericRole should share a syntax 1 name and should not be empty");
			}
			this.elementRoles = elementRoles.stream().map(ElementRole::getKey).collect(Collectors.toSet());
			this.xsdVersion = xsdVersion;
		}

		@Override
		public String toString() {
			List<String> roleNames = elementRoles.stream()
					.map(roleKey -> roleKey.getElementTypeName())
					.collect(Collectors.toList());
			Collections.sort(roleNames);
			List<String> allWords = new ArrayList<>();
			allWords.add(elementRoles.iterator().next().getSyntax1Name());
			allWords.addAll(roleNames);
			return "(" + allWords.stream().collect(Collectors.joining(", ")) + ")";
		}
	}

	static class Factory {
		private final Map<XsdVersion, Map<String, Integer>> sequenceNumbers = new HashMap<>();
		private final Map<Key, GenericRole> available = new LinkedHashMap<>();

		GenericRole findOrCreate(XsdVersion xsdVersion, List<ElementRole> roles) {
			Key key = new Key(xsdVersion, roles);
			if(! available.containsKey(key)) {
				available.put(key, create(xsdVersion, roles));
			}
			return available.get(key);
		}

		private GenericRole create(XsdVersion xsdVersion, List<ElementRole> roles) {
			sequenceNumbers.putIfAbsent(xsdVersion, new HashMap<>());
			Map<String, Integer> versionSequenceNumbers = sequenceNumbers.get(xsdVersion);
			String syntax1Name = roles.get(0).getSyntax1Name();
			int seq = versionSequenceNumbers.getOrDefault(syntax1Name, 0) + 1;
			GenericRole result = new GenericRole(xsdVersion, roles, seq);
			versionSequenceNumbers.put(syntax1Name, seq);
			return result;
		}

		List<GenericRole> findOrCreateCumulativeChildren(XsdVersion xsdVersion, FrankElement parent, FrankDocModel model) {
			Map<String, List<ElementRole>> roleGroups = parent.getCumulativeConfigChildren(xsdVersion.getChildSelector(), xsdVersion.getChildRejector())
					.stream().map(ConfigChild::getElementRole).distinct().collect(Collectors.groupingBy(ElementRole::getSyntax1Name));
			List<String> sortedNames = new ArrayList<>(roleGroups.keySet());
			Collections.sort(sortedNames);
			final List<GenericRole> result = new ArrayList<>();
			for(String syntax1Name: sortedNames) {
				List<ElementRole> bucket = roleGroups.get(syntax1Name);
				final Map<ElementRole, List<ElementRole>> byHighestCommonInterface = bucket.stream()
						.collect(Collectors.groupingBy(role -> role.promoteToHighestCommonInterface(model)));
				Set<ElementRole> conflicts = byHighestCommonInterface.keySet().stream()
						.filter(key -> byHighestCommonInterface.get(key).size() >= 2)
						.collect(Collectors.toSet());
				for(ElementRole conflict: conflicts) {
					log.warn(String.format("Expected ElementRole [%s] not to share a highest common interface, but they share [%s]",
							ElementRole.collection2String(byHighestCommonInterface.get(conflict)), conflict.toString()));
				}
				result.add(findOrCreate(xsdVersion, bucket));
			}
			return result;
		}

		List<GenericRole> findOrCreateChildren(XsdVersion version, GenericRole parent, FrankDocModel model) {
			final Map<FrankElement, Set<ElementRole>> rolesByFrankElement = new HashMap<>();
			for(ElementRole role: parent.getRoles()) {
				role.getOptions(version.getElementFilter()).forEach(
						f -> rolesByFrankElement.merge(f, new HashSet<>(Arrays.asList(role)), ElementRole::join));
			}
			Set<FrankElement> conflicts = rolesByFrankElement.keySet().stream()
					.filter(f -> rolesByFrankElement.get(f).size() >= 2).collect(Collectors.toSet());
			for(FrankElement conflict: conflicts) {
				log.warn(String.format("FrankElement [%s] was expected in only one role, but appeared in multiple: [%s]",
						conflict.toString(), ElementRole.collection2String(rolesByFrankElement.get(conflict))));
			}
			Map<String, List<ElementRole>> childRolesBySyntax1Name = parent.getRoles().stream()
					.flatMap(role -> role.getOptions(version.getElementFilter()).stream())
					.flatMap(f -> f.getConfigChildren(version.getChildSelector()).stream())
					.map(ConfigChild::getElementRole)
					.filter(role -> ! role.isSuperseded())
					.distinct()
					.collect(Collectors.groupingBy(ElementRole::getSyntax1Name));
			List<String> sortedNames = new ArrayList<>(childRolesBySyntax1Name.keySet());
			Collections.sort(sortedNames);
			List<GenericRole> result = new ArrayList<>();
			for(String syntax1Name: sortedNames) {
				result.add(findOrCreate(version, childRolesBySyntax1Name.get(syntax1Name)));
			}
			return result;
		}
	}
}
