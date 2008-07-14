/*
 * $Log: DumpIbisConsole.java,v $
 * Revision 1.8  2008-07-14 17:46:17  europe\L190409
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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

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
 * Dumps the entire Ibis Console to a zip-file which can be saved.
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
	private Set resources = new HashSet();
	private Set setFileViewer = new HashSet();
	private Set setShowAdapterStatistics = new HashSet();
	private ZipOutputStream zipOutputStream;

	public void doGet(HttpServletRequest request, HttpServletResponse response)
		throws IOException {
		process(request, response);
	}

	public void init(ServletConfig config) {
		servletContext = config.getServletContext();
	}

	public void copyResource(String resource) {
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

	public void copyServletResponse(HttpServletRequest request, String resource, String destinationFileName) {
		long timeStart = new Date().getTime();
		try {
/*
			RequestDispatcher requestDispatcher = servletContext.getRequestDispatcher(resource);
			requestDispatcher.include(request, ibisHttpServletResponseWrapper);
			String htmlString = ibisHttpServletResponseWrapper.getStringWriter().toString();
			InputStream inputStream = new ByteArrayInputStream(htmlString.getBytes());
*/
			String contextUri =
				request.getScheme()+"://"+
				request.getServerName()+":"+
				request.getServerPort()+
				request.getContextPath();
			String urlStringNew = contextUri+ Misc.replace(resource, " ", "%20");
			URL url = new URL(urlStringNew);
			BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()));
			StringBuffer sb = new StringBuffer();
			String s = br.readLine();
			while (s != null) {
				sb.append(s).append('\n');
				s = br.readLine();
			}
			String htmlString = sb.toString();
			InputStream inputStream = new ByteArrayInputStream(htmlString.getBytes());

			zipOutputStream.putNextEntry(new ZipEntry(destinationFileName));
			// HttpServletResponseSink sink = new HttpServletResponseSink(response,zipOutputStream);
			
			Misc.streamToStream(inputStream,zipOutputStream);
			zipOutputStream.closeEntry();

			if (!resource.startsWith("FileViewerServlet")) {
				extractResources(htmlString);
			}
			if (resource.equals("/showLogging.do")) {
				extractLogging(htmlString);
			}
			if (resource.equals("/showConfigurationStatus.do")) {
				extractStatistics(htmlString);
			}
		} catch (Exception e) {
			log.error("Error copying servletResponse", e);
		}
		long timeEnd = new Date().getTime();
		log.debug("dumped file [" + destinationFileName + "] in " + (timeEnd - timeStart) + " msec.");
	}

	public void extractLogging(String htmlString) {

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

	public void extractResources(String htmlString) {

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

	public void extractStatistics(String htmlString) {

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
			OutputStream out = response.getOutputStream();
			response.setContentType("application/x-zip-compressed");
			response.setHeader("Content-Disposition","attachment; filename=\"IbisConsoleDump-"+AppConstants.getInstance().getProperty("instance.name","")+"-"+Misc.getHostname()+".zip\"");
			zipOutputStream = new ZipOutputStream(out);

			copyServletResponse( request, "/showConfigurationStatus.do", directoryName + "showConfigurationStatus.html");

			for (Iterator iterator = setShowAdapterStatistics.iterator();
				iterator.hasNext();
				) {
				String s = (String) iterator.next();
				int p1 = s.length();
				int p2 = s.indexOf("adapterName=");
				String fileName = s.substring(p2 + 12, p1);
				File file = new File(fileName);
				copyServletResponse(request, "/" + s, directoryName + "showAdapterStatistics_" + file.getName() + ".html");
			}

			copyServletResponse(request, "/showLogging.do", directoryName + "showLogging.html");

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
						copyServletResponse(request, "/" + s, directoryName + "log/" + fn);
					}
				}
			}

			copyServletResponse(request, "/showEnvironmentVariables.do", directoryName + "showEnvironmentVariables.html");
			copyServletResponse(request, "/showConfiguration.do", directoryName + "showConfiguration.html");
			copyServletResponse(request, "/showSchedulerStatus.do", directoryName + "showSchedulerStatus.html");

			for (Iterator iterator = resources.iterator(); iterator.hasNext();) {
				copyResource((String)iterator.next());
			}

			zipOutputStream.close();

		} catch (Exception e) {
			log.error("Error dumping ibis console", e);
		}
	}
}
