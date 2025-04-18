package ch.epfl.bio410;

import ij.IJ;
import net.imagej.ImageJ;
import org.scijava.command.Command;
import org.scijava.plugin.Plugin;


@Plugin(type = Command.class, menuPath = "Plugins>BII>Project Template")
public class ProjectCommand implements Command {

	public void run() {
		IJ.log("Hello World");
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
