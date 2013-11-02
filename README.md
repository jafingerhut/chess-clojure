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
modifications as of Oct 30, 2013, follow these steps:

    # Get modified version of Clojure from forked repo

    % git clone git://github.com/jafingerhut/clojure.git

    # Switch to branch where proposed hashing changes have been
    # implemented.

    % cd clojure
    % git checkout better-hashing

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
sure to change *show-hash-stats* to false.  Calculating the stats
takes significant extra CPU time.


## License

Copyright (C) 2013 Paul Butcher, Andy Fingerhut

Distributed under the Eclipse Public License.
