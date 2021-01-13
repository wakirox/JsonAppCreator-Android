package it.sapienza.appinterpreter.model

enum class LayoutOrientation {
    horizontal, vertical
}

enum class ListOrientation {
    horizontal, vertical, grid;

    fun toLayout() : LayoutOrientation{
        if(this == horizontal){
            return LayoutOrientation.horizontal
        }
        return LayoutOrientation.vertical
    }
}