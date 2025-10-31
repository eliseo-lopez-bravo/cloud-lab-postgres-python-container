pipeline {
  agent any
  stages {
    stage('Remove App') {
      steps {
        dir('k8s') {
          sh '''
            kubectl delete -f ingress-fastapi.yaml -n app --ignore-not-found=true || true
            kubectl delete -f fastapi-service.yaml -n app --ignore-not-found=true || true
            kubectl delete -f fastapi-deployment.yaml -n app --ignore-not-found=true || true
          '''
        }
      }
    }
    stage('Remove Postgres') {
      steps {
        dir('k8s/postgres') {
          sh '''
            kubectl delete -f postgres-service-clusterip.yaml -n infra --ignore-not-found=true || true
            kubectl delete -f postgres-statefulset.yaml -n infra --ignore-not-found=true || true
            kubectl delete -f pg-secret.yaml -n infra --ignore-not-found=true || true
          '''
        }
      }
    }
    stage('Terraform Destroy') {
      steps {
        dir('infra') {
          sh 'terraform init -input=false || true'
          sh 'terraform destroy -auto-approve -input=false'
        }
      }
    }
  }
}
