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
      version = "~> 2.10.0"
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
    labels    = { app = "postgres" }
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

# --- POSTGRES SERVICE ---
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

# Loki + Promtail (lightweight single-node setup)
resource "helm_release" "loki_stack" {
  name       = "loki-stack"
  repository = "https://grafana.github.io/helm-charts"
  chart      = "loki-stack"
  namespace  = kubernetes_namespace.lab.metadata[0].name
  create_namespace = false

  values = [<<-EOT
loki:
  enabled: true
  auth_enabled: false
  singleBinary:
    replicas: 1
  persistence:
    enabled: false
  config:
    server:
      http_listen_port: 3100
    schema_config:
      configs:
        - from: 2020-10-24
          store: boltdb-shipper
          object_store: filesystem
          schema: v11
          index:
            prefix: index_
            period: 24h

promtail:
  enabled: true
  resources:
    limits:
      cpu: 150m
      memory: 128Mi
    requests:
      cpu: 50m
      memory: 64Mi

grafana:
  enabled: false  # disable here to avoid duplication

test_pod:
  enabled: false
EOT
  ]

  wait    = false
  timeout = 600

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

  wait       = false
  timeout    = 600

  depends_on = [helm_release.loki_stack]
}

# Grafana (standalone, lightweight)
resource "helm_release" "grafana" {
  name       = "grafana"
  repository = "https://grafana.github.io/helm-charts"
  chart      = "grafana"
  version    = "6.24.1"
  namespace  = kubernetes_namespace.lab.metadata[0].name

  values = [<<-EOT
adminUser: admin
adminPassword: admin
persistence:
  enabled: false
service:
  type: NodePort
  nodePort: 32000
resources:
  limits:
    cpu: 250m
    memory: 256Mi
  requests:
    cpu: 100m
    memory: 128Mi
EOT
  ]

  wait       = false
  timeout    = 600

  depends_on = [helm_release.prometheus_stack]
}
