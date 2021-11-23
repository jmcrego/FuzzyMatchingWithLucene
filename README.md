# FuzzyMatchingWithLucene
Code to implement sentence-based Fuzzy Matching (index and retrieval) using Java Lucene

## Install Lucene
* Download Lucene and set your java CLASSPATH:
```
cd somewhere
wget https://dlcdn.apache.org/lucene/java/8.11.0/lucene-8.11.0.tgz
tar xvzf lucene-8.11.0.tgz
export LUCENE=.../lucene-8.11.0
export CLASSPATH=$LUCENE/core/lucene-core-8.11.0.jar:$LUCENE/analysis/common/lucene-analyzers-common-8.11.0.jar:$LUCENE/queryparser/lucene-queryparser-8.11.0.jar:$LUCENE/demo/lucene-demo-8.11.0.jar
```
* Download this repository:
```
cd somewhere
git clone https://github.com/jmcrego/FuzzyMatchingWithLucene.git
cd FuzzyMatchingWithLucene
```

## Download corpora for the next examples:
* news-commentary-v14 and EMEA (english/french) and news-test2008.en (english):
```
bash ./download.sh
```
From now on we use:
```
NEWS_TRN_EN=raw/news-commentary-v14.en
NEWS_TRN_FR=raw/news-commentary-v14.fr
NEWS_TST_EN=raw/news-test2008.en
NEWS_TST_FR=raw/news-test2008.fr
EMEA_TRN_EN=raw/EMEA.en-fr.en
EMEA_TRN_FR=raw/EMEA.en-fr.fr
```

## Use LuceneIndex.java to create indexes:
Raw corpora are always used to index/query.

* To index the monolingual news corpus:
```
java LuceneIndex.java -i ./index1 -f $NEWS_TRN_EN
```
* To index the parallel news corpus. The index is created over the english side (first file in -f option):
```
java LuceneIndex.java -i ./index2 -f $NEWS_TRN_EN,$NEWS_TRN_FR
```
* To index the parallel news and emea corpora. The index is created over the english sides (first file in -f option):
```
java LuceneIndex.java -i ./index3 -f $EMEA_TRN_EN,$EMEA_TRN_FR
```

## Use LuceneQuery.java to query the previous indexes:

To find the 1-most similar sentences in index1 for each sentence in news-test2008.en:
```
java LuceneQuery.java -i ./index1 -f $NEWS_TST_EN
```
To find the 1-most similar sentences in index2 and index3 for each sentence in news-test2008.en:
```
java LuceneQuery.java -i ./index2 -i i/index3 -f $NEWS_TST_EN
```
