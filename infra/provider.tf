provider "oci" {
  region = "us-sanjose-1"
  # uses ~/.oci/config profile by default
}
provider "kubernetes" {
  config_path = "~/.kube/config"
}
provider "helm" {
  kubernetes {
    config_path = "~/.kube/config"
  }
}
