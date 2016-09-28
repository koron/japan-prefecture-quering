package main

import (
	"encoding/json"
	"flag"
	"fmt"
	"io"
	"log"
	"os"
	"strings"

	dproxy "github.com/koron/go-dproxy"
	"github.com/ulikunitz/xz"
)

func readData(name string) (interface{}, error) {
	f, err := os.Open(name)
	if err != nil {
		return nil, err
	}
	defer f.Close()
	var r io.Reader
	r = f

	if strings.HasSuffix(name, ".xz") {
		r, err = xz.NewReader(f)
		if err != nil {
			return nil, err
		}
	}

	d := json.NewDecoder(r)
	var v interface{}
	err = d.Decode(&v)
	if err != nil {
		return nil, err
	}
	return v, nil
}

func main() {
	flag.Parse()
	if flag.NArg() < 1 {
		log.Fatal("need a file")
	}

	data, err := readData(flag.Arg(0))
	if err != nil {
		log.Fatal("failed to readData: ", err)
	}

	p := dproxy.New(data)
	features := p.M("features").ProxySet()
	known := map[string]int{}
	for i, l := 0, features.Len(); i < l; i++ {
		f := features.A(i)
		props := f.M("properties")
		n1, _ := props.M("N03_001").String()
		n2, _ := props.M("N03_002").String()
		n3, _ := props.M("N03_003").String()
		n4, _ := props.M("N03_004").String()
		n7, _ := props.M("N03_007").String()
		var key string
		n, ok := known[n7]
		if !ok {
			n = 1
		} else {
			n++
		}
		known[n7] = n
		key = fmt.Sprintf("%s-%d", n7, n)
		var gd string
		if geo, err := f.M("geometry").M("coordinates").Value(); err != nil {
			log.Printf("%s: %s", err, key)
		} else {
			d, err := json.Marshal(geo)
			if err != nil {
				log.Printf("%s: %s", err, key)
			} else {
				gd = string(d)
			}
		}
		fmt.Printf("%s\t%s\t%s\t%s\t%s\t%s\t%s\n", key, n1, n2, n3, n4, n7, gd)
	}
}
