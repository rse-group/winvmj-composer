@echo off
:: Usage: deploy_amanah.bat <private_key_path> <username> <local_tunnel_port> <product_name> <product_local_directory>

if "%1"=="" (
    echo Error: Parameter 1 private_key_path belum diisi.
    exit /b 1
)
if "%2"=="" (
    echo Error: Parameter 2 username belum diisi.
    exit /b 1
)
if "%3"=="" (
    echo Error: Parameter 3 local_tunnel_port belum diisi.
    exit /b 1
)
if "%4"=="" (
    echo Error: Parameter 4 product_name belum diisi.
    exit /b 1
)
if "%5"=="" (
    echo Error: Parameter 5 product_local_directory belum diisi.
    exit /b 1
)
if "%6"=="" (
    echo Error: Parameter 6 product_prefix belum diisi.
    exit /b 1
)

set private_key_amanah=%1
set username_amanah=%2
set local_tunnel_port=%3
set product_name=%4
set product_local_directory=%5
set product_prefix=%6

echo Menjalankan Deployment Ke Server Amanah dengan parameter:
echo Lokasi private key : %private_key_amanah%
echo Username amanah : %username_amanah%
echo Tunnel port : %local_tunnel_port%
echo Nama produk : %product_name%
echo Lokasi produk : %product_local_directory%
echo Prefiks produk : %product_prefix%


:: Step 1 - Copy komponen produk ke server Amanah
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

ssh -i %private_key_amanah% %username_amanah%@localhost -p %local_tunnel_port% -o BatchMode=yes "cd /home/prices-deployment/nix-environment && nix-shell --run 'bash prices_product_deployment.sh %product_name% %product_remote_directory% %product_prefix%'" && (
    echo:
    echo Produk %product_name% berhasil di-deploy!
    echo Dengan ini, deployment produk PRICES-IDE berakhir dengan sukses.
) || (
    echo:
    echo Terdapat error dalam deployment produk %product_name% ke server Amanah.
    echo Note: Periksa komponen produk dan mohon deployment dicoba kembali.
)
exit