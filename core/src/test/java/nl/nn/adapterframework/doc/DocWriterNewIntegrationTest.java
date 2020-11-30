package nl.nn.adapterframework.doc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Ignore;
import org.junit.Test;

import nl.nn.adapterframework.doc.model.ElementType;
import nl.nn.adapterframework.doc.model.FrankDocModel;
import nl.nn.adapterframework.doc.model.FrankElement;
import nl.nn.adapterframework.doc.model.FrankElementStatistics;

// @Ignore("Test takes a long time to run, and gives little information")
public class DocWriterNewIntegrationTest {

	@Test
	public void testXsd() throws IOException {
		FrankDocModel model = FrankDocModel.populate();
		DocWriterNew docWriter = new DocWriterNew(model);
		docWriter.init();
		String xsdString = docWriter.getSchema();
		File output = new File("testFrankdoc.xsd");
		System.out.println("Output file of test xsd: " + output.getAbsolutePath());
		Writer writer = new BufferedWriter(new FileWriter(output));
		try {
			writer.append(xsdString);
		}
		finally {
			writer.close();
		}
	}

	@Test
	public void testNoXsdTypesOmittedBySorting() {
		FrankDocModel model = FrankDocModel.populate();
		DocWriterNew docWriter = new DocWriterNew(model);
		docWriter.init();
		List<SortKeyForXsd> xsdSortOrder = docWriter.xsdSortOrder;
		List<FrankElement> reconstructedElementsList = xsdSortOrder.stream()
				.filter(item -> item.getKind() == SortKeyForXsd.Kind.ELEMENT)
				.map(SortKeyForXsd::getName)
				.map(model.getAllElements()::get)
				.collect(Collectors.toList());
		Map<String, FrankElement> reconstructedElements = new HashMap<>();
		for(FrankElement element: reconstructedElementsList) {
			reconstructedElements.put(element.getFullName(), element);
		}
		List<ElementType> reconstructedTypesList = xsdSortOrder.stream()
				.filter(item -> item.getKind() == SortKeyForXsd.Kind.TYPE)
				.map(SortKeyForXsd::getName)
				.map(model.getAllTypes()::get)
				.collect(Collectors.toList());
		Map<String, ElementType> reconstructedTypes = new HashMap<>();
		for(ElementType type: reconstructedTypesList) {
			reconstructedTypes.put(type.getFullName(), type);
		}
		Map<String, ElementType> expectedTypes = new HashMap<>();
		for(String k: model.getAllTypes().keySet()) {
			ElementType t = model.getAllTypes().get(k);
			if(t.isFromJavaInterface()) {
				expectedTypes.put(k, t);
			}
		}
		Set<String> missedElements = new HashSet<>();
		for(String name: model.getAllElements().keySet()) {
			if(! reconstructedElements.containsKey(name)) {
				missedElements.add(name);
			}
		}
		assertTrue(missedElements.size() <= 10);
		assertEquals(expectedTypes, reconstructedTypes);
	}

	@Test
	public void testStatistics() throws IOException {
		FrankDocModel model = FrankDocModel.populate();
		File output = new File("testStatistics.csv");
		System.out.println("Output file of test statistics: " + output.getAbsolutePath());
		Writer writer = new BufferedWriter(new FileWriter(output));
		try {
			writer.append(FrankElementStatistics.header() + "\n");
			for(FrankElement elem: model.getAllElements().values()) {
				writer.append(elem.getStatistics().toString() + "\n");
			}
		}
		finally {
			writer.close();
		}
	}
}
