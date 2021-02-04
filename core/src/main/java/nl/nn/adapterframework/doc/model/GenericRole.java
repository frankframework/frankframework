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

/**
 * A list of {@link ElementRole} sharing a syntax 1 name and thus a generic
 * element option in the XML schema file. As an example, consider Java class
 * {@link nl.nn.adapterframework.batch.StreamTransformerPipe}. It has four
 * config child setters related to config children with syntax 1 name "child".
 * These are:
 * <ul>
 * <li> public void registerChild(IRecordHandlerManager manager)
 * <li> public void registerChild(RecordHandlingFlow flowEl)
 * <li> public void registerChild(IRecordHandler handler)
 * <li> public void registerChild(IResultHandler handler)
 * </ul>
 * These four config child setters introduce four {@link ElementRole} that have
 * syntax 1 name "child" in common. If each of these config children would produce
 * their own generic element option (e.g. <code>&lt;Child className="..." &gt;</code>,
 * then the XML schema would become invalid. If such an XSD would parse the text
 * <code>&lt;StreamTransformerPipe&gt;&lt;Child ... &gt; ... </code>, then the parser
 * would not know which of the <code>&lt;Child&gt;</code> tags is meant.
 * <p>
 * The XSD element definition for {@link nl.nn.adapterframework.batch.StreamTransformerPipe}
 * should have only one definition for child <code>&lt;Child&gt;</code>. The model
 * supports this by grouping the four involved {@link ElementRole} in a single
 * GenericRole.
 * <p>
 * This class has a static inner class Factory that is responsible for creating GenericRole
 * objects. This factory class is not public, so it cannot be used from outside the model.
 * {@link nl.nn.adapterframework.doc.DocWriterNew} creates GenericRole objects by accessing
 * {@link FrankDocModel}. GenericRole objects are not created by the {@link FrankDocModel#populate()}
 * method, because GenericRole objects can only be properly created when it is known
 * whether <code>strict.xsd</code> or <code>compatibility.xsd</code> is being created. This
 * information is stored in an instance of {@link XsdVersion}, which is selected by
 * {@link nl.nn.adapterframework.doc.DocWriterNew} before it browses the {@link FrankDocModel}.
 * 
 * @author martijn
 *
 */
public class GenericRole {
	private static Logger log = LogUtil.getLogger(GenericRole.class);
	private final @Getter XsdVersion xsdVersion;
	private final List<ElementRole> roles;
	private final @Getter String syntax1Name;

	/**
	 * The {@link FrankElement} that has an XML tag name that conflicts with the
	 * generic option element name, or null when there is no such conflict or when
	 * there are multiple conflicting {@link FrankElement}-s.
	 */
	private final @Getter FrankElement conflictingFrankElement;

	// Used to give groups in the XML schema file a unique name.
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

	/**
	 * This class ensures that each GenericRole is created only once.
	 * @author martijn
	 *
	 */
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

	// Factory class that ensures that each GenericRole is created only once.
	// In theory, generic element options can be nested infinitely. If there
	// is an interface I implemented by class A, and if A has methods
	// setChild(I i) and setChild(B b), then there are two element roles
	// (I, child), (B, child). These two roles are grouped into a GenericRole
	// which is used recursively to add the generic element option in the XML
	// schema. This class avoids infinite recursion when
	// creating GenericRole objects.
	//
	// DocWriterNew also uses the unicity of GenericRole to maintain whether a
	// GenericRole has been processed. This way, no duplicate element groups are
	// added to the XML schema file.
	//
	static class Factory {
		private final Map<XsdVersion, Map<String, Integer>> sequenceNumbers = new HashMap<>();
		private final Map<Key, GenericRole> available = new LinkedHashMap<>();

		// See JavaDoc of FrankDocModel for a description.
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

		// See JavaDoc of FrankDocModel for a description.
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

		// See JavaDoc of FrankDocModel for a description.
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
