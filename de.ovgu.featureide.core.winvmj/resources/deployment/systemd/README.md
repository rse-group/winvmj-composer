# Cloud Deployment Script Tutorial

## Prerequisites
1. **SSH Key Pairs**
    
    key pair generation script example:
    ```
     ssh-keygen-t rsa-b 4096-C "<email>"
    ```

2. **Credentials for GCP or AWS**
    - GCP : Service Account (json) with Role **Compute Network Admin** dan **Compute Admin**.
    - AWS: IAM User with permission **AmazonEC2FullAccess**, **AmazonVPCFullAccess** and **IAMReadOnlyAccess**
    
        For AWS, create json file with this format:
        ```
        {
            "access_key_id" : "<VALUE>",
            "secret_access_key" : "<VALUE>"
        }
        ```

3. **WSL For computer with WINDOWS OS**
    
    Having WSL (Windows Subsystem for Linux) is **MANDATORY** for this program to run. 
    Tested linux distribution for this program is **Ubuntu**.

4. **Domain**

    Having your own domain name is also **MANDATORY**.

5. **Terraform**

    Install Terraform on your computer, follow this documentation [here](https://developer.hashicorp.com/terraform/install)
    

## Product Preparation
The product that will be deployed neede to be in `.zip` format. The preparation step is explained in this documentation with the difference of **environment variable value** where in this program we are going to use your own domain name. 
[Product Preparation Guide](https://gitlab.com/RSE-Lab-Fasilkom-UI/PricesIDE/sple-deployment/nix-environment/-/blob/staging/README.md?ref_type=heads#product-preparation)


## Deployment

To deploy the product, run this script:
```
bash wrapper.sh <linux_username> <machine_size> <region> <product_name> <domain_name> <nginx-domain-name> <credentials-path> <cloud-provider> <instance-name> <product-prefix> <product-zip-path> <public-key-path> <private-key-path>
```

Example:
```
bash wrapper.sh rikza SMALL JAKARTA hightide hightide.rikza.net hightide.rikza /home/rikza/ta-deployment/terraform-sa.json gcp amanah-instance-gcp aisco /home/rikza/ta-deployment/hightide.zip ~/.ssh/id_rsa.pub ~/.ssh/id_rsa
```