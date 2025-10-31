pipeline {
    agent any

    environment {
        TF_VERSION = "1.10.4"
        TF_DIR = "${env.WORKSPACE}/bin"
        PATH = "${env.TF_DIR}:${env.PATH}"
    }

    stages {
        stage('Lab Setup') {
            steps {
                script {
                    echo "=== Checking Terraform installation ==="
                    sh '''
                    mkdir -p ${TF_DIR}
                    if ! command -v terraform >/dev/null 2>&1; then
                        echo "Terraform not found — installing locally..."
                        curl -fsSL https://releases.hashicorp.com/terraform/${TF_VERSION}/terraform_${TF_VERSION}_linux_amd64.zip -o /tmp/terraform.zip
                        unzip -o /tmp/terraform.zip -d ${TF_DIR}
                        rm -f /tmp/terraform.zip
                        chmod +x ${TF_DIR}/terraform
                        ${TF_DIR}/terraform version
                    else
                        echo "Terraform is already installed:"
                        terraform version
                    fi
                    '''
                }
            }
        }

        stage('Init Terraform') {
            steps {
                dir('infra') {
                    sh 'terraform init -input=false'
                }
            }
        }

        stage('Apply Terraform') {
            steps {
                dir('infra') {
                    sh 'terraform apply -auto-approve -input=false'
                }
            }
        }

        stage('Deploy Postgres (k8s manifest)') {
            when {
                expression { currentBuild.result == null || currentBuild.result == 'SUCCESS' }
            }
            steps {
                sh 'echo "Deploying PostgreSQL to Kubernetes..."'
                // Add your kubectl or helm commands here
            }
        }
    }
}
