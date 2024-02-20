#!/usr/bin/env python3
# 1. Each version has a setup Dockerfile and corresponding scripts.
# 2. For each pair of version, there is a function that creates an old-version cluster and performs upgrade.

import json
import re
import os
import docker
import subprocess
import utils
import click
import shutil
from application import Application

version_number_pattern = re.compile("(\d+)\.(\d+)\.(\d+)(.*)")


class Config:
    def __init__(self):
        pass


# Each version has a start cluster function.
# TODO: multiple versions could share the same start cluster function with version number as the only difference.
class Version:
    def __init__(self, application, version_number):
        self.application = Application(application)
        # this version number is either a git branch or tag, e.g., "cassandra-3.11.4",
        # or a git commit, e.g., a4b4981c6f86b2138114091994db03e47e17d08b
        self.version_number = version_number

    # 1. If this version doesn't have an image, use scripts under ./build-image to build a container image.
    #   1.1 Run `compile-src.sh` to compile the target version
    #   1.2 Run `docker build` to build the image

    def build_docker_image(self, force):
        client = docker.from_env()
        image_name = utils.get_image_name(self)
        if force == True or len(client.images.list(image_name)) == 0:
            curr_dir = os.path.dirname(os.path.abspath(__file__))
            target_path = (
                curr_dir
                + "/"
                + self.application.abbr
                + "/"
                + self.version_number
                + "/build-image"
            )
            print(target_path)

            # compile src
            need_compile = True
            files = os.listdir(target_path)
            for file in files:
                if file.find(".tar.gz") != -1:
                    need_compile = False
            if need_compile:
                os.system("bash " + target_path + "/compile-src.sh")

            # build the image
            client.images.build(path=target_path, tag=(image_name))

            if need_compile:
                for file in files:
                    if file.find(".tar.gz") != -1:
                        os.system("rm -rf " + target_path + "/" + file)

    # Use a docker container to compile the source code.
    # So that you can easily switch between compile dependencies for different versions.
    # TODO: This is just a todo.
    def build_compile_docker_image(self, force):
        client = docker.from_env()
        image_name = utils.get_image_name(self) + "-compile"
        if force == True or len(client.images.list(image_name)) == 0:
            curr_dir = os.path.dirname(os.path.abspath(__file__))
            source_path = (
                curr_dir
                + "/"
                + self.application.abbr
                + "/"
                + self.version_number
                + "/compile-src"
            )
            image_path = (
                curr_dir
                + "/"
                + self.application.abbr
                + "/"
                + self.version_number
                + "/build-image"
            )
            repo_path = source_path + "/" +self.application.abbr
            if not os.path.exists(repo_path):
                exit_code = utils.clone_repo(
                    self.application.name, self.version_number, repo_path
                )
                if exit_code:
                    print(
                        "Failed to clone repository of "
                        + self.application.name
                        + " "
                        + self.version_number
                    )
                    exit(exit_code)
            shutil.copy("org.jacoco.agent.rt.jar", source_path + "/org.jacoco.agent.rt.jar")
            # build the image
            docker_client = docker.DockerClient("unix:///var/run/docker.sock")
            # generator = docker_client.build(path=source_path, tag=image_name)
            # while True:
            #     try:
            #         output = generator.__next__()
            #         output = output.decode("utf8").strip("\r\n")
            #         json_output = json.loads(output)
            #         if 'stream' in json_output:
            #             click.echo(json_output['stream'].strip('\n'))
            #     except StopIteration:
            #         click.echo("Docker image build complete.")
            #         break
            #     except ValueError:
            #         click.echo("Error parsing output from docker image build: %s" % output)
            client.images.build(path=source_path, tag=image_name)
        pass

    # TODO: This should be changed to use the compile docker image.
    def compile(self):
        curr_dir = os.path.dirname(os.path.abspath(__file__))
        target_path = (
            curr_dir
            + "/../systems/"
            + self.application.abbr
            + "/"
            + self.version_number
            + "/build-image"
        )
        os.system("sh " + target_path + "/compile-src.sh")

    # Generate yml for this version.
    def generate_yml_wrapper(self, config, test_dir, subnet, test_name, network_name):
        pass

    # This includes single node setup.
    def start_cluster(self, config, test_dir, upgrade_edge, test_name, test_id):
        subnet = utils.app_name_2_subnet(self.application.name) + str(test_id) + "."
        network_name = utils.get_network_name(upgrade_edge, test_name)
        yml_file = self.generate_yml_wrapper(
            config, test_dir, subnet, test_name, network_name
        )
        p = subprocess.Popen(
            ["docker-compose", "-f", yml_file, "up", "-d"], cwd=test_dir
        )
        p.wait()
        return yml_file

    # Properly tear down a cluster. We simply kill the nodes at this point.
    # some system might require graceful shutdown, use `docker exec` to do it.
    def teardown_cluster(self, yml_file, test_dir, docker_console_log, config):
        file_output = open(docker_console_log, "a")
        p = subprocess.Popen(
            ["docker-compose", "-f", yml_file, "logs"], stdout=file_output, cwd=test_dir
        )
        p.wait()
        file_output.close()
        p = subprocess.Popen(["docker-compose", "-f", yml_file, "down"], cwd=test_dir)
        p.wait()
