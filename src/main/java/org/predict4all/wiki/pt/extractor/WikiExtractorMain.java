package org.predict4all.wiki.pt.extractor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;

public class WikiExtractorMain {
	private static final Logger LOGGER = LoggerFactory.getLogger(WikiExtractorMain.class);

	public static void main(String[] args) {

		WikiExtractorArgs config = new WikiExtractorArgs();
		final JCommander commandParsing = JCommander.newBuilder().programName("").addObject(config).build();

		try {
			commandParsing.parse(args);
			new WikiExtractor(config, null).start();
		} catch (ParameterException pe) {
			StringBuilder usage = new StringBuilder();
			commandParsing.usage(usage);
			LOGGER.error("Invalid usage, check command line usage : \n{}", usage);
			System.exit(-1);
		} catch (Exception e) {
			LOGGER.error("Something got wrong in wiki extractor", e);
		}
	}

}
