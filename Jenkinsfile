pipeline {
  agent any

  environment {
    TERRAFORM_VERSION = '1.13.4'
    HELM_VERSION      = '3.16.0'
    K8S_VERSION       = '1.30.2'
    BIN_DIR           = "${WORKSPACE}/bin"
    PATH              = "${BIN_DIR}:${env.PATH}"
    K3S_PATH          = '/usr/local/bin/k3s'
    KUBECONFIG        = '/etc/rancher/k3s/k3s.yaml'

    TF_VAR_region            = 'us-sanjose-1'
    TF_VAR_compartment_ocid  = 'ocid1.tenancy.oc1..aaaaaaaad4ipuawmwb2miwp3bosu6i6ufmkkbvh572ceabiesraziz6zhb7q'
    TF_VAR_tenancy_ocid      = 'ocid1.tenancy.oc1..aaaaaaaad4ipuawmwb2miwp3bosu6i6ufmkkbvh572ceabiesraziz6zhb7q'
    TF_VAR_user_ocid         = 'ocid1.user.oc1..aaaaaaaaz7uge6jsrevrqjdlyn7srl77oganptjtf7mft75l7gxb4spaibma'
    TF_VAR_fingerprint       = '6f:a7:6e:df:52:3d:29:ca:1d:2b:61:00:c2:ef:42:d1'
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
            if ! command -v ${K3S_PATH} >/dev/null 2>&1; then
              echo "🔧 Installing K3s..."
              curl -sfL https://get.k3s.io | INSTALL_K3S_EXEC="--disable=traefik" sh -
              sudo systemctl enable k3s
              sudo systemctl start k3s
            else
              echo "✅ K3s already installed."
              sudo systemctl start k3s || true
            fi

            export KUBECONFIG=${KUBECONFIG}

            echo "⏳ Waiting for K3s node to be Ready..."
            for i in {1..60}; do
              if sudo ${K3S_PATH} kubectl get nodes 2>/dev/null | grep -q ' Ready '; then
                echo "✅ K3s node is Ready!"
                break
              fi
              echo "⏳ Waiting for node readiness ($i/60)..."
              sleep 5
            done

            if ! sudo ${K3S_PATH} kubectl get nodes 2>/dev/null | grep -q ' Ready '; then
              echo "❌ ERROR: K3s node not ready after waiting."
              sudo ${K3S_PATH} kubectl get nodes || true
              sudo systemctl status k3s --no-pager || true
              exit 1
            fi
          '''
        }
      }
    }

stage('Create Jenkins kubeconfig from K3s') {
  steps {
    script {
      echo "🗺️ Setting up kubeconfig for Jenkins user..."
      sh '''
        # Allow K3s kubeconfig to be readable by Jenkins
        sudo chmod 644 /etc/rancher/k3s/k3s.yaml || true

        sudo mkdir -p /var/lib/jenkins/.kube
        sudo cp /etc/rancher/k3s/k3s.yaml /var/lib/jenkins/.kube/config
        sudo chown -R jenkins:jenkins /var/lib/jenkins/.kube

        # Replace localhost with node IP
        KIP=$(hostname -I | awk '{print $1}')
        if grep -q "127.0.0.1" /var/lib/jenkins/.kube/config; then
          sudo sed -i "s/127.0.0.1/${KIP}/g" /var/lib/jenkins/.kube/config
        fi

        export KUBECONFIG=/var/lib/jenkins/.kube/config
        echo "✅ Validating kubeconfig..."
        sudo -u jenkins /usr/local/bin/k3s kubectl get nodes
      '''
    }
  }
}


    stage('Prepare Grafana values (secure)') {
      steps {
        withCredentials([string(credentialsId: 'grafana-admin-password', variable: 'GRAFANA_ADMIN_PASSWORD')]) {
          sh '''
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
          mkdir -p terraform/helm
          cat > terraform/helm/prometheus-values.yaml <<EOF
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
            terraform init -input=false -upgrade
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
            terraform apply -auto-approve tfplan
          '''
        }
      }
    }

    stage('Verify Setup') {
      steps {
        sh 'sudo /usr/local/bin/k3s kubectl get pods -A'
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
