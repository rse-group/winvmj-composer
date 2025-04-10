@echo off
:: Environment variables required:
:: - 'private_key_amanah': Direktori lokasi private key akses server Amanah
:: - 'username_amanah': Credential username untuk akses server Amanah
:: - 'local_tunnel_port': Port lokal yang dibuka untuk akses server Amanah melalui SSH tunneling server Kawung
echo Eclipse Deployment Script ini bertujuan untuk melakukan deployment produk PRICES-IDE melalui Eclipse.
echo:

:: Step 1 - Copy komponen produk ke server Amanah
set product_name=${productName}
echo Script ini akan melakukan deployment produk PRICES-IDE bernama %product_name% ke server Amanah. Silahkan isi informasi produk terkait.
echo:

set /p product_local_directory= "> Masukkan lokasi direktori folder produk tersebut (pastikan folder di-archive .zip dengan nama dan susunan file yang sesuai): " 

echo:
echo Meng-copy produk %product_name% ke server Amanah...
scp -B -i %private_key_amanah% -P %local_tunnel_port% -r %product_local_directory% %username_amanah%@localhost:/tmp && (
    echo Sukses!
) || (
    echo Terdapat error ketika meng-copy produk %product_name% ke server Amanah.
    echo Mohon dicoba kembali.  
    echo Note: Pastikan credential akses server sudah benar serta tersedia produk dengan informasi dan susunan yang tepat.
    exit
)
echo:
echo Meng-extract produk %product_name%...
ssh -i %private_key_amanah% %username_amanah%@localhost -p %local_tunnel_port% -o BatchMode=yes "cd /var/www/products && sudo unzip /tmp/%product_name%.zip && rm /tmp/%product_name%.zip" && (
    echo Sukses!
    echo:
    echo Selesai memindahkan produk %product_name% ke server Amanah!
) || (
    echo Terdapat error ketika meng-extract produk %product_name% ke server Amanah.
    echo Mohon dicoba kembali.
    echo Note: Pastikan credential akses server sudah benar serta tersedia produk dengan informasi dan susunan yang tepat.
    exit
)
echo:

:: Step 2 - Deployment produk dengan mengeksekusi Prices Product Deployment Script (prices_product_deployment.sh) di dalam server melalui SSH
echo Melakukan deployment produk %product_name% ke server Amanah...
set product_remote_directory=/var/www/products/%product_name%

ssh -i %private_key_amanah% %username_amanah%@localhost -p %local_tunnel_port% -o BatchMode=yes "cd /home/prices-deployment/nix-environment && nix-shell --run 'bash prices_product_deployment.sh %product_name% %product_remote_directory%'" && (
    echo:
    echo Produk %product_name% berhasil di-deploy!
    echo Dengan ini, deployment produk PRICES-IDE berakhir dengan sukses.
) || (
    echo:
    echo Terdapat error dalam deployment produk %product_name% ke server Amanah.
    echo Note: Periksa komponen produk dan mohon deployment dicoba kembali.
)
exit
