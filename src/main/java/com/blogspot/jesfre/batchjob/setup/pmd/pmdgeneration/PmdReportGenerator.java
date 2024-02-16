package com.blogspot.jesfre.batchjob.setup.pmd.pmdgeneration;

import static com.blogspot.jesfre.velocity.utils.VelocityTemplateProcessor.getProcessor;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;

import com.blogspot.jesfre.commandline.CommandLineRunner;
import com.blogspot.jesfre.velocity.utils.VelocityTemplateProcessor;

public class PmdReportGenerator {
	// ----------- SET -----------------------
	private static final String SETUP_FILE = "C:/path/tosettings/pmd/pmdgeneration/PmdReportGenerator_setup.txt";
	private static final String PMD_RULESSHEET_PROJECT1 = "C:/path/to/setup/pmd/pmdgeneration/ruleset-PMDrules.xml";
	private static final String PMD_RULESSHEET_PROJECT2 = "C:/path/to/setup/pmd/pmdgeneration/ruleset-PMDrules-project2.XML";
	private static final String JAVA_PATH = "C:\\Program Files\\Java\\jdk1.7.0_80";
	private static final String PMD_ROOT = "C:\\dev\\pmd-bin-5.5.1";

	// ---------------------------------------
	private static final String CMD_TEMPLATE = "call " + PMD_ROOT + "\\bin\\pmd -d \"SRC_FILE\" -R \"PMD_RULES_PATH\" -f csv -r \"WORKING_DIR_PATH/REPORTS_FOLDER/CLASS_NAME_PMD_Issues_STAGE_Code_Fix_vVERSION.csv\"";
	private static final String ECHO = "echo on";

	public static void main(String[] args) throws Exception {
		System.out.println("Generating batch file.");
		PmdReportGenerator pmdReportGenerator = new PmdReportGenerator();
		PmdReportGeneratorSettings reportSettings = pmdReportGenerator.generatePmdCommandFile();
		pmdReportGenerator.generateFileListFile(reportSettings);
		pmdReportGenerator.generateCommentsFile(reportSettings, "comments-template.txt");

		System.out.println("\nExecuting batch file.");
		CommandLineRunner runner = new CommandLineRunner();
		runner.run(reportSettings.getCommandFile());

		System.out.println("\nUpdating reports.");
		PmdReportUpdater updater = new PmdReportUpdater();
		updater.updateAfterFixFiles(reportSettings.getReportOutputLocation());

		System.out.println("\nDone.");
	}

	private PmdReportGeneratorSettings generatePmdCommandFile() throws Exception {
		List<String> lines = FileUtils.readLines(new File(SETUP_FILE));
		String project = "IES"; // IES or ABE
		String jiraTicket = "";
		String version = null;
		String workingDirPath = "";
		int startingLine = 0;
		if (lines.size() > 0) {
			project = StringUtils.remove(lines.get(startingLine++), "PROJECT:");
			jiraTicket = StringUtils.remove(lines.get(startingLine++), "JIRA_TICKET:");
			version = StringUtils.remove(lines.get(startingLine++), "VERSION:");
			workingDirPath = lines.get(startingLine++);
		} else {
			throw new Exception("Setup file is empty.");
		}

		String rulesheet = PMD_RULESSHEET_PROJECT1;
		if ("ABE".equals(project)) {
			rulesheet = PMD_RULESSHEET_PROJECT2;
		}

		// New batch file-name definition
		Integer fileCount = 1;
		String newFileName = workingDirPath + "/pmd_commands_" + fileCount + ".bat";
		File newFile = new File(newFileName);
		if (StringUtils.isBlank(version)) {
			while (newFile.exists()) {
				fileCount++;
				newFileName = workingDirPath + "/pmd_commands_" + fileCount + ".bat";
				newFile = new File(newFileName);
			}
		} else {
			fileCount = Integer.valueOf(version);
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
		List<String> filePathList = new ArrayList<String>();
		int c = 0;
		for (String f : lines) {
			if (c < startingLine || StringUtils.isEmpty(f)) {
				// Skip setup lines
				c++;
				continue;
			}
			if (f.startsWith("#") || f.startsWith("//")) {
				continue;
			}
			String cName = FilenameUtils.getBaseName(f);
			System.out.println("- " + cName + ".java");

			String cmdBefore = StringUtils.replace(tmplBefore, "SRC_FILE", f);
			cmdBefore = StringUtils.replace(cmdBefore, "CLASS_NAME", cName);

			String cmdAfter = StringUtils.replace(tmplAfter, "SRC_FILE", f);
			cmdAfter = StringUtils.replace(cmdAfter, "CLASS_NAME", cName);

			// System.out.println(cmdBefore);
			// System.out.println(cmdAfter);

			resultContent.add("SET JAVA_HOME=\""+JAVA_PATH+"\"");
			resultContent.add("SET PATH=%JAVA_HOME%\\bin;%PATH%");
			resultContent.add(ECHO);
			resultContent.add("java -version");
			resultContent.add(cmdBefore);
			resultContent.add(ECHO);
			resultContent.add(cmdAfter);
			filePathList.add(f);
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

		PmdReportGeneratorSettings settings = new PmdReportGeneratorSettings();
		settings.setProject(project);
		settings.setJiraTicket(jiraTicket);
		settings.setVersion(version);
		settings.setWorkingDirPath(workingDirPath);
		settings.setCommandFile(newFileName);
		settings.setReportOutputLocation(reportsPath.getAbsolutePath());
		settings.setClassFileLocationList(filePathList);
		return settings;
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

	private void generateCommentsFile(PmdReportGeneratorSettings reportSettings, String commentsTemplate) throws IOException {
		System.out.println("Generating comments file...");
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
		
		VelocityTemplateProcessor templateProcessor = getProcessor("C:/path/to/com/blogspot/jesfre/batchjob/setup/pmd/resources/");
		String commentsContent = templateProcessor.process(commentsTemplate, contextParams);
		FileUtils.writeStringToFile(commentsFile, commentsContent);

	}

}
