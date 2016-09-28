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

### What's next?

*   register records of Japanese prefecture
*   measure performance of query with real data
