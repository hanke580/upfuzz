# TestGraphTracker

```bash
# Print the entire evolution graph
# This can be run anytime (when the fuzzing process is still ongoing)
./gradlew analyzeTestGraph     

# This will generate a file called testGraph.txt, it contains the entire evolution graph
# vim testGraph.txt

# Print a single node given a filename (e.g. 1.ser), don't need to add the path
./gradlew printNode -Pfilename="1.ser"
```


# Analyzer
```bash
./gradlew analyze -Pmode="s1"
./gradlew analyze -Pmode="s2"
```
