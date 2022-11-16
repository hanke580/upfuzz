
# echo-client.py

import socket
import time
import sys
import base64

HOST = "192.168.100.2"  # The server's hostname or IP address
PORT = 18250  # The port used by the server

cmd = "".join(sys.argv[1:])
cmd = "SELECT * FROM system_schema.keyspaces;"

print(cmd)

with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
    s.connect((HOST, PORT))
    for i in range(3):
        s.sendall(bytes(cmd, "utf-8"))
        data = s.recv(51200)
        # data_decode = base64.b64decode(data)
        print("datadecode = ", data)
        print("Received " + data.decode("utf-8"))


