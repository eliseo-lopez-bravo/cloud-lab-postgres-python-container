def setupLab(terraformVersion, helmVersion, k8sVersion) {
    echo "Installing Terraform ${terraformVersion}, Helm ${helmVersion}, and kubectl ${k8sVersion}..."
    sh """
      mkdir -p \$WORKSPACE/bin
      curl -sLo /tmp/terraform.zip https://releases.hashicorp.com/terraform/${terraformVersion}/terraform_${terraformVersion}_linux_arm64.zip
      unzip -o /tmp/terraform.zip -d \$WORKSPACE/bin
      curl -sLo /tmp/helm.tar.gz https://get.helm.sh/helm-v${helmVersion}-linux-arm64.tar.gz
      tar -xzf /tmp/helm.tar.gz -C /tmp
      mv /tmp/linux-arm64/helm \$WORKSPACE/bin/
      curl -sLo \$WORKSPACE/bin/kubectl https://dl.k8s.io/release/v${k8sVersion}/bin/linux/amd64/kubectl
      chmod +x \$WORKSPACE/bin/terraform \$WORKSPACE/bin/helm \$WORKSPACE/bin/kubectl
    """
    // ensure /etc/rancher/k3s/k3s.yaml exists (install will be performed in Jenkinsfile if missing)
}

def initTerraform() {
    echo "🚀 Running terraform init..."
    dir('terraform') {
        sh 'terraform init -input=false'
    }
}

def planTerraform() {
    echo "📋 Running terraform plan..."
    dir('terraform') {
        sh 'terraform plan -out=tfplan -input=false'
    }
}

def applyTerraform() {
    echo "🚀 Running terraform apply..."
    dir('terraform') {
        sh 'terraform apply -input=false -auto-approve tfplan || terraform apply -input=false -auto-approve'
    }
}

def verifySetup() {
    echo "✅ Verifying Terraform deployment..."
    sh '''
      cd terraform
      terraform output || true
      # list k8s namespace (use KUBECONFIG provided by Jenkinsfile)
      /bin/sh -lc "${WORKSPACE}/bin/kubectl --kubeconfig /var/lib/jenkins/.kube/config get ns || true"
    '''
}

return this
