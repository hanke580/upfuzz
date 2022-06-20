#!/usr/bin/env python3

import sys
import upgrade_path

if __name__ == "__main__":
    application = sys.argv[1]
    version = sys.argv[2]
    instance = upgrade_path.Version(application, version)
    instance.build_compile_docker_image(True)
