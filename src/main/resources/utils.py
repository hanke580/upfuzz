#!/usr/bin/env python3
import os
import subprocess


def get_image_name(version):
    return "image_" + version.application.abbr + "_" + version.version_number


# TODO: Ideally we should use the test name in the container name, not just one single version
def get_container_name_prefix(version, test_name="stressTest"):
    return (
        "container_"
        + version.application.abbr
        + "_"
        + version.version_number
        + "_"
        + test_name
    )


def get_test_dir(upgrade_edge, test_type, test_name):
    curr_dir = os.path.dirname(os.path.abspath(__file__))
    tests_dir = curr_dir + "/../tests"
    test_dir = (
        tests_dir
        + "/"
        + upgrade_edge.from_version.application.abbr
        + "/"
        + test_type
        + "/"
        + upgrade_edge.from_version.version_number
        + "_to_"
        + upgrade_edge.target_version.version_number
    )

    while os.path.exists(test_dir):
        test_dir = test_dir + "x"

    return test_dir + "/" + test_name


def get_network_name(upgrade_edge, test_name):
    return (
        "network_"
        + upgrade_edge.from_version.version_number
        + "_to_"
        + upgrade_edge.target_version.version_number
        + "_"
        + test_name
        + "_dc1ring"
    )


def is_sha1(maybe_sha):
    if len(maybe_sha) != 40:
        return False
    try:
        sha_int = int(maybe_sha, 16)
    except ValueError:
        return False
    return True


def app_name_2_abbr(name):
    switcher = {
        "cassandra": "cassandra",
        "zookeeper": "zookeeper",
        "kafka": "kafka",
        "hdfs": "hdfs",
        "mapreduce": "mapreduce",
        "yarn": "yarn",
        "mesos": "mesos",
        "hbase": "hbase",
        "hive": "hive",
    }
    return switcher.get(name)


def app_name_2_subnet(name):
    switcher = {
        "cassandra": "252.11.",
        "zookeeper": "252.10.",
        "kafka": "252.12.",
        "hdfs": "252.13.",
        "mapreduce": "252.14.",
        "yarn": "252.15.",
        "mesos": "252.16.",
        "hbase": "252.17.",
        "hive": "252.18.",
    }
    return switcher.get(name)


def clone_repo(distro_system, target_version, target_path):
    distro_system_repo = {
        "hdfs": "https://github.com/apache/hadoop.git",
        "cassandra": "https://github.com/apache/cassandra.git",
    }
    if not distro_system in distro_system_repo:
        return -1
    repository = distro_system_repo[distro_system]
    prcoess = subprocess.Popen(
        ["git", "clone", "-b", target_version, "--depth", "1", repository, target_path]
    )
    prcoess.wait()
    return prcoess.returncode
