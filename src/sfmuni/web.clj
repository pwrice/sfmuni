(ns sfmuni.web
  (:require [compojure.core :refer [defroutes GET PUT POST DELETE ANY]]
            [compojure.handler :refer [site]]
            [compojure.route :as route]
            [clojure.java.io :as io]
            [ring.middleware.stacktrace :as trace]
            [ring.middleware.session :as session]
            [ring.middleware.session.cookie :as cookie]
            [ring.adapter.jetty :as jetty]
            [ring.middleware.basic-authentication :as basic]
            [cemerick.drawbridge :as drawbridge]
            [environ.core :refer [env]]
            [clojure.data.json :as json]
  )
)

(defn- authenticated? [user pass]
  ;; TODO: heroku config:add REPL_USER=[...] REPL_PASSWORD=[...]
  (= [user pass] [(env :repl-user false) (env :repl-password false)]))

(def ^:private drawbridge
  (-> (drawbridge/ring-handler)
      (session/wrap-session)
      (basic/wrap-basic-authentication authenticated?)))

(defn json-response [data & [status]]
  {:status (or status 200)
   :headers {"Content-Type" "application/json"}
   :body (json/write-str data)})


(defn get-stop-list []
   (json-response [
      {:title "3rd St &amp; Mission St" :lat "37.7863699" :lng "-122.4021099" :stopTag "3136" :stopId "13136"}
    ])
)

(defn get-stop-prediction [stop-name]
  (json-response 
     {:tag :predictions :attrs {:stoptag 3136 :stoptitle "3rd St & Mission St" :routetag "30" :routetitle "30-Stockton" :agencytitle "San Francisco Muni"} :content [
       {:tag :direction :attrs {:title "Outbound to the Marina District"} :content [
         {:tag :prediction :attrs {:triptag "5230847" :block "3003" :vehicle 5570 :dirtag "30_OB3" :affectedbylayover true :isdeparture false :minutes 14 :seconds 871 :epochtime 1361250553707} :content nil} 
         {:tag :prediction :attrs {:triptag "5230848" :block "3011" :vehicle 5495 :dirtag "30_OB3" :affectedbylayover true :isdeparture false :minutes 32 :seconds 1951 :epochtime 1361251633707} :content nil} 
         {:tag :prediction :attrs {:triptag "5230848" :block "3011" :vehicle 5510 :dirtag "30_OB3" :affectedbylayover true :isdeparture false :minutes 41 :seconds 2516 :epochtime 1361252198287} :content nil} 
         {:tag :prediction :attrs {:triptag "5230850" :block "3023" :vehicle 5481 :dirtag "30_OB3" :affectedbylayover true :isdeparture false :minutes 71 :seconds 4291 :epochtime 1361253973707} :content nil} 
         {:tag :prediction :attrs {:triptag "5230851" :block "3009" :vehicle 5549 :dirtag "30_OB3" :affectedbylayover true :isdeparture false :minutes 91 :seconds 5491 :epochtime 1361255173707} :content nil} 
      ]}]} 
    )
)



(defroutes app
  (ANY "/repl" {:as req}
       (drawbridge req))
  (GET "/" []
       {:status 200
        :headers {"Content-Type" "text/plain"}
        :body (pr-str ["Hello" :from 'Heroku])})
  (GET "/get-stops" [] (get-stop-list))
  (GET "/get-predictions-for-stop" [stop-name] (get-stop-prediction stop-name))
  (ANY "*" []
       (route/not-found (slurp (io/resource "404.html")))))

(defn wrap-error-page [handler]
  (fn [req]
    (try (handler req)
         (catch Exception e
           {:status 500
            :headers {"Content-Type" "text/html"}
            :body (slurp (io/resource "500.html"))}))))

(defn -main [& [port]]
  (let [port (Integer. (or port (env :port) 5000))
        ;; TODO: heroku config:add SESSION_SECRET=$RANDOM_16_CHARS
        store (cookie/cookie-store {:key (env :session-secret)})]
    (jetty/run-jetty (-> #'app
                         ((if (env :production)
                            wrap-error-page
                            trace/wrap-stacktrace))
                         (site {:session {:store store}}))
                     {:port port :join? false})))

;; For interactive development:
;; (.stop server)
;; (def server (-main))
