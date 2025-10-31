pipeline {
  agent any

  environment {
    TERRAFORM_VERSION = '1.10.4'
    K8S_VERSION = '1.30.2'
    HELM_VERSION = '3.16.0'
    BIN_DIR = "${WORKSPACE}/bin"
    PATH = "${BIN_DIR}:${env.PATH}"
  }

  stages {
    stage('Checkout') {
      steps {
        checkout scm
      }
    }

    stage('Lab Setup') {
      steps {
        script {
          echo "🔧 Starting lab environment setup..."
          def deploy = load "jenkins/deploy-pipeline.groovy"
          deploy.setupLab(env.TERRAFORM_VERSION, env.HELM_VERSION, env.K8S_VERSION)
        }
      }
    }

    stage('Terraform Init') {
      steps {
        script {
          def deploy = load "jenkins/deploy-pipeline.groovy"
          deploy.initTerraform()
        }
      }
    }

    stage('Terraform Plan') {
      steps {
        script {
          def deploy = load "jenkins/deploy-pipeline.groovy"
          deploy.planTerraform()
        }
      }
    }

    stage('Terraform Apply') {
      steps {
        script {
          def deploy = load "jenkins/deploy-pipeline.groovy"
          deploy.applyTerraform()
        }
      }
    }

    stage('Verify Setup') {
      steps {
        script {
          def deploy = load "jenkins/deploy-pipeline.groovy"
          deploy.verifySetup()
        }
      }
    }
  }

  post {
    always {
      echo "🧹 Cleaning up temporary files..."
      sh 'rm -rf /tmp/terraform.zip /tmp/helm.tar.gz /tmp/linux-* || true'
    }
    failure {
      echo "❌ Something went wrong during Lab setup."
    }
  }
}
