# Terraform creates k8s namespaces only; Postgres will be deployed via k8s manifests (statefulset) applied by Jenkins.
resource "kubernetes_namespace" "infra" {
  metadata {
    name = "infra"
  }
}
resource "kubernetes_namespace" "app" {
  metadata {
    name = "app"
  }
}
