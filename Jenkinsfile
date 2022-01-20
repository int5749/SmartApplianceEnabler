library identifier: 'icheko-jenkins-shared-lib@master',
        retriever: modernSCM([
            $class: 'GitSCMSource',
            id: '13ebda5f-2be5-4751-83d4-4d4500603cc5',
            remote: 'https://github.com/icheko/jenkins-shared-lib',
            traits: [[$class: 'jenkins.plugins.git.traits.BranchDiscoveryTrait']]
        ]) _

pipeline {
    agent any

    tools {
        maven "Maven"
    }

    stages {
        stage('Build') {
            steps {
                script {
                    env.IMAGE_VERSION = readCommitVar('version')
                    println env.IMAGE_VERSION
                }
                sh "echo Version=$IMAGE_VERSION"
            }
        }
        /*stage('Build') {
            steps {
                cleanWs()
                git 'https://github.com/camueller/SmartApplianceEnabler.git'
                sh "mvn package -B -Pweb"
            }
        }
        stage('Deploy') {
            steps {
                dir('docker') {
                    sh "cp ../target/SmartApplianceEnabler*.war sae-ci/SmartApplianceEnabler.war"
                    sh "docker build --tag=avanux/smartapplianceenabler-amd64:ci ./sae-ci"
                }
            }
        }
        stage('Chrome') {
            environment {
                BROWSERSTACK_USERNAME = credentials('BROWSERSTACK_USERNAME')
                BROWSERSTACK_ACCESS_KEY = credentials('BROWSERSTACK_ACCESS_KEY')
            }
            steps {
                sh "docker stop sae"
                sh "docker volume rm -f sae"
                sh "docker volume create sae"
                sh "docker run -d --rm -v sae:/opt/sae/data -p 8081:8080 --name sae avanux/smartapplianceenabler-amd64:ci"
                dir('src/test/angular') {
                    sh "npm i"
                    sh "npm run test:chrome"
                }
            }
        }
        stage('Firefox') {
            environment {
                BROWSERSTACK_USERNAME = credentials('BROWSERSTACK_USERNAME')
                BROWSERSTACK_ACCESS_KEY = credentials('BROWSERSTACK_ACCESS_KEY')
            }
            steps {
                sh "docker stop sae"
                sh "docker volume rm -f sae"
                sh "docker volume create sae"
                sh "docker run -d --rm -v sae:/opt/sae/data -p 8081:8080 --name sae avanux/smartapplianceenabler-amd64:ci"
                dir('src/test/angular') {
                    sh "npm run test:fixefox"
                }
            }
        }
        stage('Safari') {
            environment {
                BROWSERSTACK_USERNAME = credentials('BROWSERSTACK_USERNAME')
                BROWSERSTACK_ACCESS_KEY = credentials('BROWSERSTACK_ACCESS_KEY')
            }
            steps {
                sh "docker stop sae"
                sh "docker volume rm -f sae"
                sh "docker volume create sae"
                sh "docker run -d --rm -v sae:/opt/sae/data -p 8081:8080 --name sae avanux/smartapplianceenabler-amd64:ci"
                dir('src/test/angular') {
                    sh "npm run test:safari"
                }
            }
        }
        stage('Publish') {
            steps {
                withCredentials([usernamePassword(credentialsId: 'docker', usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')]) {
                    sh "echo $PASSWORD | docker login --username $USERNAME --password-stdin"
                    dir('docker') {
                        sh "docker push avanux/smartapplianceenabler-amd64:ci"
                    }
                }
            }
        }*/
    }
}
