pipeline {
  agent any

  environment {
    TERRAFORM_VERSION = '1.13.4'
    HELM_VERSION      = '3.16.0'
    K8S_VERSION       = '1.30.2'
    BIN_DIR           = "${WORKSPACE}/bin"
    PATH              = "${BIN_DIR}:${env.PATH}"

  // TF_VARs — replace these in terraform/terraform.tfvars for production or use Jenkins Credentials
    TF_VAR_region          = 'us-sanjose-1'
    TF_VAR_tenancy_ocid    = 'ocid1.tenancy.oc1..REPLACE_ME'
    TF_VAR_user_ocid       = 'ocid1.user.oc1..REPLACE_ME'
    TF_VAR_fingerprint     = 'AA:BB:CC:DD:EE:FF:00:11:22:33:44:55:66:77:88:99'
  }

  stages {
    stage('Checkout') {
      steps { checkout scm }
    }

    stage('Lab Setup (Install K3s + Tools)') {
      steps {
        script {
          echo "🔧 Installing K3s (single-node) and tooling..."
          // load helper functions
          def deploy = load "jenkins/deploy-pipeline.groovy"
          deploy.setupLab(env.TERRAFORM_VERSION, env.HELM_VERSION, env.K8S_VERSION)
        }
      }
    }

    stage('Create Jenkins kubeconfig from K3s') {
      steps {
        script {
          echo "🗺️ Setting up kubeconfig for Jenkins user..."
          sh '''
            # ensure K3s is installed (install may have already run)
            if ! command -v k3s >/dev/null 2>&1; then
              curl -sfL https://get.k3s.io | INSTALL_K3S_EXEC="--disable=traefik" sh -
            fi

            # create .kube for jenkins and copy k3s kubeconfig
            sudo mkdir -p /var/lib/jenkins/.kube
            sudo cp /etc/rancher/k3s/k3s.yaml /var/lib/jenkins/.kube/config
            # make the config accessible to the jenkins user
            sudo chown -R jenkins:jenkins /var/lib/jenkins/.kube

            # adjust server IP to use node IP (if k3s default is 127.0.0.1)
            KIP=$(hostname -I | awk '{print $1}')
            if grep -q "127.0.0.1" /var/lib/jenkins/.kube/config; then
              sudo sed -i "s/127.0.0.1/${KIP}/g" /var/lib/jenkins/.kube/config
            fi

            # sanity check
            sudo -u jenkins ${BIN_DIR}/kubectl --kubeconfig /var/lib/jenkins/.kube/config get nodes || true
          '''
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
              # ensure KUBECONFIG env so TF kubernetes provider picks it up
              export KUBECONFIG=/var/lib/jenkins/.kube/config
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
              export KUBECONFIG=/var/lib/jenkins/.kube/config
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
              export KUBECONFIG=/var/lib/jenkins/.kube/config
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
    failure { echo "❌ Something went wrong during Lab setup." }
  }
}
