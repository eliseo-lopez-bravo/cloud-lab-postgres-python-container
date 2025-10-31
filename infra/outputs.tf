output "namespaces" {
  value = [kubernetes_namespace.infra.metadata[0].name, kubernetes_namespace.app.metadata[0].name]
}
