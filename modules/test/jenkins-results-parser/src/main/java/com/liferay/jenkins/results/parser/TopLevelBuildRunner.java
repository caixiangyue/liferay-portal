/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.jenkins.results.parser;

import java.io.File;
import java.io.IOException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeoutException;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;

import org.dom4j.Element;

/**
 * @author Michael Hashimoto
 */
public abstract class TopLevelBuildRunner<T extends TopLevelBuildData>
	extends BaseBuildRunner<T> {

	@Override
	public void run() {
		updateBuildDescription();

		setUpWorkspace();

		propagateDistFilesToDistNodes();

		invokeBatchJobs();

		waitForInvokedJobs();

		createJenkinsReport();
	}

	protected TopLevelBuildRunner(T topLevelBuildData) {
		super(topLevelBuildData);

		Build build = BuildFactory.newBuild(
			topLevelBuildData.getBuildURL(), null);

		if (!(build instanceof TopLevelBuild)) {
			throw new RuntimeException(
				"Invalid build URL " + topLevelBuildData.getBuildURL());
		}

		_topLevelBuild = (TopLevelBuild)build;
	}

	protected void createJenkinsReport() {
		Element jenkinsReportElement = _topLevelBuild.getJenkinsReportElement();

		try {
			BuildData buildData = getBuildData();

			String jenkinsReportString = StringEscapeUtils.unescapeXml(
				Dom4JUtil.format(jenkinsReportElement, true));

			File jenkinsReportFile = new File(
				buildData.getWorkspaceDir(), "jenkins-report.html");

			JenkinsResultsParserUtil.write(
				jenkinsReportFile, jenkinsReportString);

			if (!JenkinsResultsParserUtil.isCINode()) {
				return;
			}

			String userContentRelativePath =
				buildData.getUserContentRelativePath();

			userContentRelativePath = userContentRelativePath.replace(
				")", "\\)");
			userContentRelativePath = userContentRelativePath.replace(
				"(", "\\(");

			try {
				String command = JenkinsResultsParserUtil.combine(
					"ssh -o NumberOfPasswordPrompts=0 ",
					buildData.getMasterHostname(),
					" 'mkdir -p /opt/java/jenkins/userContent/",
					userContentRelativePath, "'");

				JenkinsResultsParserUtil.executeBashCommands(command);
			}
			catch (IOException | TimeoutException e) {
				throw new RuntimeException(e);
			}

			int maxRetries = 3;
			int retries = 0;

			while (retries < maxRetries) {
				try {
					retries++;

					String command = JenkinsResultsParserUtil.combine(
						"time rsync -sqI --chmod=go=rx --timeout=1200 ",
						jenkinsReportFile.getCanonicalPath(), " ",
						buildData.getMasterHostname(), "::usercontent/",
						userContentRelativePath);

					JenkinsResultsParserUtil.executeBashCommands(command);

					break;
				}
				catch (IOException | TimeoutException e) {
					if (retries == maxRetries) {
						throw new RuntimeException(
							"Unable to send the jenkins-report.html", e);
					}

					System.out.println(
						"Unable to execute bash commands, retrying... ");

					e.printStackTrace();

					JenkinsResultsParserUtil.sleep(3000);
				}
			}
		}
		catch (IOException ioe) {
			throw new RuntimeException(ioe);
		}
	}

	protected Set<String> getBatchNames() {
		Job job = getJob();

		return job.getBatchNames();
	}

	protected String[] getDistFileNames() {
		return new String[] {BuildDatabase.BUILD_DATABASE_FILE_NAME};
	}

	protected void invokeBatchJob(String batchName) {
		BuildData buildData = getBuildData();

		Map<String, String> invocationParameters = new HashMap<>();

		invocationParameters.put("BATCH_NAME", batchName);
		invocationParameters.put(
			"DIST_NODES", StringUtils.join(buildData.getDistNodes(), ","));
		invocationParameters.put("DIST_PATH", buildData.getDistPath());
		invocationParameters.put("JENKINS_GITHUB_URL", _getJenkinsGitHubURL());
		invocationParameters.put(
			"RUN_ID",
			"batch_" + JenkinsResultsParserUtil.getDistinctTimeStamp());
		invocationParameters.put("TOP_LEVEL_RUN_ID", buildData.getRunID());

		invokeJob(
			buildData.getCohortName(), buildData.getJobName() + "-batch",
			invocationParameters);
	}

	protected void invokeBatchJobs() {
		for (String batchName : getBatchNames()) {
			invokeBatchJob(batchName);
		}
	}

	protected void invokeJob(
		String cohortName, String jobName,
		Map<String, String> invocationParameters) {

		Properties buildProperties;

		try {
			buildProperties = JenkinsResultsParserUtil.getBuildProperties();
		}
		catch (IOException ioe) {
			throw new RuntimeException(ioe);
		}

		List<JenkinsMaster> jenkinsMasters =
			JenkinsResultsParserUtil.getJenkinsMasters(
				buildProperties, cohortName);

		String randomJenkinsURL =
			JenkinsResultsParserUtil.getMostAvailableMasterURL(
				"http://" + cohortName + ".liferay.com", jenkinsMasters.size());

		StringBuilder sb = new StringBuilder();

		sb.append(randomJenkinsURL);
		sb.append("/job/");
		sb.append(jobName);
		sb.append("/buildWithParameters?token=");
		sb.append(buildProperties.getProperty("jenkins.authentication.token"));

		for (Map.Entry<String, String> invocationParameter :
				invocationParameters.entrySet()) {

			sb.append("&");
			sb.append(
				JenkinsResultsParserUtil.fixURL(invocationParameter.getKey()));
			sb.append("=");
			sb.append(
				JenkinsResultsParserUtil.fixURL(
					invocationParameter.getValue()));
		}

		_topLevelBuild.addDownstreamBuilds(sb.toString());

		try {
			JenkinsResultsParserUtil.toString(sb.toString());
		}
		catch (IOException ioe) {
			throw new RuntimeException(ioe);
		}
	}

	protected void propagateDistFilesToDistNodes() {
		if (!JenkinsResultsParserUtil.isCINode()) {
			return;
		}

		BuildData buildData = getBuildData();

		File workspaceDir = buildData.getWorkspaceDir();

		FilePropagator filePropagator = new FilePropagator(
			getDistFileNames(),
			JenkinsResultsParserUtil.combine(
				buildData.getHostname(), ":", workspaceDir.toString()),
			buildData.getDistPath(), buildData.getDistNodes());

		filePropagator.setCleanUpCommand(_FILE_PROPAGATOR_CLEAN_UP_COMMAND);

		filePropagator.start(_FILE_PROPAGATOR_THREAD_COUNT);
	}

	protected void updateJenkinsReport() {
		if (!_allBuildsAreRunning()) {
			_lastGeneratedReportTime = -1;

			return;
		}

		long currentTimeMillis = System.currentTimeMillis();

		if (_lastGeneratedReportTime == -1) {
			_lastGeneratedReportTime = System.currentTimeMillis();

			createJenkinsReport();

			return;
		}

		if ((_lastGeneratedReportTime + _REPORT_GENERATION_INTERVAL) >
				currentTimeMillis) {

			return;
		}

		_lastGeneratedReportTime = System.currentTimeMillis();

		createJenkinsReport();
	}

	protected void waitForInvokedJobs() {
		while (true) {
			_topLevelBuild.update();

			updateJenkinsReport();

			System.out.println(_topLevelBuild.getStatusSummary());

			int completed = _topLevelBuild.getDownstreamBuildCount("completed");
			int total = _topLevelBuild.getDownstreamBuildCount(null);

			if (completed >= total) {
				break;
			}

			JenkinsResultsParserUtil.sleep(
				_WAIT_FOR_INVOKED_JOB_DURATION * 1000);
		}
	}

	private boolean _allBuildsAreRunning() {
		List<Build> runningBuilds = new ArrayList<>();

		runningBuilds.addAll(_topLevelBuild.getDownstreamBuilds("running"));
		runningBuilds.addAll(_topLevelBuild.getDownstreamBuilds("completed"));

		List<Build> totalBuilds = _topLevelBuild.getDownstreamBuilds(null);

		if (runningBuilds.size() >= totalBuilds.size()) {
			return true;
		}

		return false;
	}

	private String _getJenkinsGitHubURL() {
		if (JenkinsResultsParserUtil.isCINode()) {
			WorkspaceGitRepository jenkinsWorkspaceGitRepository =
				workspace.getJenkinsWorkspaceGitRepository();

			String gitHubDevBranchName =
				jenkinsWorkspaceGitRepository.getGitHubDevBranchName();

			if (gitHubDevBranchName != null) {
				return JenkinsResultsParserUtil.combine(
					"https://github-dev.liferay.com/liferay/",
					"liferay-jenkins-ee/tree/", gitHubDevBranchName);
			}
		}

		BuildData buildData = getBuildData();

		return buildData.getJenkinsGitHubURL();
	}

	private static final String _FILE_PROPAGATOR_CLEAN_UP_COMMAND =
		JenkinsResultsParserUtil.combine(
			"find ", BuildData.DIST_ROOT_PATH,
			"/*/* -maxdepth 1 -type d -mmin +",
			String.valueOf(TopLevelBuildRunner._FILE_PROPAGATOR_EXPIRATION),
			" -exec rm -frv {} \\;");

	private static final int _FILE_PROPAGATOR_EXPIRATION = 180;

	private static final int _FILE_PROPAGATOR_THREAD_COUNT = 1;

	private static final long _REPORT_GENERATION_INTERVAL = 1000 * 60 * 5;

	private static final int _WAIT_FOR_INVOKED_JOB_DURATION = 30;

	private long _lastGeneratedReportTime = -1;
	private final TopLevelBuild _topLevelBuild;

}