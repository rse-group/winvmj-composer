provider "aws" {
  region = var.region
  access_key = var.aws_access_key
  secret_key = var.aws_secret_key
}

data "aws_ami" "ubuntu" {
  most_recent = true

  filter {
    name   = "name"
    values = ["ubuntu/images/hvm-ssd/ubuntu-focal-20.04-amd64-server-*"]
  }

  filter {
    name   = "virtualization-type"
    values = ["hvm"]
  }

  owners = ["099720109477"] # Canonical's AWS account ID
}

resource "aws_instance" "amanah_app_default_template" {
  ami           = data.aws_ami.ubuntu.id
  instance_type = var.instance_type

  key_name = aws_key_pair.product_key_pair.key_name

  vpc_security_group_ids = [
    aws_security_group.ingress_ssh_security_group.id,
    aws_security_group.ingress_https_security_group.id,
    aws_security_group.ingress_http_security_group.id,
    aws_security_group.egress_security_group.id
  ]

  tags = {
    Name = var.instance_name
  }
}
output "instance_ip" {
  value       = aws_instance.amanah_app_default_template.public_ip
  description = "Public IP of the EC2 instance"
}

