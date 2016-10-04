package main

import (
	"flag"
	"fmt"
	"log"

	as "github.com/aerospike/aerospike-client-go"
)

var masterStatement = as.NewStatement("test", "demo",
	"pref", "boff", "oc", "city", "code")

func newStatement() *as.Statement {
	s := new(as.Statement)
	*s = *masterStatement
	return s
}

func toGeoJSONPoint(s string) string {
	return fmt.Sprintf(`{"type":"Point","coordinates":[%s]}`, s)
}

func query(c *as.Client, q string) error {
	s := newStatement()
	p := toGeoJSONPoint(q)
	f := as.NewGeoRegionsContainingPointFilter("gj", p)
	err := s.Addfilter(f)
	if err != nil {
		return err
	}
	if s.IsScan() {
		log.Printf("query:%q will cause a full scan")
	}
	rs, err := c.Query(nil, s)
	if err != nil {
		return nil
	}
	defer rs.Close()
	for r := range rs.Records {
		fmt.Printf("%q -> pref:%q boff:%q oc:%q city:%q code:%q\n", q,
			r.Bins["pref"], r.Bins["boff"], r.Bins["oc"], r.Bins["city"],
			r.Bins["code"])
	}
	return nil
}

func run() error {
	c, err := as.NewClient("127.0.0.1", 3000)
	if err != nil {
		return err
	}
	defer c.Close()

	for _, q := range flag.Args() {
		err := query(c, q)
		if err != nil {
			log.Printf("query:%q failed: %s", q, err)
		}
	}
	return nil
}

func main() {
	flag.Parse()
	if flag.NArg() < 1 {
		log.Fatal("need queries")
	}
	err := run()
	if err != nil {
		log.Fatalf("terminated with error: %s", err)
	}
}
