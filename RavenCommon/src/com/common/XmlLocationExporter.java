package com.common;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.xmlpull.v1.XmlSerializer;

import android.os.Environment;
import android.util.Log;
import android.util.Xml;

public class XmlLocationExporter extends LocationExporter {

	public static final String FILE_EXTENSION = ".xml";

	private XmlSerializer mSerializer;
	private FileOutputStream mOs;

	public XmlLocationExporter() {

	}

	public void writeToFile(String fileName) throws IOException {
		// create a new file called "new.xml" in the SD card
		File file = new File(Environment.getExternalStorageDirectory() + "/"
				+ fileName + FILE_EXTENSION);

		try {
			file.createNewFile();
		} catch (IOException e) {
			Log.e(TAG, "exception in createNewFile() method");
			return;
		}

		// we have to bind the new file with a FileOutputStream
		try {
			mOs = new FileOutputStream(file);
		} catch (FileNotFoundException e) {
			Log.e(TAG, "can't create FileOutputStream");
			return;
		}
		// we create a XmlmSerializer in order to write xml data
		mSerializer = Xml.newSerializer();
		try {
			// we set the FileOutputStream as output for the mSerializer, using
			// UTF-8 encoding
			mSerializer.setOutput(mOs, "UTF-8");
			// Write <?xml declaration with encoding (if encoding not null) and
			// standalone flag (if standalone not null)
			mSerializer.startDocument(null, Boolean.valueOf(true));
			// set indentation option
			mSerializer.setFeature(
					"http://xmlpull.org/v1/doc/features.html#indent-output",
					true);
			// start a tag called "root"
			mSerializer.startTag(null, "root");

			LocationWaypoint waypoint = getGraph();

			recursion(waypoint);

			end();
		} catch (Exception e) {
			Log.e(TAG, e.toString());
		}

		Log.d(TAG, "saved graph to file: " + fileName + FILE_EXTENSION);
	}

	public void end() {
		try {
			mSerializer.endTag(null, "root");
			mSerializer.endDocument();
			// write xml data into the FileOutputStream
			mSerializer.flush();
			// finally we close the file stream
			mOs.flush();
			mOs.close();

			Log.d(TAG, "Closing root on xml file");
		} catch (Exception e) {
			Log.e(TAG, e.toString());
		}
	}

	public void recursion(LocationWaypoint waypoint) {
		if (!waypoint.isVisited()) {
			waypoint.visit();

			write(waypoint); // Write way-point and links of children.
			for (Waypoint w : waypoint.getNeighbours()) {
				recursion((LocationWaypoint) w);
			}
		}
	}

	// <waypoint id="0" x="" y="">
	// <link>1</link>
	// <link>2</link>
	// </waypoint>
	// <waypoint id="1">
	// <link>1</link>
	// </waypoint>
	// <waypoint id="2">
	// <link>1</link>
	// </waypoint>

	public void write(LocationWaypoint waypoint) {
		Coordinate coord = waypoint.coordinate;

		try {
			mSerializer.startTag(null, "WayPoint");
			mSerializer.attribute(null, "id", waypoint.id + "");
			mSerializer.attribute(null, "x", coord.latitude + "");
			mSerializer.attribute(null, "y", coord.longitude + "");

			if (waypoint.numberOfChildren() > 0) {
				for (Waypoint w : waypoint.getNeighbours()) {
					mSerializer.startTag(null, "Link");
					mSerializer.text(w.id + "");
					mSerializer.endTag(null, "Link");
				}
			}

			// mSerializer.text("some text inside WayPoint");
			mSerializer.endTag(null, "WayPoint");

			Log.d(TAG, "Writing coordinate to xml file: " + coord);
		} catch (Exception e) {
			Log.e(TAG, e.toString());
		}
	}

}
