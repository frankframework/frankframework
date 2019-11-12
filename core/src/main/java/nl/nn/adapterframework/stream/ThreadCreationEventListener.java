package nl.nn.adapterframework.stream;

public interface ThreadCreationEventListener {

	public Object announceChildThread(Object owner, String correlationId);
	public void threadCreated(Object ref);
	public void threadEnded(Object ref, String result);
	public void threadAborted(Object ref, Throwable t);
	
}
