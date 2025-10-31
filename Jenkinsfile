pipeline {
  agent any

  environment {
    TERRAFORM_VERSION = '1.13.4'
    HELM_VERSION = '3.16.0'
    K8S_VERSION = '1.30.2'
    BIN_DIR = "${WORKSPACE}/bin"
    PATH = "${BIN_DIR}:${env.PATH}"

    OCI_TENANCY_OCID    = credentials('oci_tenancy_ocid')
    OCI_USER_OCID       = credentials('oci_user_ocid')
    OCI_FINGERPRINT     = credentials('oci_fingerprint')
    OCI_PRIVATE_KEY_PATH = credentials('oci_private_key_path')
    OCI_REGION          = 'us-ashburn-1'
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
