pipeline {
  agent any
  parameters {
    string(name: 'IMAGE_TAG', defaultValue: '', description: 'Image tag e.g. ghcr.io/eliseo-lopez-bravo/fastapi-anom:sha')
  }
  stages {
    stage('Terraform Apply') {
      steps {
        dir('infra') {
          sh 'terraform init -input=false || true'
          sh 'terraform apply -auto-approve -input=false'
        }
      }
    }
    stage('Deploy Postgres (k8s manifest)') {
      steps {
        dir('k8s/postgres') {
          sh 'kubectl apply -f pg-secret.yaml -n infra || true'
          sh 'kubectl apply -f postgres-statefulset.yaml -n infra || true'
          sh 'kubectl apply -f postgres-service-clusterip.yaml -n infra || true'
        }
      }
    }
    stage('Deploy App') {
      steps {
        dir('k8s') {
          sh '''
            kubectl apply -f namespaces.yaml || true
            kubectl apply -f fastapi-deployment.yaml -n app || true
            kubectl apply -f fastapi-service.yaml -n app || true
            kubectl apply -f ingress-fastapi.yaml -n app || true
            kubectl set image deployment/fastapi fastapi=${IMAGE_TAG} -n app || true
          '''
        }
      }
    }
  }
}
