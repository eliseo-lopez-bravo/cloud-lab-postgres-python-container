def setupLab(terraformVersion, helmVersion, k8sVersion) {
  echo "🔧 Installing Terraform ${terraformVersion}, Helm ${helmVersion}, and Kubernetes ${k8sVersion}"

  def binDir = env.BIN_DIR  // ✅ fix — read from Jenkins environment

  sh """
    mkdir -p ${binDir}
    curl -fsSL -o /tmp/terraform.zip https://releases.hashicorp.com/terraform/${terraformVersion}/terraform_${terraformVersion}_linux_arm64.zip
    unzip -o /tmp/terraform.zip -d ${binDir}

    arch=\$(uname -m | sed 's/aarch64/arm64/;s/x86_64/amd64/')
    curl -fsSL -o /tmp/helm.tar.gz https://get.helm.sh/helm-v${helmVersion}-linux-\${arch}.tar.gz
    tar -xzf /tmp/helm.tar.gz -C /tmp
    mv /tmp/linux-\${arch}/helm ${binDir}/

    echo "✅ Terraform, Helm installed in ${binDir}"
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
