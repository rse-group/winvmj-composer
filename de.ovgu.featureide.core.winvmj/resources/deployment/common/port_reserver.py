import csv
import sys
import socket

def port_reserver(lower, upper, csv_file):
    used_port = read_list_port(csv_file)
    for port in range(lower,upper):
        if port in used_port:
            continue
        is_port_available = check_port(('127.0.0.1',port))
        if is_port_available == True:
            return port
    return None

def read_list_port(csv_files):
    with open(csv_files,'r') as file:
        csv_reader = csv.reader(file, delimiter=',')
        port_list = []
        for row in csv_reader:
            port_list.append(int(row[1]))
        return port_list

def write_new_port(csv_file, product_name, port):
    with open(csv_file,'a', newline='') as file:
        csv_writer = csv.writer(file)
        csv_writer.writerow([product_name,port])

def check_port(address):
    try:
        socket_object = socket.create_server(address, family=socket.AF_INET)
        socket_object.close()
        return True # port bisa dipakai
    except:
        return False # port tidak bisa dipakai

def main(product_name, lower_port, upper_port, port_csv_file):
    available_port = port_reserver(lower_port, upper_port, port_csv_file)
    if available_port:
        write_new_port(port_csv_file, product_name, available_port)
    print(available_port)

if __name__ == '__main__':
    product_name = str(sys.argv[1])
    lower_port = int(sys.argv[2])
    upper_port = int(sys.argv[3])
    port_csv_file = str(sys.argv[4])
    main(product_name, lower_port, upper_port, port_csv_file)
