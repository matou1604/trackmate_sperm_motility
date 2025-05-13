package ch.epfl.bio410;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.gui.WaitForUserDialog;
import net.imagej.ImageJ;
import org.scijava.command.Command;
import org.scijava.plugin.Plugin;

import java.awt.*;
import java.awt.event.TextEvent;
import java.awt.event.TextListener;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;

import java.io.File;

import org.apache.commons.csv.CSVRecord;
import javax.swing.*;

import fiji.plugin.trackmate.FeatureModel;
import fiji.plugin.trackmate.Model;
import ij.WindowManager;

// import tracking from local package
import ch.epfl.bio410.utils.utils;
import ch.epfl.bio410.utils.TrackingConfig;
import ch.epfl.bio410.tracking.Tracking;



@Plugin(type = Command.class, menuPath = "Plugins>FRT>Sperm motility")
public class SpermMotility implements Command {

	private String path = Paths.get(System.getProperty("user.home")).toString();
	private TrackingConfig config;
	private String[] fileList = new String[]{};
	// Colony assignment parameters (TODO: to remove)
	private final int colony_min_area = 50; // Colony assignment parameters, minimum colony area
	// Detection parameters
	private final double radius = 7; 	// Detection parameters, radius of the object in um
	private final double threshold = 0.357;  // Detection parameters, quality threshold
	private final boolean medianFilter = true; // Detection parameters, do median filter (GFP channel only, before detection in TrackMate)
	// Tracking parameters
	private final double maxLinkDistance = 5; // Tracking parameters, max linking distance between objects
	private final double maxGapDistance = 8; // Tracking parameters, max gap distance to close a track across frames
	private final int maxFrameGap = 3; // Tracking parameters, max frame gap allowed for tracking
	private final double durationFilter = 8; // Tracking parameters, duration filter (min duration of a track)
	// Config


	public void run() {

		GenericDialog dlg = new GenericDialog("Sperm motility");
		dlg.addDirectoryField("Path to the image", path);
		if (dlg.wasCanceled()) return;

		// Get all the parameters

		// Set the config if needed (use existing if set or no config available)
		this.config = new TrackingConfig(
				colony_min_area,
				radius,
				threshold,
				medianFilter,
				maxLinkDistance,
				maxGapDistance,
				maxFrameGap,
				durationFilter
		);
		//String path = "C:/Users/mathi/OneDrive/Documents/EPFL/PDM Harvard/Trackmate/data/";
		//String image = "C24-TDI-TP1-Motility-02.tiff";
		// show the image
		//String imagePath = Paths.get(path, image).toString();
		// Results
//		// Save the results to CSV
//		String imageNameWithoutExtension = image.substring(0, image.lastIndexOf('.'));

		// TRACKING
		Tracking tracker = new Tracking();
		tracker.setConfig(config);

		// get path
		String inputDir = IJ.getDirectory("Choose a folder containing .tiff files");
		if (inputDir == null) {
			IJ.log("No directory selected. Exiting.");
			return;
		}

		// Get the list of .tiff files in the directory
		File dir = new File(inputDir);
		FilenameFilter filter = (dir1, name) -> name.toLowerCase().endsWith(".tiff");
		fileList = dir.list(filter);
		if (fileList == null || fileList.length == 0) {
			IJ.log("No .tiff files found in the directory.");
			return;
		}


		// create "results" folder if it doesn't exist
		String resultsPath = Paths.get(inputDir, "results").toString();
		File resultsFolder = new File(resultsPath);

		if (!resultsFolder.exists()) {
			if (resultsFolder.mkdir()) {
				IJ.log("Directory is created!");
			} else {
				IJ.log("Failed to create directory!");
				throw new RuntimeException("Failed to create results directory. Aborting.");
			}
		}


		// start for loop processing and tracking each image in loop one at a time
		for (String fileName : fileList) {
			String imagePath = Paths.get(inputDir, fileName).toString();
			String imageNameWithoutExtension = fileName.substring(0, fileName.lastIndexOf('.'));

			IJ.log("Processing image: " + fileName);
			// Open the image
			ImagePlus imp = IJ.openImage(imagePath);
			imp.show();
			IJ.run(imp, "Subtract Background...", "stack rolling=50");
			IJ.run(imp, "Enhance Contrast", "saturated=0.35");

			// Run tracking on the image
			Model model = tracker.runTracking(imp);
			FeatureModel featureModel = model.getFeatureModel();
			IJ.run("Tile");
			//see https://imagej.net/plugins/trackmate/scripting/scripting#display-spot-edge-and-track-numerical-features-after-tracking for ways to get the features

			File csvSpotsPath = Paths.get(resultsPath, "spots_" + imageNameWithoutExtension + ".csv").toFile();
			File csvTracksPath = Paths.get(resultsPath, "tracks_" + imageNameWithoutExtension + ".csv").toFile();
			try {
				tracker.saveFeaturesToCSV(model, csvSpotsPath, csvTracksPath, imagePath);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}

			// Look at tiles
			new WaitForUserDialog("Image processing complete.\n", "The image " + fileName + " has been processed. \n" +
					"The results have been saved to " + resultsPath + ".\n" +
					"Press OK to continue to the next image.").show();

			// Close the image
			IJ.run("Close All");
			IJ.log("Finished processing image: " + imagePath);
		}
	}




	/**
	 * This main function serves for development purposes.
	 * It allows you to run the plugin immediately out of
	 * your integrated development environment (IDE).
	 *
	 * @param args whatever, it's ignored
	 * @throws Exception
	 */
	public static void main(final String... args) throws Exception {
		final ImageJ ij = new ImageJ();
		ij.ui().showUI();
	}
}
