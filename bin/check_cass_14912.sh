RESULT=$(grep -r "CommitLogReplayException" failure | grep "Exception follows: java.lang.AssertionError" | awk -F'/' '{print $2}' | uniq | sort -t '_' -k2,2n | head -n 1)

if [ -z "$RESULT" ]; then
    echo "bug is not triggered"
    exit
fi

# ls failure/$RESULT/fullSequence_*
FILENAME=$(ls failure/$RESULT/fullSequence_*)
BASENAME=$(basename "$FILENAME")
NUM="${BASENAME#fullSequence_}"
NUM="${NUM%.report}"
HOURS=$(echo "scale=2; $NUM / 3600" | bc)

# Output the results
echo "Bug Report: $FILENAME"
# echo "Total time: $NUM seconds"
echo "Triggering time: $HOURS hours"
