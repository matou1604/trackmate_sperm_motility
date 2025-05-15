package ch.epfl.bio410.utils;

import ij.IJ;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class TrackingConfig {
    public double detector_radius;
    public double detector_threshold;
    public boolean detector_median_filter;
    public double tracker_linking_max_distance;
    public double tracker_gap_closing_max_distance;
    public int tracker_max_frame_gap;
    public double track_duration_min;

    public String configPath = null;
    public String configName = null;

    // Default constructor
    /**
     * Default constructor for TrackingConfig.
     * Parameters are set to default values :
     * - detector_radius = 0.31
     * - detector_threshold = 30.0
     * - detector_median_filter = true
     * - tracker_linking_max_distance = 1.0
     * - tracker_gap_closing_max_distance = 1.0
     * - tracker_max_frame_gap = 4
     * - track_duration_min = 8.0
     */
    public TrackingConfig() {
        this.detector_radius = 0.31d;
        this.detector_threshold = 30.0d;
        this.detector_median_filter = true;
        this.tracker_linking_max_distance = 1.0d;
        this.tracker_gap_closing_max_distance = 1.0d;
        this.tracker_max_frame_gap = 4;
        this.track_duration_min = 8.0d;
    }
    /**
     * Constructor for TrackingConfig.
     * @param detector_radius Radius of the object in um.
     * @param detector_threshold Quality threshold.
     * @param detector_median_filter Median filter.
     * @param tracker_linking_max_distance Max linking distance between objects.
     * @param tracker_gap_closing_max_distance Max gap distance to close a track across frames.
     * @param tracker_max_frame_gap Max frame gap allowed for tracking.
     * @param track_duration_min Duration filter (min duration of a track).
     */
    public TrackingConfig(
            double detector_radius,
            double detector_threshold,
            boolean detector_median_filter,
            double tracker_linking_max_distance,
            double tracker_gap_closing_max_distance,
            int tracker_max_frame_gap,
            double track_duration_min
    ) {
        this.detector_radius = detector_radius;
        this.detector_threshold = detector_threshold;
        this.detector_median_filter = detector_median_filter;
        this.tracker_linking_max_distance = tracker_linking_max_distance;
        this.tracker_gap_closing_max_distance = tracker_gap_closing_max_distance;
        this.tracker_max_frame_gap = tracker_max_frame_gap;
        this.track_duration_min = track_duration_min;
    }
    /**
     * Create a TrackingConfig object from a properties file.
     * @param filename Name of the file to load from resources.
     * @return TrackingConfig object with the loaded parameters.
     */
    public static TrackingConfig createFromPropertiesFile(String filename) {
        String config_path = accessConfigPathFromResources(filename);
        TrackingConfig config = new TrackingConfig(0, 0, false, 0, 0, 0, 0);
        if (config_path != null) {
            config.loadFromPropertiesFile(config_path);
        }
        config.configName = filename;
        config.configPath = config_path;
        return config;
    }
    /**
     * Create a TrackingConfig object from a properties file.
     * @param filename File to load from.
     * @return TrackingConfig object with the loaded parameters.
     */
    public static TrackingConfig createFromPropertiesFile(File filename) {
        TrackingConfig config = new TrackingConfig(0, 0, false, 0, 0, 0, 0);
        if (filename != null) {
            config.loadFromPropertiesFile(filename.getAbsolutePath());
        }
        config.configName = filename.getName();
        config.configPath = filename.getAbsolutePath();
        return config;
    }
    /** Backup function to be used if loading directly from resources fails.
     * This instead copies the file to the user's Downloads folder for loading.
     * See ResourcesFolder.java for the implementation of copyFileFromResources.
     */
    public static String copyFromResources(String pathInResources) {
        try {
            return ResourcesFolder.copyFileFromResources(pathInResources).getAbsolutePath();
        } catch (NullPointerException e) {
            return null;
        }
    }

    /**
     * Print the tracking configuration parameters.
     */
    public void printTrackingConfig() {
        printFullConfig(true);
    }
    /**
     * Print the full configuration parameters.
     */
    public void printFullConfig() {
        printFullConfig(true);
    }
    /**
     * Print the colony configuration parameters.
     */
    public void printColonyConfig() {
        printFullConfig(false);
    }

    /**
     * Print the tracking configuration parameters.
     * @param showTrackingParams Show tracking parameters.
     */
    private void printFullConfig(boolean showTrackingParams) {
        IJ.log("----- Config :");
        if (this.configPath != null) {
            IJ.log("Config loaded from " + this.configName);
        }
        if (showTrackingParams) {
            IJ.log("- Detector radius : " + this.detector_radius + "um");
            IJ.log("- Detector quality threshold : " + this.detector_threshold);
            IJ.log("- Detector using median filter : " + this.detector_median_filter);
            IJ.log("- Tracker max distance for linking : " + this.tracker_linking_max_distance + "um");
            IJ.log("- Tracker gap closing max distance : " + this.tracker_gap_closing_max_distance + "um");
            IJ.log("- Tracker max frame gap for closing : " + this.tracker_max_frame_gap);
            IJ.log("- Track minimum duration filter : " + this.track_duration_min + "frames");
        }
        IJ.log("----- End of config");
    }

    /**
     * Access a file from the resources folder.
     * @param filename Name of the config file to load from resources.
     * @return Path to the file.
     */
    private static String accessConfigPathFromResources(String filename) {
        try {
            String path = "configs/" + filename; // system file separator is not applicable here
            URL resourceUrl = TrackingConfig.class.getClassLoader().getResource(path);
            if (resourceUrl != null) {
                String real_path = resourceUrl.getPath();
                // if the path starts with a '/', remove it
                if (real_path.charAt(0) == '/') {
                    real_path = real_path.substring(1);
                }
                return real_path;
            } else {
                System.out.println("Resource not found: " + path);
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    public static List<String> listAvailableConfigs() {
        try {
            List<String> configPaths = new ArrayList<>();
            List<String> filePaths = utils.listFilesInResourceFolder("configs");
            System.out.println(filePaths);
            if (filePaths == null) {
                return null;
            }
            for (String filepath : filePaths) {
                File file = new File(filepath);
                if (file.isFile() && file.getName().endsWith(".properties")) {
                    configPaths.add(file.getName());
                }
            }
            return configPaths;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    private void loadFromPropertiesFile(String filePath) {
        try {
            Properties properties = new Properties();
            properties.load(Files.newInputStream(Paths.get(filePath)));

            this.detector_radius = Double.parseDouble(properties.getProperty("DETECTOR_RADIUS"));
            this.detector_threshold = Double.parseDouble(properties.getProperty("DETECTOR_THRESHOLD"));
            this.detector_median_filter = Boolean.parseBoolean(properties.getProperty("DETECTOR_MEDIAN_FILTER"));
            this.tracker_linking_max_distance = Double.parseDouble(properties.getProperty("TRACKER_LINKING_MAX_DISTANCE"));
            this.tracker_gap_closing_max_distance = Double.parseDouble(properties.getProperty("TRACKER_GAP_CLOSING_MAX_DISTANCE"));
            this.tracker_max_frame_gap = Integer.parseInt(properties.getProperty("TRACKER_MAX_FRAME_GAP"));
            this.track_duration_min = Double.parseDouble(properties.getProperty("TRACK_DURATION_MIN"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
