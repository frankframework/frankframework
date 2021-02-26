package nl.nn.adapterframework.jms;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;

public class JmsRealmFactoryTest {

	private JmsRealmFactory jmsRealmFactory;
	
	@Before
	public void setup() {
		jmsRealmFactory = JmsRealmFactory.getInstance();
		jmsRealmFactory.clear();
		jmsRealmFactory.registerJmsRealm(createRealm("c",false));
		jmsRealmFactory.registerJmsRealm(createRealm("b",true));
		jmsRealmFactory.registerJmsRealm(createRealm("d",false));
		jmsRealmFactory.registerJmsRealm(createRealm("a",true));
	}
	
	public JmsRealm createRealm(String name, boolean withDatasourceName) {
		JmsRealm result = new JmsRealm();
		result.setRealmName(name); 
		if (withDatasourceName) {
			result.setDatasourceName("datasource of "+name);
		}
		return result;
	}
	
	@Test
	public void registerAndRead() {
	
		JmsRealm a = jmsRealmFactory.getJmsRealm("a");
		JmsRealm b = jmsRealmFactory.getJmsRealm("b");
		JmsRealm c = jmsRealmFactory.getJmsRealm("c");
		JmsRealm d = jmsRealmFactory.getJmsRealm("d");
		JmsRealm e = jmsRealmFactory.getJmsRealm("e");

		assertNotNull(a);
		assertNotNull(b);
		assertNotNull(c);
		assertNotNull(d);
		assertNull(e);

		assertTrue(StringUtils.isNotEmpty(a.getDatasourceName()));
		assertTrue(StringUtils.isNotEmpty(b.getDatasourceName()));
		assertFalse(StringUtils.isNotEmpty(c.getDatasourceName()));
		assertFalse(StringUtils.isNotEmpty(d.getDatasourceName()));
	}
		
	@Test
	public void firstDatasourceRealm() {
		assertEquals("b",jmsRealmFactory.getFirstDatasourceJmsRealm());
	}

	@Test
	public void readInOrderOfAppearance() {
		Iterator<String> it = jmsRealmFactory.getRegisteredRealmNames();
		String sequence=it.next()+","+it.next()+","+it.next()+","+it.next();
		assertEquals("c,b,d,a",sequence);
		assertFalse(it.hasNext());
		
	}
	
	@Test
	public void readInOrderOfAppearanceViaList() {
		List<String> list = jmsRealmFactory.getRegisteredRealmNamesAsList();
		String sequence=list.get(0)+","+list.get(1)+","+list.get(2)+","+list.get(3);
		assertEquals(4,  list.size());
		assertEquals("c,b,d,a",sequence);
		
	}
	
	@Test
	public void readDatabaseRealmsInOrderOfAppearance() {
		List<String> list = jmsRealmFactory.getRegisteredDatasourceRealmNamesAsList();
		assertEquals(2,list.size());
		String sequence=list.get(0)+","+list.get(1);
		assertEquals("b,a",sequence);
		
	}
}
