import socket

ip = "192.168.225.2"
port = 36000

def HBaseCommandTest(command):
    sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    sock.connect_ex((ip, port))

    sock.send(str(command).encode())

    msg = sock.recv(8192).decode()
    print(msg)

test_commands = [
    "CREATE 'uuid3ff9e1d916ac404685410232ea566555', 'CpepL', 'OmNZnQiyB', 'B'",
    "ALTER 'uuid3ff9e1d916ac404685410232ea566555', 'ZtRTmTz'",
    "ALTER 'uuid3ff9e1d916ac404685410232ea566555', 'delete' => 'B'",
    "PUT 'uuid3ff9e1d916ac404685410232ea566555', 'uuid1b56b465f114493db036acac0ac9f873', 'ZtRTmTz':'ZtRTmTz', 'ZtRTmTz'",
    "PUT 'uuid3ff9e1d916ac404685410232ea566555', 'uuid1b56b465f114493db036acac0ac9f873', 'OmNZnQiyB':'B', 'inFeimF'",
    "PUT 'uuid3ff9e1d916ac404685410232ea566555', 'uuid3f83d9b51c624c6fb60d5be81f873266', 'OmNZnQiyB':'B', 'LOhqNoDGYyhiVGKkehFcTUwuzuO'"
]