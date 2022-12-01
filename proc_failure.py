"""
Python script for log check
1. Grep all the error log from the failure/ folder

$ grep -r "ERROR"
2. Truncate the timestamp using awk
3. Use sort and unique to grep all the uniq exceptions
4. For each unique error, grep again, find all the crash folder related to it
"""

import os
import sys
import json
import subprocess
from functools import cmp_to_key


failure_dir = "failure"
failure_stat_dir = "failure_stat"

full_stop_crash = "fullstop_crash"
event_crash = "event_crash"
inconsistency = "inconsistency"

HDFS_BLACK_LIST = ["RECEIVED SIGNAL"]

# subprocess.run(["grep", "-r", "-A", "4", "ERROR", "/Users/hanke/Desktop/Project/upfuzz/system.log"])

def print_list(l):
    for i in l:
        print(i)

# Map {uniqFailure => set(failure cases)}
def save_failureinfo(error2failure):
    dir = os.path.join(os.getcwd(), failure_stat_dir)
    if not os.path.exists(os.path.join(os.getcwd(), dir)):
        os.mkdir(dir)

    with open(os.path.join(dir, "unique_error.json"), 'w') as f:
        json.dump(error2failure, f)


def cass_grepUniqueError():
    proc = subprocess.Popen(["grep", "-hr", "ERROR", "failure"],stdout=subprocess.PIPE)
    error_arr = []
    for line in proc.stdout:
        #the real code does filtering here
        line_str = line.decode().rstrip()
        if "ERROR LOG" in line_str:
            continue
        str = ""
        arr = line_str.split()
        str += arr[0]
        str += " "
        min = 5 + more_match
        if (min > len(arr)):
            min = len(arr)
        for i in range(4, min):
            str += arr[i]
            if i != len(arr):
                str += " "
        error_arr.append(str.strip())

    print("err size = ", len(error_arr))
    unique_errors = list(set(error_arr))
    print("unique err size = ", len(unique_errors))
    return unique_errors


# compare 2 more words following the error message
more_match = 3


def hdfs_grepUniqueError():
    proc = subprocess.Popen(["grep", "-hr", "ERROR", failure_dir],stdout=subprocess.PIPE)
    error_arr = []
    for line in proc.stdout:
        #the real code does filtering here
        line_str = line.decode().rstrip()
        if "ERROR LOG" in line_str:
            continue
        blacklisted = False
        for blacklist_error in HDFS_BLACK_LIST:
            if blacklist_error in line_str:
                blacklisted = True
                break
        if blacklisted:
            continue
        arr = line_str.split()
        if (len(arr) > 3):
            str = ""
            min = 4 + more_match
            if (min > len(arr)):
                min = len(arr)
            for i in range(2, min):
                str += arr[i]
                if i != len(arr):
                    str += " "
            error_arr.append(str.strip())
        else:
            error_arr.append(line_str.strip())

    print("err size = ", len(error_arr))
    unique_errors = set(error_arr)
    print("unique err size = ", len(unique_errors))
    return unique_errors


"""
for all the failures, grep again using the class:line number, get all case ID
this time, how do we perform the grep? By class:line, there is a problem where the

return the map [unique_failure -> cases]
"""
def hdfs_construct_map(unique_errors):
    """
    for each arr, grep failure folder again
    """
    error2failure = {}

    for unique_error in unique_errors:
        target = unique_error
        error2failure[target] = set()

        proc = subprocess.Popen(["grep", "-lr", target, failure_dir],stdout=subprocess.PIPE)
        for line in proc.stdout:
            line_str = line.decode().rstrip()
            path_arr = line_str.split("/")
            failure_folder = path_arr[1]
            # print("line = ", line_str)
            error2failure[target].add(failure_folder)
            
    # transform to list
    for error_msg in error2failure:
        error2failure[error_msg] = list(error2failure[error_msg])
        error2failure[error_msg].sort(key=cmp_to_key(sort_failureIdx))
    return error2failure

def cass_construct_map(unique_errors):
    """
    for each arr, grep failure folder again
    """
    print("log status")
    error2failure = {}

    for unique_error in unique_errors:
        target = unique_error[6:]
        print("target = ", target)
        error2failure[target] = set()
        proc = subprocess.Popen(["grep", "-lr", target, failure_dir],stdout=subprocess.PIPE)
        for line in proc.stdout:
            line_str = line.decode().rstrip()
            path_arr = line_str.split("/")
            failure_folder = path_arr[1]
            error2failure[target].add(failure_folder)


    # transform to list
    for error_msg in error2failure:
        error2failure[error_msg] = list(error2failure[error_msg])
        error2failure[error_msg].sort(key=cmp_to_key(sort_failureIdx))
    return error2failure


def processHDFS():
    unique_errors = hdfs_grepUniqueError()
    error2failure = hdfs_construct_map(unique_errors)

    for error_msg in error2failure:
        print("error: ", error_msg, "\t size = ", len(error2failure[error_msg]))
    save_failureinfo(error2failure)

def processCassandra():
    unique_errors = cass_grepUniqueError()
    error2failure = cass_construct_map(unique_errors)
    for error_msg in error2failure:
        print("error: ", error_msg, "\t size = ", len(error2failure[error_msg]))
    save_failureinfo(error2failure)

def read_failureInfo():
    with open(os.path.join(failure_stat_dir, "unique_error.json"), 'r') as f:
        error2failure = json.load(f)
        print(len(error2failure))
        for error_msg in error2failure:
            print("error msg: ", error_msg)
            for failureIdx in error2failure[error_msg]:
                print("\t - ", failureIdx)
            print()
    read_failure_list(full_stop_crash)
    read_failure_list(event_crash)
    read_failure_list(inconsistency)

def read_failure_list(target):
    with open(os.path.join(failure_stat_dir, target + ".json"), 'r') as f:
        failure_list = json.load(f)
        print(target)
        for failureIdx in failure_list:
            print("\t - ", failureIdx)
        print()

def sort_failureIdx(a, b):
    a_idx = int(a.split("_")[1])
    b_idx = int(b.split("_")[1])
    if a_idx > b_idx:
        return 1;
    elif a_idx == b_idx:
        return 0;
    else:
        return -1


def getFailure(target):
    failure_list = []
    proc = subprocess.Popen(["find", "failure", "-name", target] ,stdout=subprocess.PIPE)
    for line in proc.stdout:
        line_str = line.decode().rstrip()
        path_arr = line_str.split("/")
        failure_folder = path_arr[1]
        failure_list.append(failure_folder)

    failure_list.sort(key=cmp_to_key(sort_failureIdx))
    save_failure_info(failure_list, target)

def save_failure_info(failureInfo, filename):
    dir = os.path.join(os.getcwd(), failure_stat_dir)
    if not os.path.exists(os.path.join(os.getcwd(), dir)):
        os.mkdir(dir)

    with open(os.path.join(dir, filename + ".json"), 'w') as f:
        json.dump(failureInfo, f)

def getEventCrashFailure():
    getFailure("event_crash")

def getFullStopCrashFailure():
    pass

def getInconsistencyFailure():
    pass


if __name__ == "__main__":
    args = sys.argv[1:]
    if len(args) != 1:
        print("usage: python3 proc_failure.py SYSTEM")
        print("usage: python3 proc_failure.py read")
        exit(1)
    
    if args[0] == "read":
        read_failureInfo()
        exit(0)
    if args[0] == "cassandra":
        processCassandra()
    elif args[0] == "hdfs":
        processHDFS()
    else:
        print("unknow input", args[0])
        print("please try hdfs or cassandra")
    getFailure("event_crash")
    getFailure("fullstop_crash")
    getFailure("inconsistency")

