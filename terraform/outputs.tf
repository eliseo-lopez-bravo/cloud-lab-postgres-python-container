output "lab_status" {
  description = "Confirmation message from Terraform"
  value       = local_file.lab_ready.content
}
