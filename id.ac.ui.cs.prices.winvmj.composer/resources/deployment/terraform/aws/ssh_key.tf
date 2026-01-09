resource "aws_key_pair" "product_key_pair" {
  key_name   = "product-key-pair"
  public_key =  file(var.ssh_public_key_path)
}
