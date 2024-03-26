package com.blogspot.jesfre.pmd.reportgenerator;

import static com.blogspot.jesfre.misc.PathUtils.formatPath;
import static com.blogspot.jesfre.velocity.utils.VelocityTemplateProcessor.getProcessor;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;

import com.blogspot.jesfre.commandline.CommandLineRunner;
import com.blogspot.jesfre.svn.ModifiedFile;
import com.blogspot.jesfre.svn.OperationType;
import com.blogspot.jesfre.svn.utils.SvnExport;
import com.blogspot.jesfre.svn.utils.SvnLog;
import com.blogspot.jesfre.svn.utils.SvnLogExtractor;
import com.blogspot.jesfre.velocity.utils.VelocityTemplateProcessor;

public class PmdReportGenerator {
	private static final String CMD_TEMPLATE = "call PMD_ROOT\\bin\\pmd -d \"SRC_FILE\" -R \"PMD_RULES_PATH\" -f csv -r \"WORKING_DIR_PATH/REPORTS_FOLDER/CLASS_NAME_PMD_Issues_STAGE_Code_Fix_vVERSION.csv\"";
	private static final String ECHO = "echo on";
	private static final OperationType[] OPERATIONS_TO_REVIEW = {OperationType.ADDED, OperationType.MERGED, OperationType.MODIFIED, OperationType.UPDATED};
	private static final int MAX_MONTHS_SEARCH_IN_PAST = 12;
	public static final String SOURCE_CODE_FOLDER = "sources";
	private static boolean usingRepoUrl;

	public static void main(String[] args) throws Exception {
		if (args.length == 0) {
			throw new IllegalArgumentException("Setup file was not provided.");
		}

		System.out.println("Generating batch file.");
		PmdReportGenerator pmdReportGenerator = new PmdReportGenerator();
		PmdReportGeneratorSettings reportSettings = pmdReportGenerator.loadSettingsProperties(args);

		if(reportSettings.getClassFileLocationList().isEmpty()) {
			System.out.println("Exporting files from repo...");
			usingRepoUrl = true;
			pmdReportGenerator.checkoutFilesFromRepo(reportSettings);
		}

		System.out.println("Generating PMD command file...");
		pmdReportGenerator.generatePmdCommandFile(reportSettings);

		System.out.println("Generating file with list of files to be analyzed...");
		pmdReportGenerator.generateFileListFile(reportSettings);

		System.out.println("Generating summary file...");
		pmdReportGenerator.generateCommentsFile(reportSettings);

		System.out.println("\nExecuting batch file.");
		CommandLineRunner runner = new CommandLineRunner();
		runner.run(reportSettings.getCommandFile());

		System.out.println("\nUpdating reports.");
		PmdReportUpdater updater = new PmdReportUpdater();
		updater.updateAfterFixFiles(reportSettings.getReportOutputLocation());

		System.out.println("\nDone.");
	}

	private void checkoutFilesFromRepo(PmdReportGeneratorSettings reportSettings)
			throws MalformedURLException, URISyntaxException {
		SvnLogExtractor logExtractor = new SvnLogExtractor("TBD", reportSettings.getReportOutputLocation());
		String repoUrlString = reportSettings.getRepositoryBaseUrl() + "/" + reportSettings.getRepositoryWorkingBranch();
		URL repoUrl = new URL(repoUrlString).toURI().normalize().toURL();

		Set<String> modifiedFileSet = new LinkedHashSet<String>();

		// Will discover the modified files from the repository
		List<SvnLog> logList = logExtractor
				.withComment(reportSettings.getJiraTicket())
				.verbose(reportSettings.isVerbose())
				.lookMonthsBack(3)
				.clearTempFiles(true)
				.exportLog(false)
				.listModifiedFiles(true)
				.analyzeUrl(repoUrl).extract();
		for(SvnLog log : logList) {
			for(ModifiedFile mf : log.getModifiedFiles()) {
				if(ArrayUtils.contains(OPERATIONS_TO_REVIEW, mf.getOperation())) {
					String fileUrlString = reportSettings.getRepositoryBaseUrl() + "/" + mf.getFile();
					URL fileUrl = new URL(fileUrlString).toURI().normalize().toURL();
					modifiedFileSet.add(fileUrl.toString());
				}
			}
		}

		String exportedFilePath = null;
		String sourFolderPath = reportSettings.getWorkingDirPath() + "/" + SOURCE_CODE_FOLDER;

		// Analyze the history of each file and export the latest
		for(String fileUrlString : modifiedFileSet) {
			List<SvnLog> logListIndividualFile = logExtractor
					// .withLimit(2)
					.withComment(reportSettings.getJiraTicket())
					.verbose(reportSettings.isVerbose())
					.lookMonthsBack(MAX_MONTHS_SEARCH_IN_PAST)
					.clearTempFiles(true)
					.exportLog(false)
					.analyze(fileUrlString).extract();

			if(logListIndividualFile.isEmpty()) {
				System.err.println("No log found for " + fileUrlString + " using comment " + reportSettings.getJiraTicket());
			}

			long headRev = logListIndividualFile.get(0).getRevision();
			long prevRev = logListIndividualFile.get(logListIndividualFile.size() - 1).getRevision();
			if (prevRev > 1) {
				// Compare with the revision before the list last entry's revision number
				prevRev = prevRev - 1;
			}

			String originalFileType = FilenameUtils.getExtension(fileUrlString);
			String cName = FilenameUtils.getBaseName(fileUrlString);

			String exportedFileName = cName + "_" + (headRev > 0 ? headRev : "HEAD") + "." + originalFileType;
			exportedFilePath = sourFolderPath + "/" + exportedFileName;
			if(headRev > 0 ) {
				new SvnExport()
				.verbose(reportSettings.isVerbose())
				.overwriteFile(true)
				.export(headRev, fileUrlString, formatPath(exportedFilePath));
			} else {
				new SvnExport()
				.verbose(reportSettings.isVerbose())
				.overwriteFile(true)
				.exportHead(fileUrlString, formatPath(exportedFilePath));
			}

			reportSettings.getClassFileLocationList().add(exportedFilePath);
		}

		if(reportSettings.isVerbose() && !reportSettings.getClassFileLocationList().isEmpty()) {
			System.out.println("Files committed with " + reportSettings.getJiraTicket());
			for(String fileFound : reportSettings.getClassFileLocationList()) {
				System.out.println("- " + FilenameUtils.getName(fileFound));
			}
		}
	}

	@SuppressWarnings("unchecked")
	private PmdReportGeneratorSettings loadSettingsProperties(String[] setupFileLocations) throws FileNotFoundException, IOException, ConfigurationException {
		PmdReportGeneratorSettings settings = new PmdReportGeneratorSettings();

		for(int i=0; i<setupFileLocations.length; i++) {
			String setupFileLocation = setupFileLocations[i];
			System.out.println("Loading PMD report settings from " + setupFileLocation);

			PropertiesConfiguration config = new PropertiesConfiguration();
			config.setListDelimiter('|');
			config.load(setupFileLocation);

			// Store the location of the last config file in the arguments
			settings.setConfigFile(setupFileLocation);

			if(config.containsKey("repository.baseUrl")) {
				settings.setRepositoryBaseUrl(config.getString("repository.baseUrl", ""));
			}
			if(config.containsKey("repository.workingBranch")) {
				settings.setRepositoryWorkingBranch(config.getString("repository.workingBranch", ""));
			}
			if(config.containsKey("resource.javaHome")) {
				settings.setJavaHome(config.getString("resource.javaHome", ""));
			}
			if(config.containsKey("resource.pmdHome")) {
				settings.setPmdHome(config.getString("resource.pmdHome", ""));
			}
			if(config.containsKey("project")) {
				settings.setProject(config.getString("project", "no_project"));
			}
			if(config.containsKey("jira.ticket")) {
				settings.setJiraTicket(config.getString("jira.ticket", ""));
			}
			if(config.containsKey("review.version")) {
				settings.setVersion(config.getString("review.version", "1"));
			}
			if(config.containsKey("resource.summaryTemplate")) {
				settings.setSummaryTemplate(config.getString("resource.summaryTemplate", ""));
			}
			if(config.containsKey("resource.pmdRulesFile")) {
				settings.setPmdRulesFile(config.getString("resource.pmdRulesFile", ""));
			}
			if(config.containsKey("workingDirectory")) {
				settings.setWorkingDirPath(config.getString("workingDirectory", ""));
			}

			if(config.containsKey("file")) {
				List<String> fileList = config.getList("file");
				settings.getClassFileLocationList().addAll(fileList);
			}
			if(config.containsKey("f")) {
				List<String> fList = config.getList("f");
				settings.getClassFileLocationList().addAll(fList);
			}

		}
		if(StringUtils.isBlank(settings.getJavaHome())) {
			throw new IllegalStateException("Java home path is not provided.");
		}
		if(StringUtils.isBlank(settings.getPmdHome())) {
			throw new IllegalStateException("PMD home path is not provided.");
		}
		if(StringUtils.isBlank(settings.getPmdRulesFile())) {
			throw new IllegalStateException("No PMD rules file was provided.");
		}
		return settings;
	}

	private void generatePmdCommandFile(PmdReportGeneratorSettings settings) throws Exception {
		List<String> lines = settings.getClassFileLocationList();
		String workingDirPath = settings.getWorkingDirPath();

		String rulesPath = FilenameUtils.getFullPath(settings.getPmdRulesFile());
		String rulesFilename = FilenameUtils.getName(settings.getPmdRulesFile());
		if(StringUtils.isBlank(rulesPath)) {
			// Will try to use a file located in the same directory as the configuration file  
			rulesPath = FilenameUtils.getPath(settings.getConfigFile());
		}
		String rulesheet = rulesPath + rulesFilename;

		// New batch file-name definition
		Integer fileCount = 1;
		String newFileName = workingDirPath + "/pmd_commands_" + fileCount + ".bat";
		File newFile = new File(newFileName);
		if (StringUtils.isBlank(settings.getVersion())) {
			while (newFile.exists()) {
				fileCount++;
				newFileName = workingDirPath + "/pmd_commands_" + fileCount + ".bat";
				newFile = new File(newFileName);
			}
		} else {
			fileCount = Integer.valueOf(settings.getVersion());
			newFileName = workingDirPath + "/pmd_commands_" + fileCount + ".bat";
			newFile = new File(newFileName);
		}

		int folderCount = 0;
		String reportsFolder = "pmd-reports_v" + fileCount;
		File reportsPath = new File(workingDirPath + "/" + reportsFolder);
		while (reportsPath.exists()) {
			reportsFolder = reportsFolder + "-" + (++folderCount);
			reportsPath = new File(workingDirPath + "/" + reportsFolder);
		}
		reportsPath.mkdirs();

		String cmdTemplate = StringUtils.replace(CMD_TEMPLATE, "WORKING_DIR_PATH", workingDirPath);
		cmdTemplate = StringUtils.replace(cmdTemplate, "PMD_ROOT", settings.getPmdHome());
		cmdTemplate = StringUtils.replace(cmdTemplate, "PMD_RULES_PATH", rulesheet);
		cmdTemplate = StringUtils.replace(cmdTemplate, "REPORTS_FOLDER", reportsFolder);
		cmdTemplate = StringUtils.replace(cmdTemplate, "VERSION", fileCount.toString());

		String tmplBefore = StringUtils.replace(cmdTemplate, "STAGE", "Before");
		String tmplAfter = StringUtils.replace(cmdTemplate, "STAGE", "After");

		System.out.println("Reading files...");
		List<String> resultContent = new ArrayList<String>();
		for (String f : lines) {
			String cName = FilenameUtils.getBaseName(f);
			if(settings.isVerbose()) {
				System.out.println("- " + cName + ".java");
			}

			String cmdBefore = StringUtils.replace(tmplBefore, "SRC_FILE", f);
			cmdBefore = StringUtils.replace(cmdBefore, "CLASS_NAME", cName);

			String cmdAfter = StringUtils.replace(tmplAfter, "SRC_FILE", f);
			cmdAfter = StringUtils.replace(cmdAfter, "CLASS_NAME", cName);

			resultContent.add("SET JAVA_HOME=\""+settings.getJavaHome()+"\"");
			resultContent.add("SET PATH=%JAVA_HOME%\\bin;%PATH%");
			resultContent.add(ECHO);
			resultContent.add("java -version");
			resultContent.add(cmdBefore);
			resultContent.add(ECHO);
			resultContent.add(cmdAfter);
		}
		resultContent.add("exit 0");
		if (resultContent.size() > 1) {
			if (newFile.exists()) {
				newFile.delete();
				newFile.createNewFile();
			}
			FileUtils.writeLines(newFile, resultContent);
			System.out.println("New batch file generated: " + newFileName);
		}

		settings.setCommandFile(newFileName);
		settings.setReportOutputLocation(reportsPath.getAbsolutePath());
	}

	private void generateFileListFile(PmdReportGeneratorSettings reportSettings) throws IOException {
		System.out.println("Generating list of filenames...");
		String commentsFilename = "_files_" + reportSettings.getJiraTicket() + "-v" + reportSettings.getVersion() + ".txt";
		File modifiedFilesComments = new File(reportSettings.getWorkingDirPath() + "/" + commentsFilename);
		List<String> simplenames = new ArrayList<String>();
		simplenames.add("List of full paths:");
		simplenames.addAll(reportSettings.getClassFileLocationList());
		simplenames.add("\nList of simple file names:");
		for (String fl : reportSettings.getClassFileLocationList()) {
			simplenames.add(FilenameUtils.getName(fl));
		}
		FileUtils.writeLines(modifiedFilesComments, simplenames);
	}

	private void generateCommentsFile(PmdReportGeneratorSettings reportSettings) throws IOException {
		if(StringUtils.isBlank(reportSettings.getSummaryTemplate())) {
			System.err.println("No summary-template file was provided.");
			return;
		}

		String path = FilenameUtils.getFullPath(reportSettings.getSummaryTemplate());
		String templateFilename = FilenameUtils.getName(reportSettings.getSummaryTemplate());
		if(StringUtils.isBlank(path)) {
			// Will try to use a template file located in the same directory as the configuration file  
			path = FilenameUtils.getPath(reportSettings.getConfigFile());
		}

		String commentsFilename = "_report_summary_" + reportSettings.getJiraTicket() + "-v" + reportSettings.getVersion() + ".txt";
		File commentsFile = new File(reportSettings.getWorkingDirPath() + "/" + commentsFilename);

		List<String> simplenames = new ArrayList<String>();
		for (String fl : reportSettings.getClassFileLocationList()) {
			simplenames.add(FilenameUtils.getName(fl));
		}

		Map<String, Object> contextParams = new HashMap<String, Object>();
		contextParams.put("jiraTicket", reportSettings.getJiraTicket());
		contextParams.put("version", reportSettings.getVersion());
		contextParams.put("fileList", simplenames);

		VelocityTemplateProcessor templateProcessor = getProcessor(path);
		String commentsContent = templateProcessor.process(templateFilename, contextParams);
		FileUtils.writeStringToFile(commentsFile, commentsContent);

	}

}