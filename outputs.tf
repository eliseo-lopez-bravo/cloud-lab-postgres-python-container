output "postgres_connection_info" {
  description = "PostgreSQL connection info"
  value = {
    host     = "postgres-lab-postgres"
    user     = var.postgres_user
    password = var.postgres_password
    database = var.postgres_db
  }
}
