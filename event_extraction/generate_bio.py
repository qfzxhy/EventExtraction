import codecs
import os
import re
import csv
def load_trigger():
    triggers = []
    for file in os.listdir('datas/trigger'):
        print(file)
        with codecs.open('datas/trigger/'+file,'r','utf-8') as reader:
            for line in reader.readlines():
                triggers.append(line.strip())
    return triggers

triggers = load_trigger()
def trans_bio(input_path,output_path):
    writer = codecs.open(output_path,'w','utf-8')
    with codecs.open(input_path,'r','utf-8') as reader:
        for line in reader:
            line = line.strip()
            units = line.split()
            us = [unit.split('/') for unit in units]
            for u in us:
                if u[0] in triggers and not re.match('[B|I]-.*',u[2]):
                    u[2] = 'TRIG'
                # if re.match('B-.*',u[2]):
                #     u[2] = 'B-TERM'
                # if re.match('I-.*',u[2]):
                #     u[2] = 'I-TERM'
            uss = ['/'.join(u) for u in us]
            res = ' '.join(uss)
            writer.write(res+'\n')
    pass
def trans_2(input_path,output_path):
    output = csv.writer(codecs.open(output_path, 'w', 'utf-8'), delimiter='#')
    lines = []
    with codecs.open(input_path, 'r', 'utf-8') as reader:
        for line in reader.readlines():
            line = line.strip()
            units = line.split()
            us = [unit.split('/') for unit in units]
            for unit in us:
                output.writerow([unit[0], unit[1], unit[2], str(len(units))])
            output.writerow([])
    pass

trans_bio('datas/trainCorpus','datas/newtrainCorpus')
trans_bio('datas/testCorpus','datas/newtestCorpus')
trans_2('datas/newtrainCorpus','datas/traindatas')
trans_2('datas/newtestCorpus','datas/testdatas')