package com.common;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Dictionary;
import java.util.Hashtable;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xmlpull.v1.XmlSerializer;

import android.os.Environment;
import android.util.Log;

public class XmlLocationImporter implements LocationImporter {

	public static final String FILE_EXTENSION = ".xml";

	private XmlSerializer mSerializer;
	private FileOutputStream mOs;

	public static final int WAYPOINT_TAG = 0;
	public static final int LINK_TAG = 1;

	private int mCurrentTag = -1;

	public XmlLocationImporter() {

	}

	public LocationWaypoint readFromFile(String fileName) throws IOException {
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
					+ "/" + fileName + FILE_EXTENSION);

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
			return waypointHandler.getParsedData();
		} catch (Exception e) {
			Log.e(TAG, "XML query error: ", e);
		}

		return null;
	}

	private class XmlWaypointHandler extends DefaultHandler {

		private LocationWaypoint mStartingWaypoint = null;
		private LocationWaypoint mCurrentWaypoint;

		private Dictionary<Integer, LocationWaypoint> mWaypoints = new Hashtable<Integer, LocationWaypoint>();

		public LocationWaypoint getParsedData() {
			return mStartingWaypoint;
		}

		@Override
		public void startDocument() throws SAXException {

		}

		@Override
		public void endDocument() throws SAXException {

		}

		/**
		 * Gets be called on opening tags like: <tag> Can provide attribute(s),
		 * when xml was like: <tag attribute="attributeValue">
		 */
		@Override
		public void startElement(String namespaceURI, String localName,
				String qName, Attributes attr) throws SAXException {
			if (localName.equals("WayPoint")) {
				String idString = attr.getValue("id");
				String xString = attr.getValue("x");
				String yString = attr.getValue("y");

				int id = Integer.parseInt(idString);
				double x = Double.parseDouble(xString);
				double y = Double.parseDouble(yString);

				Coordinate coordinate = new Coordinate(x, y);
				LocationWaypoint waypoint = new LocationWaypoint(coordinate);

				if (mStartingWaypoint == null) {
					mStartingWaypoint = waypoint;
				}

				mCurrentWaypoint = waypoint;

				mWaypoints.put(id, waypoint);

				mCurrentTag = WAYPOINT_TAG;
			} else if (localName.equals("Link")) {
				mCurrentTag = LINK_TAG;
			} else {
				mCurrentTag = -1;
			}
		}

		/**
		 * Gets be called on closing tags like: </tag>
		 */
		@Override
		public void endElement(String namespaceURI, String localName,
				String qName) throws SAXException {

		}

		/**
		 * Gets be called on the following structure: <tag>characters</tag>
		 */
		@Override
		public void characters(char ch[], int start, int length) {
			if (mCurrentTag == LINK_TAG && mCurrentWaypoint != null
					&& length > 0) {
				StringBuilder builder = new StringBuilder();
				try {
					int id;
					for (char c : ch) {
						id = Integer.parseInt(c + "");
						builder.append(id);
					}
				} catch (NumberFormatException e) {

				} finally {
					try {
						int id = Integer.parseInt(builder.toString());

						LocationWaypoint neighbour = mWaypoints.get(id);
						if (neighbour != null) {
							mCurrentWaypoint.addNext(neighbour);
							Log.d(TAG, "[" + mCurrentWaypoint.id + "] add "
									+ neighbour.id);
						}
					} catch (NumberFormatException e) {

					}
				}
			}
		}
	}

}
