(defproject chess-clojure "0.1.0-SNAPSHOT"
  :dependencies [
		 ;;[org.clojure/clojure "1.5.1"]
		 [org.clojure/clojure "1.6.0-alpha3"]
		 ;;[org.clojure/clojure "1.6.0-master-SNAPSHOT"]
                 [primitive-math "0.1.3"]
                 [org.clojure/math.numeric-tower "0.0.2"]]
  :main chess-clojure.core
  ;; This is enough for N-queens 6x6 board.  Probably less will work, too.
  :jvm-opts ^:replace ["-server" "-Xms1G" "-Xmx1G" "-XX:NewRatio=8"]
  ;; You will need more than 4GB of heap space for N-queens 6x9 board
  ;;:jvm-opts ^:replace ["-server" "-Xms4G" "-Xmx12G" "-XX:NewRatio=8"]
  )
