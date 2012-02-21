

f1 = open('wordlist.74k_lowercase_single_reduced.txt', mode = 'r', encoding = 'utf-8')

d = dict()

for line in f1:
    d[line.strip()] = 0
f1.close()

f2 = open('collected.text.txt', mode = 'r', encoding = 'utf-8')
for line in f2:
    words = line.strip().split(' ')
    for word in words:
        w = word.strip('.,:;')
        if word in d:
            d[word] = d[word] + 1
f2.close()

# sort the list
import operator
l = sorted(d.items(), key = operator.itemgetter(0))
#l.reverse()

f3 = open('wordlist.74k_lowercase_single_reduced_statistic.txt', mode = 'w', encoding = 'utf-8')
for i in l:
    if i[1] > 0:
        print(i[0] + ',' + str(i[1]), file = f3)

