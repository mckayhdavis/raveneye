package com.activities;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.xmlpull.v1.XmlSerializer;

import android.os.Environment;
import android.util.Log;
import android.util.Xml;

import com.common.Coordinate;

public class LocationExporter {

	public static final String TAG = LocationGathererService.TAG;

	private File mFile;
	private XmlSerializer mSerializer;
	private FileOutputStream mOs;

	public LocationExporter() {
		init();
	}

	private void init() {
		// create a new file called "new.xml" in the SD card
		mFile = new File(Environment.getExternalStorageDirectory()
				+ "/path.xml");
		try {
			mFile.createNewFile();
		} catch (IOException e) {
			Log.e(TAG, "exception in createNewFile() method");
			return;
		}

		// we have to bind the new file with a FileOutputStream
		try {
			mOs = new FileOutputStream(mFile);
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

			Log.d(TAG, "Opening root on xml file");
		} catch (Exception e) {
			Log.e(TAG, e.toString());
		}
	}

	public void end() {
		try {
			mSerializer.endTag(null, "root");
			mSerializer.endDocument();
			// write xml data into the FileOutputStream
			mSerializer.flush();
			// finally we close the file stream
			mOs.close();

			Log.d(TAG, "Closing root on xml file");
		} catch (Exception e) {
			Log.e(TAG, e.toString());
		}
	}

	public void add(Coordinate coord) {
		try {
			mSerializer.startTag(null, "WayPoint");
			mSerializer.attribute(null, "x", coord.latitude + "");
			mSerializer.attribute(null, "y", coord.longitude + "");
			// mSerializer.text("some text inside WayPoint");
			mSerializer.endTag(null, "WayPoint");

			// write xml data into the FileOutputStream
			mSerializer.flush();

			Log.d(TAG, "Writing coordinate to xml file: " + coord);
		} catch (Exception e) {
			Log.e(TAG, e.toString());
		}
	}

}
