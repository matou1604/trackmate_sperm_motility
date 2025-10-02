package ch.epfl.bio410;

import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettingsIO;
import fiji.plugin.trackmate.visualization.hyperstack.HyperStackDisplayer;
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.gui.WaitForUserDialog;
import net.imagej.ImageJ;
import org.scijava.command.Command;
import org.scijava.plugin.Plugin;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Paths;

import fiji.plugin.trackmate.FeatureModel;
import fiji.plugin.trackmate.Model;

// import tracking from local package
import ch.epfl.bio410.utils.utils;
import ch.epfl.bio410.utils.TrackingConfig;
import ch.epfl.bio410.tracking.Tracking;

import javax.imageio.ImageIO;


@Plugin(type = Command.class, menuPath = "Plugins>FRT>Sperm motility")
public class SpermMotility implements Command {

	private final String path = Paths.get(System.getProperty("user.home")).toString();
	private TrackingConfig config;
	private String[] fileList = new String[]{};
	// Detection parameters
	private final double radius = 3.5; 	// Detection parameters, radius of the object in um
	private final double threshold = 0.357;  // Detection parameters, quality threshold
	private final boolean medianFilter = true; // Detection parameters, do median filter (GFP channel only, before detection in TrackMate)
	// Tracking parameters
	private final double maxLinkDistance = 15; // Tracking parameters, max linking distance between objects
	private final double maxGapDistance = 15; // Tracking parameters, max gap distance to close a track across frames
	private final int maxFrameGap = 5; // Tracking parameters, max frame gap allowed for tracking
	private final double durationFilter = 0.3; // Tracking parameters, duration filter (min duration of a track)
	// Config
	private final double minMeanSpeed = 5; // Minimum mean speed of a track in um/s
	private final double minStraightSpeed = 5; // Minimum straight line speed of a track in um/s
	private final double minLinearity = 0.1; // Minimum linearity of a track

	public void run() {

		////////////////// User Interface /////////////////////

		GenericDialog dlg = new GenericDialog("Sperm motility");
		dlg.addMessage("Hey! Welcome to the sperm motility plugin!\n" +
				"Select the directory containing the tiff images to process.\n" +
				"You can also modify the tracking parameters. \n" +
				"This version of the plugin does not allow splitting or merging of tracks.");

		dlg.setInsets(20,0,0);
		dlg.addDirectoryField("Path to the image", path);

		dlg.setInsets(20,0,0);
		dlg.addMessage("Preprocessing:");
		dlg.addNumericField("Background subtraction (pxl)", 50, 0);

		dlg.setInsets(15,0,0);
		dlg.addMessage("Spot detection:");
		dlg.addNumericField("Detection radius (um)", radius, 2);
		dlg.addNumericField("Quality detection threshold", threshold, 3);

		dlg.setInsets(0,90,0);
		dlg.addCheckbox("Apply median filter", medianFilter);

		dlg.setInsets(15,0,0);
		dlg.addMessage("Tracking:");
		dlg.addNumericField("Max linking distance (µm)", maxLinkDistance, 2);
		dlg.addNumericField("Max gap closing distance (µm)", maxGapDistance, 2);
		dlg.addNumericField("Max frame gap", maxFrameGap, 0);
		dlg.addNumericField("Track duration filter (min)", durationFilter, 2);
		dlg.addNumericField("Minimum mean speed (um/s)", minMeanSpeed, 2);
		dlg.addNumericField("Minimum straight line speed (um/s)", minStraightSpeed, 2);
		dlg.addNumericField("Minimum linearity", minLinearity, 2);

//		dlg.setInsets(20,0,0);
//		dlg.addImage(IJ.openImage("C:\\Users\\mathi\\OneDrive\\Documents\\EPFL\\PDM Harvard\\Trackmate\\trackmate_sperm_motility\\src\\images\\color.png"));
//		IJ.run("Size...", "width=500 height=500 constrain");

		dlg.setInsets(20,10,0);
		dlg.addCheckbox("Stop between images", false);
		dlg.setInsets(0,10,0);

		dlg.addCheckbox("Skip already analysed images", false);
		dlg.showDialog();

		if (dlg.wasCanceled()) return;


		////////////////// Get user inputs /////////////////////

		String inputDir = dlg.getNextString();
		if (inputDir == null || inputDir.isEmpty()) {
			IJ.log("No directory selected. Exiting.");
			return;
		}
		// Get the list of .tiff files in the directory
		File dir = new File(inputDir);
		FilenameFilter filter = (dir1, name) -> name.toLowerCase().endsWith(".tiff");
		fileList = dir.list(filter);
		// If no .tiff files are found, exit
		if (fileList == null || fileList.length == 0) {
			IJ.log("No .tiff files found in the directory.");
			return;
		}

		// Get all the tracking parameters
		int subtractionRadius = (int) dlg.getNextNumber();
		double detectionRadius = dlg.getNextNumber();
		double detectionThreshold = dlg.getNextNumber();
		boolean applyMedianFilter = dlg.getNextBoolean();
		double linkingMaxDistance = dlg.getNextNumber();
		double gapClosingMaxDistance = dlg.getNextNumber();
		int frameGap = (int) dlg.getNextNumber();
		double trackDurationMin = dlg.getNextNumber();
		double minMeanSpeed = dlg.getNextNumber();
		double minStraightSpeed = dlg.getNextNumber();
		double minLinearity = dlg.getNextNumber();
		boolean stopBetweenImages = dlg.getNextBoolean();
		boolean skipAnalysedImages = dlg.getNextBoolean();


		// Set the config if needed (use existing if set or no config available)
		this.config = new TrackingConfig(
				subtractionRadius,
				detectionRadius,
				detectionThreshold,
				applyMedianFilter,
				linkingMaxDistance,
				gapClosingMaxDistance,
				frameGap,
				trackDurationMin,
				minMeanSpeed,
				minStraightSpeed,
				minLinearity
		);

		// TRACKING
		Tracking tracker = new Tracking();
		tracker.setConfig(config);


		// Prepare output directory
		// create "results" folder if it doesn't exist
		String resultsPath = Paths.get(inputDir, "results").toString();
		File resultsFolder = new File(resultsPath);

		if (!resultsFolder.exists()) {
			if (resultsFolder.mkdir()) {
				IJ.log("Results directory is created!");
			} else {
				IJ.log("Failed to create results directory!");
				throw new RuntimeException("Failed to create results directory. Aborting.");
			}
		}

		IJ.run("Overlay Options...", "stroke=none width=20 fill=none set"); //FIXME


		// start for loop processing and tracking each image in loop one at a time
		for (String fileName : fileList) {
			String imagePath = Paths.get(inputDir, fileName).toString();
			String imageNameWithoutExtension = fileName.substring(0, fileName.lastIndexOf('.'));

			IJ.log((java.util.Arrays.asList(fileList).indexOf(fileName) + 1) + "/" + fileList.length);

			if(skipAnalysedImages && utils.FileExists(inputDir, "tracks_" + imageNameWithoutExtension + ".csv")){
				IJ.log("Skipping already analysed image: " + fileName);
				continue;
			}

			IJ.log("Processing image: " + fileName);

			// Open the image
			ImagePlus imp = IJ.openImage(imagePath);
			imp.show();
			// get number of frames of image
			int nFrames = imp.getNFrames();
			IJ.run(imp, "Subtract Background...", "stack rolling="+subtractionRadius);
			IJ.run(imp, "Enhance Contrast", "saturated=0.35");
			IJ.run(imp, "Cyan", "");

			// Run tracking on the image
			Model model = tracker.runTracking(imp);
			FeatureModel featureModel = model.getFeatureModel();
			//IJ.run("Tile");
			//see https://imagej.net/plugins/trackmate/scripting/scripting#display-spot-edge-and-track-numerical-features-after-tracking for ways to get the features

			File csvSpotsPath = Paths.get(resultsPath, "spots_" + imageNameWithoutExtension + ".csv").toFile();
			File csvTracksPath = Paths.get(resultsPath, "tracks_" + imageNameWithoutExtension + ".csv").toFile();
			try {
				tracker.saveFeaturesToCSV(model, csvSpotsPath, csvTracksPath, imagePath);
				tracker.cleanTracksCSV(csvTracksPath);
				IJ.log("Results saved.");
			} catch (IOException e) {
				IJ.error("Error saving results", "Could not save results to CSV files.\n" +
						"The file being written is being used in another process.\n" +
						"Close it and restart.");
				throw new RuntimeException(e);
			}

//			// Save the image with the tracking result as avi with overlay
  			String outputPath = Paths.get(resultsPath, imageNameWithoutExtension + ".png").toString();
			IJ.log("Saving image with tracking result to: " + outputPath);

//			imp.setAntialiasRendering(false);
//			// make overlay on imp thicker
//			//imp.getOverlay().setStrokeWidth(20.0);
//			IJ.run(imp, "AVI... ", "compression=None frame=1 save=[" + outputPath + "]");
//			//IJ.saveAs(imp, "Tiff", "C:/Users/mathi/Downloads/mri-stack.tif");

			try {
				// Get the current Fiji window
				ImagePlus imp_tracked = WindowManager.getCurrentImage();
				if (imp_tracked == null) {
					System.out.println("No active Fiji window found.");
					return;
				}

				// Get the bounds of the Fiji window
				Rectangle windowBounds = imp_tracked.getWindow().getBounds();

				// Capture the screen area of the Fiji window
				Robot robot = new Robot();
				BufferedImage screenshot = robot.createScreenCapture(windowBounds);

				// Save the screenshot to the specified path
				File outputFile = new File(outputPath);
				ImageIO.write(screenshot, "png", outputFile);

				System.out.println("Screenshot saved to: " + outputPath);
			} catch (Exception e) {
				e.printStackTrace();
			}


			// Look at tiles
			if (stopBetweenImages) {
				new WaitForUserDialog("Tracking check.\n", "Check tracking results.").show();

				GenericDialog dialog = new GenericDialog("Tracking done");
				dialog.addMessage("Press OK to continue to the next image.");
				dialog.setCancelLabel("Cancel");
				dialog.setOKLabel("OK");
				dialog.showDialog();

				if (dialog.wasCanceled()) {
					IJ.log("User canceled the operation or closed the dialog. Exiting plugin.");
					return;
				}
			}

			// Close the image
			IJ.run("Close All");
			IJ.log("Finished processing image: " + fileName + "\n\n");
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
