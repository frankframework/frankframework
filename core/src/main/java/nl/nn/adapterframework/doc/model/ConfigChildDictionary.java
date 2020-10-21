package nl.nn.adapterframework.doc.model;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.xml.sax.SAXException;

import lombok.Getter;
import nl.nn.adapterframework.configuration.digester.DigesterRule;
import nl.nn.adapterframework.configuration.digester.DigesterRulesHandler;
import nl.nn.adapterframework.core.Resource;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.XmlUtils;

class ConfigChildDictionary {
	private static Logger log = LogUtil.getLogger(ConfigChildDictionary.class);

	class Item {
		private @Getter String methodName;
		private @Getter boolean mandatory;
		private @Getter boolean allowMultiple;
		private @Getter String syntax1Name;

		Item(String methodName, String syntax1Name, String path) {
			this.methodName = methodName;
			this.syntax1Name = syntax1Name;
			mandatory = false;
			if(methodName.startsWith("set")) {
				allowMultiple = false;
			} else if((methodName.startsWith("add")) || methodName.startsWith("register")) {
				allowMultiple = true;
			} else {
				throw new RuntimeException(String.format(
						"Do not know how many elements go in method [%s], digester rules from [%s]", methodName, path));
			}
		}
	}

	private class Handler extends DigesterRulesHandler {
		private final String path;

		Handler(String path) {
			this.path = path;
		}

		@Override
		protected void handle(DigesterRule rule) {
			String pattern = rule.getPattern();
			StringTokenizer tokenizer = new StringTokenizer(pattern, "/");
			String syntax1Name = null;
			while(tokenizer.hasMoreElements()) {
				String token = tokenizer.nextToken();
				if(!"*".equals(token)) {
					syntax1Name = token;
				}
			}
			if(StringUtils.isNotEmpty(rule.getRegisterMethod())) {
				add(rule.getRegisterMethod(), syntax1Name);
			}			
		}

		private void add(String registerMethod, String syntax1Name) {
			Item item = new Item(registerMethod, syntax1Name, path);
			if(dictionary.containsKey(item.getMethodName())) {
				ConfigChildDictionary.log.warn(String.format("In digester rules [%s], duplicate method name [%s]", path, registerMethod));
			} else {
				dictionary.put(item.getMethodName(), item);
			}
		}
	}

	private Map<String, Item> dictionary;

	ConfigChildDictionary(String path) {
		dictionary = new HashMap<>();
		Resource resource = Resource.getResource(path);
		if(resource == null) {
			throw new RuntimeException(String.format("Cannot find resource on the classpath: [%s]", path));
		}
		try {
			XmlUtils.parseXml(resource.asInputSource(), new Handler(path));
		}
		catch(IOException e) {
			throw new RuntimeException(String.format("An IOException occurred while parsing XML from [%s]", path), e);
		}
		catch(SAXException e) {
			throw new RuntimeException(String.format("A SAXException occurred while parsing XML from [%s]", path), e);
		}
	}

	Item getDictionaryItem(String methodName) {
		if(dictionary.containsKey(methodName)) {
			return dictionary.get(methodName);
		}
		return null;
	}

	int size() {
		return dictionary.size();
	}
}
