variable "namespace" {
  description = "Kubernetes namespace for the lab"
  type        = string
  default     = "cloud-lab"
}

variable "postgres_image" {
  description = "Docker image for PostgreSQL"
  type        = string
  default     = "postgres:15"
}

variable "postgres_user" {
  description = "PostgreSQL username"
  type        = string
  default     = "admin"
}

variable "postgres_password" {
  description = "PostgreSQL password"
  type        = string
  default     = "password123"
  sensitive   = true
}

variable "postgres_db" {
  description = "Default database name"
  type        = string
  default     = "labdb"
}
