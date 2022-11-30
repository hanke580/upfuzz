"""
Python script for log check
1. Grep all the error log from the failure/ folder

$ grep -r "ERROR"
2. Truncate the timestamp using awk
3. Use sort and unique to grep all the uniq exceptions
4. For each unique error, grep again, find all the crash folder related to it
"""

import os
import json
import subprocess

# subprocess.run(["grep", "-r", "-A", "4", "ERROR", "/Users/hanke/Desktop/Project/upfuzz/system.log"])

def print_list(l):
    for i in l:
        print("size = ", len(i))
        print(i)

def saveUniqueFailure(uniq_failure):
    dir = os.path.join(os.getcwd(), "failure_stat")
    if not os.path.exists(os.path.join(os.getcwd(), dir)):
        os.mkdir(dir)

    with open(os.path.join(dir, "unique_error.json"), 'w') as f:
        json.dump(uniq_failure, f)


def grepUniqueError():
    str = ""
    proc = subprocess.Popen(["grep", "-hr", "-A", "4", "ERROR", "system.log"],stdout=subprocess.PIPE)
    for line in proc.stdout:
        #the real code does filtering here
        line_str = line.decode().rstrip()
        print("line = ", line_str)
        if ("ERROR" in line_str):
            arr = line_str.split()
            str += arr[0]
            str += " "
            assert len(arr) > 5
            for i in range(4, len(arr)):
                str += arr[i]
                if i != len(arr):
                    str += " "
        else:
            str += line_str
        str += "\n"

    error_arr = str.split("--")
    filter_arr = []
    for e in error_arr:
        filter_arr.append(e.strip())

    filter_arr = list(set(filter_arr))
    print_list(filter_arr)

"""
for all the failures, grep again using the class:line number, get all case ID
this time, how do we perform the grep? By class:line, there is a problem where the

Grep1: using class:line number
Grep2: using 
"""









