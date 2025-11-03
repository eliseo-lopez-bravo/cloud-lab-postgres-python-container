pipeline {
  agent any

  environment {
    TERRAFORM_VERSION = '1.13.4'
    HELM_VERSION = '3.16.0'
    K8S_VERSION = '1.30.2'
    BIN_DIR = "${WORKSPACE}/bin"
    PATH = "${BIN_DIR}:${env.PATH}"

    # OCI and Terraform variables
    TF_VAR_region          = 'us-sanjose-1'
    TF_VAR_tenancy_ocid    = 'ocid1.tenancy.oc1..aaaaaaaad4ipuawmwb2miwp3bosu6i6ufmkkbvh572ceabiesraziz6zhb7q'
    TF_VAR_user_ocid       = 'ocid1.user.oc1..aaaaaaaaz7uge6jsrevrqjdlyn7srl77oganptjtf7mft75l7gxb4spaibma'
    TF_VAR_fingerprint     = '6f:a7:6e:df:52:3d:29:ca:1d:2b:61:00:c2:ef:42:d1'
    # optional if you use compartments
    # TF_VAR_compartment_ocid = 'ocid1.compartment.oc1..xxxxxx'
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
          echo "🔧 Setting up lab environment..."
          def deploy = load "jenkins/deploy-pipeline.groovy"
          deploy.setupLab(env.TERRAFORM_VERSION, env.HELM_VERSION, env.K8S_VERSION)
        }
      }
    }

    stage('Terraform Init') {
      steps {
        withCredentials([file(credentialsId: 'oci-private-key', variable: 'OCI_PRIVATE_KEY_PATH')]) {
          script {
            echo "🚀 Initializing Terraform..."
            sh '''
              cd terraform
              export TF_VAR_private_key_path="${OCI_PRIVATE_KEY_PATH}"
              terraform init -input=false
            '''
          }
        }
      }
    }

    stage('Terraform Plan') {
      steps {
        withCredentials([file(credentialsId: 'oci-private-key', variable: 'OCI_PRIVATE_KEY_PATH')]) {
          script {
            echo "📜 Running Terraform plan..."
            sh '''
              cd terraform
              export TF_VAR_private_key_path="${OCI_PRIVATE_KEY_PATH}"
              terraform plan -out=tfplan -input=false
            '''
          }
        }
      }
    }

    stage('Terraform Apply') {
      steps {
        withCredentials([file(credentialsId: 'oci-private-key', variable: 'OCI_PRIVATE_KEY_PATH')]) {
          script {
            echo "🧱 Applying Terraform..."
            sh '''
              cd terraform
              export TF_VAR_private_key_path="${OCI_PRIVATE_KEY_PATH}"
              terraform apply -input=false -auto-approve tfplan
            '''
          }
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
      script {
        node {
          echo "🧹 Cleaning up temporary files..."
          sh 'rm -rf /tmp/terraform.zip /tmp/helm.tar.gz /tmp/linux-* /tmp/linux-arm64 || true'
        }
      }
    }
    failure {
      echo "❌ Something went wrong during Lab setup."
    }
  }
}
