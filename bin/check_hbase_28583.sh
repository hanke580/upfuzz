grep -rl 'old_table_schema' failure | sort -t '_' -k2,2n | head -n 2
