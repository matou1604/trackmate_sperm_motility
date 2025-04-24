package ch.epfl.bio410;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
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
	// Colony assignment parameters
	private final int colony_min_area = 50; // Colony assignment parameters, minimum colony area
	// Detection parameters
	private final double radius = 0.31; 	// Detection parameters, radius of the object in um
	private final double threshold = 80.0;  // Detection parameters, quality threshold
	private final boolean medianFilter = true; // Detection parameters, do median filter (GFP channel only, before detection in TrackMate)
	// Tracking parameters
	private final double maxLinkDistance = 1.0; // Tracking parameters, max linking distance between objects
	private final double maxGapDistance = 1.0; // Tracking parameters, max gap distance to close a track across frames
	private final int maxFrameGap = 4; // Tracking parameters, max frame gap allowed for tracking
	private final double durationFilter = 8.0; // Tracking parameters, duration filter (min duration of a track)
	// Config


	public void run() {

		GenericDialog dlg = new GenericDialog("Replisome Analysis");
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
		String path = "C:/Users/mathi/Downloads";
		String image = "C24-TDI-TP1-Motility-02.czi";
		// show the image
		String imagePath = Paths.get(path, image).toString();
		// Results
//		// Save the results to CSV
//		String imageNameWithoutExtension = image.substring(0, image.lastIndexOf('.'));
//		// create "results" folder if it doesn't exist
//		String resultsPath = Paths.get(path, "results").toString();
//		File resultsFolder = new File(resultsPath);

		ImagePlus imp = IJ.openImage(imagePath);
		imp.show();


		// check that image only has one channel
		ImagePlus imageGFP = WindowManager.getImage("C2-" + imp.getTitle());
		// show the results
		imageGFP.show();
		// Tile
		IJ.run("Tile");


		// TRACKING
		Tracking tracker = new Tracking();
		tracker.setConfig(config);
		// Note : model and config are exposed for later if needed
		Model model = tracker.runTracking(imageGFP);
		FeatureModel featureModel = model.getFeatureModel();
		// see https://imagej.net/plugins/trackmate/scripting/scripting#display-spot-edge-and-track-numerical-features-after-tracking for ways to get the features

//		if (!resultsFolder.exists()) {
//			if (resultsFolder.mkdir()) {
//				IJ.log("Directory is created!");
//			} else {
//				IJ.log("Failed to create directory!");
//				throw new RuntimeException("Failed to create results directory. Aborting.");
//			}
//		}
//		File csvSpotsPath = Paths.get(resultsPath, "spots_" + imageNameWithoutExtension + ".csv").toFile();
//		File csvTracksPath = Paths.get(resultsPath, "tracks_" + imageNameWithoutExtension + ".csv").toFile();
//		try {
//			tracker.saveFeaturesToCSV(model, csvSpotsPath, csvTracksPath, imagePath);
//		} catch (IOException e) {
//			throw new RuntimeException(e);
//		}
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
