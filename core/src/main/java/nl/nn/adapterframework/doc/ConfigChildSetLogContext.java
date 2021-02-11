package nl.nn.adapterframework.doc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;

import nl.nn.adapterframework.doc.model.ConfigChildSet;
import nl.nn.adapterframework.doc.model.FrankElement;

abstract class ConfigChildSetLogContext {
	abstract boolean isTraceEnabled();
	abstract void trace(String msg);
	abstract ConfigChildSetLogContext addNestedSyntax1Name(String syntax1Name);

	static ConfigChildSetLogContext getInstance(Logger log, FrankElement frankElement, ConfigChildSet configChildSet) {
		if(log.isTraceEnabled()) {
			return new Trace(log, frankElement, configChildSet, Arrays.asList());
		} else {
			return new NoTrace();
		}
	}

	private static class NoTrace extends ConfigChildSetLogContext {
		@Override
		boolean isTraceEnabled() {
			return false;
		}

		@Override
		void trace(String msg) {
		}

		@Override
		ConfigChildSetLogContext addNestedSyntax1Name(String syntax1Name) {
			return this;
		}
	}

	private static class Trace extends ConfigChildSetLogContext {
		private final Logger log;
		private final FrankElement frankElement;
		private final ConfigChildSet configChildSet;
		private final List<String> nestedSyntax1Names;
		private final String header;

		Trace(Logger log, FrankElement frankElement, ConfigChildSet configChildSet, List<String> nestedSyntax1Names) {
			this.log = log;
			this.frankElement = frankElement;
			this.configChildSet = configChildSet;
			this.nestedSyntax1Names = nestedSyntax1Names;
			String nestedNamesStr = nestedSyntax1Names.stream().collect(Collectors.joining(", "));
			this.header = String.format("FrankElement [%s], ConfigChildSet of [%s], nested names [%s]: ",
					frankElement.getSimpleName(), configChildSet.getSyntax1Name(), nestedNamesStr);
		}

		@Override
		boolean isTraceEnabled() {
			return true;
		}

		@Override
		void trace(String msg) {
			log.trace(String.format("%s%s", header, msg));
		}

		@Override
		ConfigChildSetLogContext addNestedSyntax1Name(String syntax1Name) {
			List<String> newNestedSyntax1Names = new ArrayList<>(nestedSyntax1Names);
			newNestedSyntax1Names.add(syntax1Name);
			return new Trace(log, frankElement, configChildSet, newNestedSyntax1Names);
		}
	}
}
