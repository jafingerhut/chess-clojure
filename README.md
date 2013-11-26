# chess-clojure

Implementation of program that calculates the number of solutions to
an N-queens problem, translated from a similar version written in
Scala.  Both Scala and original Clojure versions were written by Paul
Butcher.  Modifications to Clojure program for investigating alternate
hash functions by Andy Fingerhut.

## Usage

Run `lein run help` for help on command line options.  `lein run all`
takes about 9 minutes on a 3 GHz Intel Xeon with Clojure 1.5.1 and JDK
1.6.0_65.

To reproduce results with Mark Engelberg's proposed hash function
modifications as of Nov 18, 2013, follow these steps:

    # Get modified version of Clojure from forked repo

    % git clone git://github.com/jafingerhut/clojure.git

    # Switch to branch where proposed hashing changes have been
    # implemented.

    % cd clojure
    % git checkout better-hashing-2013-11-18

    # Build modified version and install in your local Maven repo in
    # $HOME/.m2 as version 1.6.0-master-SNAPSHOT

    % mvn install

Back in this chess-clojure directory, edit project.clj to use version
1.6.0-master-SNAPSHOT of Clojure, with a line like this:

    [org.clojure/clojure "1.6.0-master-SNAPSHOT"]

Note that the 6x6 board is the default board size in the function
chess-clojure.core/do-nqueens.  You can quickly uncomment different
lines in that function to try out different board sizes, but realize
that 4 GB is _not_ enough heap space for the 6x9 board.  It will take
a long time and then finally run out of memory.  12 GB is enough, and
perhaps less would suffice, too.

If you want to do a timing run for the N-queens function by itself, be
sure to change `*show-hash-stats*` to false.  Calculating the stats
takes significant extra CPU time.

## Results

### With 6x6 board size

#### and Clojure 1.5.1

    TBD-link

#### and 1.6.0-master-SNAPSHOT

... as built from the following repo and branch:

    % git clone git://github.com/jafingerhut/clojure.git
    % cd clojure
    % git checkout better-hashing-2013-11-18

After setting `*show-hash-stats*` to `false`, then running the
command:

    % lein run nqueens hash-set
    [ ... progress output deleted ... ]
    "Elapsed time: 12238.863 msecs"
    180568

### With 6x9 board size

#### and Clojure 1.5.1

    TBD-link

#### and 1.6.0-master-SNAPSHOT

After setting `*show-hash-stats*` to `false`, setting max JVM heap
size to 12 Gbytes in `project.clj`, and changing the 6 6 board size to
6 9 in `core.clj`, ran the command:

    % lein run nqueens hash-set
    [ ... progress output deleted ... ]
    "Elapsed time: 682646.332 msecs"
    20136752

### Hardware, OS, JVM version info

    % system_profiler | head -n 20
    Hardware:
    
        Hardware Overview:
    
          Model Name: Mac Pro
          Model Identifier: MacPro2,1
          Processor Name: Quad-Core Intel Xeon
          Processor Speed: 3 GHz
          Number Of Processors: 2
          Total Number Of Cores: 8
          L2 Cache (per processor): 8 MB
          Memory: 32 GB
          Bus Speed: 1.33 GHz
          Boot ROM Version: MP21.007F.B06
          SMC Version (system): 1.15f3
          Serial Number (system): 4073301VUPZ
          Hardware UUID: 00000000-0000-1000-8000-0019E3F9D88A
    
    % uname -a
    Darwin dhcp-171-71-55-73.cisco.com 10.8.0 Darwin Kernel Version 10.8.0: Tue Jun  7 16:33:36 PDT 2011; root:xnu-1504.15.3~1/RELEASE_I386 i386
    
    % lein version
    Leiningen 2.3.3 on Java 1.6.0_65 Java HotSpot(TM) 64-Bit Server VM

## License

Copyright (C) 2013 Paul Butcher, Andy Fingerhut

Distributed under the Eclipse Public License.
