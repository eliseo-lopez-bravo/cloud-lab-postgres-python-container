variable "namespace" {
  description = "Namespace for the PostgreSQL deployment"
  type        = string
  default     = "lab-postgres"
}

variable "postgres_user" {
  description = "Postgres username"
  type        = string
  default     = "admin"
}

variable "postgres_password" {
  description = "Postgres password"
  type        = string
  default     = "labpassword"
}

variable "postgres_db" {
  description = "Database name"
  type        = string
  default     = "labdb"
}
