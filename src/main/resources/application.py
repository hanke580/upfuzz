#!/usr/bin/env python3
import utils

# Target distributed system: Cassandra, ZooKeeper, etc.
class Application:
    def __init__(self, name):
        # this is the app full name
        self.name = name
        # this is the directory name having all the scripts for each app
        self.abbr = utils.app_name_2_abbr(name)
