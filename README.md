Powered by ![visa](https://github.com/henneberger/sba-hack/raw/master/visa.png)

# Small business analytics

 \#SmallBusinessWeek Hackathon 2018

Sponored by U.S. Small Business Administration & Visa


## Overview
Import excel files to gain access to rich analytics. A sample excel file can be found [here](https://github.com/henneberger/sba-hack/blob/master/retail.csv).


## Features
* View average customer queue time for your establishments with `Powered by Visa: Wait Time`
* See merchant locations on a map with `Powered by Visa: Merchant Locations`
* See consumer spending over time
* Track pareto distributions of your customers
* Identify your data trends by any time range
* Find trending products with the `Abnormal Product Trends` card
* Find `Customer Spending` habits by inspecting products
* Identify upward and downward trending produts with the `Popular Items` card
* Track revenue over `Total Orders` as well as `Product Totals`
* Quickly Identify Top selling products

## Getting Started
#### Built on open source software!

Setup elasticsearch:
```
wget https://artifacts.elastic.co/downloads/elasticsearch/elasticsearch-6.2.4.tar.gz
tar xvf elasticsearch-6.2.4.tar.gz
bin/elasticsearch
```

Create elastic search indicies:
```
curl -XPUT 'localhost:9200/_template/wait' -H 'Content-Type: application/json' -d '{ "index_patterns": ["wait*"], "settings": { "number_of_shards": 1 }, "mappings" : { "wait" : { "properties" : { "timestamp" : { "type": "date", "format" : "epoch_millis" }, "name" : { "type" : "keyword" }, "wait" : { "type" : "long" } } } }}'
curl -XPUT 'localhost:9200/_template/product' -H 'Content-Type: application/json' -d '{ "index_patterns": ["product*"], "settings": { "number_of_shards": 1 }, "mappings" : { "product" : { "properties" : { "Country" : { "type" : "text", "fields" : { "keyword" : {  "type" : "keyword",  "ignore_above" : 256 } } }, "CustomerID" : { "type" : "text", "fields" : { "keyword" : {  "type" : "keyword",  "ignore_above" : 256 } } }, "Description" : { "type" : "text", "fields" : { "keyword" : {  "type" : "keyword",  "ignore_above" : 256 } } }, "InvoiceDate" : { "type" : "date", "format" : "epoch_millis" }, "InvoiceNo" : { "type" : "text", "fields" : { "keyword" : {  "type" : "keyword",  "ignore_above" : 256 } } }, "Quantity" : { "type" : "long" }, "StockCode" : { "type" : "text", "fields" : { "keyword" : {  "type" : "keyword",  "ignore_above" : 256 } } }, "UnitPrice" : { "type" : "long" } } } } }'
curl -XPUT 'localhost:9200/_template/sba' -H 'Content-Type: application/json' -d '{ "index_patterns": ["sba-*"], "settings": { "number_of_shards": 1 }, "mappings" : { "retail" : { "properties" : { "Country" : { "type" : "text", "fields" : { "keyword" : {  "type" : "keyword",  "ignore_above" : 256 } } }, "CustomerID" : { "type" : "text", "fields" : { "keyword" : {  "type" : "keyword",  "ignore_above" : 256 } } }, "Description" : { "type" : "text", "fields" : { "keyword" : {  "type" : "keyword",  "ignore_above" : 256 } } }, "InvoiceDate" : { "type" : "date", "format" : "epoch_millis" }, "InvoiceNo" : { "type" : "text", "fields" : { "keyword" : {  "type" : "keyword",  "ignore_above" : 256 } } }, "Quantity" : { "type" : "long" }, "StockCode" : { "type" : "text", "fields" : { "keyword" : {  "type" : "keyword",  "ignore_above" : 256 } } }, "UnitPrice" : { "type" : "long" }, "latLong":{"type":"geo_point"} } } } }'
```

Setup Kibana
```
https://artifacts.elastic.co/downloads/kibana/kibana-6.2.4-darwin-x86_64.tar.gz
tar xvf kibana-6.2.4-darwin-x86_64.tar.gz
bin/kibana
```

Import dashboards
```
Management > Saved objects > Import: dashboard.json & visualizations.json
```

Sign up for a [visa developer account](https://developer.visa.com/portal/#login) to get access to visa enriched data.

1. Create a new project and allow
    * Visa Query Insights
    * Merchant Search
1. Download your `privateKey.pem`
1. Download other credentials (User ID, Password, Visa Development Platform Certificate)

Import the cert to a local keystore
```
openssl pkcs12 -export -in cert.pem -inkey "privateKey.pem" -certfile cert.pem -out myProject_keyAndCertBundle.p12
keytool -importkeystore -srckeystore myProject_keyAndCertBundle.p12 -srcstoretype PKCS12 -destkeystore myProject_keyAndCertBundle.jks
keytool -list -v -keystore myProject_keyAndCertBundle.jks
```


Start the wait time monitor (refreshes every 30 seconds):
```
sbt run WaitTimeCron
```

Import your csv:
```
sbt run Main retail.csv
```