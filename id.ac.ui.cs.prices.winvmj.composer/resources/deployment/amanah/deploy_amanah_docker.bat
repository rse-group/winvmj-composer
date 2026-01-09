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
if "%7"=="" (
    echo Error: Parameter 7 num_backends belum diisi.
    exit /b 1
)

set private_key_amanah=%1
set username_amanah=%2
set local_tunnel_port=%3
set product_name=%4
set product_local_directory=%5
set product_prefix=%6
set num_backends=%7

echo Menjalankan Deployment Ke Server Amanah dengan parameter:
echo Lokasi private key : %private_key_amanah%
echo Username amanah : %username_amanah%
echo Tunnel port : %local_tunnel_port%
echo Nama produk : %product_name%
echo Lokasi produk : %product_local_directory%
echo Prefiks produk : %product_prefix%
echo Jumlah backend : %num_backends%

:: Set direktori ke lokasi file bat saat ini
cd /d %~dp0

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

echo Meng-extract produk %product_name%...
ssh -i %private_key_amanah% %username_amanah%@localhost -p %local_tunnel_port% -o BatchMode=yes "cd /var/www/products && sudo unzip -o /tmp/%product_name%.zip && rm /tmp/%product_name%.zip" && (
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

:: Step 2 - Copy file-file docker ke server amanah

cd ../docker/docker_config
echo.
echo Meng-copy konfigurasi Docker ke server Amanah ke /tmp dulu...
scp -B -i %private_key_amanah% -P %local_tunnel_port% ^
  Dockerfile.apigateway ^
  Dockerfile.backend ^
  Dockerfile.frontend ^
  docker-compose.base.yml ^
  docker-compose.db.yml ^
  %username_amanah%@localhost:/tmp/%product_name% && (
    echo Sukses menyalin konfigurasi Docker ke /tmp/%product_name%!
) || (
    echo Gagal menyalin konfigurasi Docker ke /tmp/%product_name%.
    exit /b 1
)


echo.
echo Memindahkan file ke /var/www/products/%product_name% ...
ssh -i %private_key_amanah% -p %local_tunnel_port% %username_amanah%@localhost "sudo mkdir -p /var/www/products/%product_name% && sudo mv /tmp/%product_name%/* /var/www/products/%product_name%/ && sudo rm -rf /tmp/%product_name%"
if errorlevel 1 (
    echo Gagal memindahkan file ke /var/www/products/%product_name%.
    exit /b 1
) else (
    echo File berhasil dipindahkan ke /var/www/products/%product_name%.
)


:: Step 3 - Deployment produk dengan mengeksekusi Prices Product Deployment Script (prices_product_deployment.sh) di dalam server melalui SSH
echo Melakukan deployment produk %product_name% ke server Amanah...
set product_remote_directory=/var/www/products/%product_name%

ssh -i "%private_key_amanah%" %username_amanah%@localhost -p %local_tunnel_port% -o BatchMode=yes "cd /home/prices-deployment/nix-environment/docker_deployment && sudo bash prices_docker_deployment.sh \"%product_name%\" \"%product_remote_directory%\" \"%num_backends%\" \"%product_prefix%\"" 2>&1 && (
    echo Deployment produk berhasil.
) || (
    echo Deployment produk gagal.
    exit /b 1
)

echo Deployment selesai...
exit /b
