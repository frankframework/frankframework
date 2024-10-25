package com.testautomationguru.utility;

/*
 * Copyright [2015] [www.testautomationguru.com]

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

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;

import org.apache.commons.io.FileUtils;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPageTree;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.tools.imageio.ImageIOUtil;

import lombok.Getter;
import lombok.Setter;

/**
* <h1>PDF Utility</h1>
* A simple pdf utility using apache pdfbox to get the text,
* compare files using plain text or pixel by pixel comparison, extract all the images from the pdf
*
* @author  www.testautomationguru.com
* @version 1.0
* @since   2015-06-13
*/
public class PDFUtil {

	private static final Logger logger = Logger.getLogger(PDFUtil.class.getName());
	private String imageDestinationPath;
	private boolean bTrimWhiteSpace = true;
	private boolean bHighlightPdfDifference = false;
	private Color imgColor = Color.MAGENTA;
	private PDFTextStripper stripper;
	private boolean bCompareAllPages = false;
	private CompareMode compareMode = CompareMode.TEXT_MODE;
	private String[] excludePattern;
	private int startPage = 1;
	private int endPage = -1;
	private @Getter @Setter double allowedRGBDeviation = 0.0;

	/*
	 * Constructor
	 */

	public PDFUtil(){
		logger.setLevel(Level.OFF);
		System.setProperty("sun.java2d.cmm", "sun.java2d.cmm.kcms.KcmsServiceProvider");
	}

   /**
   * This method is used to show log in the console. Level.INFO
   * It is set to Level.OFF by default.
   */
	public void enableLog(){
		logger.setLevel(Level.INFO);
	}

   /**
   * This method is used to change the file comparison mode text/visual
   * @param mode CompareMode
   */
	public void setCompareMode(CompareMode mode){
		this.compareMode = mode;
	}

   /**
   * This method is used to get the current comparison mode text/visual
   * @return CompareMode
   */
	public CompareMode getCompareMode(){
		return this.compareMode;
	}

   /**
   * This method is used to change the level
   * @param level java.util.logging.Level
   */
	public void setLogLevel(Level level){
		logger.setLevel(level);
	}

   /**
   * getText method by default replaces all the white spaces and compares.
   * This method is used to enable/disable the feature.
   *
   * @param flag true to enable;  false otherwise
   */
	public void trimWhiteSpace(boolean flag){
		this.bTrimWhiteSpace = flag;
	}

   /**
   * Path where images are stored
   * when the savePdfAsImage or extractPdfImages methods are invoked.
   *
   * @return String Absolute path where images are stored
   */
	public String getImageDestinationPath(){
		return this.imageDestinationPath;
	}

   /**
   * Set the path where images to be stored
   * when the savePdfAsImage or extractPdfImages methods are invoked.
   *
   * @param path Absolute path to store the images
   */
	public void setImageDestinationPath(String path){
		this.imageDestinationPath = path;
	}

   /**
   * Highlight the difference when 2 pdf files are compared in Binary mode.
   * The result is saved as an image.
   *
   * @param flag true - enable ; false - disable (default);
   */
	public void highlightPdfDifference(boolean flag){
		this.bHighlightPdfDifference = flag;
	}

   /**
   * Color in which pdf difference can be highlighted.
   * MAGENTA is the default color.
   *
   * @param colorCode color code to highlight the difference
   */
	public void highlightPdfDifference(Color colorCode){
		this.bHighlightPdfDifference = true;
		this.imgColor = colorCode;
	}

   /**
   * To compare all the pages of the PDF files. By default as soon as a mismatch is found, the method returns false and exits.
   *
   * @param flag true to enable; false otherwise
   */
	public void compareAllPages(boolean flag){
		this.bCompareAllPages = flag;
	}

   /**
   * To modify the text extracting strategy using PDFTextStripper
   *
   * @param stripper Stripper with user strategy
   */
    public void useStripper(PDFTextStripper stripper){
        this.stripper = stripper;
    }

   /**
   * Get the page count of the document.
   *
   * @param file Absolute file path
   * @return int No of pages in the document.
   * @throws java.io.IOException when file is not found.
   */
	public int getPageCount(String file) throws IOException{
		logger.fine("file :" + file);
		PDDocument doc = Loader.loadPDF(new File(file));
		int pageCount = doc.getNumberOfPages();
		logger.fine("pageCount :" + pageCount);
		doc.close();
		return pageCount;
	}

   /**
   * Get the content of the document as plain text.
   *
   * @param file Absolute file path
   * @return String document content in plain text.
   * @throws java.io.IOException when file is not found.
   */
	public String getText(String file) throws IOException{
		return this.getPDFText(file,-1, -1);
	}

   /**
   * Get the content of the document as plain text.
   *
   * @param file Absolute file path
   * @param startPage Starting page number of the document
   * @return String document content in plain text.
   * @throws java.io.IOException when file is not found.
   */
	public String getText(String file, int startPage) throws IOException{
		return this.getPDFText(file,startPage, -1);
	}

   /**
   * Get the content of the document as plain text.
   *
   * @param file Absolute file path
   * @param startPage Starting page number of the document
   * @param endPage Ending page number of the document
   * @return String document content in plain text.
   * @throws java.io.IOException when file is not found.
   */
	public String getText(String file, int startPage, int endPage) throws IOException{
		return this.getPDFText(file,startPage, endPage);
	}

   /**
   * This method returns the content of the document
   */
	private String getPDFText(String file, int startPage, int endPage) throws IOException{

		logger.info("file : " + file + ", startPage: " + startPage + ", endPage: " + endPage);

		PDDocument doc = Loader.loadPDF(new File(file));

		PDFTextStripper localStripper = new PDFTextStripper();
		if(null!=this.stripper){
		    localStripper = this.stripper;
		}

		this.updateStartAndEndPages(file, startPage, endPage);
		localStripper.setStartPage(this.startPage);
		localStripper.setEndPage(this.endPage);

		String txt = localStripper.getText(doc);
		logger.info("PDF Text before trimming : " + txt);
		if(this.bTrimWhiteSpace){
			txt = txt.trim().replaceAll("\\s+", " ").trim();
			logger.info("PDF Text after  trimming : " + txt);
		}

		doc.close();
		return txt;
	}


	public void excludeText(String... regexs){
		this.excludePattern = regexs;
	}


   /**
   * Compares two given pdf documents.
   *
   * <b>Note :</b> <b>TEXT_MODE</b> : Compare 2 pdf documents contents with no formatting.
   * 			   <b>VISUAL_MODE</b> : Compare 2 pdf documents pixel by pixel for the content and format.
   * @param file1 Absolute file path of the expected file
   * @param file2 Absolute file path of the actual file
   * @return boolean true if matches, false otherwise
   * @throws java.io.IOException when file is not found.
   */
	public double compare(String file1, String file2) throws IOException{
		return this.comparePdfFiles(file1, file2, -1, -1);
	}

   /**
   * Compares two given pdf documents.
   *
   * <b>Note :</b> <b>TEXT_MODE</b> : Compare 2 pdf documents contents with no formatting.
   * 			   <b>VISUAL_MODE</b> : Compare 2 pdf documents pixel by pixel for the content and format.
   *
   * @param file1 Absolute file path of the expected file
   * @param file2 Absolute file path of the actual file
   * @param startPage Starting page number of the document
   * @param endPage Ending page number of the document
   * @return boolean true if matches, false otherwise
   * @throws java.io.IOException when file is not found.
   */
	public double compare(String file1, String file2, int startPage, int endPage) throws IOException{
		return this.comparePdfFiles(file1, file2, startPage, endPage);
	}

   /**
   * Compares two given pdf documents.
   *
   * <b>Note :</b> <b>TEXT_MODE</b> : Compare 2 pdf documents contents with no formatting.
   * 			   <b>VISUAL_MODE</b> : Compare 2 pdf documents pixel by pixel for the content and format.
   *
   * @param file1 Absolute file path of the expected file
   * @param file2 Absolute file path of the actual file
   * @param startPage Starting page number of the document
   * @return boolean true if matches, false otherwise
   * @throws java.io.IOException when file is not found.
   */
	public double compare(String file1, String file2, int startPage) throws IOException{
		return this.comparePdfFiles(file1, file2, startPage, -1);
	}

	private double comparePdfFiles(String file1, String file2, int startPage, int endPage)throws IOException{
		if(CompareMode.TEXT_MODE==this.compareMode)
			return comparePdfFilesWithTextMode(file1, file2, startPage, endPage);
		else
			return comparePdfByImage(file1, file2, startPage, endPage);
	}

	private double comparePdfFilesWithTextMode(String file1, String file2, int startPage, int endPage) throws IOException{

		String file1Txt = this.getPDFText(file1, startPage, endPage).trim();
		String file2Txt = this.getPDFText(file2, startPage, endPage).trim();

		if(null != this.excludePattern){
			for (final String s : this.excludePattern) {
				file1Txt = file1Txt.replaceAll(s, "");
				file2Txt = file2Txt.replaceAll(s, "");
			}
		}

		logger.info("File 1 Txt : " + file1Txt);
		logger.info("File 2 Txt : " + file2Txt);

		int result = file1Txt.compareToIgnoreCase(file2Txt);

		if(result != 0){
			logger.warning("PDF content does not match");
		}

		return result;
	}

   /**
   * Save each page of the pdf as image
   *
   * @param file Absolute file path of the file
   * @param startPage Starting page number of the document
   * @return List list of image file names with absolute path
   * @throws java.io.IOException when file is not found.
   */
	public List<String> savePdfAsImage(String file, int startPage) throws IOException{
		return this.saveAsImage(file, startPage, -1);
	}

   /**
   * Save each page of the pdf as image
   *
   * @param file Absolute file path of the file
   * @param startPage Starting page number of the document
   * @param endPage Ending page number of the document
   * @return List list of image file names with absolute path
   * @throws java.io.IOException when file is not found.
   */
	public List<String> savePdfAsImage(String file, int startPage, int endPage) throws IOException{
		return this.saveAsImage(file, startPage, endPage);
	}

   /**
   * Save each page of the pdf as image
   *
   * @param file Absolute file path of the file
   * @return List list of image file names with absolute path
   * @throws java.io.IOException when file is not found.
   */
	public List<String> savePdfAsImage(String file) throws IOException{
		return this.saveAsImage(file, -1, -1);
	}

   /**
	* This method saves each page of the pdf as image
   */
	private List<String> saveAsImage(String file, int startPage, int endPage) throws IOException{

		logger.info("file : " + file + ", startPage: " + startPage + ", endPage: " + endPage);

		ArrayList<String> imgNames = new ArrayList<>();

		try {
			File sourceFile = new File(file);
			this.createImageDestinationDirectory(file);
			this.updateStartAndEndPages(file, startPage, endPage);

			String fileName = sourceFile.getName().replace(".pdf", "");

			PDDocument document = Loader.loadPDF(sourceFile);
			PDFRenderer pdfRenderer = new PDFRenderer(document);
			for (int iPage = this.startPage - 1; iPage < this.endPage; iPage++) {
				logger.fine("Page No : " + (iPage+1));
				String fname = this.imageDestinationPath + fileName + "_" + (iPage + 1) + ".png";
				BufferedImage image = pdfRenderer.renderImageWithDPI(iPage, 300, ImageType.RGB);
				ImageIOUtil.writeImage(image, fname , 300);
				imgNames.add(fname);
				logger.fine("PDf Page saved as image : " + fname);
			}
			document.close();
		}catch (Exception e) {
			e.printStackTrace();
		}
		return imgNames;
	}

   /**
   * Compare 2 pdf documents pixel by pixel for the content and format.
   *
   * @param file1 Absolute file path of the expected file
   * @param file2 Absolute file path of the actual file
   * @param startPage Starting page number of the document
   * @param endPage Ending page number of the document
   * @param highlightImageDifferences To highlight differences in the images
   * @param showAllDifferences To compare all the pages of the PDF (by default as soon as a mismatch is found in a page, this method exits)
   * @return boolean true if matches, false otherwise
   * @throws java.io.IOException when file is not found.
   */
	public double compare(String file1, String file2,int startPage, int endPage, boolean highlightImageDifferences, boolean showAllDifferences) throws IOException{
		this.compareMode = CompareMode.VISUAL_MODE;
		this.bHighlightPdfDifference = highlightImageDifferences;
		this.bCompareAllPages = showAllDifferences;
		return this.comparePdfByImage(file1, file2, startPage, endPage);
	}

   /**
   * This method reads each page of a given doc, converts to image
   * compare. If it fails, exits immediately.
   */
	private double comparePdfByImage(String file1, String file2, int startPage, int endPage) throws IOException{

		logger.info("file1 : " + file1);
		logger.info("file2 : " + file2);

		int pgCount1 = this.getPageCount(file1);
		int pgCount2 = this.getPageCount(file2);

		if(pgCount1!=pgCount2){
			logger.warning("files page counts do not match - returning false");
			return Double.NEGATIVE_INFINITY;
		}

		if(this.bHighlightPdfDifference)
			this.createImageDestinationDirectory(file2);

		this.updateStartAndEndPages(file1, startPage, endPage);

		return this.convertToImageAndCompare(file1, file2, this.startPage, this.endPage);
	}

	private double convertToImageAndCompare(String file1, String file2, int startPage, int endPage) throws IOException{

		double result = 0d;

		try (PDDocument doc1 = Loader.loadPDF(new File(file1));
			 PDDocument doc2 = Loader.loadPDF(new File(file2))) {

			PDFRenderer pdfRenderer1 = new PDFRenderer(doc1);
			PDFRenderer pdfRenderer2 = new PDFRenderer(doc2);


			for (int iPage = startPage - 1; iPage < endPage; iPage++) {
				String fileName = new File(file1).getName().replace(".pdf", "_") + (iPage + 1);
				fileName = this.getImageDestinationPath() + "/" + fileName + "_diff.png";

				logger.fine("Comparing Page No : " + (iPage + 1));
				BufferedImage image1 = pdfRenderer1.renderImageWithDPI(iPage, 300, ImageType.RGB);
				BufferedImage image2 = pdfRenderer2.renderImageWithDPI(iPage, 300, ImageType.RGB);
				double pageResult = ImageUtil.compareAndHighlight(image1, image2, fileName, this.bHighlightPdfDifference, this.imgColor.getRGB(), allowedRGBDeviation);
				result = Math.max(result, pageResult);
				if (!this.bCompareAllPages && result > allowedRGBDeviation) {
					break;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}



   /**
   * Extract all the embedded images from the pdf document
   *
   * @param file Absolute file path of the file
   * @param startPage Starting page number of the document
   * @return List list of image file names with absolute path
   * @throws java.io.IOException when file is not found.
   */
	public List<String> extractImages(String file, int startPage) throws IOException{
		return this.extractimages(file, startPage, -1);
	}

   /**
   * Extract all the embedded images from the pdf document
   *
   * @param file Absolute file path of the file
   * @param startPage Starting page number of the document
   * @param endPage Ending page number of the document
   * @return List list of image file names with absolute path
   * @throws java.io.IOException when file is not found.
   */
	public List<String> extractImages(String file, int startPage, int endPage) throws IOException{
		return this.extractimages(file, startPage, endPage);
	}

   /**
   * Extract all the embedded images from the pdf document
   *
   * @param file Absolute file path of the file
   * @return List list of image file names with absolute path
   * @throws java.io.IOException when file is not found.
   */
	public List<String> extractImages(String file) throws IOException{
		return this.extractimages(file, -1, -1);
	}

   /**
   * This method extracts all the embedded images of the pdf document
   */
	private List<String> extractimages(String file, int startPage, int endPage){

		logger.info("file : " + file + ", startPage: " + startPage + ", endPage: " + endPage);

		ArrayList<String> imgNames = new ArrayList<>();
		boolean bImageFound = false;
		try {

			this.createImageDestinationDirectory(file);
			String fileName = this.getFileName(file).replace(".pdf", "_resource");

			PDDocument document = Loader.loadPDF(new File(file));
			PDPageTree list = document.getPages();

			this.updateStartAndEndPages(file, startPage, endPage);

			int totalImages = 1;
			for(int iPage=this.startPage-1;iPage<this.endPage;iPage++){
				logger.fine("Page No : " + (iPage+1));
				PDResources pdResources = list.get(iPage).getResources();
				for (COSName c : pdResources.getXObjectNames()) {
		            PDXObject o = pdResources.getXObject(c);
		            if (o instanceof org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject object) {
		            	bImageFound = true;
		            	String fname = this.imageDestinationPath + "/" + fileName+ "_" + totalImages + ".png";
		                ImageIO.write(object.getImage(), "png", new File(fname));
		                imgNames.add(fname);
		                totalImages++;
		            }
		        }
			}
			document.close();
			if(bImageFound)
				logger.fine("Images are saved @ " + this.imageDestinationPath);
			else
				logger.fine("No images were found in the PDF");
		}catch (Exception e) {
			e.printStackTrace();
		}
		return imgNames;
	}

	private void createImageDestinationDirectory(String file) throws IOException{
		if(null==this.imageDestinationPath){
			File sourceFile = new File(file);
			String destinationDir = sourceFile.getParent() + "/temp/";
			this.imageDestinationPath=destinationDir;
			this.createFolder(destinationDir);
		}
	}

	private boolean createFolder(String dir) throws IOException{
	    FileUtils.deleteDirectory(new File(dir));
		return new File(dir).mkdir();
	}

	private String getFileName(String file){
		return new File(file).getName();
	}

	private void updateStartAndEndPages(String file, int start, int end) throws IOException{

		PDDocument document = Loader.loadPDF(new File(file));
		int pagecount = document.getNumberOfPages();
		logger.fine("Page Count : " + pagecount + ", Given start page: " + start + ", Given end page: " + end);

		if(start > 0 && start <= pagecount){
			this.startPage = start;
		}else{
			this.startPage = 1;
		}
		if(end > 0 && end >= start && end <= pagecount){
			this.endPage = end;
		}else{
			this.endPage = pagecount;
		}
		document.close();
		logger.fine("Updated start page:" + this.startPage);
		logger.fine("Updated end   page:" + this.endPage);
	}
}
