def setupLab(terraformVersion, helmVersion, k8sVersion) {
    echo "🔧 Installing Terraform ${terraformVersion}, Helm ${helmVersion}, and Kubernetes ${k8sVersion}"

    sh """
      mkdir -p \$BIN_DIR
      curl -fsSL -o /tmp/terraform.zip https://releases.hashicorp.com/terraform/${terraformVersion}/terraform_${terraformVersion}_linux_arm64.zip
      unzip -o /tmp/terraform.zip -d \$BIN_DIR

      curl -fsSL -o /tmp/helm.tar.gz https://get.helm.sh/helm-v${helmVersion}-linux-arm64.tar.gz
      tar -xzf /tmp/helm.tar.gz -C /tmp
      mv /tmp/linux-amd64/helm \$BIN_DIR/
    """
}

def initTerraform() {
    echo "🚀 Initializing Terraform..."

    sh """
      cd terraform
      export TF_VAR_tenancy_ocid=$OCI_TENANCY_OCID
      export TF_VAR_user_ocid=$OCI_USER_OCID
      export TF_VAR_fingerprint=$OCI_FINGERPRINT
      export TF_VAR_private_key_path=$OCI_PRIVATE_KEY_PATH
      export TF_VAR_region=$OCI_REGION

      terraform init -input=false
    """
}

def planTerraform() {
    echo "📋 Running Terraform plan..."
    sh """
      cd terraform
      terraform plan -out=tfplan -input=false
    """
}

def applyTerraform() {
    echo "🚢 Applying Terraform..."
    sh """
      cd terraform
      terraform apply -auto-approve tfplan
    """
}

def verifySetup() {
    echo "✅ Verifying Lab setup..."
    sh """
      cd terraform
      terraform output
    """
}

return this
