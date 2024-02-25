package com.blogspot.jesfre.pmd.reportgenerator;

import static com.blogspot.jesfre.velocity.utils.VelocityTemplateProcessor.getProcessor;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;

import com.blogspot.jesfre.commandline.CommandLineRunner;
import com.blogspot.jesfre.velocity.utils.VelocityTemplateProcessor;

public class PmdReportGenerator {
	// ----------- SET -----------------------
//	private static final String SETUP_FILE = "C:/path/tosettings/pmd/pmdgeneration/PmdReportGenerator_setup.txt";
//	private static final String PMD_RULESSHEET_PROJECT1 = "C:/path/to/setup/pmd/pmdgeneration/ruleset-PMDrules.xml";
//	private static final String PMD_RULESSHEET_PROJECT2 = "C:/path/to/setup/pmd/pmdgeneration/ruleset-PMDrules-project2.XML";
	private static final String JAVA_PATH = "C:\\Program Files\\Java\\jdk1.7.0_80";
	private static final String PMD_ROOT = "C:\\dev\\pmd-bin-5.5.1";

	// ---------------------------------------
	private static final String CMD_TEMPLATE = "call " + PMD_ROOT + "\\bin\\pmd -d \"SRC_FILE\" -R \"PMD_RULES_PATH\" -f csv -r \"WORKING_DIR_PATH/REPORTS_FOLDER/CLASS_NAME_PMD_Issues_STAGE_Code_Fix_vVERSION.csv\"";
	private static final String ECHO = "echo on";

	public static void main(String[] args) throws Exception {
		if (args.length == 0) {
			throw new IllegalArgumentException("Setup file was not provided.");
		}
		String setupFilePath = args[0];

		System.out.println("Generating batch file.");
		PmdReportGenerator pmdReportGenerator = new PmdReportGenerator();
		PmdReportGeneratorSettings reportSettings = pmdReportGenerator.loadSettingsProperties(setupFilePath);
		pmdReportGenerator.generatePmdCommandFile(reportSettings);
		pmdReportGenerator.generateFileListFile(reportSettings);
		pmdReportGenerator.generateCommentsFile(reportSettings);

		System.out.println("\nExecuting batch file.");
		CommandLineRunner runner = new CommandLineRunner();
		runner.run(reportSettings.getCommandFile());

		System.out.println("\nUpdating reports.");
		PmdReportUpdater updater = new PmdReportUpdater();
		updater.updateAfterFixFiles(reportSettings.getReportOutputLocation());

		System.out.println("\nDone.");
	}

	@SuppressWarnings("unchecked")
	private PmdReportGeneratorSettings loadSettingsProperties(String setupFileLocation) throws FileNotFoundException, IOException, ConfigurationException {
		PropertiesConfiguration config = new PropertiesConfiguration();
		config.setListDelimiter('|');
		config.load(setupFileLocation);
		String workingDir = config.getString("workingDirectory");

		PmdReportGeneratorSettings settings = new PmdReportGeneratorSettings();
		settings.setConfigFile(setupFileLocation);
		settings.setProject(config.getString("project", "no_project"));
		settings.setJiraTicket(config.getString("jira.ticket", ""));
		settings.setVersion(config.getString("review.version", "1"));
		settings.setSummaryTemplate(config.getString("resource.summaryTemplate", ""));
		settings.setPmdRulesFile(config.getString("resource.pmdRulesFile", ""));
		settings.setWorkingDirPath(workingDir);

		List<String> fileList = config.getList("file");
		settings.getClassFileLocationList().addAll(fileList);

		List<String> fList = config.getList("f");
		settings.getClassFileLocationList().addAll(fList);

		if(StringUtils.isBlank(settings.getPmdRulesFile())) {
			throw new IllegalStateException("Not PMD rules file was provided.");
		}
		
		return settings;
	}

	private void generatePmdCommandFile(PmdReportGeneratorSettings settings) throws Exception {
		List<String> lines = settings.getClassFileLocationList();
		String project = settings.getProject(); // IES or ABE
		String jiraTicket = settings.getJiraTicket();
		String version = settings.getVersion();
		String workingDirPath = settings.getWorkingDirPath();

		String rulesPath = FilenameUtils.getFullPath(settings.getPmdRulesFile());
		String rulesFilename = FilenameUtils.getName(settings.getPmdRulesFile());
		if(StringUtils.isBlank(rulesPath)) {
			// Will try to use a file located in the same directory as the configuration file  
			rulesPath = FilenameUtils.getPath(settings.getConfigFile());
		}
		String rulesheet = rulesPath + "/" + rulesFilename;
		
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

		String reportsFolder = "pmd-reports_v" + fileCount;
		File reportsPath = new File(workingDirPath + "/" + reportsFolder);
		int folderCount = 0;
		while (reportsPath.exists()) {
			reportsFolder = reportsFolder + "-" + (++folderCount);
			reportsPath = new File(workingDirPath + "/" + reportsFolder);
		}
		reportsPath.mkdirs();

		String cmdTemplate = StringUtils.replace(CMD_TEMPLATE, "WORKING_DIR_PATH", workingDirPath);
		cmdTemplate = StringUtils.replace(cmdTemplate, "PMD_RULES_PATH", rulesheet);
		cmdTemplate = StringUtils.replace(cmdTemplate, "REPORTS_FOLDER", reportsFolder);
		cmdTemplate = StringUtils.replace(cmdTemplate, "VERSION", fileCount.toString());

		String tmplBefore = StringUtils.replace(cmdTemplate, "STAGE", "Before");
		String tmplAfter = StringUtils.replace(cmdTemplate, "STAGE", "After");

		System.out.println("Reading files...");
		List<String> resultContent = new ArrayList<String>();
		for (String f : lines) {
			String cName = FilenameUtils.getBaseName(f);
			System.out.println("- " + cName + ".java");

			String cmdBefore = StringUtils.replace(tmplBefore, "SRC_FILE", f);
			cmdBefore = StringUtils.replace(cmdBefore, "CLASS_NAME", cName);

			String cmdAfter = StringUtils.replace(tmplAfter, "SRC_FILE", f);
			cmdAfter = StringUtils.replace(cmdAfter, "CLASS_NAME", cName);

			resultContent.add("SET JAVA_HOME=\""+JAVA_PATH+"\"");
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
		System.out.println("Generating summary file...");
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
		
		String commentsFilename = "_jira_comments_" + reportSettings.getJiraTicket() + "-v" + reportSettings.getVersion() + ".txt";
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
