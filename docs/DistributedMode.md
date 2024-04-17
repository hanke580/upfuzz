# Distributed testing mode
FuzzingServer config.json: listening to all IPs
```json
"serverHost" : "0.0.0.0",
```

FuzzingClient config.json
```json
"serverHost" : "x.x.x.x",
```

However, the blocking thing is that the configuration need to be transferred through socket.

Solution
(1) Disable configuration mutation.
(2) Use a shared folder between servers could solve this. 
