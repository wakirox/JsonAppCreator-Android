package it.sapienza.appinterpreter.model.screen



//class Screen(
//    var id : String,
//    var data : MutableMap<Any?,Any?>?,
//    var inheritedData : Boolean? = true,
//    var init : CallService?,
//    var layouts : List<Layout> = listOf()){
//    fun isEmpty() = layouts.isEmpty()
//
//    var dataObj: JSONObject?
//        get() = data?.let { JSONObject(it) }
//        set(value) {
//            val typeRef: TypeReference<MutableMap<Any?,Any?>?> =
//                object : TypeReference<MutableMap<Any?,Any?>?>() {}
//
//            data = jacksonObjectMapper().readValue(value.toString(), typeRef)
//        }
//
//    fun toReference() : Screen {
//        return Screen(id,null,null, null,listOf())
//    }
//
//}