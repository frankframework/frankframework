/*
 * $Log: TestPipeLineExecute.java,v $
 * Revision 1.6  2007-02-12 14:36:29  europe\L190409
 * added zipfile-upload capability
 *
 */
package nl.nn.adapterframework.webcontrol.action;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import nl.nn.adapterframework.core.IAdapter;
import nl.nn.adapterframework.core.PipeLineResult;
import nl.nn.adapterframework.util.Misc;

import org.apache.commons.lang.StringUtils;
import org.apache.struts.action.ActionError;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.DynaActionForm;
import org.apache.struts.upload.FormFile;

/**
 * Test the Pipeline of an adapter.
 * 
 * @author  Johan Verrips
 * @see nl.nn.adapterframework.configuration.Configuration
 * @see nl.nn.adapterframework.core.Adapter
 * @see nl.nn.adapterframework.core.PipeLine
 * @version Id
 */

public final class TestPipeLineExecute extends ActionBase {
	public static final String version="$RCSfile: TestPipeLineExecute.java,v $  $Revision: 1.6 $ $Date: 2007-02-12 14:36:29 $";
	
	public ActionForward execute(
	    ActionMapping mapping,
	    ActionForm form,
	    HttpServletRequest request,
	    HttpServletResponse response)
	    throws IOException, ServletException {
	
	    // Initialize action
	    initAction(request);
	    if (null == config) {
	        return (mapping.findForward("noconfig"));
	    }
	    
	
	    DynaActionForm pipeLineTestForm = (DynaActionForm) form;
	    String form_adapterName = (String) pipeLineTestForm.get("adapterName");
	    String form_message = (String) pipeLineTestForm.get("message");
		String form_remoteDirectory = (String) pipeLineTestForm.get("remoteDirectory");
	    String form_resultText = "";
	    String form_resultState = "";
	    FormFile form_file = (FormFile) pipeLineTestForm.get("file");
	
	    // if no message and no formfile, send an error
	    if ( StringUtils.isEmpty(form_message) &&
	        ( form_file==null || form_file.getFileSize() == 0 ) &&
			 StringUtils.isEmpty(form_remoteDirectory)) {
	
	        storeFormData(null, null, null, null, pipeLineTestForm);
	        errors.add("", new ActionError("errors.generic", "Nothing to send or test"));
	    }
	    // Report any errors we have discovered back to the original form
	    if (!errors.isEmpty()) {
	        saveErrors(request, errors);
	        storeFormData(null, null, null, null, pipeLineTestForm);
	        return (new ActionForward(mapping.getInput()));
	    }
	    if ((form_adapterName == null) || (form_adapterName.length() == 0)) {
	        errors.add("", new ActionError("errors.generic", "No adapter selected"));
	    }
	    // Report any errors we have discovered back to the original form
	    if (!errors.isEmpty()) {
	        saveErrors(request, errors);
	        storeFormData(null, null, form_message, form_remoteDirectory, pipeLineTestForm);
	        return (new ActionForward(mapping.getInput()));
	    }
	    // Execute the request
	    IAdapter adapter = config.getRegisteredAdapter(form_adapterName);
	    if (adapter == null) {
			errors.add("", new ActionError("errors.generic", "Adapter with specified name ["+form_adapterName+"] could not be retrieved"));
	    }
	    // Report any errors we have discovered back to the original form
	    if (!errors.isEmpty()) {
	        saveErrors(request, errors);
	        storeFormData(null, null, form_message, form_remoteDirectory, pipeLineTestForm);
	        return (new ActionForward(mapping.getInput()));
	    }
	
	    // if upload is choosen, it prevails over the message
	    if ((form_file != null) && (form_file.getFileSize() > 0)) {
			log.debug("Upload of file ["+form_file.getFileName()+"] ContentType["+form_file.getContentType()+"]");
	    	if (form_file.getFileName().endsWith(".zip")) {
	    		ZipInputStream archive = new ZipInputStream(new ByteArrayInputStream(form_file.getFileData()));
	    		for (ZipEntry entry=archive.getNextEntry(); entry!=null; entry=archive.getNextEntry()) {
	    			String name = entry.getName();
					int size = (int)entry.getSize();
					if (size>0) {
						byte[] b=new byte[size];
						int rb=0;
						int chunk=0;
						while (((int)size - rb) > 0) {
							chunk=archive.read(b,rb,(int)size - rb);
							if (chunk==-1) {
							   break;
							}
							rb+=chunk;
						}
						String currentMessage = new String(b,0,rb);
						PipeLineResult pipeLineResult =
							adapter.processMessage(name+"_" + Misc.createSimpleUUID(), currentMessage);
						form_resultText += name + ":" + pipeLineResult.getState() + "\n";
						form_resultState = pipeLineResult.getState();
					}
					archive.closeEntry();
	    		}
	    		archive.close();
	    	} else {
				form_message = new String(form_file.getFileData());
	    	}
	    }
//		// if remote directory is choosen and not upload, it prevails over the message
//		else if (form_remoteDirectory != null && form_remoteDirectory.length() > 0) {
//			form_message = "";
//			File importDir = new File(form_remoteDirectory);
//			if (!importDir.isDirectory()) {
//				errors.add("", new ActionError("errors.generic", "Remote directory is not a directory"));
//			} else {
//				//zoek alle bestanden in de directory importDir
//				FileFilter fileFilter = new FileFilter() {
//					public boolean accept(File inFile) {
//						// To return whether it ends with any extension.
//						// for example ends with .xml 
//						return inFile.isFile() && inFile.getName().endsWith(".xml");
//					}
//				};
//				File[] files = importDir.listFiles(fileFilter);
//				if (files == null) {
//					log.warn("Geen bestanden gevonden in " + importDir.getAbsolutePath());
//					errors.add("", new ActionError("errors.generic", "No files found in remote directory"));
//				} else {
//					form_resultText = "";
//					for (int i = 0; i < files.length; i++) {
//						File currentFile = files[i];
//						InputStream is = new FileInputStream(currentFile);
//						InputStreamReader reader = new InputStreamReader(is);
//						BufferedReader bufread = new BufferedReader(reader);
//						String line;
//						StringBuffer stringBuf = new StringBuffer();
//						while ((line = bufread.readLine()) != null) {
//							stringBuf.append(line);
//							stringBuf.append("\n");
//						}
//						String currentMessage = stringBuf.toString();
//						PipeLineResult pipeLineResult =
//							adapter.processMessage("testmessage" + Misc.createSimpleUUID(), currentMessage);
//						form_resultText += currentFile.getName() + ":" + pipeLineResult.getState() + "\n";
//						form_resultState = pipeLineResult.getState();
//					}
//	
//				}
//			}
//		}
	
		if(form_message != null && form_message.length() > 0) {
	    // Execute the request
			form_remoteDirectory = "";
			PipeLineResult pipeLineResult = adapter.processMessage("testmessage" + Misc.createSimpleUUID(), form_message);
	    	form_resultText = pipeLineResult.getResult();
			form_resultState = pipeLineResult.getState();
		}
	    storeFormData(form_resultText, form_resultState, form_message, form_remoteDirectory, pipeLineTestForm);
	
	    // Report any errors we have discovered back to the original form
	    if (!errors.isEmpty()) {
	        saveErrors(request, errors);
	        return (new ActionForward(mapping.getInput()));
	    }
	
	    // Forward control to the specified success URI
	    log.debug("forward to success");
	    return (mapping.findForward("success"));
	
	}

	public void storeFormData(String result, String state, String message, String remoteDirectory, DynaActionForm pipeLineTestForm) {
	
	    // refresh list of stopped adapters
	    // =================================
	    ArrayList adapters = new ArrayList();
	    adapters.add("-- select an adapter --");
	
	    // get the names of the Adapters
	    Iterator adapterNamesIt = config.getRegisteredAdapterNames();
	    while (adapterNamesIt.hasNext()) {
	        String adapterName = (String) adapterNamesIt.next();
	        adapters.add(adapterName);
	    }
	    pipeLineTestForm.set("adapters", adapters);
	    if (null!=message) pipeLineTestForm.set("message", message);
		if (null!=remoteDirectory) pipeLineTestForm.set("remoteDirectory", remoteDirectory);
	    if (null != result) {
	        pipeLineTestForm.set("result", result);	
	    }
	    if (null != state) {
	        pipeLineTestForm.set("state", state);
	
	    }	
	}
}
