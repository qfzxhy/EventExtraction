import codecs
import re
def trans_bio_normal():
    writer = codecs.open('datas/entity_datas','w','utf-8')
    lines = codecs.open('datas/trainCorpus','r','utf-8').readlines()
    for line in lines:
        line = line.strip()
        units = line.split()
        words = [unit.split('/')[0] for unit in units]
        tags = [unit.split('/')[2] for unit in units]
        entitys = extract_entity(words,''.join(tags))
        writer.write(' '.join(words) + '\n')
        writer.write('#'.join(entitys)+'\n')

    pass
def extract_entity(words,labels):
    entitys = []
    pattern = re.compile(r'(B-[^BIO]+)(I-[^BIO]+)*')
    m = pattern.search(labels)
    while m:
        entity_label = m.group()
        print(entity_label)
        label_start_index = labels.find(entity_label)
        label_end_index = label_start_index + len(entity_label)
        word_start_index = labels[:label_start_index].count('-') + labels[:label_start_index].count('O')
        word_end_index = word_start_index + entity_label.count('-')
        entitys.append(''.join(words[word_start_index:word_end_index]))
        labels = list(labels)
        labels[:label_end_index] = ['O' for _ in range(word_end_index)]
        labels = ''.join(labels)
        m = pattern.search(labels)
    return entitys

def trans():
    lines = []
    with codecs.open('datas/entity_datas','r','utf-8') as reader:
        for line in reader.readlines():
            lines.append(line.strip())
    datas = []
    i = 0
    while i < len(lines):
        datas.append([lines[i],lines[i+1],lines[i+2]])
        i += 3

        # if len(lines[i]) < max(len(lines[i+1]),len(lines[i+2])):
        #     print(i)
    datas_bio = []
    for data in datas:

        datas_bio.append(trans_bio(data))
        # break
    with codecs.open('bio_data1','w','utf-8') as writer:
        for line in datas_bio:
            writer.write(str(line) + '\n')


def trans_bio(data):
    words = data[0].split()
    label_types = ['TRIG','TERM']
    es = data[1].replace('_',' ').replace(' ','').split('#')
    ts = data[2].replace('_', ' ').replace(' ','').split('#')
    elems = [es,ts]
    bios = [[word,'O'] for word in words]

    for e in es:
        for i in range(len(words)):
            for j in range(i+1,len(words)+1):
                if ''.join(words[i:j]) == e:
                    for k in range(i,j):
                        if k == i and (k == 0 or 'TERM' not in bios[k-1][1]):
                            bios[k][1] = 'B-'+label_types[1]
                        else:
                            bios[k][1] = 'I-'+label_types[1]
                if ''.join(words[i:j]) not in e:
                    break
    for t in ts:
        for i in range(len(words)):
            for j in range(i+1,len(words)+1):
                if ''.join(words[i:j]) == t:
                    for k in range(i,j):
                        if k == i and (k == 0 or 'TRIG' not in bios[k - 1][1]):
                            bios[k][1] = 'B-'+label_types[0]
                        else:
                            bios[k][1] = 'I-'+label_types[0]
                if ''.join(words[i:j]) not in t:
                    break

    return bios
# trans_bio(['美军 加大 对 IS 网络战 力度','对_网络战','美军','IS'])
def generate_train_datas(output):
    lines = []
    with codecs.open('bio_data1', 'r', 'utf-8') as reader:
        for line in reader.readlines():
            line = line.strip()
            units = eval(line)
            for unit in units:
                output.writerow([unit[0], unit[1], str(len(units))])
            output.writerow([])
    pass

if __name__ == '__main__':
    # process_data()
    import csv
    trans()
    output = csv.writer(codecs.open('datas1.txt','w','utf-8'), delimiter='#')
    generate_train_datas(output=output)