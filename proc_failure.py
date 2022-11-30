"""
Python script for log check
1. Grep all the error log from the failure/ folder
2. Truncate the timestamp using awk
3. Use sort and unique to grep all the uniq exceptions
4. For each unique error, grep again, find all the crash folder related to it
"""


