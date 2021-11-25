#!/bin/bash
1;95;0c
raw=$PWD/raw

news(){
    wget -P $raw http://data.statmt.org/news-commentary/v14/training/news-commentary-v14.en-fr.tsv.gz
    zcat $raw/news-commentary-v14.en-fr.tsv.gz | cut -f 1 > $raw/news-commentary-v14.en
    zcat $raw/news-commentary-v14.en-fr.tsv.gz | cut -f 2 > $raw/news-commentary-v14.fr
    rm -rf $raw/news-commentary-v14.en-fr.tsv.gz
    
    wget -P $raw http://www.statmt.org/wmt14/dev.tgz
    tar xvzf $raw/dev.tgz -C $raw
    mv $raw/dev/news-test2008.{en,fr} $raw
    rm -rf $raw/dev*
}

emea(){
    wget -P $raw http://opus.nlpl.eu/download.php?f=EMEA/en-fr.txt.zip
    unzip -d $raw $raw/download.php?f=EMEA%2Fen-fr.txt.zip
    rm -f $raw/download.php\?f\=EMEA%2Fen-fr.txt.zip
    rm -f $raw/README
}

ecb(){
    wget -P $raw http://opus.nlpl.eu/download.php?f=ECB/v1/moses/en-fr.txt.zip
    unzip -d $raw $raw/download.php?f=ECB%2Fv1%2Fmoses%2Fen-fr.txt.zip
    rm -f $raw/README $raw/ECB.en-fr.ids $raw/download.php?f=ECB%2Fv1%2Fmoses%2Fen-fr.txt.zip
}

epps(){
    wget -P $raw http://www.statmt.org/europarl/v10/training/europarl-v10.fr-en.tsv.gz
    zcat $raw/europarl-v10.fr-en.tsv.gz | cut -f 1 > $raw/europarl-v10.fr-en.fr
    zcat $raw/europarl-v10.fr-en.tsv.gz | cut -f 2 > $raw/europarl-v10.fr-en.en
    rm -f $raw/europarl-v10.fr-en.tsv.gz
    exit
    wget -P $raw http://matrix.statmt.org/test_sets/test2008.tgz?1504722372
    tar xvzf $raw/test2008.tgz?1504722372 -C $raw
    mv $raw/test2008/test2008.{en,fr} $raw/
    rm -rf $raw/test2008.tgz?1504722372 $raw/test2008
}

unpc(){
    wget -P $raw https://object.pouta.csc.fi/OPUS-UNPC/v1.0/moses/en-fr.txt.zip
    unzip -d $raw $raw/en-fr.txt.zip
    rm -f $raw/{README,LICENSE,UNPC.en-fr.xml,en-fr.txt.zip}
}

book(){
    wget -P $raw https://object.pouta.csc.fi/OPUS-EUbookshop/v2/moses/en-fr.txt.zip
    unzip -d $raw $raw/en-fr.txt.zip
    rm -f $raw/{README,EUbookshop.en-fr.ids,en-fr.txt.zip}
}

#news
#emea
#ecb
#epps
#unpc
#book
