(defproject chess-clojure "0.1.0-SNAPSHOT"
  :dependencies [
		 [org.clojure/clojure "1.5.1"]
		 ;;[org.clojure/clojure "1.6.0-master-SNAPSHOT"]
                 [primitive-math "0.1.3"]
                 [org.clojure/math.numeric-tower "0.0.2"]]
  :main chess-clojure.core
  :jvm-opts ^:replace ["-server" "-Xms4G" "-Xmx12G" "-XX:NewRatio=8"])
