package it.sapienza.appinterpreter.custom_view

import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewTreeObserver
import android.widget.LinearLayout
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.ViewPager
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayout.OnTabSelectedListener
import com.google.android.material.tabs.TabLayoutMediator
import it.sapienza.androidratio.appratio.R
import it.sapienza.appinterpreter.activity.MainActivity
import it.sapienza.appinterpreter.extensions.px
import it.sapienza.appinterpreter.fragment.MainFragment
import it.sapienza.appinterpreter.model.view_model.PagerView
import kotlinx.android.synthetic.main.viewpager.*
import kotlinx.android.synthetic.main.viewpager.view.*
import org.json.JSONObject


//open class WrapContentHeightViewPager : ViewPager {
//    constructor(context: Context?) : super(context!!)
//    constructor(context: Context?, attrs: AttributeSet?) : super(
//        context!!, attrs
//    )
//
//    private var currentPosition = 0
//
//    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
//        var heightMeasureSpecv = heightMeasureSpec
//        if (currentPosition < childCount) {
//            val child = getChildAt(currentPosition)
//            child.measure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED))
//            val height = child.measuredHeight
//            if (height != 0) {
//                heightMeasureSpecv = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY)
//            }
//        }
//        super.onMeasure(widthMeasureSpec, Math.max(200.px,heightMeasureSpecv))
//    }
//
//    open fun measureViewPager(currentPosition: Int) {
//        this.currentPosition = currentPosition
//        requestLayout()
//    }
//}

class CViewPager  @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    init {
        id = View.generateViewId()
        inflate(getContext(), R.layout.viewpager, this);
    }

    var view: PagerView? = null
    var obj: JSONObject? = null

    fun configure(view: PagerView, obj: JSONObject?): View? {

        layoutParams =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

        this.view = view

        fill(obj)


        return this
    }


    fun fill(obj: JSONObject?) {

        visibility = if(view?.isVisible(obj) == true) VISIBLE else GONE

//        viewpager.adapter = object : FragmentPagerAdapter(
//            (context as MainActivity).supportFragmentManager,
//            BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT
//        ){
//            override fun getItem(position: Int): Fragment {
//                val tabView = view!!.tabs[position]
//                val bundle = bundleOf("idView" to tabView.itemView.id)
//                tabView.dataObj?.let { bundle.putString("MainFragment.data", it.toString()) } ?: kotlin.run {
//                    obj?.let { bundle.putString("MainFragment.data", it.toString()) }
//                }
//                return MainFragment().apply {
//                    arguments = bundle
//                }
//            }
//
//            override fun getPageTitle(position: Int): CharSequence? {
//                return view!!.tabs[position].name
//            }
//
//            override fun getCount(): Int {
//                return view?.tabs?.size ?: 0
//            }
//        }
//
//        tab_layout.addOnTabSelectedListener(object : OnTabSelectedListener {
//            override fun onTabSelected(tab: TabLayout.Tab) {
//                viewpager.measureViewPager(tab.position)
//            }
//
//            override fun onTabUnselected(tab: TabLayout.Tab) {}
//            override fun onTabReselected(tab: TabLayout.Tab) {}
//        })

        viewpager.adapter = object : FragmentStateAdapter(
            (context as MainActivity)
        ){
//            override fun getPageTitle(position: Int): CharSequence? {
//                return view!!.tabs[position].name
//            }

            override fun getItemCount(): Int {
                return view?.tabs?.size ?: 0
            }

            override fun createFragment(position: Int): Fragment {
                val tabView = view!!.tabs[position]
                val bundle = bundleOf("idView" to tabView.itemView.id)

                tabView.dataObj?.let { bundle.putString("MainFragment.data",it.toString()) } ?: kotlin.run {
                    obj?.let { bundle.putString("MainFragment.data",it.toString()) }
                }

                return MainFragment().apply {
                    arguments = bundle
                }
            }
        }

        //viewpager.offscreenPageLimit = view?.tabs?.size ?: 0


        TabLayoutMediator(tab_layout, viewpager) { tab, position ->
            tab.text = view!!.tabs[position].name
        }.attach()


    }
}