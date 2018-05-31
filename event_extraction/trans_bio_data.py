import codecs
import csv
import re
def process_data():
    #合并相邻的实体
    #删除触发词字数为1的句子
    lines = []
    with codecs.open('traindatas.txt', 'r', 'utf-8') as reader:
        for line in reader.readlines():
            lines.append(line.strip())
    new_lines = []
    i = 0
    while i<len(lines):
        sent = lines[i]
        trigger = lines[i+1]
        source = lines[i+2]
        target = lines[i+3]
        if len(trigger) == 1:
            i += 4
            continue
        new_lines.append(sent)
        new_lines.append(trigger)
        new_lines.append(source)
        new_lines.append(target)

        i+=4
    with codecs.open('traindatas.txt', 'w', 'utf-8') as writer:
        for line in new_lines:
            writer.write(line+'\n')
    pass
def trans():
    lines = []
    with codecs.open('traindatas.txt','r','utf-8') as reader:
        for line in reader.readlines():
            lines.append(line.strip())
    datas = []
    i = 0
    while i < len(lines):
        datas.append([lines[i],lines[i+1],lines[i+2],lines[i+3],lines[i+4],lines[i+5]])
        i += 6

        # if len(lines[i]) < max(len(lines[i+1]),len(lines[i+2]),len(lines[i+3]),len(lines[i+4])):
        #     print(i)
    datas_bio = []
    for data in datas:

        datas_bio.append(trans_bio(data))
        # break
    with codecs.open('bio_data','w','utf-8') as writer:
        for line in datas_bio:
            writer.write(str(line) + '\n')


def get_trigger_Begin_End(words,trigger_pattern):

    end = 0
    for i in range(0,len(words)):
        if re.match('.*'+trigger_pattern,''.join(words[0:i+1])):
            end = i
            break
    begin = 0
    for i in range(end,-1,-1):
        if re.match(trigger_pattern,''.join(words[i:end+1])):
            begin = i
            break
    return begin,end


    pass
def trans_bio(data):
    words = data[0].split()
    label_types = ['TRIG','TERM']
    trigger1 = data[1].replace('_',' ').replace(' ','')
    trigger2 = data[2].replace('_', ' ').replace(' ','')
    source = data[3].replace('_',' ').replace(' ','')
    target1 = data[4].replace('_',' ').replace(' ','')
    target2 = data[5].replace('_', ' ').replace(' ','')
    elems = [trigger1,trigger2,source,target1,target2]
    bios = [[word,'O'] for word in words]

    for elem_id,elem in enumerate(elems):
        kk = 1
        if elem_id < 2:
            kk = 0
        for i in range(len(words)):
            for j in range(i+1,len(words)+1):
                if ''.join(words[i:j]) == elem:
                    for k in range(i,j):
                        if k == i :
                            bios[k][1] = 'B-'+label_types[kk]
                        else:
                            bios[k][1] = 'I-'+label_types[kk]
                if ''.join(words[i:j]) not in elem:
                    break


    return bios
# trans_bio(['美军 加大 对 IS 网络战 力度','对_网络战','美军','IS'])
def generate_train_datas(output):
    lines = []
    with codecs.open('bio_data', 'r', 'utf-8') as reader:
        for line in reader.readlines():
            line = line.strip()
            units = eval(line)
            for unit in units:
                output.writerow([unit[0], unit[1], str(len(units))])
            output.writerow([])
    pass

if __name__ == '__main__':
    # process_data()
    trans()
    output = csv.writer(codecs.open('datas.txt','w','utf-8'), delimiter='#')
    generate_train_datas(output=output)
