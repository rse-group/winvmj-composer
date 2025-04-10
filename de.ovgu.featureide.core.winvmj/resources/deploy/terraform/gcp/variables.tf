variable "zone" {
  description = "GCP zone where the instance will be created"
  type        = string
  default     = "asia-southeast2-c"
}

variable "instance_type" {
  description = "The type of GCP VM instance to launch"
  type        = string
  default     = "n1-standard-1"
}

variable "instance_name" {
  description = "The name of GCP VM instance to launch"
  type        = string
  default     = "amanah-instance"
}

variable "project_name" {
  description = "GCP project name"
  type        = string
  default     = "ta-rikza-deployment"
}

variable "gcp_credentials" {}

variable "ssh_user" {
  description = "The SSH username for the instance"
  type        = string
}
