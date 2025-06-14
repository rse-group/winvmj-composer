provider "google" {
  credentials = file(var.gcp_credentials)
  project = var.project_name
}