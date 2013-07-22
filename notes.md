
18/7/2013 initial version
    a java implementation of (venti)[http://swtch.com/plan9port/man/man7/venti.html]
    to serve as a sandbox for some ideas. This version is a simple thread per connection
    server that implements the venti protocol and stores all data in memory.

23/7/2013 add disk storage
    Blocks are stored in a RecordIO file. The file format is reverse engineered from the
    facebook (folly)[http://github.com/facebook/folly] library.

