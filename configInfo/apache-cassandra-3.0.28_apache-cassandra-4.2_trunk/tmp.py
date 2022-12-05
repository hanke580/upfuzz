import json

f = open('addedClassConfig.json')
# f = open('commonConfig.json')
count = 0
data = json.load(f)

for i in data:
    if "warn" in i or "fail" in i:
        count+=1
        print(i)

print(count)
f.close()
