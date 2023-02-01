script="$0"
CURR_DIR="$(dirname $script)"

docker build -t cass-image-3.11.4 $CURR_DIR
