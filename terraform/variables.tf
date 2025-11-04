variable "tenancy_ocid" {
  description = "The OCID of the tenancy"
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
  ephemeral = true

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

variable "kubeconfig_path" {
  description = "Path to kubeconfig for the Kubernetes provider"
  type = string
  default = "/var/lib/jenkins/.kube/config"
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
  description = "Postgres admin password (use tfvars or secrets, do not commit)"
  type = string
  sensitive = true
  default = "ChangeMe123!"
}

variable "postgres_db" {
  description = "Default database name"
  type        = string
  default     = "labdb"
}
