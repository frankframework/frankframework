package nl.nn.adapterframework.frankdoc.model;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.xml.sax.SAXException;

import nl.nn.adapterframework.configuration.digester.DigesterRule;
import nl.nn.adapterframework.configuration.digester.DigesterRulesHandler;
import nl.nn.adapterframework.core.Resource;
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
			XmlUtils.parseXml(resource.asInputSource(), new Handler(path, result));
			log.trace("Successfully created config child descriptors");
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

		Handler(String path, DigesterRules digesterRules) {
			this.path = path;
			this.digesterRules = digesterRules;
		}

		@Override
		protected void handle(DigesterRule rule) throws SAXException {
			String pattern = rule.getPattern();
			StringTokenizer tokenizer = new StringTokenizer(pattern, "/");
			String roleName = null;
			while(tokenizer.hasMoreElements()) {
				String token = tokenizer.nextToken();
				if(!"*".equals(token)) {
					roleName = token;
				}
			}
			String registerTextMethod = rule.getRegisterTextMethod();
			if(StringUtils.isNotEmpty(rule.getRegisterMethod())) {
				if(StringUtils.isNotEmpty(registerTextMethod)) {
					log.warn("digester-rules.xml, role name {}: Have both registerMethod and registerTextMethod, ignoring the latter", roleName);
				}
				addTypeObject(rule.getRegisterMethod(), roleName);
			} else {
				if(StringUtils.isNotEmpty(registerTextMethod)) {
					if(registerTextMethod.startsWith("set")) {
						log.warn("digester-rules.xml: Ignoring registerTextMethod {} because it starts with \"set\" to avoid confusion with attributes", registerTextMethod);
					} else {
						addTypeText(registerTextMethod, roleName);
					}
				} else {
					// roleName is not final, so a lambda wont work in the trace statement.
					// We use isTraceEnabled() instead.
					if(log.isTraceEnabled()) {
						log.trace("digester-rules.xml, ignoring role name {} because there is no registerMethod and no registerTextMethod attribute", roleName);
					}
				}
			}
		}

		private void addTypeObject(String registerMethod, String roleName) throws SAXException {
			log.trace("Have ConfigChildSetterDescriptor for ObjectConfigChild: roleName = {}, registerMethod = {}", () -> roleName, () -> registerMethod);
			ConfigChildSetterDescriptor descriptor = new ConfigChildSetterDescriptor.ForObject(registerMethod, roleName);
			checkDuplicate(descriptor);
		}

		private void addTypeText(String registerMethod, String roleName) throws SAXException {
			log.trace("Have ConfigChildSetterDescriptor for TextConfigChild: roleName = {}, registerMethod = {}", () -> roleName, () -> registerMethod);
			ConfigChildSetterDescriptor descriptor = new ConfigChildSetterDescriptor.ForText(registerMethod, roleName);
			checkDuplicate(descriptor);
		}

		private void checkDuplicate(ConfigChildSetterDescriptor descriptor) {
			if(digesterRules.configChildDescriptors.containsKey(descriptor.getMethodName())) {
				log.warn("In digester rules [{}], duplicate method name [{}], ignoring", path, descriptor.getMethodName());
			} else {
				digesterRules.configChildDescriptors.put(descriptor.getMethodName(), descriptor);
			}
		}
	}

	// Used by unit tests
	DigesterRules() {
	}

	private Map<String, ConfigChildSetterDescriptor> configChildDescriptors = new HashMap<>();

	ConfigChildSetterDescriptor getConfigChildSetterDescriptor(String methodName) {
		return configChildDescriptors.get(methodName);
	}

	int getNumConfigChildSetterDescriptors() {
		return configChildDescriptors.size();
	}
}
