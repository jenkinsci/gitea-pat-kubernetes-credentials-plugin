// buildPlugin()
// Replace with above once finalized
podTemplate(yaml: '''
apiVersion: v1
kind: Pod
spec:
  containers:
  - name: maven
    image: maven:3.8.4-jdk-8
    command:
    - sleep
    args:
    - infinity
''') {
    node(POD_LABEL) {
        container('maven') {
            checkout scm
            script {
                if (env.TAG_NAME) {
                    echo env.TAG_NAME
                    sh 'mvn -B -ntp -Dmaven.test.failure.ignore -Drevision=${TAG_NAME} -Dchangelist= clean package'
                } else {
                    sh 'mvn -B -ntp -Dmaven.test.failure.ignore package'
                }
            }
        }
        junit '**/target/surefire-reports/TEST-*.xml'
        archiveArtifacts '**/target/*.hpi'
    }
}
