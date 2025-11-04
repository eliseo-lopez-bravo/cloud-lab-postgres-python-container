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

resource "kubernetes_namespace" "lab" {
  metadata {
    name = var.namespace
  }
}

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
            limits = { cpu = "500m", memory = "512Mi" }
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

# Loki + promtail are installed as loki-stack via grafana helm repo
resource "helm_release" "loki_stack" {
  name       = "loki-stack"
  repository = "https://grafana.github.io/helm-charts"
  chart      = "loki-stack"
  version    = "2.15.0"
  namespace  = kubernetes_namespace.lab.metadata[0].name

  values = [file("${path.module}/helm/loki-values.yaml")]

  depends_on = [kubernetes_namespace.lab]
}

# Grafana release (values file written by Jenkins at runtime)
resource "helm_release" "grafana" {
  name       = "grafana"
  repository = "https://grafana.github.io/helm-charts"
  chart      = "grafana"
  version    = "6.24.1"
  namespace  = kubernetes_namespace.lab.metadata[0].name

  # Jenkins generates terraform/helm/grafana-values.yaml before plan/apply
  values = [file("${path.module}/helm/grafana-values.yaml")]

  depends_on = [kubernetes_namespace.lab]
}

# Prometheus stack (kube-prometheus-stack) for metrics
resource "helm_release" "prometheus_stack" {
  name       = "prom-stack"
  repository = "https://prometheus-community.github.io/helm-charts"
  chart      = "kube-prometheus-stack"
  version    = "45.6.0"  # pick a recent compatible version; change if needed
  namespace  = kubernetes_namespace.lab.metadata[0].name

  values = [file("${path.module}/helm/prometheus-values.yaml")]

  depends_on = [kubernetes_namespace.lab]
}

