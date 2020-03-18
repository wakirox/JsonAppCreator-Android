package it.sapienza.androidratio.appratio

import it.sapienza.appinterpreter.utils.AppParser
import org.junit.Test as test
import org.junit.Assert.*

class TestTest() {

    @test fun simple(){
        val parser = AppParser("""{
  "name" : "My first app",
  "version" : "alpha-01",
  "main" : {
    "id" : "screen01",
    "data" : {
      "name" : "Hello world!"
    },
    "inheritedData" : false,
    "layouts" : [
      {
        "id" : "layout01",
        "orientation" : "vertical",
        "inheritedData" : true,
        "views" : [
          {
            "type" : "text",
            "mapping" : "name",
            "action" : {
              "id" : "action01",
              "inheritedData" : true,
              "type" : "click",
              "event" : {
                "type" : "alert",
                "value" : "Hello world",
                "thenDo" : {
                  "type" : "alert",
                  "value" : "Hello world",
                  "thenDo" : {
                    "type" : "alert",
                    "value" : "Hello world"
                  }
                }
              }
            }
          }
        ]
      }
    ]
  }
}""")

        val v = parser.parseApplication()
        print(v)
    }

}