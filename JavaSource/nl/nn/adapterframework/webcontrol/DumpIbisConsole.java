/*
 * $Log: DumpIbisConsole.java,v $
 * Revision 1.9  2008-07-24 12:38:07  europe\L190409
 * removed threadUnsafeness
 * now uses requestDispatcher
 *
 * Revision 1.8  2008/07/14 17:46:17  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added .zip to filename
 *
 * Revision 1.7  2008/06/24 08:01:35  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * remove subdirectory from zip
 * added newlines to logfile lines
 *
 * Revision 1.6  2008/06/18 12:40:23  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * quite a lot of modifications.
 * N.B. is still not threadsafe
 *
 * Revision 1.1  2008/04/04 14:07:54  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * *** empty log message ***
 *
 * Revision 1.5  2007/08/30 15:12:12  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * modified getRootLogger()
 *
 * Revision 1.4  2007/02/16 14:22:03  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * retrieve logfiles automatically
 *
 * Revision 1.3  2007/02/12 14:41:14  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * Logger from LogUtil
 *
 * Revision 1.2  2006/08/22 11:57:57  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * removed call tot response sink
 *
 * Revision 1.1  2006/08/22 07:51:42  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * first version
 *
 */
package nl.nn.adapterframework.webcontrol;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.Misc;

import org.apache.log4j.FileAppender;
import org.apache.log4j.Logger;

/**
 * Dumps the entire Ibis-Console to a zip-file which can be saved.
 * 
 * This functionality is developed because developers don't have rigths for the Ibis Console in the IUFWeb environment.
 * 
 * @author  Peter Leeuwenburgh
 * @version Id
 */
public class DumpIbisConsole extends HttpServlet {
	private Logger log = LogUtil.getLogger(this);

	private String directoryName = "";
	private ServletContext servletContext;

	public void doGet(HttpServletRequest request, HttpServletResponse response)
		throws IOException {
		process(request, response);
	}

	public void init(ServletConfig config) {
		servletContext = config.getServletContext();
	}

	public void copyResource(ZipOutputStream zipOutputStream, String resource) {
		try {
			String fileName = directoryName + resource;

			InputStream inputStream = servletContext.getResourceAsStream(resource);
			zipOutputStream.putNextEntry(new ZipEntry(fileName));
			Misc.streamToStream(inputStream,zipOutputStream);
			zipOutputStream.closeEntry();
		} catch (Exception e) {
			log.error("Error copying resource", e);
		}
	}

	public void copyServletResponse(ZipOutputStream zipOutputStream, HttpServletRequest request, HttpServletResponse response, String resource, 
		String destinationFileName, Set resources, Set setFileViewer, Set setShowAdapterStatistics) {
		long timeStart = new Date().getTime();
		try {
			IbisServletResponseWrapper ibisHttpServletResponseGrabber =  new IbisServletResponseWrapper(response);
			RequestDispatcher requestDispatcher = servletContext.getRequestDispatcher(resource);
			requestDispatcher.include(request, ibisHttpServletResponseGrabber);
			String htmlString = ibisHttpServletResponseGrabber.getStringWriter().toString();

			zipOutputStream.putNextEntry(new ZipEntry(destinationFileName));
			
			PrintWriter pw = new PrintWriter(zipOutputStream);
			pw.print(htmlString);
			pw.flush();
			zipOutputStream.closeEntry();

			if (!resource.startsWith("FileViewerServlet")) {
				extractResources(resources, htmlString);
			}
			if (resource.equals("/showLogging.do")) {
				extractLogging(setFileViewer, htmlString);
			}
			if (resource.equals("/showConfigurationStatus.do")) {
				extractStatistics(setShowAdapterStatistics, htmlString);
			}
		} catch (Exception e) {
			log.error("Error copying servletResponse", e);
		}
		long timeEnd = new Date().getTime();
		log.debug("dumped file [" + destinationFileName + "] in " + (timeEnd - timeStart) + " msec.");
	}

	public void extractLogging(Set setFileViewer, String htmlString) {

		String hs = htmlString.toUpperCase();
		int p0 = hs.indexOf("<A ");
		while (p0 >= 0) {
			int p1 = hs.indexOf(">", p0);
			if (p1 > p0) {
				int p2 = hs.indexOf(" HREF", p0);
				if (p2 > p0 && p2 < p1) {
					int p3 = hs.indexOf("\"", p2);
					if (p3 > p2 && p3 < p1) {
						p3++;
						int p4 = hs.indexOf("\"", p3);
						if (p4 > p3 && p4 < p1) {
							String s = htmlString.substring(p3, p4);
							if (s.startsWith("FileViewerServlet")) {
								setFileViewer.add(s);
							}
						}
					}
				}
			}
			p0 = hs.indexOf("<A ", p1);
		}
	}

	public void extractResources(Set resources, String htmlString) {

		String hs = htmlString.toUpperCase();
		int p0 = hs.indexOf("<LINK ");
		while (p0 >= 0) {
			int p1 = hs.indexOf(">", p0);
			if (p1 > p0) {
				int p2 = hs.indexOf(" HREF", p0);
				if (p2 > p0 && p2 < p1) {
					int p3 = hs.indexOf("\"", p2);
					if (p3 > p2 && p3 < p1) {
						p3++;
						int p4 = hs.indexOf("\"", p3);
						if (p4 > p3 && p4 < p1) {
							String s = htmlString.substring(p3, p4);
							resources.add(s);
						}
					}
				}
			}
			p0 = hs.indexOf("<LINK ", p1);
		}

		p0 = hs.indexOf("<IMG ");
		while (p0 >= 0) {
			int p1 = hs.indexOf(">", p0);
			if (p1 > p0) {
				int p2 = hs.indexOf(" SRC", p0);
				if (p2 > p0 && p2 < p1) {
					int p3 = hs.indexOf("\"", p2);
					if (p3 > p2 && p3 < p1) {
						p3++;
						int p4 = hs.indexOf("\"", p3);
						if (p4 > p3 && p4 < p1) {
							String s = htmlString.substring(p3, p4);
							resources.add(s);
						}
					}
				}
			}
			p0 = hs.indexOf("<IMG ", p1);
		}
	}

	public void extractStatistics(Set setShowAdapterStatistics, String htmlString) {

		String hs = htmlString.toUpperCase();
		int p0 = hs.indexOf("<A ");
		while (p0 >= 0) {
			int p1 = hs.indexOf(">", p0);
			if (p1 > p0) {
				int p2 = hs.indexOf(" HREF", p0);
				if (p2 > p0 && p2 < p1) {
					int p3 = hs.indexOf("\"", p2);
					if (p3 > p2 && p3 < p1) {
						p3++;
						int p4 = hs.indexOf("\"", p3);
						if (p4 > p3 && p4 < p1) {
							String s = htmlString.substring(p3, p4);
							if (s.startsWith("showAdapterStatistics.do")) {
								setShowAdapterStatistics.add(s);
							}
						}
					}
				}
			}
			p0 = hs.indexOf("<A ", p1);
		}
	}

	public void process(HttpServletRequest request, HttpServletResponse response) {
		try {
			Set resources = new HashSet();
			Set setFileViewer = new HashSet();
			Set setShowAdapterStatistics = new HashSet();

			OutputStream out = response.getOutputStream();
			response.setContentType("application/x-zip-compressed");
			response.setHeader("Content-Disposition","attachment; filename=\"IbisConsoleDump-"+AppConstants.getInstance().getProperty("instance.name","")+"-"+Misc.getHostname()+".zip\"");
			ZipOutputStream zipOutputStream = new ZipOutputStream(out);

			copyServletResponse(zipOutputStream, request, response, "/showConfigurationStatus.do", directoryName + "showConfigurationStatus.html", resources, setFileViewer, setShowAdapterStatistics);

			for (Iterator iterator = setShowAdapterStatistics.iterator();
				iterator.hasNext();
				) {
				String s = (String) iterator.next();
				int p1 = s.length();
				int p2 = s.indexOf("adapterName=");
				String fileName = s.substring(p2 + 12, p1);
				File file = new File(fileName);
				copyServletResponse(zipOutputStream, request, response, "/" + s, directoryName + "showAdapterStatistics_" + file.getName() + ".html", resources, setFileViewer, setShowAdapterStatistics);
			}

			copyServletResponse(zipOutputStream, request, response, "/showLogging.do", directoryName + "showLogging.html", resources, setFileViewer, setShowAdapterStatistics);

			FileAppender fa = (FileAppender)LogUtil.getRootLogger().getAppender("file");
			File logFile = new File(fa.getFile());
			String logFileName = logFile.getName();

			for (Iterator iterator = setFileViewer.iterator();iterator.hasNext();) {
				String s = (String) iterator.next();
				if (s.indexOf("resultType=text") >= 0) {
					int p1 = s.length();
					int p2 = s.indexOf("fileName=");
					String fileName = s.substring(p2 + 9, p1);
					File file = new File(fileName);
					String fn = file.getName();
					if (fn.startsWith(logFileName)) {
						copyServletResponse(zipOutputStream, request, response, "/" + s, directoryName + "log/" + fn, resources, setFileViewer, setShowAdapterStatistics);
					}
				}
			}

			copyServletResponse(zipOutputStream, request, response, "/showEnvironmentVariables.do", directoryName + "showEnvironmentVariables.html", resources, setFileViewer, setShowAdapterStatistics);
			copyServletResponse(zipOutputStream, request, response, "/showConfiguration.do", directoryName + "showConfiguration.html", resources, setFileViewer, setShowAdapterStatistics);
			copyServletResponse(zipOutputStream, request, response, "/showSchedulerStatus.do", directoryName + "showSchedulerStatus.html", resources, setFileViewer, setShowAdapterStatistics);

			for (Iterator iterator = resources.iterator(); iterator.hasNext();) {
				copyResource(zipOutputStream, (String)iterator.next());
			}

			zipOutputStream.close();

		} catch (Exception e) {
			log.error("Error dumping ibis console", e);
		}
	}
}
