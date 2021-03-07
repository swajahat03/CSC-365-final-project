from sys import *

with open(argv[1]) as f:
    lines = f.readlines()
    
newLines = []
for line in lines:
    if line != '\n':
        newLines.append(line.strip())

lines = newLines
for i in range(len(lines)):
    if i == 0:
        print("\"%s\"" % lines[i])
    else:
        print("+ \" %s\"" % lines[i])
