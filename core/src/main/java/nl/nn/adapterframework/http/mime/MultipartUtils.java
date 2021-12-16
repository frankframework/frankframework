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

import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;

import org.apache.logging.log4j.Logger;

import nl.nn.adapterframework.util.LogUtil;

public abstract class MultipartUtils {
	private static Logger log = LogUtil.getLogger(MultipartUtils.class);

	public static final String FORM_DATA = "form-data";
	public static final String MULTIPART = "multipart/";

	public static boolean isMultipart(HttpServletRequest request) {
		String httpMethod = request.getMethod().toUpperCase();
		if("POST".equals(httpMethod) || "PUT".equals(httpMethod) || "PATCH".equals(httpMethod)) {
			return request.getContentType().startsWith(MULTIPART);
		}
		return false;
	}

	public static String getFieldName(BodyPart part) {
		try {
			String[] cd = part.getHeader("Content-Disposition");
			if(cd != null) {
				String cdFields = cd[0]; //form-data; name="file1"; filename="file1"
				if(cdFields != null && cdFields.toLowerCase().startsWith(FORM_DATA)) {
					return parseParameterField(cdFields, "name");
				}
			}
		} catch (MessagingException e) {
			log.warn("unable to determine fieldname from part ["+part+"]");
		}
		return null;
	}

	public static boolean isBinary(BodyPart part) {
		try {
			String[] cd = part.getHeader("Content-Transfer-Encoding");
			if(cd != null) {
				String cdFields = cd[0]; //Content-Transfer-Encoding - binary || 8bit
				if(cdFields != null && cdFields.equalsIgnoreCase("binary")) {
					return true;
				}
			}
		} catch (MessagingException e) {
			log.warn("unable to determine if part ["+part+"] is binary");
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
