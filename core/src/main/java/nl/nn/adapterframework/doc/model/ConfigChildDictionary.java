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

	public static final ConfigChildDictionary EMPTY = new ConfigChildDictionary();

	/**
	 * This class is similar to {@link ConfigChild}, but it is not the same. As an example,
	 * consider a digester rule that links setter {@code setAbc()} to a syntax 1 name {@code abc}.
	 * This rule is represented by an instance of this class, {@code Item}. If there are
	 * two classes {@code X} and {@code Y} with method {@code setAbc()}, then two
	 * different instances of {@link ConfigChild} are needed. The reason is that
	 * {@code X.setAbc()} and {@code Y.setAbc()} can have a different {@code sequenceInConfig}.
	 * That field is obtained from an {@code IbisDoc} annotation.
	 *
	 */
	class Item {
		private @Getter String methodName;
		private @Getter boolean mandatory;
		private @Getter boolean allowMultiple;
		private @Getter String syntax1Name;
		private @Getter Integer defaultOrder;

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

	void setMethodNameOrder(String methodName, int order) {
		Item item = getDictionaryItem(methodName);
		if(item == null) {
			throw new IllegalArgumentException(String.format(
					"Cannot set default order for method [%s] because it is not in the config child dictionary",
					methodName));
		}
		item.defaultOrder = order;
	}

	int size() {
		return dictionary.size();
	}

	private ConfigChildDictionary() {
		dictionary = new HashMap<>();
	}
}
