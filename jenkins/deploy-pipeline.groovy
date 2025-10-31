pipeline {
    agent any

    environment {
        TF_VERSION = "1.8.5"  // you can adjust version
        TF_BIN = "/usr/local/bin/terraform"
    }

    stages {
        stage('Lab Setup') {
            steps {
                script {
                    echo "=== Checking Terraform installation ==="
                    sh '''
                    if ! command -v terraform >/dev/null 2>&1; then
                        echo "Terraform not found — installing..."
                        sudo yum install -y unzip curl || sudo apt-get install -y unzip curl
                        curl -fsSL https://releases.hashicorp.com/terraform/${TF_VERSION}/terraform_${TF_VERSION}_linux_amd64.zip -o /tmp/terraform.zip
                        sudo unzip -o /tmp/terraform.zip -d /usr/local/bin
                        rm -f /tmp/terraform.zip
                        terraform version
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
                // Add your kubectl apply -f manifest.yaml or helm commands here
            }
        }
    }
}
