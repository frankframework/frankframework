/*
   Copyright 2019 Integration Partners

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
package nl.nn.adapterframework.extensions.aspose.services.conv.impl.convertors;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.Logger;
import org.apache.tika.mime.MediaType;

import com.aspose.pdf.Document;

import nl.nn.adapterframework.extensions.aspose.ConversionOption;
import nl.nn.adapterframework.extensions.aspose.services.conv.CisConversionResult;
import nl.nn.adapterframework.extensions.aspose.services.util.ConvertorUtil;
import nl.nn.adapterframework.extensions.aspose.services.util.FileUtil;
import nl.nn.adapterframework.util.DateUtils;
import nl.nn.adapterframework.util.LogUtil;

abstract class AbstractConvertor implements Convertor {

	private static final Logger LOGGER = LogUtil.getLogger(AbstractConvertor.class);

	private List<MediaType> supportedMediaTypes;

	private String pdfOutputlocation;

	private static AtomicInteger atomicCount = new AtomicInteger(1);

	AbstractConvertor(String pdfOutputlocation, MediaType... args) {
		this.pdfOutputlocation = pdfOutputlocation;
		supportedMediaTypes = Arrays.asList(args);
	}

	/**
	 * Converts the the inputstream to the given file the builder object can also be
	 * updated (metaData set and any attachments added).
	 * 
	 * @param mediaType
	 * @param conversionOption
	 */
	protected abstract void convert(MediaType mediaType, File file, CisConversionResult builder,
			ConversionOption conversionOption) throws Exception;

	@Override
	public List<MediaType> getSupportedMediaTypes() {
		return supportedMediaTypes;
	}

	private void checkForSupportedMediaType(MediaType mediaType) {

		boolean supported = false;

		for (MediaType mediaTypeSupported : getSupportedMediaTypes()) {
			if (mediaTypeSupported.equals(mediaType)) {
				supported = true;
				break;
			}
		}

		// Create informative message.
		if (!supported) {
			StringBuilder builder = new StringBuilder();
			builder.append("This convertor '");
			builder.append(this.getClass().getSimpleName());
			builder.append("' only supports ");

			boolean first = true;
			for (MediaType mediaTypeSupported : getSupportedMediaTypes()) {
				if (!first) {
					builder.append(" or");
				}
				first = false;
				builder.append(" '");
				builder.append(mediaTypeSupported.toString());
				builder.append("'");
			}
			builder.append("! (received '");
			builder.append(mediaType);
			builder.append("')");
			throw new IllegalArgumentException(builder.toString());
		}

	}

	/**
	 * Should not be overloaded by the concrete classes.
	 */
	@Override
	public final CisConversionResult convertToPdf(MediaType mediaType, String filename, File file,
			ConversionOption conversionOption) {

		checkForSupportedMediaType(mediaType);

		CisConversionResult result = new CisConversionResult();
		File resultFile = null;
		try {
			resultFile = UniqueFileGenerator.getUniqueFile(pdfOutputlocation, this.getClass().getSimpleName(), "pdf");
			result.setConversionOption(conversionOption);
			result.setMediaType(mediaType);
			result.setDocumentName(ConvertorUtil.createTidyNameWithoutExtension(filename));
			result.setPdfResultFile(resultFile);
			result.setResultFilePath(resultFile.getAbsolutePath());
			
			LOGGER.debug("Convert to file... " + file.getName());
			convert(mediaType, file, result, conversionOption);
			LOGGER.debug("Convert to file finished. " + file.getName());

		} catch (Exception e) {
			if (isPasswordException(e)) {
				result = CisConversionResult.createPasswordFailureResult(filename, conversionOption, mediaType);
			} else {
				result.setFailureReason(createTechnishefoutMsg(e));
			}
			// Clear the file to state that the conversion has failed.
			result.setPdfResultFile(null);
		}
		return result;
	}

	protected String getPdfOutputlocation() {
		return pdfOutputlocation;
	}
	
	protected int getNumberOfPages(File file) throws IOException {
		int result = 0;
		if(file != null) {
			try (InputStream inStream = new FileInputStream(file)){
				Document doc = new Document(inStream);
				result = doc.getPages().size();
			} catch (IOException e) {
				throw e;
			}
		}
		
		return result;
	}

	protected String createTechnishefoutMsg(Exception e) {
		String tijdstip = DateUtils.format(new Date(), "dd-MM-yyyy HH:mm:ss");
		LOGGER.warn("Conversion in " + this.getClass().getSimpleName() + " failed! (Tijdstip: " + tijdstip + ")", e);
		StringBuilder msg = new StringBuilder();
		msg.append(
				"Het omzetten naar pdf is mislukt door een technische fout. Neem contact op met de functioneel beheerder.");
		msg.append("(Tijdstip: ");
		msg.append(tijdstip);
		msg.append(")");
		return msg.toString();
	}

	protected void deleteFile(File file) throws IOException {
		FileUtil.deleteFile(file);
	}

	/**
	 * Create a unique file in the pdfOutputLocation with the given extension
	 */
	protected File getUniqueFile() {

		SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss");
		int count = atomicCount.addAndGet(1);

		String fileNamePdf = String.format("conv_%s_%s_%05d%s", this.getClass().getSimpleName(),
				format.format(new Date()), count, ".bin");
		return new File(pdfOutputlocation, fileNamePdf);
	}

	protected abstract boolean isPasswordException(Exception e);

}
