variable "tenancy_ocid" {
  description = "The OCID of the tenancy"
  type        = string
}

variable "compartment_ocid" {
  description = "The compartment"
  type        = string
}

variable "user_ocid" {
  description = "The OCID of the user"
  type        = string
}

variable "fingerprint" {
  description = "The fingerprint of the public key added to the user"
  type        = string
}

variable "private_key_path" {
  description = "Path to the private API key file"
  type        = string
}

variable "region" {
  description = "The OCI region"
  type        = string
  default     = "us-sanjose-1"
}

variable "compartment_ocid" {
  description = "Compartment OCID where resources are created"
  type        = string
}

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
