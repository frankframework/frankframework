/*
   Copyright 2021-2023 WeAreFrank!

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

import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;

import lombok.extern.log4j.Log4j2;

@Log4j2
public abstract class MultipartUtils {

	public static final String FORM_DATA = "form-data";
	public static final String MULTIPART = "multipart/";
	public static final String ATTACHMENT = "attachment";

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
			if(id != null && StringUtils.isNotBlank(id[0])) {
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

	/**
	 * Check for the filename in the <code>Content-Disposition</code> header.
	 * Eg. Content-Disposition form-data; name="file"; filename="dummy.jpg"
	 * Eg. Content-Disposition attachment; filename="dummy.jpg"
	 */
	public static String getFileName(BodyPart part) throws MessagingException {
		String[] cd = part.getHeader("Content-Disposition");
		if(cd != null) {
			String cdFields = cd[0];
			if (cdFields.startsWith(FORM_DATA) || cdFields.startsWith(ATTACHMENT)) {
				String filename = parseParameterField(cdFields, "filename");
				if(StringUtils.isNotEmpty(filename)) {
					return filename.trim();
				}
			}
		}
		return null;
	}

	public static boolean isBinary(BodyPart part) {
		try {
			//Check if a filename is present (indicating it's a file and not a field)
			String filename = getFileName(part);
			if(filename != null) {
				return true;
			}

			//Check if the transfer encoding has been set when MTOM
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
			String[] f = field.trim().split("=", 2);
			String name = f[0];
			if(f.length > 1 && fieldName.equalsIgnoreCase(name) && StringUtils.isNotBlank(f[1])) {
				return f[1].substring(1, f[1].length()-1);
			}
		}
		return null;
	}
}
