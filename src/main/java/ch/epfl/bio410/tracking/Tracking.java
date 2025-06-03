package ch.epfl.bio410.tracking;

import ch.epfl.bio410.utils.utils;
import fiji.plugin.trackmate.visualization.PerTrackFeatureColorGenerator;
import ij.IJ;
import ij.ImagePlus;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import ch.epfl.bio410.utils.TrackingConfig;
import fiji.plugin.trackmate.*;
import fiji.plugin.trackmate.detection.DetectorKeys;
import fiji.plugin.trackmate.detection.LogDetectorFactory;
import fiji.plugin.trackmate.features.FeatureFilter;
import fiji.plugin.trackmate.features.track.TrackIndexAnalyzer;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettingsIO;
import fiji.plugin.trackmate.tracking.jaqaman.SparseLAPTrackerFactory;
import fiji.plugin.trackmate.visualization.table.TrackTableView;
import fiji.plugin.trackmate.visualization.hyperstack.HyperStackDisplayer;
import fiji.plugin.trackmate.FeatureModel;
import fiji.plugin.trackmate.Model;
import org.apache.commons.csv.CSVRecord;


public class Tracking {
    // Default config

    private TrackingConfig trackingConfig;
    private DisplaySettings displaySettings;

    public String trackingConfigName;
    public String trackingConfigPath;
    /** Returns the default configuration parameters. */
    public TrackingConfig loadDefaultConfig() {
        this.trackingConfig = new TrackingConfig();
        return this.trackingConfig;
    }
    public void setConfig(TrackingConfig trackingConfig) {
        this.trackingConfig = trackingConfig;
    }
    /**
     * Set the configuration parameters for tracking.
     * @param detector_radius Radius of the object in um
     * @param detector_threshold Quality threshold
     * @param detector_median_filter Median filter
     * @param tracker_linking_max_distance Max linking distance between objects
     * @param tracker_gap_closing_max_distance Max gap distance to close a track across frames
     * @param tracker_max_frame_gap Max frame gap allowed for tracking
     * @param track_duration_min Duration filter (min duration of a track)
     * @return
     */
    public TrackingConfig setConfig(
            double detector_radius,
            double detector_threshold,
            boolean detector_median_filter,
            double tracker_linking_max_distance,
            double tracker_gap_closing_max_distance,
            int tracker_max_frame_gap,
            double track_duration_min,
            double min_mean_speed
    ) {
        this.trackingConfig = new TrackingConfig(
                detector_radius,
                detector_threshold,
                detector_median_filter,
                tracker_linking_max_distance,
                tracker_gap_closing_max_distance,
                tracker_max_frame_gap,
                track_duration_min,
                min_mean_speed
        );
        return this.trackingConfig;
    }

    /**
     * Creates a TrackMate tracker from the specified configuration parameters.
     * @return TrackMate model object.
     */
    public Model runTracking(ImagePlus imp) {
        IJ.log("------------------ TRACKMATE ------------------");
        this.trackingConfig.printTrackingConfig(); // show parameters
        IJ.log("Tracking started");
        // if config is not set, use default config
        if (this.trackingConfig == null) {
            this.loadDefaultConfig();
        }
        // Instantiate model object and logger
        Model model = new Model();
        model.setLogger(Logger.IJ_LOGGER);
        // Prepare settings object
        Settings settings = new Settings(imp);


        // Configure detector
        settings.detectorFactory = new LogDetectorFactory();
        settings.detectorSettings.put(DetectorKeys.KEY_DO_SUBPIXEL_LOCALIZATION, true);
        settings.detectorSettings.put(DetectorKeys.KEY_RADIUS, this.trackingConfig.detector_radius);
        settings.detectorSettings.put(DetectorKeys.KEY_TARGET_CHANNEL, 1);
        settings.detectorSettings.put(DetectorKeys.KEY_THRESHOLD, this.trackingConfig.detector_threshold);
        settings.detectorSettings.put(DetectorKeys.KEY_DO_MEDIAN_FILTERING, this.trackingConfig.detector_median_filter);

        // Filter results of detection
        FeatureFilter detect_filter_quality = new FeatureFilter("QUALITY", this.trackingConfig.detector_threshold, true); //changed from 30 to 0!!         //settings.initialSpotFilterValue = 0.0;
        settings.addSpotFilter(detect_filter_quality); //TODO: should this be 0 or 0.32 as set by user???

        // Configure tracker
        settings.trackerFactory = new SparseLAPTrackerFactory();
        settings.trackerSettings = settings.trackerFactory.getDefaultSettings();
        settings.trackerSettings.put("LINKING_MAX_DISTANCE", this.trackingConfig.tracker_linking_max_distance);
        settings.trackerSettings.put("GAP_CLOSING_MAX_DISTANCE", this.trackingConfig.tracker_gap_closing_max_distance);
        settings.trackerSettings.put("MAX_FRAME_GAP", this.trackingConfig.tracker_max_frame_gap);
        // Prevent track splitting and merging
        settings.trackerSettings.put("ALLOW_TRACK_SPLITTING", false);
        settings.trackerSettings.put("ALLOW_TRACK_MERGING", false);


        // Add the analyzers for all features
        settings.addAllAnalyzers();


        // Configure track filter
        FeatureFilter track_duration_filter = new FeatureFilter(
                "TRACK_DURATION",
                this.trackingConfig.track_duration_min,
                true);
        settings.addTrackFilter(track_duration_filter);
        //FeatureFilter detect_filter_speed = new FeatureFilter(
               // "TRACK_MEAN_SPEED",
               // this.trackingConfig.min_mean_speed,
               // true);
        //settings.addTrackFilter(detect_filter_speed);

        // Instantiate and run trackmate
        TrackMate trackmate = new TrackMate(model, settings);
        boolean ok = trackmate.checkInput();
        if (!ok) {
            System.out.println(trackmate.getErrorMessage());
            return null;
        }

        ok = trackmate.process();
        if (!ok) {
            System.out.println(trackmate.getErrorMessage());
            return null;
        }

        // Display the results on top of the image
        SelectionModel selectionModel = new SelectionModel(model);
        DisplaySettings displaySettings = DisplaySettingsIO.readUserDefault();
        this.displaySettings = displaySettings;
        // Color tracks and spots by ID
        displaySettings.setTrackColorBy(DisplaySettings.TrackMateObject.TRACKS, TrackIndexAnalyzer.TRACK_INDEX);
        displaySettings.setSpotColorBy(DisplaySettings.TrackMateObject.TRACKS, TrackIndexAnalyzer.TRACK_INDEX);
        // Use metric to color tracks
//        displaySettings.setTrackColorBy(DisplaySettings.TrackMateObject.TRACKS, "TRACK_MEAN_SPEED");
//        displaySettings.setSpotColorBy(DisplaySettings.TrackMateObject.TRACKS, "TRACK_MEAN_SPEED");
        //PerTrackFeatureColorGenerator trackColor = PerTrackFeatureColorGenerator(model, "TRACK_DURATION");
        HyperStackDisplayer displayer = new HyperStackDisplayer(model, selectionModel, imp, displaySettings);
        displayer.render();
        displayer.refresh();

        // Echo results with the logger we set at start:
        model.getLogger().log(model.toString());
        IJ.log("------------------ TRACKMATE FINISHED ------------------\n");
        return model;
    }
    /**
     * Save the features of the tracks to CSV files.
     * @param model TrackMate model object
     * @param csvFileTracks File to save the tracks features
     * @throws IOException
     */
    public void saveFeaturesToCSV(Model model, File csvFileTracks, String imagePath) throws IOException { // removed File csvFileSpots,
        // Create a selection model for the TrackMate model
        SelectionModel sm = new SelectionModel(model);
        DisplaySettings ds = this.displaySettings;
        // if display settings are not set, throw exception as tracking must be run first
        if (ds == null) {
            throw new IOException("Display settings not set. Please run tracking first.");
        }

        // Create tables for tracks
        TrackTableView trackTableView = new TrackTableView(model, sm, ds, imagePath);

        // Export the tables to CSV files
        //trackTableView.getSpotTable().exportToCsv(csvFileSpots);
        trackTableView.getTrackTable().exportToCsv(csvFileTracks);

        // Save all spots table (includes all spots, even those not in tracks)
        // AllSpotsTableView spotsTableView = AllSpotsTableView(model, sm, ds);
        // spotsTableView.exportToCsv(csvFileAllSpots.getAbsolutePath());
    }

    /**
     * This function opens the tracks csv just saved and removes certain columns
     * that are not needed for the analysis.
     * It creates a new CSV file with the cleaned tracks.
     * This is useful to reduce the size of the CSV file and
     * to remove unnecessary columns that are not used in the analysis.
     * @param csvFileTracks CSV file with the tracks to clean
     */

    public void cleanTracksCSV(File csvFileTracks) throws IOException {
        // Read the CSV file
        List<CSVRecord> records = utils.readCsv(csvFileTracks, 0);
        // Create a new CSV file to save the cleaned tracks
        File cleanedCsvFile = new File(csvFileTracks.getParent(), csvFileTracks.getName());
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(cleanedCsvFile))) {
            // Write the header
            writer.write("LABEL,TRACK_ID,NUMBER_SPOTS,NUMBER_GAPS,TRACK_DURATION,TRACK_DISPLACEMENT,TRACK_MEAN_SPEED,TOTAL_DISTANCE_TRAVELED,MEAN_STRAIGHT_LINE_SPEED,LINEARITY_OF_FORWARD_PROGRESSION, MOTILE\n");
            // Write the cleaned records
            for (CSVRecord record : records) {
                String label = record.get("LABEL");
                String trackId = record.get("TRACK_ID");
                String numberSpots = record.get("NUMBER_SPOTS");
                String numberGaps = record.get("NUMBER_GAPS");
                String trackDuration = record.get("TRACK_DURATION");
                String trackDisplacement = record.get("TRACK_DISPLACEMENT");
                String trackMeanSpeed = record.get("TRACK_MEAN_SPEED");
                String motile = ""; // add a column called "motile" with value 1 if trackMeanSpeed > 5 µm/s, else 0
                if (utils.isNumeric(record.get("TRACK_MEAN_SPEED")) && Double.parseDouble(record.get("TRACK_MEAN_SPEED")) > this.trackingConfig.min_mean_speed) {
                    motile = "1";
                } else if(utils.isNumeric(record.get("TRACK_MEAN_SPEED")) && Double.parseDouble(record.get("TRACK_MEAN_SPEED")) <= this.trackingConfig.min_mean_speed) {
                    motile = "0";
                }
                String totalDistanceTraveled = record.get("TOTAL_DISTANCE_TRAVELED");
                String meanStraightLineSpeed = record.get("MEAN_STRAIGHT_LINE_SPEED");
                String linearityOfForwardProgression = record.get("LINEARITY_OF_FORWARD_PROGRESSION");
                writer.write(label + "," + trackId + "," + numberSpots + "," + numberGaps + "," + trackDuration + "," + trackDisplacement + "," + trackMeanSpeed + "," + totalDistanceTraveled + "," + meanStraightLineSpeed + "," + linearityOfForwardProgression + "," + motile + "\n");
            }
        }
    }
}