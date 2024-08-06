find failure -type f -name "inconsistency_*" -print0 | xargs -0 grep -l "SELECT" | awk -F'_' '{print $0, $(NF-1)}' | sort -k2,2n | cut -d' ' -f1 > input_file
