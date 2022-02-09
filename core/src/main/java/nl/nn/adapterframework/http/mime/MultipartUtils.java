/*
   Copyright 2021 WeAreFrank!

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package nl.nn.adapterframework.http.mime;

import java.util.Locale;
import java.util.Map;

import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.fileupload.ParameterParser;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;

import nl.nn.adapterframework.util.LogUtil;

public abstract class MultipartUtils {
	private static Logger log = LogUtil.getLogger(MultipartUtils.class);

	public static final String FORM_DATA = "form-data";
	public static final String MULTIPART = "multipart/";

	public static boolean isMultipart(HttpServletRequest request) {
		String httpMethod = request.getMethod().toUpperCase();
		if("POST".equals(httpMethod) || "PUT".equals(httpMethod) || "PATCH".equals(httpMethod)) {
			return (request.getContentType() != null && request.getContentType().startsWith(MULTIPART));
		}
		return false;
	}

	public static String getFieldName(BodyPart part) {
		try {
			String[] id = part.getHeader("Content-ID"); //MTOM requests
			if(id != null) {
				String idField = id[0];
				return idField.substring(1, idField.length()-1);
			}

			String[] cd = part.getHeader("Content-Disposition"); //MTOM Attachments and FORM-DATA requests
			if(cd != null) {
				String cdFields = cd[0]; //form-data; name="file1"; filename="file1" || attachment; name="file1"; filename="file1"
				if(cdFields != null) {
					return parseParameterField(cdFields, "name");
				}
			}
		} catch (MessagingException e) {
			log.warn("unable to determine fieldname from part ["+part+"]", e);
		}
		return null;
	}

	public static String getFileName(BodyPart part) {
		String[] cd = part.getHeader("Content-Disposition");
		if(cd != null) {
			String cdFields = cd[0];
			if (cdl.startsWith(FORM_DATA) || cdl.startsWith(ATTACHMENT)) {
				
			}
		}
	}

    private String getFileName(String pContentDisposition) {
        String fileName = null;
        if (pContentDisposition != null) {
            String cdl = pContentDisposition.toLowerCase(Locale.ENGLISH);
            if (cdl.startsWith(FORM_DATA) || cdl.startsWith(ATTACHMENT)) {
                ParameterParser parser = new ParameterParser();
                parser.setLowerCaseNames(true);
                // Parameter parser can handle null input
                Map<String, String> params = parser.parse(pContentDisposition, ';');
                if (params.containsKey("filename")) {
                    fileName = params.get("filename");
                    if (fileName != null) {
                        fileName = fileName.trim();
                    } else {
                        // Even if there is no value, the parameter is present,
                        // so we return an empty file name rather than no file
                        // name.
                        fileName = "";
                    }
                }
            }
        }
        return fileName;
    }

	public static boolean isBinary(BodyPart part) {
		try {
			//Check if a filename is present (indicating it's a file and not a field)
			String[] cd = part.getHeader("Content-Disposition");
			if(cd != null) {
				String cdFields = cd[0];
				if(StringUtils.isNotEmpty(parseParameterField(cdFields, "filename"))) {
					return true;
				}
			}

			//Check if the transfer encoding has been set
			String[] cte = part.getHeader("Content-Transfer-Encoding");
			if(cte != null) {
				String cteFields = cte[0]; //Content-Transfer-Encoding - binary || 8bit
				if(cteFields != null && cteFields.equalsIgnoreCase("binary")) {
					return true;
				}
			}
		} catch (MessagingException e) {
			log.warn("unable to determine if part ["+part+"] is binary", e);
		}
		return false;
	}

	private static String parseParameterField(String cdFields, String fieldName) {
		for(String field : cdFields.split(";")) {
			String[] f = field.trim().split("=");
			String name = f[0];
			if(fieldName.equalsIgnoreCase(name)) {
				return f[1].substring(1, f[1].length()-1);
			}
		}
		return null;
	}
}
