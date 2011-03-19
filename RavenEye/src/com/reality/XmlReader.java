package com.reality;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import android.os.Environment;
import android.util.Log;

public class XmlReader {

	private final static String TAG = RealityActivity.TAG;

	public static Waypoint getWaypoints() {
		Waypoint waypoint = null;

		try {
			/* Get a SAXParser from the SAXPArserFactory. */
			SAXParserFactory spf = SAXParserFactory.newInstance();
			SAXParser sp = spf.newSAXParser();

			/* Get the XMLReader of the SAXParser we created. */
			XMLReader xr = sp.getXMLReader();
			/* Create a new ContentHandler and apply it to the XML-Reader */
			XmlWaypointHandler waypointHandler = new XmlWaypointHandler();
			xr.setContentHandler(waypointHandler);

			File file = new File(Environment.getExternalStorageDirectory()
					+ "/path.xml");

			InputStream in = null;
			try {
				in = new BufferedInputStream(new FileInputStream(file));

				xr.parse(new InputSource(in));
			} finally {
				if (in != null) {
					in.close();
				}
			}

			/* Our ExampleHandler now provides the parsed data to us. */
			waypoint = waypointHandler.getParsedData();
		} catch (Exception e) {
			Log.e(TAG, "XML query error: ", e);
		}

		return waypoint;
	}

}
