#Convert Snake Case String to Camel Case.
n=input().split('_')
print(n[0]+''.join(i.title() for i in n[1:]))
