// buildPlugin()
// Replace with above once finalized
pipeline {
    agent {
        kubernetes {
            yaml '''
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
            '''
            defaultContainer 'maven'
        }
    }
    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }
        stage('Build') {
            steps {
                script {
                    if (env.TAG_NAME) {
                        echo env.TAG_NAME
                        sh 'mvn -B -ntp -Dmaven.test.failure.ignore -Drevision=${TAG_NAME} -Dchangelist= clean package'
                    } else {
                        sh 'mvn -B -ntp -Dmaven.test.failure.ignore package'
                    }
                }
            }
        }
    }
    post {
        always {
            junit '**/target/surefire-reports/TEST-*.xml'
        }
        success {
            archiveArtifacts '**/target/*.hpi'
        }
    }
}
