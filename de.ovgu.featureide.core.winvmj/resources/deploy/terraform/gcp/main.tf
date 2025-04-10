resource "google_compute_network" "vpc_network" {
  name = "my-vpc-network"
}

resource "google_compute_firewall" "default" {
  name    = "default-allow-http-https-ssh"
  network = google_compute_network.vpc_network.name

  allow {
    protocol = "tcp"
    ports    = ["80", "443", "22"]
  }

  source_ranges = ["0.0.0.0/0"]
}

resource "google_compute_instance" "default" {
  name         = var.instance_name
  machine_type = var.instance_type
  zone         = var.zone

  boot_disk {
    initialize_params {
      image = "projects/ubuntu-os-cloud/global/images/ubuntu-2004-focal-v20240519"
    }
  }

  metadata = {
    ssh-keys = "${var.ssh_user}:${file("~/.ssh/id_rsa.pub")}"
  }

  network_interface {
    network = google_compute_network.vpc_network.name

    access_config {
    }
  }

  tags = ["http-server", "https-server", "ssh-server"]
}

output "instance_ip" {
  value = google_compute_instance.default.network_interface[0].access_config[0].nat_ip
  description = "Public IP of the VM instance"
}
