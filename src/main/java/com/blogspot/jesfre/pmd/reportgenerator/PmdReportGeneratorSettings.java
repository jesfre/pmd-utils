package com.blogspot.jesfre.pmd.reportgenerator;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author <a href="mailto:jorge.ruiz.aquino@gmail.com">Jorge Ruiz Aquino</a>
 *         Aug 6, 2022
 */
public class PmdReportGeneratorSettings {

	private String configFile;
	private String repositoryBaseUrl;
	private String repositoryWorkingBranch;
	private boolean verbose;
	private String summaryTemplate;
	private String pmdRulesFile;
	private String javaHome;
	private String pmdHome;
	private String project;
	private String jiraTicket;
	private String version;
	private String workingDirPath;
	private List<String> classFileLocationList = new ArrayList<String>();
	private String commandFile;
	private String reportOutputLocation;

	public String getConfigFile() {
		return configFile;
	}

	public void setConfigFile(String configFile) {
		this.configFile = configFile;
	}

	public String getRepositoryBaseUrl() {
		return repositoryBaseUrl;
	}

	public void setRepositoryBaseUrl(String repositoryBaseUrl) {
		this.repositoryBaseUrl = repositoryBaseUrl;
	}

	public String getRepositoryWorkingBranch() {
		return repositoryWorkingBranch;
	}

	public void setRepositoryWorkingBranch(String repositoryWorkingBranch) {
		this.repositoryWorkingBranch = repositoryWorkingBranch;
	}

	public boolean isVerbose() {
		return verbose;
	}

	public void setVerbose(boolean verbose) {
		this.verbose = verbose;
	}

	public String getSummaryTemplate() {
		return summaryTemplate;
	}

	public void setSummaryTemplate(String summaryTemplate) {
		this.summaryTemplate = summaryTemplate;
	}

	public String getPmdRulesFile() {
		return pmdRulesFile;
	}

	public void setPmdRulesFile(String pmdRulesFile) {
		this.pmdRulesFile = pmdRulesFile;
	}

	public String getJavaHome() {
		return javaHome;
	}

	public void setJavaHome(String javaHome) {
		this.javaHome = javaHome;
	}

	public String getPmdHome() {
		return pmdHome;
	}

	public void setPmdHome(String pmdHome) {
		this.pmdHome = pmdHome;
	}

	public String getProject() {
		return project;
	}

	public void setProject(String project) {
		this.project = project;
	}

	public String getJiraTicket() {
		return jiraTicket;
	}

	public void setJiraTicket(String jiraTicket) {
		this.jiraTicket = jiraTicket;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public String getWorkingDirPath() {
		return workingDirPath;
	}

	public void setWorkingDirPath(String workingDirPath) {
		this.workingDirPath = workingDirPath;
	}

	public List<String> getClassFileLocationList() {
		return classFileLocationList;
	}

	public String getCommandFile() {
		return commandFile;
	}

	public void setCommandFile(String commandFile) {
		this.commandFile = commandFile;
	}

	public String getReportOutputLocation() {
		return reportOutputLocation;
	}

	public void setReportOutputLocation(String reportOutputLocation) {
		this.reportOutputLocation = reportOutputLocation;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("PmdReportGeneratorSettings [configFile=");
		builder.append(configFile);
		builder.append(", repositoryBaseUrl=");
		builder.append(repositoryBaseUrl);
		builder.append(", repositoryWorkingBranch=");
		builder.append(repositoryWorkingBranch);
		builder.append(", verbose=");
		builder.append(verbose);
		builder.append(", summaryTemplate=");
		builder.append(summaryTemplate);
		builder.append(", pmdRulesFile=");
		builder.append(pmdRulesFile);
		builder.append(", javaHome=");
		builder.append(javaHome);
		builder.append(", pmdHome=");
		builder.append(pmdHome);
		builder.append(", project=");
		builder.append(project);
		builder.append(", jiraTicket=");
		builder.append(jiraTicket);
		builder.append(", version=");
		builder.append(version);
		builder.append(", workingDirPath=");
		builder.append(workingDirPath);
		builder.append(", classFileLocationList=");
		builder.append(classFileLocationList);
		builder.append(", commandFile=");
		builder.append(commandFile);
		builder.append(", reportOutputLocation=");
		builder.append(reportOutputLocation);
		builder.append("]");
		return builder.toString();
	}

}