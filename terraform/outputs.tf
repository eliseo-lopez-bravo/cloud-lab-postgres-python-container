output "postgres_connection_string" {
  description = "Connection string for the PostgreSQL service"
  value       = "postgresql://${var.postgres_user}:${var.postgres_password}@postgres-service.${var.namespace}.svc.cluster.local:5432/${var.postgres_db}"
  sensitive   = true
}
output "region" {
  description = "The region in which the lab was deployed"
  value       = var.region
}

output "postgres_namespace" {
  value = kubernetes_namespace.lab.metadata[0].name
}

# Grafana service (helm chart exposes service name)
output "grafana_release_name" {
  value = helm_release.grafana.name
}

output "loki_release_name" {
  value = helm_release.loki_stack.name
}
