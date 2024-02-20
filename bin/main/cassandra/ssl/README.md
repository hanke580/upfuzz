# Configure SSL for Cassandra Cluster

[Reference](http://cloudurable.com/blog/cassandra-ssl-cluster-setup/index.html)

## Internode Encryption


Generate keys
- `./setupkeys-cassandra-security.sh`

Copy keys to the configuration folder `/opt/cassandra/conf/certs/`
- `./install-serts.sh`


Modify cassandra.yaml

```yaml
server_encryption_options:
    internode_encryption: all
    keystore: /opt/cassandra/conf/certs/cassandra.keystore
    keystore_password: cassandra
    truststore: /opt/cassandra/conf/certs/cassandra.truststore
    truststore_password: cassandra
    # More advanced defaults below:
    protocol: TLS

client_encryption_options:
    enabled: true
    # If enabled and optional is set to true encrypted and unencrypted connections are handled.
    optional: false
    keystore: /opt/cassandra/conf/certs/cassandra.keystore
    keystore_password: cassandra
    truststore: /opt/cassandra/conf/certs/cassandra.truststore
    truststore_password: cassandra
    require_client_auth: true
    protocol: TLS

```

Restart cassandra, we can see that cassandra is starting up using SSL port (7001)

```bash
INFO  [main] 2022-11-06 22:31:16,210 QueryProcessor.java:174 - Preloaded 0 prepared statements
INFO  [main] 2022-11-06 22:31:16,211 StorageService.java:699 - Cassandra version: 3.11.13
INFO  [main] 2022-11-06 22:31:16,211 StorageService.java:700 - Thrift API version: 20.1.0
INFO  [main] 2022-11-06 22:31:16,211 StorageService.java:701 - CQL supported versions: 3.4.4 (default: 3.4.4)
INFO  [main] 2022-11-06 22:31:16,211 StorageService.java:703 - Native protocol supported versions: 3/v3, 4/v4, 5/v5-beta (default: 4/v4)
INFO  [main] 2022-11-06 22:31:16,238 IndexSummaryManager.java:87 - Initializing index summary manager with a memory pool size of 24 MB and a resize interval of 60 minutes
INFO  [main] 2022-11-06 22:31:16,256 MessagingService.java:704 - Starting Encrypted Messaging Service on SSL port 7001
INFO  [main] 2022-11-06 22:31:16,281 OutboundTcpConnection.java:108 - OutboundTcpConnection using coalescing strategy DISABLED
```


## Server Client Encryption

### modify cqlshrc

```bash
$ mkdir ~/.cassandra # or this could be created already
$ cd ~/.cassandra
$ touch cqlshrc # edit this file
```

```bash
[connection]
hostname = 192.168.50.4
port = 9042
factory = cqlshlib.ssl.ssl_transport_factory


[ssl]
certfile =  /opt/cassandra/conf/certs/test_CLIENT.cer.pem
validate = false
# Next 2 lines must be provided when require_client_auth = true in the cassandra.yaml file
userkey = /opt/cassandra/conf/certs/test_CLIENT.key.pem
usercert = /opt/cassandra/conf/certs/test_CLIENT.cer.pem
```

### test connection
```bash
$ /opt/cassandra/bin/cqlsh --ssl
Connected to test at 192.168.50.4:9042.
[cqlsh 5.0.1 | Cassandra 3.9 | CQL spec 3.4.2 | Native protocol v4]
Use HELP for help.
```
