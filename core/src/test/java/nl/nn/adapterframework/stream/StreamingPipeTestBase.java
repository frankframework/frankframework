package nl.nn.adapterframework.stream;

import java.io.Writer;
import java.util.Arrays;
import java.util.Collection;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.pipes.PipeTestBase;

@RunWith(Parameterized.class)
public abstract class StreamingPipeTestBase<P extends StreamingPipe> extends PipeTestBase<P> {

	@Parameter(0)
	public String  description=null;
	@Parameter(1)
	public boolean classic=true;
	@Parameter(2)
	public boolean provideStreamForInput=false;
	@Parameter(3)
	public boolean writeOutputToStream=false;
	
	@Parameters(name = "{index}: {0}: provide [{2}] stream out [{3}]")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                 { "classic", 			false, false, false }, 
                 { "new, no stream", 	 true, false, false }, 
                 { "output to stream", 	 true, false, true  }, 
                 { "consume stream", 	 true, true,  false }, 
                 { "stream through",  	 true, true,  true  }
           });
    }
	@Override
	protected PipeRunResult doPipe(P pipe, Object input, IPipeLineSession session) throws Exception {
		PipeRunResult prr;
		if (provideStreamForInput) {
			MessageOutputStream target;
			if (writeOutputToStream) {
				target = pipe.provideOutputStream(null, session, new MessageOutputStreamCap());
			} else {
				target = pipe.provideOutputStream(null, session, null);
			}
			
			try (Writer writer = target.asWriter()) {
				writer.write((String)input); // TODO: proper conversion of non-string classes..
			}
			Object result=target.getResponse();
			return new PipeRunResult(null, result);
		} else {
			if (classic) {
				prr = pipe.doPipe(input,session);
			} else {
				if (writeOutputToStream) {
					prr = pipe.doPipe(input,session,new MessageOutputStreamCap());
				} else {
					prr = pipe.doPipe(input,session,null);
				}
			}		
			return prr;
		}
	}

}
