package org.frankframework.align;

import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.net.URL;

import org.junit.jupiter.api.Test;

import org.frankframework.testutil.TestFileUtils;

public abstract class AlignTestBase {
	public static final String BASEDIR="/Align/";

	public abstract void testFiles(String schemaFile, String namespace, String rootElement, String inputFile, boolean potentialCompactionProblems, String expectedFailureReason) throws Exception;

	public void testFiles(String schemaFile, String namespace, String rootElement, String inputFile, String expectedFailureReason) throws Exception {
		testFiles(schemaFile, namespace, rootElement, inputFile, false, expectedFailureReason);
	}
	public void testFiles(String schemaFile, String namespace, String rootElement, String inputFile, boolean potentialCompactionProblems) throws Exception {
		testFiles(schemaFile, namespace, rootElement, inputFile, potentialCompactionProblems, null);
	}
	public void testFiles(String schemaFile, String namespace, String rootElement, String inputFile) throws Exception {
		testFiles(schemaFile, namespace, rootElement, inputFile, false, null);
	}

	public URL getSchemaURL(String schemaFile) {
		URL result=TestFileUtils.getTestFileURL(BASEDIR+schemaFile);
		if (result==null) {
			fail("cannot find schema ["+schemaFile+"]");
		}
		return result;
	}

	protected String getTestFile(String file) throws IOException {
		return TestFileUtils.getTestFile(BASEDIR+file);
	}



	@Test
	public void testOK_abc() throws Exception {
		testFiles("Abc/abc.xsd","urn:test","a","Abc/abc");
	}



	@Test
	public void testArrays() throws Exception {
		// straight test
		testFiles("Arrays/arrays.xsd","urn:arrays","arrays","Arrays/arrays",true);
	}

	@Test
	public void testEmptyArrays() throws Exception {
		// straight test
		testFiles("Arrays/arrays.xsd","urn:arrays","arrays","Arrays/empty-arrays",true);
	}

	@Test
	public void testSingleComplexArray() throws Exception {
		// straight test
		testFiles("Arrays/arrays.xsd","urn:arrays","array1","Arrays/single-complex-array",true);
	}

	@Test
	public void testSingleElementArrays() throws Exception {
		// straight test
		testFiles("Arrays/arrays.xsd","urn:arrays","arrays","Arrays/single-element-arrays",true);
	}

	@Test
	public void testSingleSimpleArray() throws Exception {
		// straight test
		testFiles("Arrays/arrays.xsd","urn:arrays","singleSimpleRepeatedElement","Arrays/single-simple-array",true);
	}



	@Test
	public void testAttributes() throws Exception {
		testFiles("DataTypes/DataTypes.xsd","urn:datatypes","DataTypes","/DataTypes/Attributes");
	}

	@Test
	public void testBooleans() throws Exception {
		testFiles("DataTypes/DataTypes.xsd","urn:datatypes","DataTypes","/DataTypes/Booleans",true);
	}

	@Test
	public void testDateTime() throws Exception {
		testFiles("DataTypes/DataTypes.xsd","urn:datatypes","DataTypes","/DataTypes/DateTime");
	}

	@Test
	public void testDiacritics() throws Exception {
		testFiles("DataTypes/DataTypes.xsd","urn:datatypes","DataTypes","/DataTypes/Diacritics",true);
	}

	@Test
	public void testNull() throws Exception {
		testFiles("DataTypes/DataTypes.xsd","urn:datatypes","DataTypes","/DataTypes/Null");
	}

	@Test
	public void testNumbers() throws Exception {
		testFiles("DataTypes/DataTypes.xsd","urn:datatypes","DataTypes","/DataTypes/Numbers");
	}

	@Test
	public void testSpecialChars() throws Exception {
		testFiles("DataTypes/DataTypes.xsd","urn:datatypes","DataTypes","/DataTypes/SpecialChars",true);
	}

	@Test
	public void testStrings() throws Exception {
		testFiles("DataTypes/DataTypes.xsd","urn:datatypes","DataTypes","/DataTypes/Strings",true);
	}



	@Test
	public void test_hcda() throws Exception {
		testFiles("HCDA/HandleCollectionDisbursementAccount3_v3.0.xsd","","HandleCollectionDisbursementAccount","HCDA/HandleCollectionDisbursementAccount");
	}




	@Test
	public void testAnyAttribute() throws Exception {
		testFiles("Any/AnyAttribute.xsd", "urn:anyAttribute", "root", "Any/anyAttribute");
	}

	@Test
	public void testAnyElement() throws Exception {
		testFiles("Any/AnyElement.xsd", "urn:anyElement", "root", "Any/anyElement", false, null);
	}

	@Test
	public void testLabelValue() throws Exception {
		testFiles("Any/LabelValue.xsd", "urn:labelValue", "root", "Any/labelValue", true);
	}

	@Test
	public void testMixedContent() throws Exception {
		testFiles("Mixed/mixed.xsd","urn:mixed","root","Mixed/mixed-simple");
		testFiles("Mixed/mixed.xsd","urn:mixed","root","Mixed/mixed-complex");
		testFiles("Mixed/mixed.xsd","urn:mixed","root","Mixed/mixed-empty");
	}

	@Test
	public void testMixedContentUnknown() throws Exception {
		testFiles("Mixed/mixed.xsd","urn:mixed","root","Mixed/mixed-unknown");
	}


	@Test
	public void testRepeatedElements() throws Exception {
		testFiles("RepeatedElements/sprint.xsd","","sprint","/RepeatedElements/sprint-withRepeatedElement");
		testFiles("RepeatedElements/sprint.xsd","","sprint","/RepeatedElements/sprint-withoutRepeatedElement");
	}

	@Test
	public void testSimple() throws Exception {
		testFiles("Simple/simple.xsd","urn:simple","simple","/Simple/simple");
	}


	@Test
	public void testChoiceOfSequence() throws Exception {
		testFiles("ChoiceOfSequence/transaction.xsd","","transaction","ChoiceOfSequence/order");
		testFiles("ChoiceOfSequence/transaction.xsd","","transaction","ChoiceOfSequence/invoice");
	}

	@Test
	public void testOptionalArray() throws Exception {
		// this test was originally for a wildcard, but conversion fails on multiple occurences of element 'Id' (for Party and OrganisationName)
		// wildcard was 'solved' by setting a proper type for PartyAgreementRole
		testFiles("OptionalArray/hbp.xsd","urn:pim","Root","OptionalArray/hbp",true);
	}

	@Test
	public void testChoiceOfOptions() throws Exception {
		testFiles("ChoiceOfOptions/Options.xsd","","Options","ChoiceOfOptions/option");
	}

	@Test
	public void testEmbeddedChoice() throws Exception {
		testFiles("EmbeddedChoice/EmbeddedChoice.xsd", "", "EmbeddedChoice", "/EmbeddedChoice/EmbeddedChoice");
	}

	@Test
	public void testDoubleId() throws Exception {
		testFiles("DoubleId/Party.xsd","","Party","DoubleId/Party");
	}

	@Test
	public void testFamilyTree() throws Exception {
		testFiles("FamilyTree/family.xsd", "urn:family", "family", "FamilyTree/family", true);
	}

	@Test
	public void testPetstorePet() throws Exception {
		testFiles("Petstore/petstore.xsd", "", "Pet", "Petstore/pet");
	}

	@Test
	public void testTextAndAttributes() throws Exception {
		testFiles("TextAndAttributes/schema.xsd", "", "Root", "TextAndAttributes/input", true);
	}

	@Test
	public void testMultipleChoices() throws Exception {
		testFiles("MultipleChoices/MultipleChoices.xsd", "", "EmbeddedChoice", "MultipleChoices/MultipleChoices");
	}

//	@Test
//	public void testPetstorePets() throws Exception {
//		testFiles("Petstore/petstore.xsd", "", "Pets", "Petstore/petstore", true);
//	}

}
