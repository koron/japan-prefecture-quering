package main

import (
	"bufio"
	"errors"
	"flag"
	"fmt"
	"io"
	"log"
	"os"
	"strings"

	as "github.com/aerospike/aerospike-client-go"
	humanize "github.com/dustin/go-humanize"
	"github.com/ulikunitz/xz"
)

type reader struct {
	io.Reader
	f *os.File
}

func (r *reader) Close() error {
	return r.f.Close()
}

func open(name string) (io.ReadCloser, error) {
	f, err := os.Open(name)
	if err != nil {
		return nil, err
	}

	if strings.HasSuffix(name, ".xz") {
		r, err := xz.NewReader(f)
		if err != nil {
			f.Close()
			return nil, err
		}
		return &reader{r, f}, nil
	}

	return f, nil
}

type procTSV func([]string) error

func enumTSV(r io.Reader, p procTSV) error {
	br := bufio.NewReaderSize(r, 10*1000*1000)
	for {
		raw, isPrefix, err := br.ReadLine()
		if err != nil {
			if err == io.EOF {
				break
			}
			return err
		}
		if isPrefix {
			return errors.New("unexpected isPrefix")
		}
		l := string(raw)
		cols := strings.SplitN(l, "\t", 8)
		if len(cols) != 7 {
			return fmt.Errorf("unexpected split: %s", l)
		}
		if p != nil {
			if err := p(cols); err != nil {
				return err
			}
		}
	}
	return nil
}

func toGJV(s string) as.GeoJSONValue {
	if strings.HasPrefix(s, "[[[[") {
		return as.NewGeoJSONValue(
			fmt.Sprintf(`{"type":"MultiPolygon","coordinates":%s}`, s))
	}
	return as.NewGeoJSONValue(
		fmt.Sprintf(`{"type":"Polygon","coordinates":%s}`, s))
}

func createIndex(c *as.Client) error {
	p := as.NewWritePolicy(0, 0)
	p.RecordExistsAction = as.REPLACE
	t, err := c.CreateIndex(p, "test", "demo", "demo_gj", "gj", as.GEO2DSPHERE)
	if err != nil {
		return err
	}
	err = <-t.OnComplete()
	if err != nil {
		return err
	}
	return nil
}

var (
	optCreateIndex = flag.Bool("createindex", false, "create index at first")
	optVerbose     = flag.Bool("verbose", false, "show verbose messages")
	optDryrun      = flag.Bool("dryrun", false, "dry run")
)

func run() error {
	r, err := open(flag.Arg(0))
	if err != nil {
		return err
	}
	defer r.Close()

	var c *as.Client
	if !*optDryrun {
		c, err = as.NewClient("127.0.0.1", 3000)
		if err != nil {
			return err
		}
		defer c.Close()
		if *optCreateIndex {
			if err = createIndex(c); err != nil {
				return err
			}
		}
	}

	cnt := 1
	err = enumTSV(r, func(cols []string) error {
		if !*optDryrun {
			key, _ := as.NewKey("test", "demo", cols[0])
			pref := as.NewBin("pref", cols[1])
			boff := as.NewBin("boff", cols[2])
			oc := as.NewBin("oc", cols[3])
			city := as.NewBin("city", cols[4])
			code := as.NewBin("code", cols[5])
			// skip too large entry
			if l := len(cols[6]); l > 1024*1024 {
				log.Printf("skipped a large entry: %#v (%s at %d)",
					cols[:5], humanize.Bytes(uint64(l)), cnt)
				return nil
			}
			gj := as.NewBin("gj", toGJV(cols[6]))
			err := c.PutBins(nil, key, pref, boff, oc, city, code, gj)
			if err != nil {
				return err
			}
		} else {
			if len(cols[6]) > 1024*1024 {
				fmt.Printf("long line at %d\n", cnt)
			}
		}
		if *optVerbose && cnt%1000 == 0 {
			fmt.Printf("progress %d\n", cnt)
		}
		cnt++
		return nil
	})
	if err != nil {
		fmt.Printf("terinated at line %d\n", cnt)
	}
	if *optVerbose && err == nil {
		fmt.Printf("completed")
	}
	return err
}

func main() {
	flag.Parse()
	if flag.NArg() < 1 {
		log.Fatal("require more args")
	}

	err := run()
	if err != nil {
		log.Fatal(err)
	}
}
