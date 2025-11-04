terraform {
  required_version = ">= 1.4.0"

  required_providers {
    oci = {
      source  = "oracle/oci"
      version = ">= 5.0.0"
    }
    kubernetes = {
      source  = "hashicorp/kubernetes"
      version = "~> 2.32.0"
    }
    helm = {
      source  = "hashicorp/helm"
      version = "~> 2.9.0"
    }
  }
}

# --- PROVIDERS ---

provider "oci" {
  tenancy_ocid     = var.tenancy_ocid
  user_ocid        = var.user_ocid
  fingerprint      = var.fingerprint
  private_key_path = var.private_key_path
  region           = var.region
  compartment_ocid = var.compartment_ocid
}

provider "kubernetes" {
  config_path = var.kubeconfig_path
}

provider "helm" {
  kubernetes {
    config_path = var.kubeconfig_path
  }
}

# --- NAMESPACE ---

resource "kubernetes_namespace" "lab" {
  metadata {
    name = var.namespace
  }
}

# --- POSTGRES DEPLOYMENT ---

resource "kubernetes_deployment" "postgres" {
  metadata {
    name      = "postgres-db"
    namespace = kubernetes_namespace.lab.metadata[0].name
    labels = { app = "postgres" }
  }

  spec {
    replicas = 1
    selector {
      match_labels = { app = "postgres" }
    }
    template {
      metadata { labels = { app = "postgres" } }
      spec {
        container {
          name  = "postgres"
          image = var.postgres_image
          port { container_port = 5432 }

          env {
            name  = "POSTGRES_USER"
            value = var.postgres_user
          }
          env {
            name  = "POSTGRES_PASSWORD"
            value = var.postgres_password
          }
          env {
            name  = "POSTGRES_DB"
            value = var.postgres_db
          }

          resources {
            limits   = { cpu = "500m", memory = "512Mi" }
            requests = { cpu = "250m", memory = "256Mi" }
          }
        }
      }
    }
  }
}

resource "kubernetes_service" "postgres_service" {
  metadata {
    name      = "postgres-service"
    namespace = kubernetes_namespace.lab.metadata[0].name
  }

  spec {
    selector = { app = "postgres" }
    port {
      port        = 5432
      target_port = 5432
    }
    type = "ClusterIP"
  }
}

# --- HELM RELEASES ---

# Loki + promtail
resource "helm_release" "loki_stack" {
  name       = "loki-stack"
  repository = "https://grafana.github.io/helm-charts"
  chart      = "loki-stack"
  version    = "2.15.0"
  namespace  = kubernetes_namespace.lab.metadata[0].name

  values = [file("${path.module}/helm/loki-values.yaml")]

  depends_on = [kubernetes_namespace.lab]
}

# Prometheus stack
resource "helm_release" "prometheus_stack" {
  name       = "prom-stack"
  repository = "https://prometheus-community.github.io/helm-charts"
  chart      = "kube-prometheus-stack"
  version    = "45.6.0"
  namespace  = kubernetes_namespace.lab.metadata[0].name

  values = [file("${path.module}/helm/prometheus-values.yaml")]

  depends_on = [helm_release.loki_stack]
}

# Grafana
resource "helm_release" "grafana" {
  name       = "grafana"
  repository = "https://grafana.github.io/helm-charts"
  chart      = "grafana"
  version    = "6.24.1"
  namespace  = kubernetes_namespace.lab.metadata[0].name

  values = [file("${path.module}/helm/grafana-values.yaml")]

  depends_on = [helm_release.prometheus_stack]
}

# --- OUTPUTS ---

output "grafana_admin_password" {
  description = "Grafana admin password (use this for login)"
  value       = helm_release.grafana.metadata[0].name != "" ? "Run: kubectl get secret --namespace ${var.namespace} grafana -o jsonpath=\"{.data.admin-password}\" | base64 --decode" : ""
}

output "grafana_url" {
  description = "Grafana URL (if exposed via LoadBalancer or NodePort)"
  value       = "Run: kubectl get svc -n ${var.namespace} -l app.kubernetes.io/name=grafana"
}
