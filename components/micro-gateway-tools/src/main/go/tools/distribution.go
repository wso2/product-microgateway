package main

import (
    "fmt"
	"log"
	"os"
	"path/filepath"

	"main/go/tools/util"
)

func main() {
	runtimePath, inErr := filepath.Abs(filepath.Dir(os.Args[0]) + "/../runtime.zip")
	destPath, destErr := filepath.Abs(filepath.Dir(os.Args[0]) + "/../runtime")

	if inErr != nil && destErr != nil {
		log.Fatal(inErr)
		log.Fatal(destErr)
		return
	}

	if _, existsErr := os.Stat(destPath); os.IsNotExist(existsErr) {
	    fmt.Println("Initializing Runtime...")
		err := util.Unzip(runtimePath, destPath)
		if err != nil {
			log.Fatal(err)
		}
	}
}
