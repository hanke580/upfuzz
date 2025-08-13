# take 3 values, compute the average

import sys

values = [float(x) for x in sys.argv[1:]]

print(sum(values) / len(values))