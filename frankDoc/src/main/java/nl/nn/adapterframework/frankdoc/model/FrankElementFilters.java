/* 
Copyright 2021 WeAreFrank! 

Licensed under the Apache License, Version 2.0 (the "License"); 
you may not use this file except in compliance with the License. 
You may obtain a copy of the License at 

    http://www.apache.org/licenses/LICENSE-2.0 

Unless required by applicable law or agreed to in writing, software 
distributed under the License is distributed on an "AS IS" BASIS, 
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
See the License for the specific language governing permissions and 
limitations under the License. 
*/

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
		return new HashSet<>(Arrays.asList("org.springframework"));
	}
}
