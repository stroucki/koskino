koskino
=======

A sandbox for experiments with backup storage. First cut is a multi-threaded, file system backed implementation of plan9's venti.

The code base was forked from Spyros Anastasopoulos.

The thought is that filesystems are a kind of database, and that this
could remove the need for a venti index file and bloom filter. The
system's block cache would provide the read cache.

Lessfs does something similar, and is closer to what I wanted to do, but
this is written in Java, and lessfs is written in C.

Both lessfs and stroucki/koskino ended up having poor performance
(around 6 MB/s write), apparently due to the thrashing of the data
disk. Venti also did poorly on it, though. When attempting to see
how much storage was being used, the du command ran for days, due
to bad stat performance. When trying to remove the data, the rm command
barely made 10 GB progress per day, so the filesystem was effectively
rendered unusable by having many files on it. Reformatting to use
xfs rather than ext4 gave much better performance with venti, so
at some time I'll try stroucki/koskino and lessfs on it. xfs is a
filesystem I have always been happy with in the past. ext4 seems more
like a toy to me now.

2014-06-24: It works as a venti server, storing blocks in the file system.
