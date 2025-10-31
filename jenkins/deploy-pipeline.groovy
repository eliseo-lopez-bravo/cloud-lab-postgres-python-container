pipeline {
    agent any

    environment {
        TF_VERSION = "1.10.4"
        KUBECTL_VERSION = "v1.31.1"
        HELM_VERSION = "v3.16.3"
        BIN_DIR = "${WORKSPACE}/bin"
        PATH = "${WORKSPACE}/bin:${PATH}"
    }

    stages {
        stage('Lab Setup') {
            steps {
                script {
                    echo "=== Starting Lab Setup Environment ==="

                    sh '''
                    set -e

                    echo "📦 Creating bin directory..."
                    mkdir -p ${BIN_DIR}

                    echo "📐 Detecting architecture..."
                    ARCH=$(uname -m)
                    case "$ARCH" in
                        x86_64)   TF_ARCH="amd64";  K8S_ARCH="amd64";  HELM_ARCH="amd64" ;;
                        aarch64)  TF_ARCH="arm64";  K8S_ARCH="arm64";  HELM_ARCH="arm64" ;;
                        arm64)    TF_ARCH="arm64";  K8S_ARCH="arm64";  HELM_ARCH="arm64" ;;
                        *)        echo "❌ Unsupported architecture: $ARCH"; exit 1 ;;
                    esac

                    echo "=== Installing Terraform ${TF_VERSION} ==="
                    if ! command -v terraform >/dev/null 2>&1; then
                        curl -fsSL https://releases.hashicorp.com/terraform/${TF_VERSION}/terraform_${TF_VERSION}_linux_${TF_ARCH}.zip -o /tmp/terraform.zip
                        unzip -o /tmp/terraform.zip -d ${BIN_DIR}
                        rm -f /tmp/terraform.zip
                        chmod +x ${BIN_DIR}/terraform
                    fi
                    terraform version

                    echo "=== Installing kubectl ${KUBECTL_VERSION} ==="
                    if ! command -v kubectl >/dev/null 2>&1; then
                        curl -fsSL -o ${BIN_DIR}/kubectl https://dl.k8s.io/release/${KUBECTL_VERSION}/bin/linux/${K8S_ARCH}/kubectl
                        chmod +x ${BIN_DIR}/kubectl
                    fi
                    kubectl version --client

                    echo "=== Installing Helm ${HELM_VERSION} ==="
                    if ! command -v helm >/dev/null 2>&1; then
                        curl -fsSL https://get.helm.sh/helm-${HELM_VERSION}-linux-${HELM_ARCH}.tar.gz -o /tmp/helm.tar.gz
                        tar -xzf /tmp/helm.tar.gz -C /tmp
                        mv /tmp/linux-${HELM_ARCH}/helm ${BIN_DIR}/helm
                        chmod +x ${BIN_DIR}/helm
                        rm -rf /tmp/helm.tar.gz /tmp/linux-${HELM_ARCH}
                    fi
                    helm version

                    echo "✅ Lab setup completed successfully!"
                    '''
                }
            }
        }

        stage('Terraform Init') {
            steps {
                script {
                    echo "=== Initializing Terraform ==="
                    sh '''
                    terraform -chdir=./Infra init -input=false
                    '''
                }
            }
        }

        stage('Terraform Plan') {
            steps {
                script {
                    echo "=== Running Terraform Plan ==="
                    sh '''
                    terraform -chdir=./Infra plan -out=tfplan
                    '''
                }
            }
        }

        stage('Terraform Apply') {
            steps {
                script {
                    echo "=== Applying Terraform Infrastructure ==="
                    sh '''
                    terraform -chdir=./Infra apply -auto-approve tfplan
                    '''
                }
            }
        }

        stage('Verify Setup') {
            steps {
                script {
                    echo "=== Verifying Kubernetes and Helm Setup ==="
                    sh '''
                    echo "kubectl version:"
                    kubectl version --client

                    echo "helm version:"
                    helm version
                    '''
                }
            }
        }
    }

    post {
        always {
            echo "🧹 Cleaning temporary files..."
            sh 'rm -rf /tmp/terraform.zip /tmp/helm.tar.gz /tmp/linux-* || true'
        }
        success {
            echo "🚀 Lab setup and IaC environment successfully provisioned."
        }
        failure {
            echo "❌ Something went wrong during Lab setup."
        }
    }
}
