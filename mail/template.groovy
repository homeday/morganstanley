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
