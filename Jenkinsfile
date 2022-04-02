// buildPlugin()
// Replace with above once finalized
podTemplate(yaml: '''
apiVersion: v1
kind: Pod
spec:
  containers:
  - name: maven
    image: maven:3.6.3-jdk-8
    command:
    - sleep
    args:
    - infinity
''') {
    node(POD_LABEL) {
        container('maven') {
            checkout scm
            sh 'mvn -B -ntp -Dmaven.test.failure.ignore package'
        }
//         junit '**/target/surefire-reports/TEST-*.xml'
        archiveArtifacts '**/target/*.hpi'
    }
}
