package nl.nn.adapterframework.doc;

import static org.junit.Assert.assertEquals;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.Ignore;
import org.junit.Test;

import nl.nn.adapterframework.doc.model.ElementType;
import nl.nn.adapterframework.doc.model.FrankDocModel;
import nl.nn.adapterframework.doc.model.FrankElement;

@Ignore
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
		assertEquals(model.getAllElements(), reconstructedElements);
		assertEquals(model.getAllTypes(), reconstructedTypes);
	}
}
