package it.sapienza.androidratio.appratio

import it.sapienza.appinterpreter.utils.AppParser
import org.junit.Test as test
import org.junit.Assert.*
import java.io.File

class TestTest() {

    @test fun simple(){
        val parser = AppParser("""{
          "name": "My first app",
          "author": "Appkin",
          "version": "1.0",
          "mainView": {
            "type": "layout",
            "views": [{ "type": "text", "label": "Hello world!" }]
          }
        }""".trimIndent())

        val application = parser.parseApplication2()
        assertEquals("My first app", application.name)
        assertEquals(2, application.views.size)
    }

    @test fun parsesChainedRestAction() {
        val parser = AppParser("""{
          "name": "REST app",
          "author": "Appkin",
          "version": "1.0",
          "mainView": {
            "type": "layout",
            "views": [{ "id": "result", "type": "text", "label": "Result" }]
          },
          "actions": [{
            "id": "load",
            "type": "click",
            "event": {
              "type": "rest",
              "url": "https://example.com/data",
              "thenDo": { "type": "show", "view": { "id": "result" } }
            }
          }]
        }""".trimIndent())

        val application = parser.parseApplication2()
        assertEquals(1, application.actions.size)
        assertNotNull(application.actions.single().event)
    }

    @test fun parsesMenuSamples() {
        listOf(
            "v3_masterpieces.json",
            "v3_app_sample.json",
            "v3_race_signup.json",
            "v3_the_movie_db.json",
            "v3_pokedex.json"
        ).forEach { name ->
            val json = File("src/main/res/raw/$name").readText()
            assertNotNull("Failed to parse $name", AppParser(json).parseApplication2())
        }
    }

}
