/*
 * $Log: FileRecordReceiver.java,v $
 * Revision 1.4  2004-08-26 13:32:09  L190409
 * added @deprecated tag to javadoc
 *
 */
package nl.nn.adapterframework.receivers;

/**
 * A {@link PullingReceiverBase PullingReceiver} that uses a {@link FileRecordListener} as its listener.
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>classname</td><td>nl.nn.adapterframework.receivers.FileRecordReceiver</td><td>&nbsp;</td></tr>
 * <tr><td>{@link PullingReceiverBase#setName(String) name}</td>  <td>name of the receiver as known to the adapter</td><td>&nbsp;</td></tr>
 * <tr><td>{@link PullingReceiverBase#setNumThreads(int) numThreads}</td><td>the number of threads listening in parallel for messages</td><td>1</td></tr>
 * <tr><td>{@link PullingReceiverBase#setOnError(String) onError}</td><td>one of 'continue' or 'close'. Controls the behaviour of the receiver when it encounters an error sending a reply</td><td>continue</td></tr>
 * <tr><td>{@link FileRecordListener#setInputDirectory(String) listener.inputDirectory}</td><td>the directory to look for files</td><td>&nbsp; </td></tr>
 * <tr><td>{@link FileRecordListener#setWildcard(String) listener.wildcard}</td><td>the wildcard for the files to process</td><td>&nbsp;</td></tr>
 * <tr><td>{@link FileRecordListener#setDirectoryProcessedFiles(String) listener.directoryProcessedFiles}</td><td>the directory to put processed files in</td><td>&nbsp;</td></tr>
 * <tr><td>{@link FileRecordListener#setResponseTime(long) listener.setResponseTime}</td><td>set the time to delay when no records are to be processed and this class has to look for the arrival of a new file</td><td>1000 milliseconds</td></tr>
 * </table>
 * <p>Creation date: (17-12-2003 10:24:13)</p>
 * @version Id
 * @author Johan Verrips IOS
 * @deprecated please use use {@link  nl.nn.adapterframework.receivers.GenericReceiver GenericReceiver} in combination with {@link  nl.nn.adapterframework.receivers.FileRecordListener FileRecordListener} instead
 */
public class FileRecordReceiver extends PullingReceiverBase {
	public static final String version="$Id: FileRecordReceiver.java,v 1.4 2004-08-26 13:32:09 L190409 Exp $";

/**
 * FileRecordReceiver constructor comment.
 */
public FileRecordReceiver() {
	
	super();
	FileRecordListener frl=new FileRecordListener();
	frl.setName(this.getName());
	setListener(frl);
}
}
