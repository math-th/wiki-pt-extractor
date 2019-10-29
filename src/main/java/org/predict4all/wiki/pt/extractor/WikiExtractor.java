package org.predict4all.wiki.pt.extractor;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WikiExtractor {
	private static final Logger LOGGER = LoggerFactory.getLogger(WikiExtractor.class);

	private static final String NAME_TO_EXPLORE_URL = "to_explore.txt";
	private static final String NAME_EXPLORED_URL = "explored.txt";
	private static final String NAME_FILES = "files";

	private static final long TIMEOUT_SECOND = 10;
	private static final int MIN_CHAR = 500;
	private static final long PAUSE_MS = 40;

	// Language
	private static final String LANG = "fr";
	private static final String REF_ID = "Notes_et_références";

	private static final String VALID_HOST_1 = "fr.wikipedia.org";

	private final Set<String> VALID_HOST = new HashSet<>(Arrays.asList(VALID_HOST_1));

	private final AtomicInteger count = new AtomicInteger(0);
	private final int maxCount;
	private final int workingThread;
	private final File outputRootDirectory;
	private final File outputFilesDirectory;
	private final BlockingQueue<String> queue;
	private final ExecutorService executorService;

	private final String startUrl;

	private final ConcurrentHashMap<String, String> toExploreUrls;
	private final ConcurrentHashMap<String, String> exploredUrls;

	private boolean stopProcess = false;

	private final ExtractionListener extractionListener;

	public WikiExtractor(WikiExtractorArgs args, final ExtractionListener extractionListener) {
		this.toExploreUrls = new ConcurrentHashMap<>();
		this.exploredUrls = new ConcurrentHashMap<>();
		this.queue = new LinkedBlockingQueue<>();
		this.maxCount = args.getCount();
		this.workingThread = args.getThread();
		this.outputRootDirectory = new File(args.getOutput());
		this.outputFilesDirectory = new File(this.outputRootDirectory.getPath() + File.separator + NAME_FILES);
		this.startUrl = args.getInput().get(0);
		this.extractionListener = extractionListener;
		executorService = Executors.newFixedThreadPool(workingThread);
	}

	public void start() throws IOException, InterruptedException {
		Thread stopping = new Thread(() -> {
			try {
				System.in.read();
				LOGGER.info("Stopping process...");
				stopProcess = true;
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
		stopping.setDaemon(true);
		stopping.start();
		// Get URL to explore
		File toExploreFile = new File(this.outputRootDirectory.getPath() + File.separator + NAME_TO_EXPLORE_URL);
		if (toExploreFile.exists()) {
			try (Scanner scan = new Scanner(toExploreFile, "UTF-8")) {
				while (scan.hasNextLine()) {
					String url = scan.nextLine();
					if (StringUtils.isNotBlank(url)) {
						this.queue.put(url);
						this.toExploreUrls.put(url, url);
					}
				}
			}
		} else {
			this.queue.put(startUrl);
			this.toExploreUrls.put(startUrl, startUrl);
			LOGGER.info("Will start on URL : {}", startUrl);
		}

		// Get URL already explored
		File exploredFile = new File(this.outputRootDirectory.getPath() + File.separator + NAME_EXPLORED_URL);
		if (exploredFile.exists()) {
			try (Scanner scan = new Scanner(exploredFile, "UTF-8")) {
				while (scan.hasNextLine()) {
					String url = scan.nextLine();
					if (StringUtils.isNotBlank(url)) {
						this.exploredUrls.put(url, url);
					}
				}
			}
		}
		LOGGER.info("Read {} to explore URL, {} explored URL", toExploreUrls.size(), exploredUrls.size());

		this.outputFilesDirectory.mkdirs();
		LOGGER.info("Data will be saved to {}", this.outputFilesDirectory.getAbsolutePath());
		try {
			executorService.invokeAll(IntStream.range(0, this.workingThread).mapToObj(i -> new PageDownloader()).collect(Collectors.toList()))
					.forEach(future -> {
						try {
							future.get();
						} catch (Throwable t) {
							LOGGER.error("Problem in task execution", t);
						}
					});
			LOGGER.info("Execution ended");
			this.executorService.shutdown();
		} catch (InterruptedException e) {
			LOGGER.error("Problem in task execution", e);
		}

		//Save explored
		if (exploredFile.getParentFile() != null) {
			exploredFile.getParentFile().mkdirs();
		}
		LOGGER.info("Will save {} explored URL", exploredUrls.size());
		try (PrintWriter pwExplored = new PrintWriter(exploredFile, "UTF-8")) {
			this.exploredUrls.forEach((_url, url) -> pwExplored.println(url));
		}

		//Save to explore
		LOGGER.info("Will save {} to explore URL", toExploreUrls.size());
		if (toExploreFile.getParentFile() != null) {
			toExploreFile.getParentFile().mkdirs();
		}
		try (PrintWriter pwToExplore = new PrintWriter(toExploreFile, "UTF-8")) {
			this.toExploreUrls.forEach((_url, url) -> pwToExplore.println(url));
		}
	}

	private static final Pattern ARTICLE_ID_MATCHER = Pattern.compile("wgArticleId\":(\\s*)(\\d+)", Pattern.MULTILINE);

	private class PageDownloader implements Callable<Void> {

		@Override
		public Void call() throws Exception {
			while (count.get() < maxCount && !stopProcess) {
				String url = queue.poll(TIMEOUT_SECOND, TimeUnit.SECONDS);
				if (url != null) {
					if (!StringUtils.contains(url, "action=edit")) {
						try {
							Document doc = Jsoup.connect(url).get();
							String pageTitle = StringUtils.trimToEmpty(doc.select("head > title").text());
							pageTitle = pageTitle.replaceAll("[\\\\/:*?\"<>|]", "_");
							pageTitle = StringUtils.trim(StringUtils.split(pageTitle, "—")[0]);
							Element contentText = doc.getElementById("mw-content-text");
							if (!outputFilesDirectory.exists())
								outputFilesDirectory.mkdirs();
							//LOGGER.info("Try to parse and extract : {}", url);
							if (StringUtils.equals(LANG, contentText.attr("lang"))) {
								String articleId = null;
								Elements elementsByTag = doc.select("head > script");
								for (Element scriptElement : elementsByTag) {
									String data = scriptElement.data();
									if (data.contains("wgArticleId")) {
										Matcher matcher = ARTICLE_ID_MATCHER.matcher(data);
										if (matcher.find() && matcher.groupCount() >= 2) {
											articleId = matcher.group(2);
										}
									}
								}

								StringBuilder content = new StringBuilder();
								contentText.select(".mw-parser-output").forEach(potentialText -> {
									Elements children = potentialText.children();
									for (int i = 0; i < children.size(); i++) {
										if ("p".equals(children.get(i).tagName())) {
											String text = this.analayzeElement(children.get(i));
											if (StringUtils.isNotBlank(text)) {
												content.append(text).append("\n");
											}
										}
										if (children.get(i).getElementById(REF_ID) != null) {
											break;
										}
									}
								});
								// Check validity
								if (content.length() > MIN_CHAR) {
									File filePath = new File(outputFilesDirectory.getPath() + File.separator + pageTitle + ".txt");
									try (PrintWriter pw = new PrintWriter(filePath, "utf-8")) {
										pw.print(content.toString());
									}

									if (extractionListener != null) {
										extractionListener.extracted(articleId, content, filePath);
									}

									int page = count.incrementAndGet();
									LOGGER.info("{} / {} == OK", page, maxCount);
								} else {
									//LOGGER.warn("Didn't save page {} because not enough content", pageTitle);
								}
							} else {
								LOGGER.error("Page {} is not in french", url);
							}
							toExploreUrls.remove(url);
							exploredUrls.put(url, url);
						} catch (Exception e) {
							LOGGER.warn("Error while reading URL {}, will try the next URL", url, e);
							if (e instanceof HttpStatusException && ((HttpStatusException) e).getStatusCode() == 429) {
								stopProcess = true;
								throw e;
							}
						}
					} else {
						LOGGER.info("Ignore page {} because it's an edit page", url);
					}
				} else {
					LOGGER.error("Couldn't get any new URL");
				}
				Thread.sleep(PAUSE_MS);
			}
			return null;
		}

		private String analayzeElement(Element potentialText) {
			potentialText.select(".reference").remove();
			potentialText.select("a").forEach(e -> {
				String absUrl = e.absUrl("href");
				try {
					URL url = new URL(absUrl);
					if (VALID_HOST.contains(url.getHost())) {
						if (!toExploreUrls.containsKey(absUrl) && !exploredUrls.containsKey(absUrl)) {
							toExploreUrls.put(absUrl, absUrl);
							queue.put(absUrl);
						} else {
							// LOGGER.warn("URL already found : {}", absUrl);
						}
					} else {
						//LOGGER.warn("Found invalid host in URL : {}", absUrl);
					}
				} catch (Exception e1) {
					LOGGER.warn("Incorrect url : {}", absUrl);
				}
			});
			return potentialText.text();
		}
	}
}
