package com.common;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.common.Coordinate;
import com.common.Waypoint;

public class XmlWaypointHandler extends DefaultHandler {

	private Waypoint mWaypoint = null;
	private Waypoint mCurrentWaypoint;

	public Waypoint getParsedData() {
		return this.mWaypoint;
	}

	@Override
	public void startDocument() throws SAXException {

	}

	@Override
	public void endDocument() throws SAXException {

	}

	/**
	 * Gets be called on opening tags like: <tag> Can provide attribute(s), when
	 * xml was like: <tag attribute="attributeValue">
	 */
	@Override
	public void startElement(String namespaceURI, String localName,
			String qName, Attributes attr) throws SAXException {
		if (localName.equals("WayPoint")) {
			String xString = attr.getValue("x");
			String yString = attr.getValue("y");

			double x = Double.parseDouble(xString);
			double y = Double.parseDouble(yString);

			Coordinate coordinate = new Coordinate(x, y);

			if (mWaypoint == null) {
				mWaypoint = new Waypoint(coordinate);
				mCurrentWaypoint = mWaypoint;
			} else {
				Waypoint waypoint = new Waypoint(coordinate);
				
				mCurrentWaypoint.setNext(waypoint);
				mCurrentWaypoint = waypoint;
			}
		}
	}

	/**
	 * Gets be called on closing tags like: </tag>
	 */
	@Override
	public void endElement(String namespaceURI, String localName, String qName)
			throws SAXException {

	}

	/**
	 * Gets be called on the following structure: <tag>characters</tag>
	 */
	@Override
	public void characters(char ch[], int start, int length) {
		// myParsedExampleDataSet.setExtractedString(new String(ch, start,
		// length));
	}
}
