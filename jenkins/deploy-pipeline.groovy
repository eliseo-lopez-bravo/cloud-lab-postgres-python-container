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

            echo "=== Installing Terraform ${TF_VERSION} for ${TF_ARCH} ==="
            if [ ! -x "${BIN_DIR}/terraform" ]; then
                curl -fsSL https://releases.hashicorp.com/terraform/${TF_VERSION}/terraform_${TF_VERSION}_linux_${TF_ARCH}.zip -o /tmp/terraform.zip
                unzip -o /tmp/terraform.zip -d ${BIN_DIR}
                rm -f /tmp/terraform.zip
                chmod +x ${BIN_DIR}/terraform
            fi

            echo "Verifying Terraform binary type..."
            file ${BIN_DIR}/terraform || true

            echo "Running Terraform version check..."
            ${BIN_DIR}/terraform version || { echo "Terraform binary failed to execute"; exit 126; }

            echo "=== Installing kubectl ${KUBECTL_VERSION} ==="
            if [ ! -x "${BIN_DIR}/kubectl" ]; then
                curl -fsSL -o ${BIN_DIR}/kubectl https://dl.k8s.io/release/${KUBECTL_VERSION}/bin/linux/${K8S_ARCH}/kubectl
                chmod +x ${BIN_DIR}/kubectl
            fi
            kubectl version --client

            echo "=== Installing Helm ${HELM_VERSION} ==="
            if [ ! -x "${BIN_DIR}/helm" ]; then
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
