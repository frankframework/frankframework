/*
 * $Log: DumpIbisConsole.java,v $
 * Revision 1.12  2009-08-04 11:36:35  L190409
 * use openZipDownload
 *
 * Revision 1.11  2008/09/17 12:28:58  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * add SystemOut and statistics file to Dump
 *
 * Revision 1.10  2008/08/27 16:26:50  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * some simplifications
 *
 * Revision 1.9  2008/07/24 12:38:07  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
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
import java.io.OutputStreamWriter;
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
import nl.nn.adapterframework.util.StreamUtil;

import org.apache.commons.lang.StringUtils;
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

	public void copyResource(ZipOutputStream zipOutputStream, String resourceName, String resourceContents) {
		try {
			String fileName = directoryName + resourceName;

			PrintWriter pw = new PrintWriter(new OutputStreamWriter(zipOutputStream,Misc.DEFAULT_INPUT_STREAM_ENCODING));
			zipOutputStream.putNextEntry(new ZipEntry(fileName));
			pw.print(resourceContents);
			pw.flush();
			zipOutputStream.closeEntry();
		} catch (Exception e) {
			log.error("Error copying resource", e);
		}
	}

	public void copyServletResponse(ZipOutputStream zipOutputStream, HttpServletRequest request, HttpServletResponse response, String resource,
		String destinationFileName, Set resources, Set linkSet, String linkFilter) {
		long timeStart = new Date().getTime();
		try {
			IbisServletResponseWrapper ibisHttpServletResponseGrabber =  new IbisServletResponseWrapper(response);
			RequestDispatcher requestDispatcher = servletContext.getRequestDispatcher(resource);
			requestDispatcher.include(request, ibisHttpServletResponseGrabber);
			String htmlString = ibisHttpServletResponseGrabber.getStringWriter().toString();

			ZipEntry zipEntry=new ZipEntry(destinationFileName);
//			if (resourceModificationTime!=0) {
//				zipEntry.setTime(resourceModificationTime);
//			}

			zipOutputStream.putNextEntry(zipEntry);
			
			PrintWriter pw = new PrintWriter(zipOutputStream);
			pw.print(htmlString);
			pw.flush();
			zipOutputStream.closeEntry();

			if (!resource.startsWith("FileViewerServlet")) {
				extractResources(resources, htmlString);
			}
			if (linkSet!=null) {
				followLinks(linkSet, htmlString, linkFilter);
			}
		} catch (Exception e) {
			log.error("Error copying servletResponse", e);
		}
		long timeEnd = new Date().getTime();
		log.debug("dumped file [" + destinationFileName + "] in " + (timeEnd - timeStart) + " msec.");
	}

	public void followLinks(Set resourceSet, String htmlString, String linkStartFilter) {

		String hs = htmlString.toUpperCase();
		int posLinkStart = hs.indexOf("<A ");
		while (posLinkStart >= 0) {
			int posLinkStartClose = hs.indexOf(">", posLinkStart);
			if (posLinkStartClose > posLinkStart) {
				int posHref = hs.indexOf(" HREF", posLinkStart);
				if (posHref > posLinkStart && posHref < posLinkStartClose) {
					int posHrefContentStart = hs.indexOf("\"", posHref);
					if (posHrefContentStart > posHref && posHrefContentStart < posLinkStartClose) {
						posHrefContentStart++;
						int posHrefContentEnd = hs.indexOf("\"", posHrefContentStart);
						if (posHrefContentEnd > posHrefContentStart && posHrefContentEnd < posLinkStartClose) {
							String s = htmlString.substring(posHrefContentStart, posHrefContentEnd);
							if (StringUtils.isEmpty(linkStartFilter) || s.startsWith(linkStartFilter)) {
								resourceSet.add(s);
							}
						}
					}
				}
			}
			posLinkStart = hs.indexOf("<A ", posLinkStartClose);
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


	public void process(HttpServletRequest request, HttpServletResponse response) {
		try {
			Set resources = new HashSet();
			Set setFileViewer = new HashSet();
			Set setShowAdapterStatistics = new HashSet();

			ZipOutputStream zipOutputStream = StreamUtil.openZipDownload(response,"IbisConsoleDump-"+AppConstants.getInstance().getProperty("instance.name","")+"-"+Misc.getHostname()+".zip");

			copyServletResponse(zipOutputStream, request, response, "/showConfigurationStatus.do", directoryName + "showConfigurationStatus.html", resources, setShowAdapterStatistics, "showAdapterStatistics.do");

			for (Iterator iterator = setShowAdapterStatistics.iterator();
				iterator.hasNext();
				) {
				String s = (String) iterator.next();
				int p1 = s.length();
				int p2 = s.indexOf("adapterName=");
				String fileName = s.substring(p2 + 12, p1);
				File file = new File(fileName);
				copyServletResponse(zipOutputStream, request, response, "/" + s, directoryName + "showAdapterStatistics_" + file.getName() + ".html", resources, null,null);
			}

			copyServletResponse(zipOutputStream, request, response, "/showLogging.do", directoryName + "showLogging.html", resources, setFileViewer, "FileViewerServlet");

			// find filename of ibis logfiles
			FileAppender fa = (FileAppender)LogUtil.getRootLogger().getAppender("file");
			File logFile = new File(fa.getFile());
			String ibisLogFileName = logFile.getName();
			
			// find filename of stats logfiles
			String statLogFileStart = AppConstants.getInstance().getResolvedProperty("instance.name");
			
			String sysLogFileStart = "SystemOut";

			for (Iterator iterator = setFileViewer.iterator();iterator.hasNext();) {
				String s = (String) iterator.next();
				if (s.indexOf("resultType=text") >= 0) {
					int p1 = s.length();
					int p2 = s.indexOf("fileName=");
					String fileName = s.substring(p2 + 9, p1);
					File file = new File(fileName);
					String fn = file.getName();
					if (fn.startsWith(ibisLogFileName) || 
					    fn.startsWith(statLogFileStart) || 
						fn.startsWith(sysLogFileStart)) {
						copyServletResponse(zipOutputStream, request, response, "/" + s, directoryName + "log/" + fn, resources, null,null);
					}
				}
			}

			copyServletResponse(zipOutputStream, request, response, "/showEnvironmentVariables.do", directoryName + "showEnvironmentVariables.html", resources, null,null);
			copyServletResponse(zipOutputStream, request, response, "/showConfiguration.do", directoryName + "showConfiguration.html", resources, null,null);
			copyServletResponse(zipOutputStream, request, response, "/showSchedulerStatus.do", directoryName + "showSchedulerStatus.html", resources, null,null);
			copyServletResponse(zipOutputStream, request, response, "/showMonitors.do", directoryName + "showMonitors.html", resources, null,null);

			for (Iterator iterator = resources.iterator(); iterator.hasNext();) {
				copyResource(zipOutputStream, (String)iterator.next());
			}

			//copyResource(zipOutputStream,"statistics.xml", configuration.getStatisticsOfZo());
			zipOutputStream.close();

		} catch (Exception e) {
			log.error("Error dumping ibis console", e);
		}
	}
}
