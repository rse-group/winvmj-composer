@echo off
:: Environment variables required:
:: - 'private_key_amanah': Direktori lokasi private key akses server Amanah
:: - 'username_amanah': Credential username untuk akses server Amanah
:: - 'local_tunnel_port': Port lokal yang dibuka untuk akses server Amanah melalui SSH tunneling server Kawung
echo Script ini bertujuan untuk melakukan deployment produk PRICES-IDE melalui Eclipse.
echo:

:: Step 1 - Copy komponen produk ke server Amanah
echo Silahkan isi informasi produk PRICES-IDE yang akan di-deploy ke server Amanah.
echo:

set product_name=${productName}
set /p product_local_directory= "> Masukkan lokasi direktori folder produk tersebut (pastikan folder di-archive dengan nama dan susunan file yang sesuai): " 
set /p product_backend_port= "> Masukkan port back-end produk yang telah di-compile PRICES-IDE di Eclipse (port dapat dilihat di 'src/aisco.product.[nama_produk]/[nama_produk].java'): "

echo:
echo Meng-copy produk %product_name% ke server Amanah...
scp -B -i %private_key_amanah% -P %local_tunnel_port% -r %product_local_directory% %username_amanah%@localhost:~/nix-prices-deployment/products && (
    echo Sukses!
) || (
    echo Terdapat error ketika meng-copy produk %product_name% ke server Amanah.
    echo Mohon dicoba kembali.  
    echo Note: Pastikan credential akses server sudah benar serta tersedia produk dengan informasi dan susunan yang tepat.
    exit
)
echo:
echo Meng-extract produk %product_name%...
ssh -i %private_key_amanah% %username_amanah%@localhost -p %local_tunnel_port% -o BatchMode=yes "cd ~/nix-prices-deployment/products && unzip %product_name%.zip && rm %product_name%.zip" && (
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

:: Step 2 - Deployment produk dengan mengesekusi script deployment (prices_product_deployment.sh) di dalam server melalui SSH
echo Melakukan deployment produk %product_name% ke server Amanah...
set product_remote_directory=/home/%username_amanah%/nix-prices-deployment/products/%product_name%

ssh -i %private_key_amanah% %username_amanah%@localhost -p %local_tunnel_port% -o BatchMode=yes "cd ~/nix-prices-deployment && nix-shell --run 'bash prices_product_deployment.sh %product_name% %product_remote_directory% %product_backend_port%'" && (
    echo:
    echo Produk %product_name% berhasil di-deploy!
    echo Dengan ini, deployment produk PRICES-IDE berakhir dengan sukses.
) || (
    echo:
    echo Terdapat error dalam deployment produk %product_name% ke server Amanah.
    echo Note: Periksa komponen produk dan mohon deployment dicoba kembali.
)
exit
