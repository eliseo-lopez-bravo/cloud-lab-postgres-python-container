pipeline {
  agent any

  environment {
    TERRAFORM_VERSION = '1.13.4'
    HELM_VERSION      = '3.16.0'
    K8S_VERSION       = '1.30.2'
    BIN_DIR           = "${WORKSPACE}/bin"
    PATH              = "${BIN_DIR}:${env.PATH}"

    // OCI and Terraform variables
    TF_VAR_region          = 'us-sanjose-1'
    TF_VAR_tenancy_ocid    = 'ocid1.tenancy.oc1..REPLACE_ME'
    TF_VAR_user_ocid       = 'ocid1.user.oc1..REPLACE_ME'
    TF_VAR_fingerprint     = 'AA:BB:CC:DD:EE:FF:00:11:22:33:44:55:66:77:88:99'
  }

  stages {
    stage('Checkout') {
      steps { checkout scm }
    }

    stage('Setup Jenkins Environment') {
      steps {
        script {
          echo "⚙️ Configuring Jenkins environment (sudoers, dirs)..."
          sh '''
            if [ ! -f /etc/sudoers.d/jenkins ]; then
              echo "jenkins ALL=(ALL) NOPASSWD: ALL" | sudo tee /etc/sudoers.d/jenkins
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
              echo "✅ K3s already installed, skipping."
            fi

            # Wait for node to be Ready
            echo "⏳ Waiting for K3s node to be Ready..."
            for i in {1..30}; do
              if sudo kubectl get nodes 2>/dev/null | grep -q "Ready"; then
                echo "✅ K3s node is Ready."
                break
              fi
              sleep 5
            done
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

            # Replace 127.0.0.1 with actual IP
            KIP=$(hostname -I | awk '{print $1}')
            if grep -q "127.0.0.1" /var/lib/jenkins/.kube/config; then
              sudo sed -i "s/127.0.0.1/${KIP}/g" /var/lib/jenkins/.kube/config
            fi

            # Verify connectivity
            sudo -u jenkins kubectl --kubeconfig /var/lib/jenkins/.kube/config get nodes
          '''
        }
      }
    }

    stage('Prepare Grafana values (secure)') {
      steps {
        withCredentials([string(credentialsId: 'grafana-admin-password', variable: 'GRAFANA_ADMIN_PASSWORD')]) {
          sh '''
            echo "🧩 Creating secure grafana-values.yaml..."
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
            chmod 600 terraform/helm/grafana-values.yaml || true
          '''
        }
      }
    }

    stage('Prepare Prometheus values') {
      steps {
        sh '''
          echo "📊 Creating prometheus-values.yaml..."
          mkdir -p terraform/helm
          cat > terraform/helm/prometheus-values.yaml <<'EOF'
fullnameOverride: "prom-stack"
prometheus:
  prometheusSpec:
    retention: "7d"
    serviceMonitorSelectorNilUsesHelmValues: false
grafana:
  enabled: false
EOF
        '''
      }
    }

    stage('Terraform Init') {
      steps {
        withCredentials([file(credentialsId: 'oci-private-key', variable: 'OCI_PRIVATE_KEY_PATH')]) {
          sh '''
            cd terraform
            export TF_VAR_private_key_path="${OCI_PRIVATE_KEY_PATH}"
            export KUBECONFIG=/var/lib/jenkins/.kube/config
            terraform init -input=false
          '''
        }
      }
    }

    stage('Terraform Plan') {
      steps {
        withCredentials([file(credentialsId: 'oci-private-key', variable: 'OCI_PRIVATE_KEY_PATH')]) {
          sh '''
            cd terraform
            export TF_VAR_private_key_path="${OCI_PRIVATE_KEY_PATH}"
            export KUBECONFIG=/var/lib/jenkins/.kube/config
            terraform plan -out=tfplan -input=false
          '''
        }
      }
    }

    stage('Terraform Apply') {
      steps {
        withCredentials([file(credentialsId: 'oci-private-key', variable: 'OCI_PRIVATE_KEY_PATH')]) {
          sh '''
            cd terraform
            export TF_VAR_private_key_path="${OCI_PRIVATE_KEY_PATH}"
            export KUBECONFIG=/var/lib/jenkins/.kube/config
            terraform apply -input=false -auto-approve tfplan
          '''
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
