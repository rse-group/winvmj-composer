# HTTP-Only Deployment Guide

This guide explains how to deploy your application without SSL certificates, using just HTTP and IP addresses.

## Usage

To deploy your application with HTTP-only (no SSL), use the same command as before, but replace the domain name with the IP address:

### Original Command (with SSL/HTTPS)
```bash
bash wrapper.sh docker yes ubuntu SMALL SINGAPORE /mnt/c/Users/ASUS/aws-key.json aws 270925-1 ~/.ssh/id_ed25519.pub HeroBank HeroBank.example.com HeroBank.example.com herobank /mnt/c/Users/ASUS/HeroBank.zip ~/.ssh/id_ed25519
```

### HTTP-Only Command (no SSL)
Replace both domain name parameters with the IP address:

```bash
bash wrapper.sh docker yes ubuntu SMALL SINGAPORE /mnt/c/Users/ASUS/aws-key.json aws 270925-1 ~/.ssh/id_ed25519.pub HeroBank <IP_ADDRESS> <IP_ADDRESS> herobank /mnt/c/Users/ASUS/HeroBank.zip ~/.ssh/id_ed25519
```

For example, if your IP address is `54.123.45.67`:
```bash
bash wrapper.sh docker yes ubuntu SMALL SINGAPORE /mnt/c/Users/ASUS/aws-key.json aws 270925-1 ~/.ssh/id_ed25519.pub HeroBank 54.123.45.67 54.123.45.67 herobank /mnt/c/Users/ASUS/HeroBank.zip ~/.ssh/id_ed25519
```

## What Changes

When you use an IP address instead of a domain name:

1. **No DNS Setup Required**: The deployment script will skip DNS configuration prompts
2. **No SSL Certificate Generation**: Certbot will not attempt to generate SSL certificates
3. **HTTP-Only Nginx Configuration**: Nginx will be configured to serve content over HTTP on port 80
4. **Direct IP Access**: Your application will be accessible at `http://<IP_ADDRESS>`

## Parameter Explanation

In your command:
- `HeroBank` - Product name
- `54.123.45.67` - Replace domain with IP address (appears twice in the command)
- `54.123.45.67` - Replace nginx certificate name with IP address  
- `herobank` - Product prefix
- `/mnt/c/Users/ASUS/HeroBank.zip` - Path to your application zip file
- `~/.ssh/id_ed25519` - Path to your private SSH key

## Benefits of HTTP-Only Deployment

- **Faster Deployment**: No waiting for DNS propagation or SSL certificate generation
- **Simpler Setup**: No need to configure domain names or SSL certificates
- **Development-Friendly**: Perfect for testing and development environments
- **Cost-Effective**: No need to purchase domain names for testing

## Security Considerations

**Note**: HTTP-only deployment is recommended for:
- Development environments
- Internal/private networks
- Testing purposes

For production environments, it's recommended to use HTTPS with proper SSL certificates and domain names.