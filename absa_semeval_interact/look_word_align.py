import re
import codecs
def assert_entity():
   def extract(sentence, labels,type):
       entitys = []
       pattern = re.compile(r'(B-' + type + ')(I-' + type + ')*')
       m = pattern.search(labels)
       while m:
           entity_label = m.group()
           # print(entity_label)
           label_start_index = labels.find(entity_label)
           label_end_index = label_start_index + len(entity_label)
           word_start_index = labels[:label_start_index].count('-') + labels[:label_start_index].count('O')
           word_end_index = word_start_index + entity_label.count('-')
           entitys.append(' '.join(sentence[word_start_index:word_end_index]))
           labels = list(labels)
           labels[:label_end_index] = ['O' for _ in range(word_end_index)]
           labels = ''.join(labels)
           m = pattern.search(labels)
       return entitys
   writer = codecs.open('laptop14_data/assert','w','utf-8')
   gold_ops = []
   with codecs.open('laptop14_data/laptop14_op_test.txt', 'r', 'utf-8') as reader:
       for line in reader.readlines():
           if line.strip() == 'NIL':
               gold_ops.append(set())
           else:
               gold_ops.append(set([' '.join(unit.split()[:-1]) for unit in line.strip().split(', ')]))
           # print(gold_ops[-1])
   with codecs.open('laptop14_data/bio_laptop_2014_test_op','r','utf-8') as reader:
       for index,line in enumerate( reader.readlines()):
           line = line.strip()
           units = eval(line)

           sentence = [unit[0] for unit in units]
           # print(sentence)
           labels = ''.join([unit[1] for unit in units])
           # print(labels)
           entitys = set(extract(sentence,labels,'opword'))

           if len(entitys) != len(gold_ops[index]) or len(entitys & gold_ops[index]) != len(entitys):
               print(index)
               print(gold_ops[index])
               print(entitys)
           for j,entity  in enumerate(entitys):
               writer.write(entity)
               if j != len(entitys) - 1:
                   writer.write(', ')
           writer.write('\n')
assert_entity()