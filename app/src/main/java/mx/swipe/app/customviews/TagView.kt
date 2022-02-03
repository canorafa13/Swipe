package mx.swipe.app.customviews

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import com.bumptech.glide.Glide
import mx.swipe.app.R
import mx.swipe.app.databinding.CustomTagsViewBinding
import java.lang.Exception

class TagView: ConstraintLayout {
    private lateinit var binding: CustomTagsViewBinding
    constructor(context: Context) : super(context) {
        initStyle()
    }
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        initStyle()
    }
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        initStyle()
    }
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes) {
        initStyle()
    }

    private fun initStyle(){
        val view = LayoutInflater.from(context).inflate(R.layout.custom_tags_view, this, true)
        binding = CustomTagsViewBinding.bind(view)
        binding.apply {
            zLogoOne.visibility = View.GONE
            zLogoTwo.visibility = View.GONE
            zText.visibility = View.GONE
            zNext.visibility = View.GONE
            zBadge.visibility = View.GONE
        }
        //setLogoAndText("https://server.com/imagen.png", "Escuchanos!", false)

    }

    fun setAction(listener: OnClickListener){
        binding.zAction.setOnClickListener(listener)
    }

    fun setBackground(color: Int){
        binding.zBackground.setCardBackgroundColor(color)
    }

    fun setBackground(color: String){
        try {
            setBackground(Color.parseColor(color))
        }catch (e: Exception){}
    }

    fun setTextColor(color: Int){
        binding.zText.setTextColor(color)
    }

    fun setTextColor(color: String){
        try{
            setTextColor(Color.parseColor(color))
        }catch (e: Exception){}
    }

    fun setTextAndLogo(text: String, url: String){
        binding.apply {
            zLogoOne.visibility = View.GONE
            zText.visibility = View.VISIBLE
            zText.text = text
            zLogoTwo.visibility = View.VISIBLE
            zNext.visibility = View.GONE
            zLogoTwo.layoutParams
        }

        Glide.with(context)
            .load(url)
            .into(binding.zLogoTwo)

    }

    fun setLogoAndText(url: String, text: String, withIcon: Boolean){
        binding.apply {
            zLogoOne.visibility = View.VISIBLE
            zText.visibility = View.VISIBLE
            zText.text = text
            zLogoTwo.visibility = View.GONE
        }
        if (withIcon){
            binding.zNext.visibility = View.VISIBLE
        }


        Glide.with(context)
            .load(url)
            .into(binding.zLogoOne)

    }

    fun setCountBadge(count: Int){
        if (count > 0){
            binding.apply {
                zBadge.visibility = View.VISIBLE
                zTextBadge.text = when(count){
                    in (1..99) -> "$count"
                    else -> "99+"
                }
            }
        }
    }
}