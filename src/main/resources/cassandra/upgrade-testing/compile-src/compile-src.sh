script="$0"
CURR_DIR="$(dirname $script)"
rm -rf $CURR_DIR/cassandra
SRC_LOCATION=`cat ${CURR_DIR}/../../cassandra.srclocation`
cp -r $SRC_LOCATION $CURR_DIR/
cd $CURR_DIR/cassandra
ant clean
git checkout cassandra-3.11.6
ant
rm -rf .git/
cd -

tar -czf $CURR_DIR/cassandra.tar.gz -C $CURR_DIR cassandra
