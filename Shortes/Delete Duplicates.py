#Delete Ducplicate in a given set of strings
for i in range(int(input())):
  print("".join(dict.fromkeys(input())))
