def setupLab(terraformVersion, helmVersion, k8sVersion) {
    echo "Installing Terraform ${terraformVersion}, Helm ${helmVersion}, and preparing for Kubernetes ${k8sVersion}..."
    sh '''
      mkdir -p ${BIN_DIR}
      curl -Lo /tmp/terraform.zip https://releases.hashicorp.com/terraform/${TERRAFORM_VERSION}/terraform_${TERRAFORM_VERSION}_linux_arm64.zip
      unzip -o /tmp/terraform.zip -d ${BIN_DIR}
      curl -Lo /tmp/helm.tar.gz https://get.helm.sh/helm-v${HELM_VERSION}-linux-arm64.tar.gz
      tar -xzf /tmp/helm.tar.gz -C /tmp
      mv /tmp/linux-amd64/helm ${BIN_DIR}/helm
      chmod +x ${BIN_DIR}/terraform ${BIN_DIR}/helm
    '''
}

def initTerraform() {
    echo "🚀 Running terraform init..."
    sh '''
      cd terraform
      terraform init -input=false
    '''
}

def planTerraform() {
    echo "📋 Running terraform plan..."
    sh '''
      cd terraform
      terraform plan -out=tfplan -input=false
    '''
}

def applyTerraform() {
    echo "🚀 Running terraform apply..."
    sh '''
      cd terraform
      terraform apply -input=false -auto-approve tfplan
    '''
}

def verifySetup() {
    echo "✅ Verifying Terraform deployment..."
    sh '''
      cd terraform
      terraform output
    '''
}

return this