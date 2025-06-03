variable "region" {
  description = "AWS region where the instance will be created"
  type        = string
  default     = "ap-southeast-1" #Singapore
}

variable "instance_type" {
  description = "The type of AWS EC2 instance to launch"
  type        = string
  default     = "t2.medium"
}

variable "instance_name" {
  description = "The name of AWS EC2 instance to launch"
  type        = string
  default     = "amanah-instance"
}

variable "aws_access_key" {}
variable "aws_secret_key" {}