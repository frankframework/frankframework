package nl.nn.adapterframework.extensions.cmis;

import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.Properties;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import nl.nn.adapterframework.core.PipeLineSessionBase;
import nl.nn.adapterframework.http.HttpSender;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.util.ClassUtils;

public class CmisSenderTest {

	private String url;
	private String repo;
	private String titanUser;
	private String titanPassword;
	
	private String id1;
	private String id2;
	
	private int length1=0;
	private int length2=0;
	
	private String testProperties="CmisSender.properties";
	private Properties properties;

	private int numParallel=5; // not used for ramp up test
	
	private int numCycles=10;
	private int maxConnections=20;
	private int numSenders=2;
	
	private boolean testViaHttpSender=false;
	
	private int threadCount[] = {  1,  5, 10, 15, 20, 25, 30 };
	
	private StringBuffer results=new StringBuffer();
	private String separator="\t";

	
	
	private CmisSender cmisSenders[];
	private HttpSender httpSenders[];
	
	@Before
	public void setUp() throws Exception {
		properties=new Properties();
		properties.load(ClassUtils.getResourceURL(this, testProperties).openStream());
		url=properties.getProperty("url");
		repo=properties.getProperty("repo");
		titanUser=properties.getProperty("titanUser");
		titanPassword=properties.getProperty("titanPassword");
		id1=properties.getProperty("id.1");
		id2=properties.getProperty("id.2");
		length1=Integer.parseInt(properties.getProperty("length.1"));
		length2=Integer.parseInt(properties.getProperty("length.2"));
		if (testViaHttpSender) {
			httpSenders = new HttpSender[numSenders];
			for (int i=0; i<numSenders; i++) {
				httpSenders[i]=createHttpSender(i);
			}
		} else {
			cmisSenders = new CmisSender[numSenders];
			for (int i=0; i<numSenders; i++) {
				cmisSenders[i]=createCmisSender(i);
			}
		}
	}
	
	@After
	public void tearDown() {
		// nothing
	}
	
	public CmisSender createCmisSender(int i) throws Exception {
		CmisSender sender = new CmisSender();
		sender.setName("CmisSender "+i);
		sender.setUrl(url);
		sender.setRepository(repo);
		sender.setAction("get");
		sender.setBindingType("browser");
		sender.setUsername(titanUser);
		sender.setPassword(titanPassword);
		sender.setMaxConnections(maxConnections);
		sender.configure();
		sender.open();
		return sender;
	}

	public HttpSender createHttpSender(int i) throws Exception {
		HttpSender sender = new HttpSender();
		sender.setName("HttpSender "+i);
		String fullUrl=url+"/"+repo+"/root?objectId="+id1+"&cmisselector=content";
		System.out.println("url: "+fullUrl);
		sender.setUrl(fullUrl);
		sender.setMethodType("GET");
//		sender.setRepository(repo);
//		sender.setAction("get");
//		sender.setBindingType("browser");
		sender.setUserName(titanUser);
		sender.setPassword(titanPassword);
		sender.setMaxConnections(maxConnections);
		sender.configure();
		sender.open();
		return sender;
	}
	
	@Test
	public void testGet() throws Exception {
		testGet(0);
	}
	
	public void testGet(int i) throws Exception {
		String id=id1;
//		int expectedLength=length1;
		String result;
		PipeLineSessionBase session = new PipeLineSessionBase();
	
		int index=i % numSenders;
		
		if (testViaHttpSender) {
			String message=""; 

			ParameterResolutionContext prc= new ParameterResolutionContext(message, session);
			result=httpSenders[index].sendMessage(null, message,prc);
			
		} else {
			String message="<cmis><id>"+id+"</id></cmis>";
			ParameterResolutionContext prc= new ParameterResolutionContext(message, session);
			result=cmisSenders[index].sendMessage(null, message,prc);
		}
		
		assertNotNull(result);
//		assertEquals(expectedLength,result.length());
	}

	@Test
	public void testGetMultipleTimes() throws Exception {
	
		long t0=System.currentTimeMillis();
		for (int i=0;i<numCycles;i++) {
			testGet();
		}
		long t1=System.currentTimeMillis();
		long total=t1-t0;
		long average=total/numCycles;
		System.out.println("numCycles ["+numCycles+"] total ["+total+"] average ["+average+"]");
	}

    public void testParallel(int numParallel) throws Exception {
    	System.out.println("start testing with ["+numParallel+"] threads...");
		long t0=System.currentTimeMillis();
    	ArrayList<CmisSenderTester> threads = new ArrayList<CmisSenderTester>();
		for (int i=0;i<numParallel;i++) {
			threads.add(new CmisSenderTester());
		}
		for (int i=0;i<numParallel;i++) {
			threads.get(i).start();
		}
		System.out.println("waiting for threads to end");
		for (int i=0;i<numParallel;i++) {
			threads.get(i).join();
			System.out.println("joind thread "+i);
		}
		long t1=System.currentTimeMillis();
		long totalTimeTesting=t1-t0;
		long totalTime=0;
		int  totalCycles=0;
		for (int i=0;i<numParallel;i++) {
			long total=threads.get(i).total;
			int cycles=threads.get(i).cycles;
			totalTime+=total;
			totalCycles+=cycles;
			long average=total/cycles;
			System.out.println("thread ["+i+"] cycles ["+cycles+"] time spent ["+total+"] average ["+average+"]");
		}
		long totalBytes=totalCycles*length1;
		System.out.println("message size ["+length1+"] total bytes ["+totalBytes+"]");
		System.out.println("average for each of ["+numParallel+"] threads:  numCycles ["+numCycles+"] totalCycles ["+totalCycles+"] average for each message ["+(totalTime/totalCycles)+"] [ms/#] average thread speed ["+(totalBytes/totalTime)+"] [KB/s]");
		System.out.println("total time spent testing  ["+totalTimeTesting+"] numCycles ["+numCycles+"] totalCycles ["+totalCycles+"] average ["+(totalTimeTesting/totalCycles)+"] overall througput ["+(totalBytes/totalTimeTesting)+"] [KB/s]");
		String result=length1+separator+numParallel+separator+totalCycles+separator+totalTime+separator+totalTimeTesting;
		System.out.println(result);
		results.append(result).append("\n");
		
    }

    @Test
    public void testParallel() throws Exception {
    	System.out.println("warming up...");
    	testGet();
    	testGet();
    	testGet();
    	testParallel(numParallel);
    }
    
    @Test
    public void testRampUp() throws Exception {
    	System.out.println("warming up...");
    	testGet();
    	testGet();
    	testGet();
    	results.append("msgsize"+separator+"#threads"+separator+"totcycles"+separator+"totalTime"+separator+"totalTimeTesting\n");
    	for (int i=0; i<threadCount.length; i++) {
        	testParallel(threadCount[i]);
    	}
    	System.out.println(results);
    }
    

    private class CmisSenderTester extends Thread {

		private long total;
		private int cycles=0;
    	
    	@Override
		public void run() {
    		long t0=System.currentTimeMillis();
			try {
				for (int i=0;i<numCycles;i++) {
					cycles++;
					testGet();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			long t1=System.currentTimeMillis();
			total=t1-t0;
		}
   	
    }
    
}
