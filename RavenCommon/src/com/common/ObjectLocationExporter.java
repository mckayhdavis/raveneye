package com.common;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

import android.os.Environment;
import android.util.Log;

public class ObjectLocationExporter extends LocationExporter {

	public static final String FILE_EXTENSION = ".ser";

	public ObjectLocationExporter() {

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

		ObjectOutputStream os = null;
		try {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			os = new ObjectOutputStream(bos);

			// TODO: warning, will get a stack-overflow exception using this!!
			// Deep nested graph structure in way-points don't make for good
			// serializable objects... that is why I switched to XML.
			os.writeObject(getGraph());
			os.close();

			byte[] data = bos.toByteArray();

			FileOutputStream fos = new FileOutputStream(file);
			os = new ObjectOutputStream(bos);
			os.close();

			// os.flush();
		} finally {
			if (os != null) {
				os.close();
			}
		}

		Log.d(TAG, "saved graph to file: " + fileName + FILE_EXTENSION);
	}

}
