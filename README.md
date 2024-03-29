# PDFFilter

PDFFilter allows searching PDFs content as text and perform a custom task.  
For example, a PDF is moved to a custom folder or a script is executed (currently only on Windows) if a search matches.  
The search method can be a simple word, one out of multible words or a regex.  

## Installation / Running

1. Download PDFFilter.jar from the [release page](https://github.com/ddomnik/PDFFilter/releases)

1. Make sure a filter.json is located in the same folder as the PDFFilter.jar or specify a custom filter file using the following command:  
`java -jar .\PDFFilter.jar  "/path/to/myFilter.json"`

1. Simply run the .jar by executing this command:  
`java -jar .\PDFFilter.jar`

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


### Example filter:

Please note hat everything after an `#` needs to be removed to make this filter work.

```
{
  "settings":{
    "folder": "./test/pdfs",
    "no_match": "./test/filtered/nomatch",
    "processing_error": "./test/filtered/error"
  },
  "filter":
  [
    {
      "keywords": ["Invoice"],                     #Comment: Move all PDFs that contain "Invoice" to the given folder. 
      "move_to": "./test/filtered/invoices",
      "append_date": true                          #Comment: If a date is found, it gets added to the PDF filename
    },
    {
      "keywords": ["Ads", "Newsletter", "News"],   #Comment: Move all PDFs that contain at least of these 3 words.
      "move_to": "./test/filtered/spam",
      "append_date": true                          #Comment: If a date is found, it gets added to the PDF filename
    },
    {
      "regex": "(item-)[1]\\d\\d",                 #Comment: Move all PDFs that contain the word "item-1XX" where XX is a digit. For example if "item-123" is found.
      "move_to": "./test/filtered/item"
    },
  ]
}
```
