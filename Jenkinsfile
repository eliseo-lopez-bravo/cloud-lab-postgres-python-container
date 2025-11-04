pipeline {
  agent any

  environment {
    TERRAFORM_VERSION = '1.13.4'
    HELM_VERSION      = '3.16.0'
    K8S_VERSION       = '1.30.2'
    BIN_DIR           = "${WORKSPACE}/bin"
    PATH              = "${BIN_DIR}:${env.PATH}"

    // OCI environment variables (use Jenkins credentials for production)
    TF_VAR_region          = 'us-sanjose-1'
    TF_VAR_tenancy_ocid    = 'ocid1.tenancy.oc1..REPLACE_ME'
    TF_VAR_user_ocid       = 'ocid1.user.oc1..REPLACE_ME'
    TF_VAR_fingerprint     = 'AA:BB:CC:DD:EE:FF:00:11:22:33:44:55:66:77:88:99'
  }

  stages {
    stage('Checkout') {
      steps {
        checkout scm
      }
    }

    stage('Setup Jenkins Environment') {
      steps {
        script {
          echo "⚙️ Configuring Jenkins environment (sudoers, dirs)..."
          sh '''
            if [ ! -f /etc/sudoers.d/jenkins ]; then
              echo 'jenkins ALL=(ALL) NOPASSWD: ALL' | sudo tee /etc/sudoers.d/jenkins
              sudo chmod 440 /etc/sudoers.d/jenkins
            fi
            mkdir -p ${BIN_DIR}
          '''
        }
      }
    }

    stage('Install or Verify K3s') {
      steps {
        script {
          echo "🐳 Installing or verifying K3s cluster..."
          sh '''
            if ! command -v k3s >/dev/null 2>&1; then
              echo "🚀 Installing K3s (single-node)..."
              curl -sfL https://get.k3s.io | INSTALL_K3S_EXEC="--disable=traefik" sudo sh -
            else
              echo "✅ K3s already installed."
            fi

            echo "⏳ Waiting for K3s node to be Ready..."
            for i in {1..30}; do
              if sudo kubectl get nodes 2>/dev/null | grep -q " Ready "; then
                echo "✅ K3s node is Ready!"
                break
              fi
              echo "⏳ Waiting for node readiness ($i/30)..."
              sleep 5
            done

            if ! sudo kubectl get nodes 2>/dev/null | grep -q " Ready "; then
              echo "❌ ERROR: K3s node not ready after waiting."
              exit 1
            fi
          '''
        }
      }
    }

    stage('Create Jenkins kubeconfig from K3s') {
      steps {
        script {
          echo "🗺️ Preparing kubeconfig for Jenkins..."
          sh '''
            sudo mkdir -p /var/lib/jenkins/.kube
            sudo cp /etc/rancher/k3s/k3s.yaml /var/lib/jenkins/.kube/config
            sudo chown -R jenkins:jenkins /var/lib/jenkins/.kube

            KIP=$(hostname -I | awk '{print $1}')
            if grep -q 127.0.0.1 /var/lib/jenkins/.kube/config; then
              sudo sed -i "s/127.0.0.1/${KIP}/g" /var/lib/jenkins/.kube/config
            fi

            # Ensure kubectl is always accessible
            sudo ln -sf /usr/local/bin/k3s /usr/local/bin/kubectl
            echo "export PATH=$PATH:/usr/local/bin" | sudo tee -a /var/lib/jenkins/.bashrc

            echo "🔍 Verifying cluster access as Jenkins user..."
            sudo -u jenkins env "PATH=$PATH:/usr/local/bin" kubectl --kubeconfig /var/lib/jenkins/.kube/config get nodes
          '''
        }
      }
    }

    stage('Prepare Grafana values (secure)') {
      steps {
        // grafana-admin-password must exist in Jenkins Credentials as Secret Text
        withCredentials([string(credentialsId: 'grafana-admin-password', variable: 'GRAFANA_ADMIN_PASSWORD')]) {
          sh '''
            echo "🔒 Creating secure Grafana Helm values..."
            mkdir -p terraform/helm
            cat > terraform/helm/grafana-values.yaml <<EOF
adminUser: admin
adminPassword: "${GRAFANA_ADMIN_PASSWORD}"
service:
  type: ClusterIP
persistence:
  enabled: false
ingress:
  enabled: false
security:
  enabled: false
EOF
            chmod 600 terraform/helm/grafana-values.yaml
          '''
        }
      }
    }

    stage('Prepare Prometheus values') {
      steps {
        sh '''
          echo "📊 Creating Prometheus Helm values..."
          mkdir -p terraform/helm
          cat > terraform/helm/prometheus-values.yaml <<EOF
fullnameOverride: "prom-stack"
prometheus:
  prometheusSpec:
    retention: "7d"
    serviceMonitorSelectorNilUsesHelmValues: false
grafana:
  enabled: false   # Installed separately via Grafana release
EOF
        '''
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
          echo "🔍 Verifying environment setup..."
          sh '''
            sudo -u jenkins env "PATH=$PATH:/usr/local/bin" kubectl get pods -A
          '''
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
