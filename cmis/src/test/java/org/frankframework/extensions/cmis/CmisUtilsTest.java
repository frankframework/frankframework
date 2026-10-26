package org.frankframework.extensions.cmis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Calendar;

import org.apache.chemistry.opencmis.commons.data.Properties;
import org.apache.chemistry.opencmis.commons.data.PropertyData;
import org.junit.jupiter.api.Test;

import org.frankframework.util.DomBuilderException;
import org.frankframework.util.XmlUtils;

class CmisUtilsTest {

	@Test
	void processPropertiesDateTime() throws DomBuilderException {
		String cmisInput = """
				<cmis>
					<objectTypeId>Zoo</objectTypeId>
					<fileName>Penguins.jpg</fileName>
					<properties>
						<property name="generic" type="datetime">2026-10-22T16:04:11</property>
						<property name="almostIsoDefaultFormatter" type="datetime">2026-10-22T16:04:11.727+02:00</property>
						<property name="almostIsoCustomFormat" type="datetime" formatString="yyyy-MM-dd'T'HH:mm:ss">2026-10-22T16:04:11.727+02:00</property>
						<property name="isoFormat" type="datetime" formatString="yyyy-MM-dd'T'HH:mm:ss.SSSxxx">2026-10-22T16:04:11.727+02:00</property>
					</properties>
				</cmis>""";
		Properties cmisProps = CmisUtils.processProperties(XmlUtils.buildElement(cmisInput));

		PropertyData<?> propertyData0 = cmisProps.getProperties().get("generic");
		assertNotNull(propertyData0);
		Calendar value0 = assertInstanceOf(Calendar.class, propertyData0.getFirstValue());
		assertEquals(1792677851000L, value0.toInstant().toEpochMilli());
		assertEquals("""
						<property name="generic" type="datetime">2026-10-22T16:04:11.000+02:00</property>
						""".trim(), CmisUtils.getPropertyXml(propertyData0).asXmlString());

		PropertyData<?> propertyData1 = cmisProps.getProperties().get("almostIsoDefaultFormatter");
		assertNotNull(propertyData1);
		Calendar value1 = assertInstanceOf(Calendar.class, propertyData1.getFirstValue());
		assertEquals(1792677851727L, value1.toInstant().toEpochMilli());
		assertEquals("""
						<property name="almostIsoDefaultFormatter" type="datetime">2026-10-22T16:04:11.727+02:00</property>
						""".trim(), CmisUtils.getPropertyXml(propertyData1).asXmlString());

		PropertyData<?> propertyData2 = cmisProps.getProperties().get("almostIsoCustomFormat");
		assertNotNull(propertyData2);
		Calendar value2 = assertInstanceOf(Calendar.class, propertyData2.getFirstValue());
		assertEquals(1792677851727L, value2.toInstant().toEpochMilli());
		assertEquals("""
						<property name="almostIsoCustomFormat" type="datetime">2026-10-22T16:04:11.727+02:00</property>
						""".trim(), CmisUtils.getPropertyXml(propertyData2).asXmlString());

		PropertyData<?> propertyData3 = cmisProps.getProperties().get("isoFormat");
		assertNotNull(propertyData3);
		Calendar value3 = assertInstanceOf(Calendar.class, propertyData3.getFirstValue());
		assertEquals(1792677851727L, value3.toInstant().toEpochMilli());
		assertEquals("""
						<property name="isoFormat" type="datetime">2026-10-22T16:04:11.727+02:00</property>
						""".trim(), CmisUtils.getPropertyXml(propertyData3).asXmlString());
	}
}
