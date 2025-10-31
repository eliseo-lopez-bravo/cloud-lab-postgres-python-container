pipeline {
    agent any

    environment {
        TF_VERSION = "1.10.4"
        KUBECTL_VERSION = "v1.31.0"
        HELM_VERSION = "v3.16.2"
        BIN_DIR = "${WORKSPACE}/bin"
        PATH = "${BIN_DIR}:${PATH}"
    }

    stages {
        stage('Checkout') {
            steps {
                git branch: 'main',
                    credentialsId: 'github-ssh',
                    url: 'https://github.com/eliseo-lopez-bravo/cloud-lab-postgres-python-container.git'
            }
        }

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
                    echo "Detected architecture: ${ARCH}"
                    case "$ARCH" in
                        x86_64)   TF_ARCH="amd64";  K8S_ARCH="amd64";  HELM_ARCH="amd64" ;;
                        aarch64)  TF_ARCH="arm64";  K8S_ARCH="arm64";  HELM_ARCH="arm64" ;;
                        arm64)    TF_ARCH="arm64";  K8S_ARCH="arm64";  HELM_ARCH="arm64" ;;
                        *)        echo "❌ Unsupported architecture: $ARCH"; exit 1 ;;
                    esac
                    echo "Using Terraform architecture: ${TF_ARCH}"

                    echo "🧹 Cleaning old binaries..."
                    rm -f ${BIN_DIR}/terraform ${BIN_DIR}/kubectl ${BIN_DIR}/helm

                    echo "=== Installing Terraform ${TF_VERSION} for ${TF_ARCH} ==="
                    curl -fsSL https://releases.hashicorp.com/terraform/${TF_VERSION}/terraform_${TF_VERSION}_linux_${TF_ARCH}.zip -o /tmp/terraform.zip
                    unzip -o /tmp/terraform.zip -d ${BIN_DIR}
                    rm -f /tmp/terraform.zip
                    chmod +x ${BIN_DIR}/terraform

                    echo "Verifying Terraform binary type..."
                    file ${BIN_DIR}/terraform || true

                    echo "Running Terraform version check..."
                    ${BIN_DIR}/terraform version || { echo "Terraform binary failed to execute"; exit 126; }

                    echo "=== Installing kubectl ${KUBECTL_VERSION} ==="
                    curl -fsSL -o ${BIN_DIR}/kubectl https://dl.k8s.io/release/${KUBECTL_VERSION}/bin/linux/${K8S_ARCH}/kubectl
                    chmod +x ${BIN_DIR}/kubectl
                    ${BIN_DIR}/kubectl version --client

                    echo "=== Installing Helm ${HELM_VERSION} ==="
                    curl -fsSL https://get.helm.sh/helm-${HELM_VERSION}-linux-${HELM_ARCH}.tar.gz -o /tmp/helm.tar.gz
                    tar -xzf /tmp/helm.tar.gz -C /tmp
                    mv /tmp/linux-${HELM_ARCH}/helm ${BIN_DIR}/helm
                    chmod +x ${BIN_DIR}/helm
                    rm -rf /tmp/helm.tar.gz /tmp/linux-${HELM_ARCH}
                    ${BIN_DIR}/helm version

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
                    cd ${WORKSPACE}/terraform
                    terraform init -input=false
                    '''
                }
            }
        }

        stage('Terraform Plan') {
            steps {
                script {
                    echo "=== Running Terraform Plan ==="
                    sh '''
                    cd ${WORKSPACE}/terraform
                    terraform plan -out=tfplan
                    '''
                }
            }
        }

        stage('Terraform Apply') {
            steps {
                script {
                    echo "=== Applying Terraform ==="
                    sh '''
                    cd ${WORKSPACE}/terraform
                    terraform apply -auto-approve tfplan
                    '''
                }
            }
        }

        stage('Verify Setup') {
            steps {
                script {
                    echo "=== Verifying Deployment Environment ==="
                    sh '''
                    echo "Terraform version:"
                    terraform version

                    echo "kubectl version:"
                    kubectl version --client

                    echo "Helm version:"
                    helm version
                    '''
                }
            }
        }
    }

    post {
        always {
            echo "🧹 Cleaning temporary files..."
            sh 'rm -rf /tmp/terraform.zip /tmp/helm.tar.gz /tmp/linux-*'
        }
        success {
            echo "🎉 Lab environment setup and Terraform deployment completed successfully!"
        }
        failure {
            echo "❌ Something went wrong during Lab setup."
        }
    }
}
