package nl.nn.adapterframework.frankdoc.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This class omits config children that are not allowed by digester-rules.xml.
 * It is not possible to check against digester-rules.xml while creating config children.
 * This is difficult to explain, but please check the discussion in pull request
 * https://github.com/ibissource/iaf/pull/2156.
 * 
 * Please note that omitting is not the same as rejecting or excluding. The latter
 * words refer to the situation that a superclass can have a config child that should
 * not be inherited by a derived class. The Frank!Doc takes measures against automatic
 * inheritance for rejected/excluded config children.
 * 
 * This class is about config children that should not have been created, also not as
 * a way to exclude them. Omitting does not happen by deleting the objets, but by setting
 * a Boolean that is taken into account by the predicates in class ElementChild.
 * 
 * @author martijn
 *
 */
class ConfigChildrenThatViolateDigesterRulesOmitter {
	enum Conclusion {
		BUSY,
		ACCEPTED,
		OMITTED;
	}

	static ConfigChildrenThatViolateDigesterRulesOmitter getInstance(List<String> remainingPattern) {
		List<PatternComponent> backtrackComponents = new ArrayList<>();
		boolean needRoot = false;

		for(String word: remainingPattern) {
			if(word.equals("*")) {
				needRoot = true;
			} else if(needRoot) {
				backtrackComponents.add(new RootComponent(word));
				needRoot = false;
			} else {
				backtrackComponents.add(new NonRootComponent(word));
			}
		}
		Collections.reverse(backtrackComponents);
		return new ConfigChildrenThatViolateDigesterRulesOmitter(backtrackComponents);
	}

	private final List<PatternComponent> backtrackComponents;

	ConfigChildrenThatViolateDigesterRulesOmitter(List<PatternComponent> backtrackComponents) {
		this.backtrackComponents = backtrackComponents;
	}

	Conclusion analyze(ConfigChild configChild) {
		return new Analysis(configChild).analyze();
	}

	private class Analysis {
		Conclusion conclusion = Conclusion.BUSY;
		List<ConfigChild> current;
		FrankElement rootElement;

		Analysis(ConfigChild subject, FrankElement rootElement) {
			current = Arrays.asList(subject);
			this.rootElement = rootElement;
		}

		Conclusion analyze() {
			for(PatternComponent component: backtrackComponents) {
				if(conclusion == Conclusion.BUSY) {
					component.accept(this);
				} else {
					break;
				}
			}
			if(conclusion == Conclusion.BUSY) {
				if(current.isEmpty()) {
					conclusion = Conclusion.OMITTED;
				} else {
					conclusion = Conclusion.ACCEPTED;
				}
			}
			return conclusion;
		}
	}

	private abstract static class PatternComponent {
		abstract void accept(Analysis analysis);
	}

	private static class RootComponent extends PatternComponent {
		private final String roleName;

		RootComponent(String roleName) {
			this.roleName = roleName;
		}

		@Override
		void accept(Analysis analysis) {
			boolean rootMatched = analysis.current.stream().map(ConfigChild::getOwningElement).anyMatch(f -> matchesAsRoot(f, analysis));
			if(rootMatched) {
				analysis.conclusion = Conclusion.ACCEPTED;
			} else {
				analysis.conclusion = Conclusion.OMITTED;
			}
		}

		private boolean matchesAsRoot(FrankElement f, Analysis analysis) {
			return f.equals(analysis.rootElement) && f.getSimpleName().toLowerCase().equals(roleName);
		}
	}

	private static class NonRootComponent extends PatternComponent {
		private final String roleName;

		NonRootComponent(String roleName) {
			this.roleName = roleName;
		}

		@Override
		void accept(Analysis analysis) {
			// TODO: Reuse code from RootComponent - matching as root should result
			// in Conclusion.ACCEPTED, even if analysis.current becomes empty.
			analysis.current = analysis.current.stream()
					.map(ConfigChild::getOwningElement)
					.flatMap(f -> f.getConfigParents().stream())
					.filter(ElementChild.DIGESTER_RULES_ACCEPTED)
					.collect(Collectors.toList());
		}

		}
	}
}
