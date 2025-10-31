def setupLab(terraformVersion, helmVersion, k8sVersion) {
    sh """
      set -e
      echo "📦 Preparing environment directories..."
      mkdir -p ${env.BIN_DIR}

      echo "📐 Detecting architecture..."
      ARCH=\$(uname -m)
      case "\$ARCH" in
        x86_64) TF_ARCH=amd64; K8S_ARCH=amd64; HELM_ARCH=amd64 ;;
        aarch64) TF_ARCH=arm64; K8S_ARCH=arm64; HELM_ARCH=arm64 ;;
        *) echo "❌ Unsupported architecture: \$ARCH"; exit 1 ;;
      esac

      echo "⬇️ Installing Terraform v${terraformVersion}..."
      curl -fsSL https://releases.hashicorp.com/terraform/${terraformVersion}/terraform_${terraformVersion}_linux_\${TF_ARCH}.zip -o /tmp/terraform.zip
      unzip -o /tmp/terraform.zip -d ${env.BIN_DIR}
      chmod +x ${env.BIN_DIR}/terraform
      ${env.BIN_DIR}/terraform version

      echo "⬇️ Installing Helm v${helmVersion}..."
      curl -fsSL https://get.helm.sh/helm-v${helmVersion}-linux-\${HELM_ARCH}.tar.gz -o /tmp/helm.tar.gz
      tar -xzf /tmp/helm.tar.gz -C /tmp
      mv /tmp/linux-\${HELM_ARCH}/helm ${env.BIN_DIR}/helm
      chmod +x ${env.BIN_DIR}/helm
      ${env.BIN_DIR}/helm version
    """
}

def initTerraform() {
    sh """
      echo "🚀 Initializing Terraform..."
      cd ${env.WORKSPACE}/terraform
      terraform init -input=false
    """
}

def planTerraform() {
    sh """
      echo "🧠 Running Terraform plan..."
      cd ${env.WORKSPACE}/terraform
      terraform plan -out=tfplan
    """
}

def applyTerraform() {
    sh """
      echo "⚙️ Applying Terraform..."
      cd ${env.WORKSPACE}/terraform
      terraform apply -auto-approve tfplan
    """
}

def verifySetup() {
    sh """
      echo "✅ Verifying deployment..."
      kubectl get pods -A || echo '⚠️ Kubernetes not configured yet'
    """
}

return this
