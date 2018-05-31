import codecs
import random

def load_dic(path):
    with codecs.open(path, 'r', 'utf-8') as reader:
        ents = [ent.strip() for ent in reader]
    return ents
def sample_word():
    words = load_dic('datas/w_voc.txt')
    triggers = []
    for i in range(20):
        triggers.extend(load_dic('datas/trigger/'+ str(i+1)+'.txt'))
    with codecs.open('datas/corpus/train_col','r','utf-8') as reader:
        for line in reader:
            units = line.strip().split('#')
            if len(units) > 1 and 'TRIG' in units[2]:
                triggers.append(units[0])

    sample_words = set(triggers)
    size = len(sample_words)
    print(size)
    with codecs.open('datas/trigger/triggers', 'w', 'utf-8') as writer:
        for word in sample_words:
            writer.write(word + '\n')
    for i in range(size):
        sample_id = random.randint(0,len(words)-1)
        sample_words.add(words[sample_id])
    with codecs.open('datas/trigger/sample_words','w','utf-8') as writer:
        for word in sample_words:
            writer.write(word + '\n')
    print(sample_words)
sample_word()