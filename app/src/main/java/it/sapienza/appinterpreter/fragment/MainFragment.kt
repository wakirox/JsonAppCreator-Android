package it.sapienza.appinterpreter.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import it.sapienza.androidratio.appratio.R
import it.sapienza.appinterpreter.ContainerConfiguration
import it.sapienza.appinterpreter.activity.MainActivity
import it.sapienza.appinterpreter.extensions.app
import it.sapienza.appinterpreter.model.view_model.helper.MView
import kotlinx.android.synthetic.main.main_fragment.*
import org.json.JSONObject

class MainFragment : Fragment(R.layout.main_fragment) {

    var mView : MView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        this.mView = activity!!.application.app()!!.viewBy(requireArguments().getString("idView")!!)!!

//        val typeRef: TypeReference<MutableMap<Any?, Any?>?> =
//            object : TypeReference<MutableMap<Any?, Any?>?>() {}
//
//        requireArguments().getJsonExtra(typeRef)?.let { it ->
//            mView.data = it
//        }

        requireArguments().getString("MainFragment.data")?.let {
            mView!!.dataObj = JSONObject(it)
        }

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = super.onCreateView(inflater, container, savedInstanceState)
//        activity?.runOnUiThread{
            mView?.let { addView(it,view as ViewGroup) }
//        }
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        activity?.runOnUiThread{
//            mView?.let { addView(it) }
//        }

    }

    fun addView(model: MView, root : ViewGroup = fragment_root) {
        ContainerConfiguration.createContainer(
            context!!,
            activity!!.application.app()!!,
            model,
            root
        )
    }


}