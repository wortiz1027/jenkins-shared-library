#!/usr/bin/env groovy

// Agradecimientos :: https://danielschaaff.com/2018/02/09/better-jenkins-notifications-in-declarative-pipelines.html?utm_source=pocket_mylist

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import hudson.tasks.test.AbstractTestResultAction;
import hudson.model.Actionable;

def call(String buildStatus = 'STARTED', String channel = '#jenkins') {

  buildStatus = buildStatus ?: 'SUCCESSFUL'
  channel = channel ?: '#jenkins'
  
  def colorName  = 'RED'
  def colorCode  = '#FF0000'
  def subject    = "${buildStatus}: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}] (<${env.RUN_DISPLAY_URL}|Open>) (<${env.RUN_CHANGES_DISPLAY_URL}|  Changes>)'"
  def title      = "${env.JOB_NAME} Build: ${env.BUILD_NUMBER}"
  def title_link = "${env.RUN_DISPLAY_URL}"
  def branchName = "${env.BRANCH_NAME}"
  
  def commit = sh(returnStdout: true, script: 'git rev-parse HEAD')
  def author = sh(returnStdout: true, script: "git --no-pager show -s --format='%an'").trim()

  def message = sh(returnStdout: true, script: 'git log -1 --pretty=%B').trim()
  
  def failedTestsString = "```"
  
  if (buildStatus == 'STARTED') {
    color = 'YELLOW'
    colorCode = '#FFFF00'
  } else if (buildStatus == 'SUCCESSFUL') {
    color = 'GREEN'
    colorCode = 'good'
  } else if (buildStatus == 'UNSTABLE') {
    color = 'YELLOW'
    colorCode = 'warning'
  } else {
    color = 'RED'
    colorCode = 'danger'
  }
  
  @NonCPS
  def getTestSummary = { ->
    def testResultAction = currentBuild.rawBuild.getAction(AbstractTestResultAction.class)
    def summary = ""

    if (testResultAction != null) {
        def total = testResultAction.getTotalCount()
        def failed = testResultAction.getFailCount()
        def skipped = testResultAction.getSkipCount()

        summary = "Test results:\n\t"
        summary = summary + ("Passed: " + (total - failed - skipped))
        summary = summary + (", Failed: " + failed + " ${testResultAction.failureDiffString}")
        summary = summary + (", Skipped: " + skipped)
    } else {
        summary = "No tests found"
    }
    
    return summary
  }
  
  @NonCPS
  def getFailedTests = { ->
    def testResultAction = currentBuild.rawBuild.getAction(AbstractTestResultAction.class)    

    if (testResultAction != null) {
        def failedTests = testResultAction.getFailedTests()

        if (failedTests.size() > 9) {
            failedTests = failedTests.subList(0, 8)
        }

        for(CaseResult cr : failedTests) {
            failedTestsString = failedTestsString + "${cr.getFullDisplayName()}:\n${cr.getErrorDetails()}\n\n"
        }
        failedTestsString = failedTestsString + "```"
    }
    return failedTestsString
}
  
  def testSummaryRaw = getTestSummary()  
  def testSummary = "`${testSummaryRaw}`"
  
  slackSend (color: colorCode, message: subject, attachments: [
                                            [
                                                title       : title.toString(),
                                                title_link  : "${env.BUILD_URL}",
                                                color       : "${colorCode}",
                                                text        : subject.toString(),                                                
                                                "mrkdwn_in" : [
                                                              "fields"
                                                ],
                                                fields: [
                                                          [
                                                           title: "Branch",
                                                           value: branchName.toString(),
                                                           short: true
                                                          ],
                                                          [
                                                           title: "Author",
                                                           value: author.toString(),
                                                           short: true
                                                          ],                                                          
                                                          [
                                                           title: "Commit Message",
                                                           value: message.toString(),
                                                           short: false
                                                          ],
                                                          [
                                                           title: "Test Summary",
                                                           value: testSummary.toString(),
                                                           short: false
                                                          ]
                                                ]
                                            ],
                                            [
                                              title: "Failed Tests",
                                              color: "${colorCode}",
                                              text: "${failedTestsString}",
                                              "mrkdwn_in": [
                                                            "text"
                                                           ],                                                
                                            ]
                                         ], channel: channel)
  
}
