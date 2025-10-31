# Simple placeholder Terraform resource (to verify setup)
resource "local_file" "lab_ready" {
  content  = "Terraform lab setup initialized successfully!"
  filename = "${path.module}/lab_setup_status.txt"
}
