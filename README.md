# Japan Prefecture Quering

## Using aerospike

*   http://www.aerospike.com/
*   http://www.aerospike.com/docs/tools/aql

### test aerospike capability

```
# Create a index of GeoJSON
CREATE INDEX demo_gj ON test.demo (gj) GEO2DSPHERE

# Register two records, one of two has clipping area.
INSERT INTO test.demo (PK, name, gj) VALUES('key1', 'region1', GEOJSON('{"type":"Polygon","coordinates":[[[137,34],[137,38],[141,38],[141,34],[137,34]]]}'))
INSERT INTO test.demo (PK, name, gj) VALUES('key2', 'clip1', GEOJSON('{"type":"Polygon","coordinates":[[[137,34],[137,38],[141,38],[141,34],[137,34]],[[138,35],[140,35],[140,37],[138,37],[138,35]]]}'))

# Three query: not matched, 1 matched, 2 matched.
SELECT name FROM test.demo WHERE gj CONTAINS CAST('{"type":"Point","coordinates":[0,0]}' as GEOJSON)
SELECT name FROM test.demo WHERE gj CONTAINS CAST('{"type":"Point","coordinates":[139,36]}' as GEOJSON)
SELECT name FROM test.demo WHERE gj CONTAINS CAST('{"type":"Point","coordinates":[139,34.5]}' as GEOJSON)
```

Result

```
aql> SELECT name FROM test.demo WHERE gj CONTAINS CAST('{"type":"Point","coordinates":[0,0]}' as GEOJSON)
0 rows in set (0.001 secs)

aql> SELECT name FROM test.demo WHERE gj CONTAINS CAST('{"type":"Point","coordinates":[139,36]}' as GEOJSON)
+-----------+
| name      |
+-----------+
| "region1" |
+-----------+
1 row in set (0.002 secs)

aql> SELECT name FROM test.demo WHERE gj CONTAINS CAST('{"type":"Point","coordinates":[139,34.5]}' as GEOJSON)
+-----------+
| name      |
+-----------+
| "region1" |
| "clip1"   |
+-----------+
2 rows in set (0.002 secs)
```

It looks good for the purpose.

### register records

Converted GeoJSON to TSV (see cmd/geojson2tsv).

Imported TSV into aerospike (see cmd/import2as).

    $ go build ./import2as
    $ ./import2as -verbose data/japan_cities.tsv.xz

Long records are rejected by aerospike.
It must be removed by hand that over 1M bytes line.

Try to query like this

```
aql> SELECT pref,boff,oc,city,code FROM test.demo WHERE gj CONTAINS CAST('{"type":"Point","coordinates":[139.780935,35.702265]}' as GEOJSON)
+-------------+------+-------------+------+---------+
| pref        | boff | oc          | city | code    |
+-------------+------+-------------+------+---------+
| "東京都" | ""   | "台東区" | ""   | "13106" |
+-------------+------+-------------+------+---------+
1 row in set (0.030 secs)

aql> SELECT pref,boff,oc,city,code FROM test.demo WHERE gj CONTAINS CAST('{"type":"Point","coordinates":[139.780935,35.702265]}' as GEOJSON)
+-------------+------+-------------+------+---------+
| pref        | boff | oc          | city | code    |
+-------------+------+-------------+------+---------+
| "東京都" | ""   | "台東区" | ""   | "13106" |
+-------------+------+-------------+------+---------+
1 row in set (0.003 secs)
```

### Benchmark query performance

```
Benchmark                            Mode  Cnt     Score    Error  Units
QueryBenchmark.queryConstant        thrpt  100   383.016 ±  3.982  ops/s
QueryBenchmark.queryOutConstant     thrpt  100  1506.053 ± 11.647  ops/s
QueryBenchmark.queryOutRandom       thrpt  100  1489.143 ±  8.485  ops/s
QueryBenchmark.queryRandomAllJapan  thrpt  100   144.591 ±  7.659  ops/s
QueryBenchmark.queryRandomKanto     thrpt  100    70.607 ±  2.387  ops/s
```

```
$ vagrant up
$ unxz -k data/japan_cities.tsv.xz
$ go build ./cmd/import2as
$ ./import2as -verbose -createindex data/japan_cities.tsv
$ gradle jmh
```

If `go build` was failed, try below commands to insntall dependencies.

```
$ go get -v -u github.com/aerospike/aerospike-client-go
$ go get -v -u github.com/dustin/go-humanize
$ go get -v -u github.com/ulikunitz/xz
```

#### Benchmark with small data (only prefectures)

```
Benchmark                            Mode  Cnt     Score    Error  Units
QueryBenchmark.queryConstant        thrpt  100   133.613 ±  0.752  ops/s
QueryBenchmark.queryOutConstant     thrpt  100  1868.665 ± 13.626  ops/s
QueryBenchmark.queryOutRandom       thrpt  100  1878.190 ±  6.964  ops/s
QueryBenchmark.queryRandomAllJapan  thrpt  100   372.713 ±  7.644  ops/s
QueryBenchmark.queryRandomKanto     thrpt  100   201.212 ±  1.972  ops/s
```

How to run benchmark with small data set.

```
$ vagrant destroy -f
$ vagrant up
$ go build ./cmd/import2as
$ ./import2as -createindex -verbose ./data/japan.tsv.xz
$ gradle jmh
```

It wastes about 3MB for data.

```
aql> show sets
+------------------+--------+~+-------------------+----------+
| disable-eviction | ns     | | memory_data_bytes | deleting |
+------------------+--------+~+-------------------+----------+
| "false"          | "test" | | 3113364           | "false"  |
+------------------+--------+~+-------------------+----------+
1 row in set (0.000 secs)
OK
```
