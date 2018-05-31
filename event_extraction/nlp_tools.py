import nltk
import codecs
import os
data_dir = 'res14_data'
train_path = os.path.join(data_dir,'bio_res_2014_train')
test_path = os.path.join(data_dir,'bio_res_2014_test')
def generate_postag_file(fname):
    posseq_list = []
    with codecs.open(fname,'r','utf-8') as reader:
        for line in reader.readlines():
            line = line.strip()
            units = eval(line)
            tokens = [unit[0] for unit in units]
            tags = nltk.pos_tag(tokens)
            postags = []
            for tag in tags:
                if tag[1][0] == 'N':
                    postags.append('n')
                    continue
                if tag[1][0] == 'V':
                    postags.append('v')
                    continue
                if tag[1][0] == 'R':
                    postags.append('adv')
                    continue
                if tag[1][0] == 'J':
                    postags.append('adj')
                    continue
                postags.append('o')

            posseq = ' '.join(postags)
            posseq_list.append(posseq)
    if 'test' in fname:
        fname = 'test'
    else:
        fname = 'train'
    with codecs.open(fname+'_postag_new','w','utf-8') as writer:
        for line in posseq_list:
            writer.write(line+'\n')
    pass
# def generate_chunk_file(fname):
#     chuckseq_list = []
#     with codecs.open(fname, 'r', 'utf-8') as reader:
#         for line in reader.readlines():
#             line = line.strip()
#             units = eval(line)
#             tokens = [unit[0] for unit in units]
#             tags = nltk.pos_tag(tokens)
#             chucks = nltk.ne_chunk(tags)
#             posseq = ' '.join([tag[1] for tag in tags])
#             chuckseq_list.append(posseq)
#     with codecs.open(fname + '_chunk', 'w', 'utf-8') as writer:
#         for line in chuckseq_list:
#             writer.write(line + '\n')
if __name__ == '__main__':
    generate_postag_file(train_path)
    generate_postag_file(test_path)


