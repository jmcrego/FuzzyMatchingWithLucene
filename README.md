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
* To index a raw corpus (ex: news-commentary-v14.en):
```
java LuceneIndex.java -i ./index1 -f news,$NEWS_TRN_EN
```
* To index a raw parallel corpus (ex: news-commentary-v14.en and news-commentary-v14.fr). The index is created over the english side:
```
java LuceneIndex.java -i ./index2 -f news,$NEWS_TRN_EN,$NEWS_TRN_FR
```
* To index a raw parallel corpus (ex: news-commentary-v14.fr and news-commentary-v14.en). The index is created over the french side:
```
java LuceneIndex.java -i ./index3 -f news,$NEWS_TRN_FR,$NEWS_TRN_EN
```
* To index a raw parallel corpus (ex: EMEA.en-fr.en and EMEA.en-fr.fr). The index is created over the english side:
```
java LuceneIndex.java -i ./index4 -f emea,$EMEA_TRN_EN,$EMEA_TRN_FR
```
* To index two raw parallel corpora (ex: news-commentary and EMEA). The index is created over the english side:
```
java LuceneIndex.java -i ./index5 -f news,$NEWS_TRN_EN,$NEWS_TRN_FR -f emea,$EMEA_TRN_EN,$EMEA_TRN_FR
```

## Use LuceneQuery.java to query the previous indexes:

To find the n-most similar sentences in index1 of each sentence available in news-test2008.en:
```
java LuceneQuery.java -i ./index1 -f $NEWS_TST_EN -n 1 -fuzzymatch -mins 0.5 -txt -name news
```
To find the n-most similar sentences in index2 of each sentence available in news-test2008.en:
```
java LuceneQuery.java -i ./index2 -f $NEWS_TST_EN -n 1 -fuzzymatch -mins 0.5 -txt -name news
```
To find the n-most similar sentences in index3 of each sentence available in news-test2008.fr:
```
java LuceneQuery.java -i ./index3 -f $NEWS_TST_FR -n 1 -fuzzymatch -mins 0.5 -txt -name news
```
To find the n-most similar sentences in index5 of each sentence available in news-test2008.fr:
```
java LuceneQuery.java -i ./index5 -f $NEWS_TST_FR -n 1 -fuzzymatch -mins 0.5 -txt -name news
```
