package nl.nn.adapterframework.filesystem;

import java.util.Iterator;
import java.util.Map;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.HasPhysicalDestination;
import nl.nn.adapterframework.core.IDataIterator;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeStartException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.pipes.IteratingPipe;

public class ForEachAttachmentPipe<F, A, FS extends IWithAttachments<F,A>> extends IteratingPipe {

	private FS fileSystem;
	
	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		getFileSystem().configure();
	}
	
	@Override
	public void start() throws PipeStartException {
		super.start();
		try {
			FS fileSystem=getFileSystem();
			fileSystem.open();
		} catch (FileSystemException e) {
			throw new PipeStartException("Cannot open fileSystem",e);
		}
	}
	
	@Override
	public void stop()  {
		try {
			getFileSystem().close();
		} catch (FileSystemException e) {
			log.warn("Cannot close fileSystem",e);
		}
		super.stop();
	}

	@Override
	protected IDataIterator<String> getIterator(Object input, IPipeLineSession session, String correlationID, Map threadContext) throws SenderException {
		
		FS ifs = getFileSystem();
		F file;
		
		try {
			file = ifs.toFile((String)input);
		} catch (Exception e) {
			throw new SenderException("unable to get file", e);
		}
		Iterator<A> it = ifs.listAttachments(file);
		
		return new IDataIterator<String>() {

			Iterator<A> it;
			
			IDataIterator(Iterator<A> it) {
				this.it=it;
			}
			
			@Override
			public boolean hasNext() throws SenderException {
				// TODO Auto-generated method stub
				return false;
			}

			@Override
			public String next() throws SenderException {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public void close() throws SenderException {
				// TODO Auto-generated method stub
				
			}
			
		};
	}

	
	public String getPhysicalDestinationName() {
		if (getFileSystem() instanceof HasPhysicalDestination) {
			return ((HasPhysicalDestination)getFileSystem()).getPhysicalDestinationName();
		}
		return null;
	}

	public FS getFileSystem() {
		return fileSystem;
	}

	public void setFileSystem(FS fileSystem) {
		this.fileSystem = fileSystem;
	}

}
