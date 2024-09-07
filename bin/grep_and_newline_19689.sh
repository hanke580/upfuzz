#!/bin/bash

# Function to process each file
process_file() {
    local file="$1"
    local count=0
    local prev_value=0
    local current_value=0

    echo "Processing file: $file"

    failure_dir=$(dirname $(dirname "$file"))

    input_line=$(grep -ir "column_index" "$failure_dir" | head -n 1)

    local number=$(echo "$input_line" | awk -F': ' '{print $NF}' | tr -d '\r\n')
    
    if [[ "$number" -ge 3 ]]; then
      return
    fi

    echo DROP num = $(grep -r "ALTER TABLE " $file | grep "DROP" | wc -l)
    insert_num=$(grep -r "INSERT INTO " $file | wc -l)
    echo INSERT num = $insert_num

    # if [[ "$insert_num" -eq 1 ]]; then
    #  return
    # fi

    echo "Processing file: $file"

    echo "column_index_size=$number"
    grep "CREATE TABLE" $file
    grep "INSERT INTO" $file
    grep "ALTER TABLE " $file | grep "DROP"

    # Use grep with -n to include line numbers for easier processing
    grep -n " rows)" "$file" | while IFS= read -r line; do
        # Extract the number of rows from the current line
        current_value=$(echo "$line" | grep -oP '\(\K[0-9]+(?= rows\))')

        # echo "$line"
        ((count++))
        
        # For the second line in a pair, compare it with the first
        if ((count % 2 == 0)); then
            echo
            if (( prev_value < current_value && current_value <= $insert_num )); then
		echo prev_value = $prev_value, current_value=$current_value
                echo "FOUND!"
            fi
        else
            # If it's the first line in a pair, store its value for comparison
            prev_value=$current_value
        fi
    done

    # After processing a file, print two newlines before moving to the next
    echo -e "\n"
}

# Read file paths from stdin
while IFS= read -r file; do
    process_file "$file"
done

