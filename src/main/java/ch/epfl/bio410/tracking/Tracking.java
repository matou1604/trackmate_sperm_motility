package ch.epfl.bio410.tracking;

import ij.IJ;
import ij.ImagePlus;
import java.io.File;
import java.io.IOException;
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
            int colony_min_area,
            double detector_radius,
            double detector_threshold,
            boolean detector_median_filter,
            double tracker_linking_max_distance,
            double tracker_gap_closing_max_distance,
            int tracker_max_frame_gap,
            double track_duration_min
    ) {
        this.trackingConfig = new TrackingConfig(
                colony_min_area,
                detector_radius,
                detector_threshold,
                detector_median_filter,
                tracker_linking_max_distance,
                tracker_gap_closing_max_distance,
                tracker_max_frame_gap,
                track_duration_min
        );
        return this.trackingConfig;
    }

    /**
     * Creates a TrackMate tracker from the specified configuration parameters, in order to track replisomes in the GFP channel.
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
        FeatureFilter detect_filter_quality = new FeatureFilter("QUALITY", 30, true);
        settings.addSpotFilter(detect_filter_quality);

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
//        displaySettings.setTrackColorBy(DisplaySettings.TrackMateObject.TRACKS, "TRACK_DURATION");
//        displaySettings.setSpotColorBy(DisplaySettings.TrackMateObject.SPOTS, "SPOT_QUALITY");
//        PerTrackFeatureColorGenerator trackColor = PerTrackFeatureColorGenerator(model, "TRACK_DURATION");
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
     * @param csvFileSpots File to save the spots features
     * @param csvFileTracks File to save the tracks features
     * @throws IOException
     */
    public void saveFeaturesToCSV(Model model, File csvFileSpots, File csvFileTracks, String imagePath) throws IOException {
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
        trackTableView.getSpotTable().exportToCsv(csvFileSpots);
        trackTableView.getTrackTable().exportToCsv(csvFileTracks);

        // Save all spots table (includes all spots, even those not in tracks)
        // AllSpotsTableView spotsTableView = AllSpotsTableView(model, sm, ds);
        // spotsTableView.exportToCsv(csvFileAllSpots.getAbsolutePath());
    }
}