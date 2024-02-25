package com.blogspot.jesfre.batchjob.setup.pmd.pmdgeneration;

import java.util.ArrayList;
import java.util.List;

/**
 * @TODO MErge with {CodeDiffGeneratorSettings}
 * 
 * @author <a href="mailto:jorge.ruiz.aquino@gmail.com">Jorge Ruiz Aquino</a>
 *         Aug 6, 2022
 */
public class PmdReportGeneratorSettings {

	private String project;
	private String jiraTicket;
	private String version;
	private String workingDirPath;
	private List<String> classFileLocationList = new ArrayList<String>();
	private String commandFile;
	private String reportOutputLocation;

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

}
