package com.blogspot.jesfre.pmd.reportgenerator;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

/**
 * This class updates the PMD reports in given specified location
 * 
 * @author <a href="mailto:jorge.ruiz.aquino@gmail.com">Jorge Ruiz Aquino</a>
 *         Aug 1, 2022
 */
public class PmdReportUpdater {
	private static final String TBD = "TBD";
	private static final String SINGLE_LINE = "\\/{2}[\\*\\s]*PMD_Override.*";
	private static final String MULTI_LINE = "\\/\\*+[\\s\\n\\r\\*]*PMD_Override(?:.|[\\n\\r])*?\\*\\/";
	private static final String ALL_COMMENTS = SINGLE_LINE + "|" + MULTI_LINE;
	// private static final Pattern COMMENTS_PATTERN = Pattern.compile("/\\*(\\s|[\\n\\r])*PMD_Override(?:.|[\\n\\r])*?\\*/");
	private static final Pattern COMMENTS_PATTERN = Pattern.compile(ALL_COMMENTS);
	
	public void updateAfterFixFiles(String reportLocation) throws IOException {
		File folder = new File(reportLocation);
		Collection<File> fileList = FileUtils.listFiles(folder, new String[] { "csv" }, false);
		for (File file : fileList) {
			if (file.getName().contains("Before_Code_Fix") || file.getName().startsWith("code-review")) {
				// Only update After_Code_Fix files
				continue;
			}
			System.out.println(file.getName() + " - Updating file... ");

			Reader reader = new FileReader(file);
			CSVParser csvParser = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(reader);
			List<List<String>> newReportContent = getNewReportContent(csvParser);
			reader.close();
			csvParser.close();

			updateReport(file, newReportContent);
			System.out.println();
		}
	}

	private List<List<String>> getNewReportContent(CSVParser csvParser) throws IOException {
		System.out.println("\t Creating new content");
		List<String> headerLabels = getUpdatedHeaderLabels(csvParser);
		final int headerCount = headerLabels.size();
		int justifiedRules = 0;

		Map<String, Integer> countRulesInReport = new HashMap<String, Integer>();
		List<List<String>> newReportContent = new ArrayList<List<String>>();
		newReportContent.add(headerLabels);

		String classFileLocation = null;
		List<String> justifications = Collections.emptyList();
		int justificationPointer = 0;
		for (CSVRecord record : csvParser) {
			if (StringUtils.isBlank(classFileLocation)) {
				classFileLocation = record.get(2); // Java file location
				justifications = getJustifications(classFileLocation);
			}

			List<String> recordContent = new ArrayList<String>();
			for (int i = 0; i < headerCount - 1; i++) {
				recordContent.add(record.get(i));
			}
			String rule = record.get(7);
			if (rule.contains("#15")) {
				recordContent.add("Existing code");
				justifiedRules++;
			} else if (!justifications.isEmpty() && justificationPointer < justifications.size()) {
				String justification = justifications.get(justificationPointer++);
				recordContent.add(justification);
				justifiedRules++;
			} else {
				recordContent.add(TBD);
				rule = TBD;
			}

			newReportContent.add(recordContent);
			incrementMap(countRulesInReport, rule);
		}

		System.out.println("\t - Found PMD justifications: " + justifications.size());
		System.out.println("\t - Found PMD reported lines: " + (newReportContent.size() - 1));
		System.out.println("\t - Found rules: " + countRulesInReport.toString());
		if (countRulesInReport.containsKey(TBD)) {
			System.err.println("WARNING. " + countRulesInReport.get(TBD) + " PMD reports to be analyzed.");
		}
		if ((newReportContent.size() - 1) != justifiedRules) {
			System.err.println("WARNING. Reported errors: " + (newReportContent.size() - 1) + " vs justifications: " + justifiedRules);
		}
		return newReportContent;
	}

	/**
	 * @param csvParser
	 * @return
	 */
	private List<String> getUpdatedHeaderLabels(CSVParser csvParser) {
		Map<String, Integer> headerMap = csvParser.getHeaderMap();
		List<String> headerLabels = new ArrayList<String>(headerMap.keySet());
		if (headerLabels.size() < 9) {
			headerLabels.add("Justification");
		}
		return headerLabels;
	}

	private List<String> getJustifications(String classFileLocation) throws IOException {
		File classFile = new File(classFileLocation);
		System.out.println("\t Searching comments in " + classFile.getName());

		String content = FileUtils.readFileToString(classFile);
		Matcher m = COMMENTS_PATTERN.matcher(content);

		List<String> commentList = new ArrayList<String>();
		String originalComment = null;
		try {
			while (m.find()) {
				originalComment = m.group().trim();
				// Clean the comment and make it a single line
				String comment = originalComment.replaceAll("//", "")
						.replaceAll("/\\*", "")
						.replaceAll("\\*/", "")
						.replaceAll("\\*", "")
						.replaceAll("\\s+", " ")
						.trim();
				if (comment.startsWith("PMD")) {
					commentList.add(comment);
				}
			}
		} catch (Error e) {
			System.err.println(e);
			System.err.println("Comment before error: " + originalComment);
		}
		return commentList;
	}

	private void updateReport(File f, List<List<String>> updatedReportContent) throws IOException {
		Writer writer = new FileWriter(f);
		
		CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT);
		for (List<String> recordContent : updatedReportContent) {
			csvPrinter.printRecord(recordContent);
		}
		writer.flush();
		writer.close();
		System.out.println("\t Updated ");
	}

	private static void incrementMap(Map<String, Integer> map, String key) {
		int count = map.containsKey(key) ? map.get(key) : 0;
		map.put(key, count + 1);
	}
}
