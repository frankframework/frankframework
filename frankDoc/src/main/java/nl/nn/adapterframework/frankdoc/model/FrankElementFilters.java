package nl.nn.adapterframework.frankdoc.model;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

public final class FrankElementFilters {
	private FrankElementFilters() {
	}

	public static Set<String> getIncludeFilter() {
		return new HashSet<>(Arrays.asList("nl.nn.adapterframework", "nl.nn.ibistesttool"));
	}

	/**
	 * Before doclets were being applied for the Frank!Doc, exclude filters were regular expressions.
	 * Implementing with filtering with regular expression for doclets takes too much time.
	 * Therefore, regular expressions have been replaced by simple package names.
	 */
	public static Set<String> getExcludeFilter() {
		Set<String> excludeFilters = new TreeSet<>();
		// Exclude classes that will give conflicts with existing, non-compatible bean definition of same name and class
		excludeFilters.add("nl.nn.adapterframework.extensions.esb.WsdlGeneratorPipe");
		excludeFilters.add("nl.nn.adapterframework.extensions.sap.SapSender");
		excludeFilters.add("nl.nn.adapterframework.extensions.sap.SapListener");
		excludeFilters.add("nl.nn.adapterframework.extensions.sap.SapLUWManager");
		excludeFilters.add("nl.nn.adapterframework.extensions.sap.jco2.SapSender");
		excludeFilters.add("nl.nn.adapterframework.extensions.sap.jco2.SapListener");
		excludeFilters.add("nl.nn.adapterframework.extensions.sap.jco2.SapLUWManager");
		excludeFilters.add("nl.nn.adapterframework.extensions.sap.jco3.SapSender");
		excludeFilters.add("nl.nn.adapterframework.extensions.sap.jco3.SapListener");
		excludeFilters.add("nl.nn.adapterframework.extensions.sap.jco3.SapLUWManager");
		excludeFilters.add("nl.nn.adapterframework.pipes.CommandSender");
		excludeFilters.add("nl.nn.adapterframework.pipes.EchoSender");
		excludeFilters.add("nl.nn.adapterframework.pipes.FixedResultSender");
		excludeFilters.add("nl.nn.adapterframework.pipes.LogSender");
		excludeFilters.add("nl.nn.adapterframework.pipes.MailSender");
		// Here are replacements for:
		// excludeFilters.add(".*\\.IbisstoreSummaryQuerySender");
		excludeFilters.add("nl.nn.adapterframework.webcontrol.action.IbisstoreSummaryQuerySender");
		excludeFilters.add("nl.nn.adapterframework.webcontrol.api.IbisstoreSummaryQuerySender");
		// End of replacements

		// Exclude classes that cannot be used directly in configurations
		excludeFilters.add("nl.nn.adapterframework.pipes.MessageSendingPipe");
		
		// Exclude classes that should only be used in internal configurations
		excludeFilters.add("nl.nn.adapterframework.doc.IbisDocPipe");
		excludeFilters.add("nl.nn.adapterframework.webcontrol..*");
		excludeFilters.add("nl.nn.adapterframework.pipes.CreateRestViewPipe");
		return excludeFilters;
	}

	public static Set<String> getExcludeFiltersForSuperclass() {
		// TODO: Include the below line, but to remove some attributes that are too much
		// in the runtime-generated XSDs. Is not done while the compile-time XSDs are
		// being compared to the runtime generated ones.
		// return new HashSet<>(Arrays.asList("org.springframework"));
		return new HashSet<>();
	}
}
