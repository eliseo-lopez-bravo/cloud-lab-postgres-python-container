output "postgres_connection_string" {
  description = "Connection string for the PostgreSQL service"
  value       = "postgresql://${var.postgres_user}:${var.postgres_password}@postgres-service.${var.namespace}.svc.cluster.local:5432/${var.postgres_db}"
  sensitive   = true
}
