package it.sapienza.appinterpreter.paginatedmanager

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.paging.DataSource
import androidx.paging.ItemKeyedDataSource
import androidx.paging.PagedList
import androidx.paging.toLiveData
import it.sapienza.appinterpreter.custom_view.EventManager
import it.sapienza.appinterpreter.extensions.getArray
import it.sapienza.appinterpreter.model.event.RESTService
import org.json.JSONObject
import java.util.concurrent.Executor

class ListViewDataTimeViewModel(context : Context,
                                dataMapping : String,
                                request : RESTService,
                                data : JSONObject? = null) : ViewModel() {
    val dataSourceFactory = ListViewDataSourceFactory(context, dataMapping, request, data)
    val dataList: LiveData<PagedList<JSONObject>> =
        dataSourceFactory.toLiveData(
            pageSize = 10
        )

    fun invalidateDataSource() =
        dataSourceFactory.sourceLiveData.value?.invalidate()
}


class ListViewDataSourceFactory(private val context : Context,
                                private val dataMapping : String,
                                private val request : RESTService,
                                private val data : JSONObject? = null) :

    DataSource.Factory<Int, JSONObject>() {
    val sourceLiveData = MutableLiveData<ListViewDataSource>()
    private var latestSource: ListViewDataSource? = null

    override fun create(): DataSource<Int, JSONObject> {
        latestSource = ListViewDataSource(context, dataMapping, request, data)
        sourceLiveData.postValue(latestSource)
        return latestSource!!
    }
}


class ListViewDataSource(private val context : Context,
                         private val dataMapping : String,
                         private val request : RESTService,
                         private val data : JSONObject? = null) : ItemKeyedDataSource<Int, JSONObject>() {

    override fun getKey(item: JSONObject): Int {
        return request.paginated?.getCurrentPage(item) ?: 0
    }

    override fun loadInitial(
        params: LoadInitialParams<Int>,
        callback: LoadInitialCallback<JSONObject>
    ) {
       EventManager.manageSyncronous(context, request, data)?.let {
           val values = it.getArray(dataMapping)!!

           val resultList = mutableListOf<JSONObject>()

           for(i in 0 until values.length()){
               val jsonObject = values.getJSONObject(i)
               jsonObject.put("parent_page", params.requestedInitialKey ?: 0)
               resultList.add(jsonObject)
           }

           callback.onResult(resultList,request.paginated?.getCurrentPage(it) ?: 0,request.paginated?.getTotalPages(it) ?: 0)
       } ?: kotlin.run { callback.onResult(mutableListOf<JSONObject>()) }
    }

    override fun loadAfter(params: LoadParams<Int>, callback: LoadCallback<JSONObject>) {
        callForPage(params.key+1, callback)
    }

    override fun loadBefore(params: LoadParams<Int>, callback: LoadCallback<JSONObject>) {
        if(params.key-1 >= 0) {
            callForPage(params.key - 1, callback)
        }
    }

    private fun callForPage(
        page : Int,
        callback: LoadCallback<JSONObject>
    ) {
        EventManager.manageSyncronous(context, request, data, page = page)?.let {
            val values = it.getArray(dataMapping)!!

            val resultList = mutableListOf<JSONObject>()

            for (i in 0 until values.length()) {
                val jsonObject = values.getJSONObject(i)
                jsonObject.put("parent_page",page)
                resultList.add(jsonObject)
            }

            callback.onResult(resultList)
        } ?: kotlin.run { callback.onResult(mutableListOf<JSONObject>()) }
    }

}