package nl.nn.adapterframework.pipes;

import org.junit.Test;

import nl.nn.adapterframework.core.PipeLineSessionBase;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.soap.SoapWrapperPipe;
import nl.nn.adapterframework.testutil.TestAssertions;

public class SoapWrapperPipeTest<P extends SoapWrapperPipe> extends PipeTestBase<P> {

	private String TARGET_NAMESPACE="urn:fakenamespace";
	
	@Override
	public P createPipe() {
		P pipe = (P)new SoapWrapperPipe();
		return pipe;
	}

	
	public void addParam(String name, String value) {
		Parameter param = new Parameter();
		param.setName(name);
		param.setValue(value);
		pipe.addParameter(param);
	}
	
	

	@Test
	public void testBasicUnwrap() throws Exception {
//		pipe.setOutputNamespace(TARGET_NAMESPACE);
		pipe.setDirection("unwrap");
		pipe.configure();
		pipe.start();
		
		String input = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\"><soapenv:Body>"
				+"<root xmlns=\""+TARGET_NAMESPACE+"\">\n"
				+"<attrib>1</attrib>\n"
				+"<attrib>2</attrib>\n"
				+"</root></soapenv:Body></soapenv:Envelope>";
		String expected = "<root xmlns=\""+TARGET_NAMESPACE+"\">\n"
				+"<attrib>1</attrib>\n"
				+"<attrib>2</attrib>\n"
				+"</root>";
		
		PipeRunResult prr = doPipe(pipe, input,new PipeLineSessionBase());
		
		String actual = (String)prr.getResult();
		System.out.println("result ["+actual+"]");
		
		TestAssertions.assertEqualsIgnoreCRLF(expected, actual);
	}

	@Test
	public void testBasicUnwrapRemoveNamespaces() throws Exception {
		pipe.setDirection("unwrap");
		pipe.setRemoveOutputNamespaces(true);
		pipe.configure();
		pipe.start();
		
		String input = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\"><soapenv:Body>"
				+"<root xmlns=\""+TARGET_NAMESPACE+"\">\n"
				+"<attrib>1</attrib>\n"
				+"<attrib>2</attrib>\n"
				+"</root></soapenv:Body></soapenv:Envelope>";
		String expected = "<root>\n"
				+"<attrib>1</attrib>\n"
				+"<attrib>2</attrib>\n"
				+"</root>";
		
		PipeRunResult prr = doPipe(pipe, input,new PipeLineSessionBase());
		
		String actual = (String)prr.getResult();
		System.out.println("result ["+actual+"]");
		
		TestAssertions.assertEqualsIgnoreCRLF(expected, actual);
	}

	@Test
	public void testBasicUnwrapSwitchRoot() throws Exception {
		pipe.setDirection("unwrap");
		pipe.setRoot("OtherRoot");
		pipe.configure();
		pipe.start();
		
		String input = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\"><soapenv:Body>"
				+"<root xmlns=\""+TARGET_NAMESPACE+"\">\n"
				+"<attrib>1</attrib>\n"
				+"<attrib>2</attrib>\n"
				+"</root></soapenv:Body></soapenv:Envelope>";
		String expected = "<OtherRoot xmlns=\""+TARGET_NAMESPACE+"\">"
				+"<attrib>1</attrib>"
				+"<attrib>2</attrib>"
				+"</OtherRoot>";
		
		PipeRunResult prr = doPipe(pipe, input,new PipeLineSessionBase());
		
		String actual = (String)prr.getResult();
		System.out.println("result ["+actual+"]");
		
		TestAssertions.assertEqualsIgnoreCRLF(expected, actual);
	}

	@Test
	public void testBasicUnwrapRemoveNamespacesAndSwitchRoot() throws Exception {
		pipe.setDirection("unwrap");
		pipe.setRemoveOutputNamespaces(true);
		pipe.setRoot("OtherRoot");
		pipe.configure();
		pipe.start();
		
		String input = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\"><soapenv:Body>"
				+"<root xmlns=\""+TARGET_NAMESPACE+"\">\n"
				+"<attrib>1</attrib>\n"
				+"<attrib>2</attrib>\n"
				+"</root></soapenv:Body></soapenv:Envelope>";
		String expected = "<OtherRoot>"
				+"<attrib>1</attrib>"
				+"<attrib>2</attrib>"
				+"</OtherRoot>";
		
		PipeRunResult prr = doPipe(pipe, input,new PipeLineSessionBase());
		
		String actual = (String)prr.getResult();
		System.out.println("result ["+actual+"]");
		
		TestAssertions.assertEqualsIgnoreCRLF(expected, actual);
	}
	
	
	@Test
	public void testBasicWrap() throws Exception {
		pipe.setOutputNamespace(TARGET_NAMESPACE);
		pipe.configure();
		pipe.start();
		
		String input = "<root>\n"
				+"<attrib>1</attrib>\n"
				+"<attrib>2</attrib>\n"
				+"</root>";
		String expected = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\"><soapenv:Body>"
				+"<root xmlns=\""+TARGET_NAMESPACE+"\">\n"
				+"<attrib>1</attrib>\n"
				+"<attrib>2</attrib>\n"
				+"</root></soapenv:Body></soapenv:Envelope>";
		
		PipeRunResult prr = doPipe(pipe, input,new PipeLineSessionBase());
		
		String actual = (String)prr.getResult();
		System.out.println("result ["+actual+"]");
		
		TestAssertions.assertEqualsIgnoreCRLF(expected, actual);
	}

	@Test
	public void testBasicWrapChangeRoot() throws Exception {
		pipe.setOutputNamespace(TARGET_NAMESPACE);
		pipe.setRoot("OtherRoot");
		pipe.configure();
		pipe.start();
		
		String input = "<root>\n"
				+"<attrib>1</attrib>\n"
				+"<attrib>2</attrib>\n"
				+"</root>";
		String expected = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\"><soapenv:Body>"
				+"<OtherRoot xmlns=\""+TARGET_NAMESPACE+"\">"
				+"<attrib>1</attrib>"
				+"<attrib>2</attrib>"
				+"</OtherRoot></soapenv:Body></soapenv:Envelope>";
		
		PipeRunResult prr = doPipe(pipe, input,new PipeLineSessionBase());
		
		String actual = (String)prr.getResult();
		System.out.println("result ["+actual+"]");
		
		TestAssertions.assertEqualsIgnoreCRLF(expected, actual);
	}

}
