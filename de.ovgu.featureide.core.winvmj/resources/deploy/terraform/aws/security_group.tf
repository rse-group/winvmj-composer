resource "aws_security_group" "ingress_ssh_security_group" {
  name        = "ssh-security-group"
  description = "Allow SSH inbound traffic"
  vpc_id      = aws_default_vpc.default.id

  ingress {
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "ssh-security-group"
  }
}

resource "aws_security_group" "ingress_https_security_group" {
  name        = "https-security-group"
  description = "Allow HTTPS inbound traffic"
  vpc_id      = aws_default_vpc.default.id

  ingress {
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "https-security-group"
  }
}

resource "aws_security_group" "egress_security_group" {
  name        = "egress-security-group"
  description = "Allow outbound traffic"
  vpc_id      = aws_default_vpc.default.id

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "egress-security-group"
  }
}

resource "aws_security_group" "ingress_http_security_group" {
  name        = "http-security-group"
  description = "Allow HTTP inbound traffic"
  vpc_id      = aws_default_vpc.default.id

  ingress {
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "http-security-group"
  }
}
