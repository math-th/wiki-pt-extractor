package org.predict4all.wiki.pt.extractor;

import java.io.File;

public interface ExtractionListener {
	public void extracted(String id, StringBuilder content, File filePath);
}
