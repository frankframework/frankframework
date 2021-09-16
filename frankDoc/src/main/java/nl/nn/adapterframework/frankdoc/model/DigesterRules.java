package nl.nn.adapterframework.frankdoc.model;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.xml.sax.SAXException;

import nl.nn.adapterframework.configuration.digester.DigesterRule;
import nl.nn.adapterframework.configuration.digester.DigesterRulesHandler;
import nl.nn.adapterframework.core.Resource;
import nl.nn.adapterframework.frankdoc.doclet.FrankMethod;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.XmlUtils;

class DigesterRules {
	private static Logger log = LogUtil.getLogger(DigesterRules.class);

	static DigesterRules getInstance(String path) throws IOException, SAXException {
		log.trace("Creating config child descriptors from file [{}]", () -> path);
		Resource resource = Resource.getResource(path);
		if(resource == null) {
			throw new IOException(String.format("Cannot find resource on the classpath: [%s]", path));
		}
		try {
			DigesterRules result = new DigesterRules();
			Handler handler = new Handler(path, result);
			XmlUtils.parseXml(resource.asInputSource(), handler);
			log.trace("Successfully created config child descriptors");
			for(DigesterRulesPattern.ViolationChecker v: result.violationCheckers.values()) {
				if(! v.checkImplemented(handler.rootRoleNames)) {
					throw new SAXException(String.format("Checking against [%s] has not been implemented, pattern is [%s]",
							v.toString(), v.getOriginalPattern()));
				}
			}
			return result;
		}
		catch(IOException e) {
			throw new IOException(String.format("An IOException occurred while parsing XML from [%s]", path), e);
		}
		catch(SAXException e) {
			throw new SAXException(String.format("A SAXException occurred while parsing XML from [%s]", path), e);
		}
	}

	private static class Handler extends DigesterRulesHandler {
		private final String path;
		private final DigesterRules digesterRules;
		private final Set<String> rootRoleNames = new HashSet<>();

		Handler(String path, DigesterRules digesterRules) {
			this.path = path;
			this.digesterRules = digesterRules;
		}

		@Override
		protected void handle(DigesterRule rule) throws SAXException {
			DigesterRulesPattern pattern = new DigesterRulesPattern(rule.getPattern());
			if(pattern.getError() != null) {
				throw new SAXException(pattern.getError());
			}
			if(pattern.isRoot()) {
				rootRoleNames.add(pattern.getRoleName());
			}
			String registerTextMethod = rule.getRegisterTextMethod();
			if(StringUtils.isNotEmpty(rule.getRegisterMethod())) {
				if(StringUtils.isNotEmpty(registerTextMethod)) {
					log.warn("digester-rules.xml, role name {}: Have both registerMethod and registerTextMethod, ignoring the latter", pattern.getRoleName());
				}
				addTypeObject(rule.getRegisterMethod(), pattern);
			} else {
				if(StringUtils.isNotEmpty(registerTextMethod)) {
					if(registerTextMethod.startsWith("set")) {
						log.warn("digester-rules.xml: Ignoring registerTextMethod {} because it starts with \"set\" to avoid confusion with attributes", registerTextMethod);
					} else {
						addTypeText(registerTextMethod, pattern);
					}
				} else {
					// roleName is not final, so a lambda wont work in the trace statement.
					// We use isTraceEnabled() instead.
					if(log.isTraceEnabled()) {
						log.trace("digester-rules.xml, ignoring role name {} because there is no registerMethod and no registerTextMethod attribute", pattern.getRoleName());
					}
				}
			}
		}

		private void addTypeObject(String registerMethod, DigesterRulesPattern pattern)	throws SAXException {
			log.trace("Have ConfigChildSetterDescriptor for ObjectConfigChild: roleName = {}, registerMethod = {}", () -> pattern.getRoleName(), () -> registerMethod);
			ConfigChildSetterDescriptor descriptor = new ConfigChildSetterDescriptor.ForObject(registerMethod, pattern.getRoleName());
			checkDuplicateAndRegister(descriptor, pattern);
		}

		private void addTypeText(String registerMethod, DigesterRulesPattern pattern) throws SAXException {
			log.trace("Have ConfigChildSetterDescriptor for TextConfigChild: roleName = {}, registerMethod = {}", () -> pattern.getRoleName(), () -> registerMethod);
			ConfigChildSetterDescriptor descriptor = new ConfigChildSetterDescriptor.ForText(registerMethod, pattern.getRoleName());
			checkDuplicateAndRegister(descriptor, pattern);
		}

		private void checkDuplicateAndRegister(ConfigChildSetterDescriptor descriptor, DigesterRulesPattern pattern) {
			if(digesterRules.configChildSetterDescriptors.containsKey(descriptor.getMethodName())) {
				log.warn("In digester rules [{}], duplicate method name [{}], ignoring", path, descriptor.getMethodName());
			} else {
				digesterRules.configChildSetterDescriptors.put(descriptor.getMethodName(), descriptor);
				DigesterRulesPattern.ViolationChecker violationChecker = pattern.getViolationChecker();
				if(violationChecker != null) {
					log.trace("Role name [{}] has ViolationChecker [{}]", () -> descriptor.getRoleName(), () -> violationChecker.toString());
					digesterRules.violationCheckers.put(descriptor.getRoleName(), violationChecker);
					digesterRules.violationCheckerSubjects.put(descriptor.getRoleName(), new ArrayList<>());
				}
			}
		}
	}

	// Used by unit tests
	DigesterRules() {
	}

	private Map<String, ConfigChildSetterDescriptor> configChildSetterDescriptors = new HashMap<>();
	private Map<String, DigesterRulesPattern.ViolationChecker> violationCheckers = new HashMap<>();
	private Map<String, List<ConfigChild>> violationCheckerSubjects = new HashMap<>();

	// Only used in unit tests
	ConfigChildSetterDescriptor getConfigChildSetterDescriptor(String methodName) {
		return configChildSetterDescriptors.get(methodName);
	}

	// Only used in unit tests.
	int getNumConfigChildSetterDescriptors() {
		return configChildSetterDescriptors.size();
	}

	boolean methodHasDigesterRule(FrankMethod m) {
		return configChildSetterDescriptors.containsKey(m.getName());
	}

	ConfigChildAndRoleName createConfigChild(FrankElement parent, FrankMethod method) {
		if(! methodHasDigesterRule(method)) {
			throw new IllegalArgumentException("Cannot happen because it is checked elsewhere that the method name appears in digester-rules.xml");
		}
		ConfigChildSetterDescriptor descriptor = configChildSetterDescriptors.get(method.getName());
		log.trace("Have ConfigChildSetterDescriptor [{}]", () -> descriptor.toString());
		ConfigChild configChild = descriptor.createConfigChild(parent, method);
		if(violationCheckers.containsKey(descriptor.getRoleName())) {
			// The toString() method of the ConfigChild does not work yet because the ElementRole has not been set.
			log.trace("Role name [{}] has ViolationChecker [{}], assigning config child for owner [{}]",
					() -> descriptor.getRoleName(),
					() -> violationCheckers.get(descriptor.getRoleName()).toString(), () -> configChild.getOwningElement().toString());
			violationCheckerSubjects.get(descriptor.getRoleName()).add(configChild);
		}
		return new ConfigChildAndRoleName(configChild, descriptor.getRoleName());
	}

	class ConfigChildAndRoleName {
		final ConfigChild configChild;
		final String roleName;

		ConfigChildAndRoleName(ConfigChild configChild, String roleName) {
			this.configChild = configChild;
			this.roleName = roleName;
		}
	}

	void omitViolatingConfigChildren() {
		log.trace("Enter");
		int pass = 1;
		List<ConfigChild> newViolators = new ArrayList<>();
		do {
			if(log.isTraceEnabled()) {
				log.trace("Pass [{}]", pass++);
			}
			newViolators = omitViolatingConfigChildrenOnce();
			// If we would do setViolatesDigesterRules() immediately in omitViolatingConfigChildrenOnce(),
			// we would miss an opportunity to test iterating until no more violators are found.
			// The order in which the violators would be found would determine how many
			// iterations we would need.
			newViolators.forEach(c -> c.setViolatesDigesterRules(true));
		} while(! newViolators.isEmpty());
		log.trace("Leave");
	}

	private List<ConfigChild> omitViolatingConfigChildrenOnce() {
		List<ConfigChild> result = new ArrayList<>();
		for(String roleName: violationCheckers.keySet()) {
			DigesterRulesPattern.ViolationChecker checker = violationCheckers.get(roleName);
			List<ConfigChild> remainingSubjects = violationCheckerSubjects.get(roleName).stream()
					.filter(c -> ! c.isViolatesDigesterRules())
					.collect(Collectors.toList());
			for(ConfigChild subject: remainingSubjects) {
				if(! checker.check(subject)) {
					log.trace("ConfigChild [{}] violates [{}]", () -> subject.toString(), () -> checker.toString());
					result.add(subject);
				}
			}
		}
		return result;
	}
}
