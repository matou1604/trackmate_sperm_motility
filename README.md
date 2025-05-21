# Sperm Motility

This **ImageJ plugin** is intended to provide an automated analysis of sperm motility, by tracking the live sperm found in images. 
This plugin takes as input a folder of TIFF images, and outputs a CSV file with raw results: .

## User guide

### a. Installation

The plugin is available for download [here](https://github.com/matou1604/trackmate_sperm_motility/blob/main/target/project-template-1.0.0-SNAPSHOT.jar).

Once downloaded, you can drag the `.jar` file into a specific folder of the ImageJ folder. 
First, find the ImageJ file location by right-clicking on the app and selecting *open file location*. 
Then drag the `.jar` file into the **plugins** folder. The complete path should be something like this: `C:\Fiji.app\plugins`. You can rename the file if you want to.

Restart ImageJ if it was open and the plugin will be available in the *Plugins* menu:
<div style="text-align: left;">
    <img src="src\images\plugin.png" alt="plugin" style="width: 60%; background: transparent;">
</div>


### b. Running the plugin

When running the plugin, a user interface window page will appear.

<div style="padding: 5px;">
    <img style="float: right; width:50%; margin-top: 10px; margin-left: 20px; margin-bottom:10px;" src="src/images/gui.png" align="right">
</div>


- Select the folder containing the TIFF images you want to analyze. 
A results folder will be created in the same directory as the input folder, and the results will be saved there.

- You can then modify the parameters of the tracking part of the analysis: 
    - **Detection radius**: the radius of the detection circle.
    - **Quality detection threshold**: the minimum quality of the detection. 
    - **Apply median filter**: if checked, a median filter will be applied to the image before detection. 
    - **Max linking distance**: the maximum distance between two detections to be considered as the same sperm.
    - **Max gap closing distance**: the maximum distance between two detections to be considered as the same sperm, if they are not detected in consecutive frames.
    - **Max frame gap**: the maximum number of frames between two detections to be considered as the same sperm, if they are not detected in consecutive frames.
    - **Track duration filter**: the minimum duration of a track to be considered as valid. (IN MINUTES OR FRAMES)


<br>

During the analysis, the plugin will display the image being analysed. 
If you want to see the results of the analysis, you can check the *Stop between images* box.

<br>

## Hardware requirements

## Trackmate scripting (LAP)


