package org.predict4all.wiki.pt.extractor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.beust.jcommander.Parameter;

public class WikiExtractorArgs {

	@Parameter(description = "<url> Wiki url where the script should begin (default is set to \"https://fr.wikipedia.org/wiki/France\")")
	private List<String> input = new ArrayList<>(Arrays.asList("https://fr.wikipedia.org/wiki/France"));

	@Parameter(names = "-thread", description = "Thread pool size (number of parallel thread)")
	private int thread = Runtime.getRuntime().availableProcessors();

	@Parameter(names = "-count", description = "Number of page wanted")
	private int count = 500;

	@Parameter(names = "-output", description = "Path to the output directory")
	private String output = "./result";

	public List<String> getInput() {
		return input;
	}

	public int getThread() {
		return thread;
	}

	public int getCount() {
		return count;
	}

	public String getOutput() {
		return output;
	}

}
