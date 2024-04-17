package org.example;

import org.apache.commons.imaging.ImageInfo;
import org.apache.commons.imaging.Imaging;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;

public class ImageConverter {
    private static final double RESIZE_HEIGHT_OR_WIDTH = 9;
    private static final double LOGO_DPI = 266.8;
    private static final int REQUIRED_MAX_SIZE = 20 ;
    private static final double ONE_INCH_TO_CENTIMETER  = 2.54;
    private static final String CONVERSION_FORMAT_TIFF = "TIFF";

    public enum classType {
        BWLOGO,
        COLOURLOGO
    }

    public static void main(String[] args) {
        classType type = classType.BWLOGO;
        String fileName = "png4.png";
        String filePath = getInputFilePath(fileName);
        String outputFleName = getOutputFileName(filePath);

        try {
            // Read the input image
            BufferedImage inputImage = ImageIO.read(new File(filePath));
            int dpi = getImageDpi(filePath);
            if (dpi < 0) {
                throw new RuntimeException("Error occurred while converting the image");
            }
            if (isResizeNeeded(inputImage, dpi)) {
                BufferedImage outputImage = convertImage(inputImage, type);
                if (outputImage == null) {
                    throw new RuntimeException("Error occurred while converting the image");
                }
                saveImage(outputImage, outputFleName);
                System.out.println("Image converted successfully.");
            } else {
                System.out.println("No resize is necessary");
            }

        } catch (IOException e) {
            System.out.println("Error occurred while converting the image: " + e.getMessage());
        }
    }

    private static boolean isResizeNeeded(BufferedImage inputImage, int dpi) {
        double widthCm = pixelsToCm(dpi, inputImage.getWidth());
        double heightCm = pixelsToCm(dpi, inputImage.getHeight());
        return widthCm > REQUIRED_MAX_SIZE || heightCm > REQUIRED_MAX_SIZE;
    }

    private static void saveImage(BufferedImage image, String outputFileName) throws IOException {
        File outputFile = new File(outputFileName);
        ImageIO.write(image, CONVERSION_FORMAT_TIFF, outputFile);
        System.out.println("Converted output file: "+outputFileName);
    }

    private static String getInputFilePath(String fileName){
        URL resourceUrl = ImageConverter.class.getClassLoader().getResource(fileName);
        String filePath = null;
        if (resourceUrl != null) {
            // Get the file path from the URL
            filePath = resourceUrl.getPath();
            System.out.println("Input file path: " + filePath);
        } else {
            System.out.println("File not found: " + fileName);
        }
        return filePath;
    }
    private static String getOutputFileName(String filePath){
        int lastIndexOfSlash = filePath.lastIndexOf('/');
        String inputFileName = filePath.substring(lastIndexOfSlash + 1);
        return inputFileName.replaceAll("\\.[^.]*$", "") + ".tiff";
    }

    private static int getImageDpi(String filePath){
        ImageInfo imageInfo;
        try {
            imageInfo = Imaging.getImageInfo(new File(filePath));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (imageInfo.getPhysicalHeightDpi() > 0 && imageInfo.getPhysicalWidthDpi() > 0) {
            return imageInfo.getPhysicalWidthDpi();
        } else {
            return -1;
        }
    }

    private static double pixelsToCm(int dpi, int heightOrWidth) {
        try {
            return (double) heightOrWidth / dpi * ONE_INCH_TO_CENTIMETER;
        }catch (ArithmeticException e){
            return -1.0;
        }
    }

    private static BufferedImage convertImage(BufferedImage inputImage, classType type) {
        int desiredWidth = cmToPixels();
        int desiredHeight = cmToPixels();

        if (type == classType.COLOURLOGO) {
            BufferedImage outputImage = new BufferedImage(desiredWidth, desiredHeight, BufferedImage.TYPE_INT_RGB);
            Image resizedImage = inputImage.getScaledInstance(desiredWidth, desiredHeight, Image.SCALE_SMOOTH);
            Graphics2D g2d = outputImage.createGraphics();
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.drawImage(resizedImage, 0, 0, desiredWidth, desiredHeight, null);
            g2d.dispose();
            return outputImage;
        } else if (type == classType.BWLOGO) {
            BufferedImage outputImage = new BufferedImage(desiredWidth, desiredHeight, BufferedImage.TYPE_BYTE_GRAY);
            Image resizedImage = inputImage.getScaledInstance(desiredWidth, desiredHeight, Image.SCALE_SMOOTH);
            Graphics2D g2d = outputImage.createGraphics();
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.drawImage(resizedImage, 0, 0, desiredWidth, desiredHeight, null);
            g2d.dispose();
            return outputImage;
        } else {
            return null;
        }
    }
    private static int cmToPixels() {
        return (int) (RESIZE_HEIGHT_OR_WIDTH * LOGO_DPI / ONE_INCH_TO_CENTIMETER);
    }
}

