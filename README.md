# FuzzyMatchingWithLucene
Code to implement sentence-based Fuzzy Matching (index and retrieval) using Java Lucene

## Install Lucene
* Download Lucene and set your java CLASSPATH:
```
cd .../somewhere/
wget https://dlcdn.apache.org/lucene/java/8.11.0/lucene-8.11.0.tgz
tar xvzf lucene-8.11.0.tgz
export LUCENE=.../somewhere/lucene-8.11.0
export CLASSPATH=$LUCENE/core/lucene-core-8.11.0.jar:$LUCENE/analysis/common/lucene-analyzers-common-8.11.0.jar:$LUCENE/queryparser/lucene-queryparser-8.11.0.jar:$LUCENE/demo/lucene-demo-8.11.0.jar
```
* Download this repository:
```
git clone https://github.com/jmcrego/FuzzyMatchingWithLucene.git
```

## Download corpora for the next examples:
* news-commentary-v14 (english/french):
```
wget -P ./raw http://data.statmt.org/news-commentary/v14/training/news-commentary-v14.en-fr.tsv.gz
zcat ./raw/news-commentary-v14.en-fr.tsv.gz | cut -f 1 > ./raw/news-commentary-v14.en
zcat ./raw/news-commentary-v14.en-fr.tsv.gz | cut -f 2 > ./raw/news-commentary-v14.fr
rm -rf ./raw/news-commentary-v14.en-fr.tsv.gz
```
* news-test2008 (english):
```
wget -P ./raw/ http://www.statmt.org/wmt14/dev.tgz
tar xvzf ./raw/dev.tgz -C ./raw/
mv ./raw/dev/news-test2008.en ./raw/
rm -rf ./raw/dev*
```
## Use FuzzyMatchingWithLucene
* To index a raw corpus (ex: news-commentary-v14.en):
```
java LuceneIndex.java -i ./index1 -f news,./raw/news-commentary-v14.en
```
To query the previous index and find the n-most similar sentences of each sentence available in test file:
```
java LuceneQuery.java -i ./index1 -f ./raw/news-test2008.en -n 1 -fuzzymatch -mins 0.5 -txt -name news
```
* To index a raw parallel corpus (ex: news-commentary-v14.en and news-commentary-v14.fr). The index is created over the english side:
```
java LuceneIndex.java -i index2 -f news,./raw/news-commentary-v14.en,./raw/news-commentary-v14.fr
```
Then:
```
java LuceneQuery.java -i ./index2 -f ./raw/news-test2008.en -n 1 -fuzzymatch -mins 0.5 -txt -name news
```
