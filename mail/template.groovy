import jenkins.model.*
import hudson.model.*

// These are available from the binding:
def build = binding.getVariable("build")
def project = build.getProject()
def rootUrl = jenkins.model.Jenkins.instance.getRootUrl()

return """
<html>
  <body>
    <h2>Build Report</h2>
    <p>Job: ${project.name}</p>
    <p>Build: <a href="${rootUrl}${build.url}">${build.displayName}</a></p>
    <p>Status: ${build.result}</p>
  </body>
</html>
"""



def emailContent() {
  return '''
  -------------------------------------------------------------------------------
  Build ${ENV,var="JOB_NAME"} #${BUILD_NUMBER} ${BUILD_STATUS}
  URL: ${BUILD_URL}
  -------------------------------------------------------------------------------
  Changes:
  ${CHANGES}
  -------------------------------------------------------------------------------
  Failed Tests:
  ${FAILED_TESTS,maxTests=500,showMessage=false,showStack=false}
  -------------------------------------------------------------------------------
  For complete test report and logs see https://nightlies.apache.org/cassandra/${JOB_NAME}/${BUILD_NUMBER}/
  '''
}
