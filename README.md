# PDFFilter

PDFFilter allows searching PDFs content as text and perform either a move or run a custom script (currently only on Windows) if a keyword or regex has been found.

## Installation / Running

1. Download PDFFilter.jar from the [release page](https://github.com/ddomnik/PDFFilter/releases)

1. Simply run the .jar by executing this command:  
`java -jar .\PDFFilter.jar`
 
1. Make sure a filter.json is located in the same folder as the PDFFilter.jar or specify a custom filter file using the following command:  
`java -jar .\PDFFilter.jar  "/path/to/myFilter.json"`

## filter.json

### settings:
```
{
  "settings":{
    "folder": "./test/pdfs",   // Path to directory that contains the PDFs to be filtered
    "no_match": "./test/filtered/nomatch",    // (optional) if set, PDFs that do not match any filter will be moved in that directory
    "processing_error": "./test/filtered/error"   // Path for PDFs that could not be processed
  }
```

### filter:

A simple filter can contain the following:

```
    {
      "keywords": ["key1", "key2"],
      "regex": "(ABC)[1]\\d\\d",
      "move_to": "./test/filtered",
      "run_script": "./myScript.ps1",
      "append_date": true
    }
```

where:
- `keywords` or `regex` or both is given
- `move_to` or `run_script` or both is given
- `append_date` is optional

If a keyword from `keywords` or the `regex` is matched in a PDF the `move_to` and/or `run_script` is executed.  
The key `move_to` is a path (folder) where the PDF is moved to. This can be absolute or relative to the .jar  
The key `run_script` is path (relative or absolute) to a Windows .ps1 script, that is executed. The script is called with a _-pdf_ parameter containing the PDFs path.  If the key `append_date` is set to _true_ a date with the pattern dd.MM.yyyy and dd-MM-yyyy is searched within the PDF. If found, it will be added to the filename if `move_to` is set. Returns the first date found.
