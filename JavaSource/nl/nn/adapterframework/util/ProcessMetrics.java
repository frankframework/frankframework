/*
 * $Log: ProcessMetrics.java,v $
 * Revision 1.1  2004-07-20 13:27:29  L190409
 * first version
 *
 *  */
package nl.nn.adapterframework.util;

/**
 * Utility class to report process parameters like memory usage as an xml-element.
 * 
 * @since 4.2
 */
public class ProcessMetrics {

	private final static long K_LIMIT=10*1024;
	private final static long M_LIMIT=K_LIMIT*1024;
	private final static long G_LIMIT=M_LIMIT*1024;
	
	public static void addNumberProperty(XmlBuilder list, String name, long value) {
		String valueString;
		
		if (value < K_LIMIT) {
			valueString = Long.toString(value);
		} else {
			if (value < M_LIMIT) {
				valueString = Long.toString(value/1024)+"K";
			} else { 
				if (value < G_LIMIT) {
					valueString = Long.toString(value/(1024*1024))+"M";
				} else { 
						valueString = Long.toString(value/(1024*1024*1024))+"G";
				}
			}
		}
		addProperty(list,name,valueString);
	}
	
	public static void addProperty(XmlBuilder list, String name, String value) {
		XmlBuilder p=new XmlBuilder("property");
		p.addAttribute("name", name);
		p.setValue(value);
		list.addSubElement(p);
	}

	public static String toXml() {
		XmlBuilder xmlh=new XmlBuilder("processMetrics");
		XmlBuilder props=new XmlBuilder("properties");
		xmlh.addSubElement(props);
		
		long freeMem = Runtime.getRuntime().freeMemory();
		long totalMem = Runtime.getRuntime().totalMemory();
		
		addNumberProperty(props, "freeMemory", freeMem);
		addNumberProperty(props, "totalMemory", totalMem);
		addNumberProperty(props, "heapSize", totalMem-freeMem);
		return xmlh.toXML();
	}


}
