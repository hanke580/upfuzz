# Config Test

> You don't to this step if (1) the config info has been generated in `configInfo` folder or (2) not test config.

Static analysis tool [dinv-monitor](https://github.com/zlab-purdue/dinv-monitor) to extract config info used for upfuzz config testing.

#### Extract ConfigInfo

```bash
cd /PATH/TO/dinv-monitor
# JDK11
./gradlew :spotlessApply build
# Extract config info
./dinv-scripts/cass-configInfo.sh /PATH/TO/cassandra1 /PATH/TO/cassandra2
# Copy the config info to upfuzz
mkdir /PATH/TO/upfuzz/configInfo/apache-cassandra-X.X.X_apache-cassandra-X.X.X/
cp output/* /PATH/TO/upfuzz/configInfo/apache-cassandra-X.X.X_apache-cassandra-X.X.X/
```

### Blacklist
The following configuration are causing issues while mutating in Cassandra:

- `native_transport_max_negotiable_version` (supported values 3 <= x <= 4)
- `repair_session_max_tree_depth` (supported values x >= 10)
