//package nl.nn.adapterframework.jdbc.dbms;
//
//import org.codehaus.jackson.map.ObjectMapper;
//import org.junit.Test;
//
//public class TranslatorTest {
//
//	@Test
//	public void dotest() throws Exception {
//		SqlTranslator translator = new SqlTranslator("oracle", "mysql");
//		String query = "INSERT INTO IBISTEMP (tkey,tblob1) VALUES (SEQ_IBISTEMP.CURRVAL,EMPTY_BLOB(), SYSDATE)";
//		String translated = translator.translate(query);
//		ObjectMapper mapper = new ObjectMapper();
//
//		System.err.println("Query: " + query);
//		System.err.println("Trans: " + translated);
//	}
//}
