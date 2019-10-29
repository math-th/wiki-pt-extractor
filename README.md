# WIKIPEDIA - PLAIN TEXT EXTRACTOR

**Disclaimer : this project wasn't designed to be shared, so it might be unstable or unclean. Please take it into account when using it**

## GOAL

This project is a **simple, quick, light script to extract pages content from Wikipedia**. It was designed to extract only a clean text, excluding tables, images, references and small articles. It can be used to create big corpora from Wikipedia pages.

The script will start exploring an URL (default is Wikipédia "France" page), download its content and save it to a text file, and then explore every Wikipedia links found in the page, while the desired count is not reached. The script handle a stop/restart process : you can stop the script by giving any input to your console (e.g. enter something and then hit Enter), explored and to explored URL are saved, so on the next launch in the same output directory will not download the same pages twice.

It was initially designed to extract French pages, but the language could be easilly changed.

**This script makes an usage of Wikipedia servers sending them many requests, so please use it carefully to avoid using their resources ! (or you will get a 429 HTTP code :-) )**

## USAGE

This project runs on Java > 8

Download a prebuilt version : [wiki-pt-extractor-1.0.0-all.jar](TODO)

```
java -jar wiki-pt-extractor-1.0.0-all.jar [options] <url> Wiki url where the script should begin (default is set to "https://fr.wikipedia.org/wiki/France")
  Options:
    -count
      Number of page wanted
      Default: 500
    -output
      Path to the output directory
      Default: ./result
    -thread
      Thread pool size (number of parallel thread)
      Default: 8
```


## PROJECT

Shared under **GNU General Public License v3.0**

*This was developed in the collaborative project PREDICT4ALL involving [CMRRF Kerpape](http://www.kerpape.mutualite56.fr/fr), [Hopital Raymond Poincaré](http://raymondpoincare.aphp.fr/) and [BdTln Team, LIFAT, Université de Tours](https://lifat.univ-tours.fr/teams/bdtin/), funded by [Fondation Paul Bennetot](https://www.fondationpaulbennetot.org/).*