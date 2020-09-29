package it.sapienza.appinterpreter.utils

import android.util.Log
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import it.sapienza.appinterpreter.model.Layout
import it.sapienza.appinterpreter.model.ModelApplication
import it.sapienza.appinterpreter.model.event.AlertMessage
import it.sapienza.appinterpreter.model.event.CallService
import it.sapienza.appinterpreter.model.event.Event
import it.sapienza.appinterpreter.model.event.ShowView
import it.sapienza.appinterpreter.model.view_model.ListView


class AppParser(val obj : String) {

    inline fun<T> measureTimeMillisPair(function: () -> T): T {
        val startTime = System.currentTimeMillis()
        val result: T = function.invoke()
        val endTime = System.currentTimeMillis()

        Log.d("TIME","Function took ${endTime - startTime}")
        return result
    }

    fun parseApplication2() : ModelApplication {

        /*
        * per facilitare la scrittura del modello si è deciso di dare la possibilità di riutilizzare determinati elementi
        * all'iterno del modello specificando solamente l'id di un elemento è possibile referenziarlo dalla lista generale dell'app
        * Per evitare problemi con la scrittura non è
        * possibile referenziare un elemento che si trova definito all'interno di un altro elemento*/

        //measureTimeMillisPair {

        val mapper = jacksonObjectMapper()

        val model = measureTimeMillisPair { mapper.readValue<ModelApplication>(obj) }

        model.actions.find { it.isEmpty() }?.let {
            throw Exception("Empty model.actions[id=${it.id}]")
        }

        model.views.find { it.isEmpty() }?.let {
            throw Exception("Empty model.views[id=${it.id}]")
        }

        model.layouts.find { it.isEmpty() }?.let {
            throw Exception("Empty model.layouts[id=${it.id}]")
        }

        model.screens.forEach {
            it.layouts.forEach {
                findScreens(it,model)
            }
        }
        model.actions.forEach { it.event?.eventInstance?.let { ev -> findScreens(ev, model) } }
        model.layouts.filter { !it.isEmpty() }.forEach { findScreens(it,model) }
        if(!model.main.isEmpty()){
            model.main.layouts.filter { !it.isEmpty() }.forEach { findScreens(it,model) }
        }
//        model.initService?.let {
//            it.thenDo?.eventInstance?.let { ev -> findScreens(ev, model) }
//        }

        checkModelConsistency(model)

        return model

        //}
    }


    fun parseApplication() : ModelApplication {

        /*
        * per facilitare la scrittura del modello si è deciso di dare la possibilità di riutilizzare determinati elementi
        * all'iterno del modello specificando solamente l'id di un elemento è possibile referenziarlo dalla lista generale dell'app
        * Per evitare problemi con la scrittura non è
        * possibile referenziare un elemento che si trova definito all'interno di un altro elemento*/

        val mapper = jacksonObjectMapper()
        val model = mapper.readValue<ModelApplication>(obj)



//        model.initService?.let {
//            it.thenDo?.eventInstance?.let { ev -> analyzeEvent(ev, model) }
//        }

        model.main.takeIf { !it.isEmpty() }?.let {
            model.main = it.toReference()
            model.screens.add(it)
        }

        model.screens.forEach {
            model.layouts.addAll(it.layouts.filter { l->!l.isEmpty() })
        }

        model.layouts.iterator().forEach {
            it.views.forEach { v ->
                v.action?.event?.let {
                    it.eventInstance?.let { ev -> analyzeEvent(ev, model) }
                }
            }
        }

        var toAddLayout = mutableListOf<Layout>()
        model.layouts.forEach {
            //converto le view in realtà
            it.views.forEach{

                if(it is ListView){
                    toAddLayout.add(it.layout)
                }

                it.action?.takeIf { !it.isEmpty() }?.let {
                    model.actions.add(it)
                }
            }

        }

        model.layouts.addAll(toAddLayout)

        checkModelConsistency(model)

        model.actions.filter { e->e.event?.eventInstance is ShowView}.onEach {
//            var ev = (it.event?.eventInstance as ShowScreen)
//            if(ev.screen.isEmpty() ){
//                ev.screen = model.screens.find { e->e.id == ev.screen.id }!!
//            }
        }

        //setto le azioni qualora siano solamente riferimenti
        model.layouts.forEach { it.views.forEach{
            it.action?.let { ac ->
                if (ac.isEmpty()) {
                    it.action = model.actions.find { ai -> ai.id == ac.id }!!
                }
            }
        } }


//        if(model.main.isEmpty()){
//            model.main = model.screens.find { it.id == model.main.id }!!
//        }

        return model
    }

    private fun findScreens(
        ev: Event,
        model: ModelApplication
    ) {
        when(ev){
            is ShowView -> if(!ev.screen.isEmpty()){
                model.screens.add(ev.screen)
                ev.screen.layouts.filter { !it.isEmpty() }.forEach { findScreens(it,model) }
            }
            is CallService -> {
                ev.thenDo?.eventInstance?.let { findScreens(it,model) }
            }
            is AlertMessage -> {
                ev.thenDoOK?.eventInstance?.let { findScreens(it,model) }
                ev.thenDoKO?.eventInstance?.let { findScreens(it,model) }
            }
        }
    }

    private fun findScreens(
        layout: Layout,
        model: ModelApplication
    ) {
        layout.views.forEach {
            it.action?.event?.eventInstance?.let {
                findScreens(it,model)
            }
            if(it is ListView && !it.layout.isEmpty()){
                findScreens(it.layout,model)
            }
        }
    }


    private fun analyzeEvent(
        ev: Event,
        model: ModelApplication
    ) {
        when(ev){
            is ShowView -> if(!ev.screen.isEmpty()){
                model.screens.add(ev.screen)
                model.layouts.addAll(ev.screen.layouts.filter { l -> !l.isEmpty() })
                ev.screen = ev.screen.toReference()
            }
            is CallService -> {
                ev.thenDo?.eventInstance?.let { analyzeEvent(it,model) }
            }
            is AlertMessage -> {
                ev.thenDoOK?.eventInstance?.let { analyzeEvent(it,model) }
                ev.thenDoKO?.eventInstance?.let { analyzeEvent(it,model) }
            }
        }
    }

    private fun checkModelConsistency(model: ModelApplication) {
        if(model.actions.any { it.isEmpty() }) throw Exception("A action model is empty")
        if(model.layouts.any { it.isEmpty() }) throw Exception("A layout model is empty")
        if(model.screens.any { it.isEmpty() }) throw Exception("A screen model is empty")

        if (model.layouts.map { it.id }.toSet().size != model.layouts.size) {
            throw Exception("You used a not unique layout id")
        }

        if (model.screens.map { it.id }.toSet().size != model.screens.size) {
            throw Exception("You used a not unique screen id")
        }

        if (model.actions.map { it.id }.toSet().size != model.actions.size) {
            throw Exception("You used a not unique layout id")
        }
    }

//    fun parseApplication() : ModelApplication {
//        var application = ModelApplication()
//
//        application.name = obj.getString("name")
//        application.version = obj.getString("version")
//
//        if(obj.has("debugMode")){
//            application.debugMode = obj.getBoolean("debugMode")
//        }
//
//        application.main = parseScreen(obj.getJSONObject("main"))
//
//        if(obj.has("screens")){
//            application.screens = mutableListOf()
//            val arr = obj.getJSONArray("screens")
//            for (i in 0 until arr.length()){
//                application.screens?.add(parseScreen(arr.getJSONObject(i)))
//            }
//        }
//
//        if(obj.has("layouts")){
//            application.layouts = mutableListOf()
//            val arr = obj.getJSONArray("layouts")
//            for (i in 0 until arr.length()){
//                application.layouts?.add(parseLayout(arr.getJSONObject(i)))
//            }
//        }
//
//        if(obj.has("actions")){
//            application.actions = mutableListOf()
//            val arr = obj.getJSONArray("actions")
//            for (i in 0 until arr.length()){
//                application.actions?.add(parseAction(arr.getJSONObject(i)))
//            }
//        }
//
//
//        return application
//    }
//
//    private fun parseAction(obj: JSONObject): Action {
//        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
//    }
//
//    private fun parseLayout(obj: JSONObject): Layout {
//        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
//    }
//
//    private fun parseScreen(obj : JSONObject) : Screen {
//        var screen = Screen()
//
//        screen.id = obj.getString("id")
//
//        if(obj.has("data")) {
//            screen.data = obj.getJSONObject("data")
//        }
//
//        if(obj.has("inheritedData")){
//            screen.inheritedData = obj.getBoolean("inheritedData")
//        }
//
//        return screen
//    }



}