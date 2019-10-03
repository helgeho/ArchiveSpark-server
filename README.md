## ArchiveSpark-server

[![ArchiveSpark Logo](https://github.com/helgeho/ArchiveSpark/raw/master/logo.png)](https://github.com/helgeho/ArchiveSpark)

A server application that provides a web service API for [ArchiveSpark](https://github.com/helgeho/ArchiveSpark), the distributed platform for easy data processing, extraction as well as derivation for web archives. This way, third-party applications can use ArchiveSpark as a service to integrate temporal web archive data with a flexible, easy-to-use interface. 

The server can be hosted with a any [Data Specification](https://github.com/helgeho/ArchiveSpark/blob/master/docs/DataSpecs.md) and a configurable set of [Enrichment Functions](https://github.com/helgeho/ArchiveSpark/blob/master/docs/EnrichFuncs.md), either on a single machine environment or distributed computer clusters.

### Demo

We are currently hosting a demos ([start-cdx.scala](demos/start-cdx.scala) based on the Wayback Machine's [CDX server](https://github.com/internetarchive/wayback/tree/master/wayback-cdx-server): `http://tempas.L3S.de/archivespark/cdx/:action/*EnrichFuncs*`

Query strings, e.g., `?url=nytimes.com` are directly passed to the CDX server, for details and an overview of the available options please read https://github.com/internetarchive/wayback/tree/master/wayback-cdx-server.

We provide the following actions / endpoints:
* `/enrich/*EnrichFuncs*`: Enrich the records with the given Enrichment Functions definitions separated by comma and return the results as JSON.
* `/map/*EnrichFuncs*`: Enrich the records with the given Enrichment Functions and return the value of the last Enrichment Function.
* `/flatmap/*EnrichFuncs*`: Enrich the records with the given Enrichment Functions and return the items of the list of values defined by the last Enrichment Function.
* `/mapCdx/*EnrichFuncs*`: Same as `map` but the output includes the corresponding CDX information.
* `/flatmapCdx/*EnrichFuncs*`: Same as `flatmap` but the output includes the corresponding CDX information.
* `/links`: Lists all links from the records in the form `src timestamp dst text`.

See below for all details and the concrete configurations.

*Please note: Our demo is hosted on a virtual machine / single server environment, so responses might be slow.*

#### Example Calls


* Hyperlinks on first three *nytimes.com* webpages in 2012 returned by the CDX server: http://tempas.L3S.de/archivespark/cdx/links?url=nytimes.com&matchPrefix=true&from=2012&limit=3
* First three *nytimes.com* webpages in 2012 returned by the CDX server enriched with title and entities (using Stanford's CoreNLP 3.4.1 NER): http://tempas.L3S.de/archivespark/cdx/enrich/Title,Entities?url=nytimes.com&matchPrefix=true&from=2012&limit=3
* First three *nytimes.com* webpages in 2012 returned by the CDX server enriched with title, linked pages in SURT format and anchor texts: http://tempas.L3S.de/archivespark/cdx/enrich/Title,HtmlText.ofEach(Html.all("a")),SURT.of(HtmlAttribute("href").ofEach(Html.all("a")))?url=nytimes.com&matchPrefix=true&from=2012&limit=3
* Titles of first three *nytimes.com* webpages in 2012 returned by the CDX server: http://tempas.L3S.de/archivespark/cdx/map/Title?url=nytimes.com&matchPrefix=true&from=2012&limit=3
* Linked pages from the first three *nytimes.com* webpages in 2012 and after returned by the CDX server in SURT format: http://tempas.L3S.de/archivespark/cdx/flatmap/SURT.of(HtmlAttribute("href").ofEach(Html.all("a")))?url=nytimes.com&matchPrefix=true&from=2012&limit=3
* First three CDX records of *nytimes.com* webpages in 2012 returned by the CDX server with titles: http://tempas.L3S.de/archivespark/cdx/mapCdx/Title?url=nytimes.com&matchPrefix=true&from=2012&limit=3
* First three CDX records of *nytimes.com* webpages in 2012 returned by the CDX server with linked pages in SURT format: http://tempas.L3S.de/archivespark/cdx/flatmapCdx/SURT.of(HtmlAttribute("href").ofEach(Html.all("a")))?url=nytimes.com&matchPrefix=true&from=2012&limit=3

### Run

1. To run ArchiveSpark-server you need to have Scala installed, which is freely available from http://www.scala-lang.org.

2. Now either build the server yourself (`sbt assembly`) or download a [pre-built release](https://github.com/helgeho/ArchiveSpark-server/releases).

3. Copy the ArchiveSpark-server assembly JAR file together with the latest ArchiveSpark core and dependendy JARs ([ArchiveSpark releases](https://github.com/helgeho/ArchiveSpark/releases)) and potentially required JAR files for additional DataSpecs or Enrichment Function into the same directory.

4. Create the start-up script for your server in this directory as well, like in this example: [start-cdx.scala](demos/start-cdx.scala)
   
5. Now you can start your server by running the following command from within this directory:

```
scala -J-Xmx4g -cp "*.jar" YOUR-START-SCRIPT.scala
``` 

### Configuration

ArchiveSpark-server provides a secured environment (`ArchiveSparkServer.env`) to parse the [Enrichment Functions](https://github.com/helgeho/ArchiveSpark/blob/master/docs/EnrichFuncs.md) provided in the URL of a service call. For security reasons, only tokens explicitely added to this environment as well as methods defined on these tokens can be called through the URL. Tokens can be Enrichment Functions, functions to create Enrichment Functions or objects that provide access to Enrichment Functions. E.g.,

```scala
ArchiveSparkServer.env.addToken(HtmlText)
ArchiveSparkServer.env.addToken(Html)
ArchiveSparkServer.env.addToken("Title", HtmlText.of(Html.first("title")))
```

For a list of available [Enrichment Functions](https://github.com/helgeho/ArchiveSpark/blob/master/docs/EnrichFuncs.md) please check the [ArchiveSpark documentation](https://github.com/helgeho/ArchiveSpark/blob/master/docs/README.md).

To load your dataset, call `ArchiveSparkServer.init` and create a new RDD using `ArchiveSpark.load` with the [DataSpec](https://github.com/helgeho/ArchiveSpark/blob/master/docs/DataSpecs.md) of your choice. The required SparkContext should be created globally, outside this initializer. The following snippet shows an example using a local SparkContext (you can also connect it to your cluster, for more read [Spark Programming Guide](https://spark.apache.org/docs/latest/rdd-programming-guide.html)) with the `CdxQueryWaybackSpec` DataSpec included in this project:

```scala
val conf = new SparkConf().setAppName("ArchiveSpark-server").setMaster("local[*]")
val sc = new SparkContext(conf)

val actions = ArchiveSparkServer.init { (action, request, queries) =>
  ArchiveSpark.load(sc, CdxQueryWaybackSpec("http://web.archive.org/cdx/search/cdx", queries))
}
```

For a list of available [DataSpecs](https://github.com/helgeho/ArchiveSpark/blob/master/docs/DataSpecs.md) please check the [ArchiveSpark documentation](https://github.com/helgeho/ArchiveSpark/blob/master/docs/README.md).

In addition to the pre-configured endpoints (`/enrich`, `/map`, `/flatmap`, `/mapCdx`, `/flatmapCdx`, see [DefaultActions.scala](src/main/scala/org/archive/archivespark/server/DefaultActions.scala) for definitions), you can register your own endpoints on the `actions` object returned by `init`, like so (this `links` endpoint extracts links in the form `src timestamp dst text`):

```scala
actions.register("links", (rdd, funcs, request, queries) => {
  val Links = Html.all("a")
  val linkProps = Seq(
    Root[CdxRecord].map("surtUrl") { cdx: CdxRecord => cdx.surtUrl },
    Root[CdxRecord].map("timestamp") { cdx: CdxRecord => cdx.timestamp },
    SURT.of(HtmlAttribute("href").ofEach(Links)),
    HtmlText.ofEach(Links)
  )
  val linkValues = Values(linkProps: _*).ofEach(Links)
  ArchiveSparkServer.callAction[WaybackRecord]("flatmap", ServerUtil.enrich(rdd)(linkProps), Seq(linkValues), request, queries)
})
```

(this is equivalent to `/flatmap/Cdx("surtUrl"),Cdx("timestamp"),SURT.of(HtmlAttribute("href").ofEach(Html.all("a"))),HtmlText.ofEach(Html.all("a")),Values(Cdx("surtUrl"),Cdx("timestamp"),SURT.of(HtmlAttribute("href").ofEach(Html.all("a"))),HtmlText.ofEach(Html.all("a"))).ofEach(Html.all("a"))`)

Finally, start your server by defining the base directory and a port:

```scala
ArchiveSparkServer.start("/archivespark", 8080)
```

### License

The MIT License (MIT)

Copyright (c) 2018-2019 [Helge Holzmann](http://www.HelgeHolzmann.de) ([Internet Archive](http://www.archive.org)) <[helge@archive.org](mailto:helge@archive.org)>

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
