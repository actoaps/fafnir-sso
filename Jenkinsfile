pipeline {

    agent {
        label 'master'
    }

    tools {
        jdk 'jdk9'
    }

    stages {
        stage("gradle build") {
            steps {
                sh './gradlew build'
                junit allowEmptyResults: true, testResults: '/target/surefire-reports/**/*.xml'

            }
        }

        stage ("sonar analysis") {
            steps {
                withSonarQubeEnv('Sonar') {
                    sh "${tool 'SonarScanner'}/bin/sonar-scanner"
                }
            }
        }

        stage("docker ") {
            steps {
                script {
                    docker.withRegistry('https://registry.hub.docker.com', 'docker-hub-credentials') {
                        def temp = docker.build('actoaps/oauth-sso', './auth')
                        temp.push('1.0.${BUILD_NUMBER}')
                        temp.push('latest')
                    }
                }
            }
        }
    }
}