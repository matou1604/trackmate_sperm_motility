package ch.epfl.bio410.utils;

import ij.ImagePlus;
import ij.plugin.GaussianBlur3D;
import ij.plugin.ImageCalculator;
import ij.IJ;
import ij.process.ImageProcessor;
import ij.process.LUT;
import ij.plugin.LutLoader;

import java.awt.*;
import java.awt.image.IndexColorModel;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import java.io.File;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * This class implements utils functions
 */


public class utils {

    public static LUT getGlasbeyLUT() {
        InputStream lutStream = utils.class.getClassLoader().getResourceAsStream("glasbey.lut");
        IndexColorModel glasbeyLut = null;
        try {
            glasbeyLut = new LutLoader().open(lutStream);
        } catch (IOException e) {
            IJ.log("Failed to load Glasbey LUT");
            e.printStackTrace();
        }
        byte[] r = new byte[256];
        byte[] g = new byte[256];
        byte[] b = new byte[256];
        glasbeyLut.getBlues(b);
        glasbeyLut.getGreens(g);
        glasbeyLut.getReds(r);

        // Set first color to black
        r[0] = (byte) Color.BLACK.getRed();
        g[0] = (byte) Color.BLACK.getGreen();
        b[0] = (byte) Color.BLACK.getBlue();

        return new LUT(r, g, b);
    }

    /**
     * This method removes noise from the image by applying a median filter
     * @param imp contains the pixel data of the image and some basic methods to manipulate it.
     * @return the processed image as an ImagePlus
     */
    public static ImagePlus remove_noise(ImagePlus imp) {
        //imp.show();
        /* Subtract Background but segmentation actually works best without subtracting background
            - Background subtraction plugin causes halo in background
            - Manual BS (using image calculator) causes for black background to be same color as bacteria
        Integer rolling = 100;
        IJ.run(imp, "Subtract Background...", "rolling="+rolling+" light sliding stack"); */
        // Despeckle
        IJ.run(imp, "Median...", "radius=2 stack");
        //IJ.run(imp,"Despeckle", "stack"); same but with radius 1

        return imp; //dog(imp, sigma); It seems that dog isn't useful either
    }


    /**
     * This method performs a difference of Gaussian, removing high-frequency spatial detail (random noise).
     * Here enhancing edges allows for spot detection later in the detect() function.
     * Dog works with subtraction a blurred version of the image to a less blurry one (different sigma)
     *
     * @param imp contains the pixel data of the image and some basic methods to manipulate it.
     * @param sigma is the standard deviation of the gaussian we want to apply to the image to blur it
     * @return the processed image as an ImagePlus
     */
    public static ImagePlus dog(ImagePlus imp, double sigma) {
        ImagePlus g1 = imp.duplicate(); // Duplicate the input image
        ImagePlus g2 = g1.duplicate(); // Duplicate the image again
        double sigma2 = Math.sqrt(2) * sigma;
        GaussianBlur3D.blur(g1, sigma, sigma, 0);
        GaussianBlur3D.blur(g2, sigma2, sigma2, 0);
        ImagePlus dog = ImageCalculator.run(g1, g2, "Subtract create stack");
        return dog;

    }

    /**
     * This method lists all files in a folder in the resources folder.
     *
     * @param folder The folder to list files from
     * @return A list of file paths
     */
    public static java.util.List<String> listFilesInResourceFolder(String folder) {
        URL url = TrackingConfig.class.getClassLoader().getResource(folder);
        System.out.println(url);
        if (url == null) {
            return null;
        }

        List<String> files = new ArrayList<>();
        try {
            URI uri = url.toURI();
            if (uri.getScheme().equals("file")) {
                // Running from IDE - read files directly from the filesystem
                File[] fileList = new File(uri).listFiles();
                if (fileList != null) {
                    for (File file : fileList) {
                        if (file.isFile()) {
                            files.add(file.getAbsolutePath());
                        }
                    }
                }
            } else if (uri.getScheme().equals("jar")) {
                // Running from JAR - read entries in the JAR file
                String[] parts = uri.toString().split("!");
                // if jar name contains -sources, remove it
                if (parts[0].contains("-sources")) {
                    parts[0] = parts[0].replace("-sources", "");
                }
                try (JarFile jarFile = new JarFile(parts[0].substring(5))) {
                    Enumeration<JarEntry> entries = jarFile.entries();
                    while (entries.hasMoreElements()) {
                        String name = entries.nextElement().getName();
                        if (name.startsWith(folder + "/") && !name.equals(folder + "/")) {
                            files.add(name);
                        }
                    }
                }
            }
        } catch (URISyntaxException e) {
            e.printStackTrace();
        } catch (IOException e) {
            return null;
        }
        return files;
    }

    /**
     * Reads a CSV file and returns a list of CSV records (columns).
     * @param csvFilePath Path to the CSV file
     * @return List of CSV records
     * @throws IOException If an error occurs while reading the file
     */
    public static List<CSVRecord> readCsv(String csvFilePath, int skipped_lines) throws IOException {
        return readCsv(new File(csvFilePath), skipped_lines);
    }

    /**
     * Reads a CSV file and returns a list of CSV records (columns).
     * @param csvFile CSV file to read
     * @return List of CSV records
     * @throws IOException If an error occurs while reading the file
     */
    public static List<CSVRecord> readCsv(File csvFile, int skipped_lines) throws IOException {
        try (FileReader reader = new FileReader(csvFile);
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader())) {
            // Skip the first n-1 lines (header) and collect the remaining records
            return csvParser.getRecords().stream().skip(skipped_lines).collect(Collectors.toList());
        }
    }

    /**
     * This method adds the pixel size for original image (width and height) to the image properties
     * @param colonyLabels is the image to which we want to add the pixel size
     * @param imageDIC is the image from which we want to get the pixel size
     */
    public static void add_pixel_size(ImagePlus colonyLabels, ImagePlus imageDIC){
        int[] dimensions = colonyLabels.getDimensions();
        int channels = dimensions[2];
        int slices = dimensions[3];
        int frames = dimensions[4];
        colonyLabels.getCalibration().setXUnit("µm");
        double pixelWidth = imageDIC.getCalibration().pixelWidth;
        double pixelHeight = imageDIC.getCalibration().pixelHeight;
        IJ.run(colonyLabels, "Properties...", "channels="+channels+" slices="+slices+" frames="+frames+" pixel_width="+pixelWidth+" pixel_height="+pixelHeight+" voxel_depth=1.0");
    }

    public static boolean FileExists(String path, String fileName){
        return new File (Paths.get(path, "results", fileName).toString()).exists();
    }


}