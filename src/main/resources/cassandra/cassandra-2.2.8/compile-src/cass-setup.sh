tar zxf /cassandra.tar.gz

# create necessary dirs (some version of cassandra cannot create these)
mkdir -p /var/log/cassandra
mkdir -p /var/lib/cassandra

# change environment setup

# Note that runtime setup such as SEED is set in 
#sed -i 's/Xss128k/Xss256k/' /cassandra/conf/cassandra-env.sh
sed -i 's/#MAX_HEAP_SIZE="4G"/MAX_HEAP_SIZE="512M"/' /cassandra/conf/cassandra-env.sh
sed -i 's/#HEAP_NEWSIZE="800M"/HEAP_NEWSIZE="200M"/' /cassandra/conf/cassandra-env.sh

# config on-disk data locations
sed -i 's/^# data_file_directories/data_file_directories/' /cassandra/conf/cassandra.yaml
sed -i 's/^#     - \/var\/lib\/cassandra\/data/    - \/var\/lib\/cassandra\/data/' /cassandra/conf/cassandra.yaml
sed -i 's/^# hints_directory/hints_directory/' /cassandra/conf/cassandra.yaml
sed -i 's/^# commitlog_directory/commitlog_directory/' /cassandra/conf/cassandra.yaml
sed -i 's/^# cdc_raw_directory/cdc_raw_directory/' /cassandra/conf/cassandra.yaml
sed -i 's/^# saved_caches_directory/saved_caches_directory/' /cassandra/conf/cassandra.yaml

export CASSANDRA_HOME=/cassandra/
