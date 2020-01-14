package nl.nn.adapterframework.doc.objects;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

public class DigesterXmlHandler extends DefaultHandler {
    private String currentIbisBeanName;
    private List<IbisMethod> ibisMethods = new ArrayList<IbisMethod>();

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        if ("pattern".equals(qName)) {
            String pattern = attributes.getValue("value");
            StringTokenizer tokenizer = new StringTokenizer(pattern, "/");
            while (tokenizer.hasMoreElements()) {
                String token = tokenizer.nextToken();
                if (!"*".equals(token)) {
                    currentIbisBeanName = token;
                }
            }
        } else if ("set-next-rule".equals(qName)) {
            String methodName = attributes.getValue("methodname");
            ibisMethods.add(new IbisMethod(methodName, currentIbisBeanName));
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName)
            throws SAXException {
        if ("pattern".equals(qName)) {
            currentIbisBeanName = null;
        }
    }

    public List<IbisMethod> getIbisMethods() {
        return ibisMethods;
    }

}
