package org.example;

import org.apache.commons.imaging.ImageInfo;
import org.apache.commons.imaging.Imaging;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Scanner;

class ImageConverter {
    private static final double RESIZE_HEIGHT_OR_WIDTH = 9.03;
    private static final double LOGO_DPI = 266;
    private static final int MAX_ALLOWED_SIZE = 20 ;
    private static final double ONE_INCH_TO_CENTIMETER  = 2.54;
    private static String CONVERSION_FORMAT_TIFF = "tiff";

    public enum classType {
        BWLOGO,
        COLOURLOGO
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter the file name:");
        String fileName = scanner.nextLine();
        System.out.println("Enter classType BWLOGO or COLOURLOGO:");
        String classTypeStr = scanner.nextLine();
        System.out.println("Enter only one image format as jpg, png, bmp, gif, tiff:");
        CONVERSION_FORMAT_TIFF = scanner.nextLine();
        classType type = classType.valueOf(classTypeStr);

        File file = new File(fileName);
        String absolutePath = null;
        if (file.exists() && type != null) {
            absolutePath = file.getAbsolutePath();
            System.out.println("Absolute path of input file: " + absolutePath);
            System.out.println("Input classType: "+type.name());
        } else {
            System.out.println("File not found in the current directory.");
            return;
        }

        assert absolutePath != null;
        String outputFleName = getOutputFileName(absolutePath);

        try {
            // Read the input image
            BufferedImage inputImage = ImageIO.read(new File(absolutePath));
            int dpi = getImageDpi(absolutePath);
            if (dpi < 0) {
                throw new RuntimeException("Image DPI is missing in the metadata");
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
                saveImage(inputImage, outputFleName);
            }

        } catch (IOException e) {
            System.out.println("Error occurred while converting the image: " + e.getMessage());
        }
    }

    private static boolean isResizeNeeded(BufferedImage inputImage, int dpi) {
        int widthInPixels = inputImage.getWidth();
        int heightInPixels = inputImage.getHeight();
        double centimetersWidth = widthInPixels * ONE_INCH_TO_CENTIMETER / dpi;
        double centimetersHeight = heightInPixels * ONE_INCH_TO_CENTIMETER / dpi;
        System.out.println("Current width: " + centimetersWidth + " cm");
        System.out.println("Current height: " + centimetersHeight + " cm");
        return centimetersWidth > MAX_ALLOWED_SIZE || centimetersHeight > MAX_ALLOWED_SIZE;
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
        return inputFileName.replaceAll("\\.[^.]*$", "") + "."+CONVERSION_FORMAT_TIFF;
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
        int desiredWidth =  cmToPixels();
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

