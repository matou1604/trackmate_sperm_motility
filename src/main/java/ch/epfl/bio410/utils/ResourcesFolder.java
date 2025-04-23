package ch.epfl.bio410.utils;

import ij.IJ;
import org.apache.commons.io.FileUtils;
import java.io.File;
import java.io.IOException;
import java.net.URL;

/** Code courtesy of Rémy Dornier @ EPFL */

class ResourcesFolder {
    public static File copyFileFromResources(String pathFromResourcesFolder){
        // get the URL of the file inside the jar
        URL url = ResourcesFolder.class.getClassLoader().getResource(pathFromResourcesFolder);

        if(url != null) {
            // get the temporary location where to copy the file
            String outputPath = System.getProperty("user.home") + File.separator + "Downloads";

            // create the new file in the temporary location
            File outputFile = new File(outputPath + File.separator + pathFromResourcesFolder);

            // copy the file
            try {
                FileUtils.copyURLToFile(url, outputFile);
            }catch (IOException e){
                IJ.log("ERROR -- Cannot copy the file '"+url.getPath()+"' to '" +outputFile.getAbsolutePath()+"'");
                return null;
            }

            return outputFile;
        }
        return null;
    }
}
