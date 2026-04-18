pipeline {
    agent any

    environment {
        SSH_CREDENTIALS = 'droplet-ssh'
        DEPLOY_USER     = 'root'
        DEPLOY_HOST     = '139.162.31.27'
        APP_DIR         = '/home/ubuntu/apps/backend'
        APP_NAME        = 'backend'
    }

    stages {
        stage('📥 Checkout Code') {
            steps {
                echo "===> Checking out code from GitHub repository"
                checkout([
                    $class: 'GitSCM',
                    branches: [[name: '*/main']],
                    userRemoteConfigs: [[
                        url: 'git@github.com:podalbinh/Class-System-BE.git',
                        credentialsId: 'GITHUB_CREDENTIALS'  // nếu private repo
                    ]],
                    extensions: [
                        [$class: 'WipeWorkspace'],        // xoá sạch workspace trước
                        [$class: 'CleanBeforeCheckout']   // git clean -fdx (xoá file không track)
                    ]
                ])
                echo "✅ Code checkout completed"
            }
        }

        stage('Deploy') {
            steps {
                withCredentials([usernamePassword(credentialsId: "${SSH_CREDENTIALS}", usernameVariable: 'SSH_USER', passwordVariable: 'SSH_PASS')]) {
                    sh """
                        echo "===> Copy files to server"

                        sshpass -p "$SSH_PASS" scp -r -o StrictHostKeyChecking=no . root@${DEPLOY_HOST}:${APP_DIR}/
                        sshpass -p "$SSH_PASS" ssh -o StrictHostKeyChecking=no root@${DEPLOY_HOST} "cd ${APP_DIR} && docker run --rm -v \$(pwd):/app -w /app maven:3.9.6-eclipse-temurin-17 mvn clean package -DskipTests"

                        echo "===> Deploy with single command (includes Maven build)"
                        sshpass -p "$SSH_PASS" ssh -o StrictHostKeyChecking=no root@${DEPLOY_HOST} "cd ${APP_DIR} && docker compose up -d --build" """
                }
            }
        }
    }
    post {
        always {
            cleanWs(deleteDirs: true, disableDeferredWipeout: false)
        }
    }
}
