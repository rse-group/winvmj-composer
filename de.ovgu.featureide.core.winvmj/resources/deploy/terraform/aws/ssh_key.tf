resource "aws_key_pair" "product_key_pair" {
  key_name   = "product-key-pair"
  public_key =  file("~/.ssh/id_rsa.pub")
}
