stage('Lab Setup') {
    steps {
        script {
            echo "=== Checking Terraform installation ==="
            sh '''
            mkdir -p ${TF_DIR}
            ARCH=$(uname -m)
            case "$ARCH" in
                x86_64)   TF_ARCH="amd64" ;;
                aarch64)  TF_ARCH="arm64" ;;
                arm64)    TF_ARCH="arm64" ;;
                *)        echo "Unsupported architecture: $ARCH"; exit 1 ;;
            esac

            if ! command -v terraform >/dev/null 2>&1; then
                echo "Terraform not found — installing locally for ${TF_ARCH}..."
                curl -fsSL https://releases.hashicorp.com/terraform/${TF_VERSION}/terraform_${TF_VERSION}_linux_${TF_ARCH}.zip -o /tmp/terraform.zip
                unzip -o /tmp/terraform.zip -d ${TF_DIR}
                rm -f /tmp/terraform.zip
                chmod +x ${TF_DIR}/terraform
                ${TF_DIR}/terraform version
            else
                echo "Terraform is already installed:"
                terraform version
            fi
            '''
        }
    }
}
