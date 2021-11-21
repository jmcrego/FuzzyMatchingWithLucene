# FuzzyMatchingWithLucene
Code to implement sentence-based Fuzzy Matching (index and retrieval) using Java Lucene

## Install
* Download Lucene:
```
cd .../somewhere/
wget https://dlcdn.apache.org/lucene/java/8.11.0/lucene-8.11.0.tgz
tar xvzf lucene-8.11.0.tgz
```
* Set your $CLASSPATH:
```
export LUCENE=.../somewherehome/lucene-8.11.0
export CLASSPATH=$LUCENE/core/lucene-core-8.11.0.jar:$LUCENE/analysis/common/lucene-analyzers-common-8.11.0.jar:$LUCENE/queryparser/lucene-queryparser-8.11.0.jar:$LUCENE/demo/lucene-demo-8.11.0.jar
```
