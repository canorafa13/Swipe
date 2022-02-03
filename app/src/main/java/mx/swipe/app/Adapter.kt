package mx.swipe.app

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipDescription
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.util.Log
import android.view.DragEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.drawToBitmap
import androidx.core.view.marginStart
import androidx.recyclerview.widget.RecyclerView

data class Item(val title: String, val isFolder: Boolean)

class Adapter(private val data: List<Item>, private val withScreen: Int) : RecyclerView.Adapter<Adapter.ViewHolder>() {
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view){
        val name: TextView = view.findViewById(R.id.zTitleItemHome)
        val zItem: ConstraintLayout = view.findViewById(R.id.zItem)
        val zIconItemHome: ImageView = view.findViewById(R.id.zIconItemHome)
        val zFolderItemHome: CardView = view.findViewById(R.id.zFolderItemHome)
        val zActionItemHome: View = view.findViewById(R.id.zActionItemHome)
    }

    @SuppressLint("ResourceType")
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(viewGroup.context).inflate(R.layout.z_item, viewGroup, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        viewHolder.name.text = data[position].title
        viewHolder.zItem.post {
            val withItem = viewHolder.zItem.width
            val withMarginItem = viewHolder.zItem.marginStart
            val count: Int = (withScreen / (withItem + withMarginItem * 2))
            val sobrante = withScreen - ((withItem + withMarginItem * 2) * count)
            viewHolder.zItem.setPadding((sobrante/count)/2, 0, (sobrante/count)/2, 0 )
        }

        if (data[position].isFolder){
            /**Soltar*/
            viewHolder.zFolderItemHome.visibility = View.VISIBLE
            viewHolder.zActionItemHome.setOnDragListener(object: View.OnDragListener{
                override fun onDrag(v: View?, event: DragEvent?): Boolean {
                    event?.let {
                        val q = when(event.action){
                            DragEvent.ACTION_DRAG_STARTED -> true
                            DragEvent.ACTION_DRAG_LOCATION -> true
                            DragEvent.ACTION_DROP -> true
                            DragEvent.ACTION_DRAG_ENDED -> true
                            else -> false
                        }
                    }
                    Log.e("FOLDER", event.toString())
                    return true
                }
            })
        }else{
            /**Arrastrar y soltar*/
            viewHolder.zIconItemHome.apply {
                visibility = View.VISIBLE
            }

            viewHolder.zActionItemHome.apply {
                tag = data[position].title
                setOnLongClickListener { v ->
                    Log.e("LONG", "true")
                    val item = ClipData.Item(v.tag as? CharSequence)
                    val dragData = ClipData(
                        v.tag as? CharSequence,
                        arrayOf(ClipDescription.MIMETYPE_TEXT_PLAIN),
                        item)

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        val bitmap = viewHolder.zItem.drawToBitmap(Bitmap.Config.ARGB_8888)
                        v.startDragAndDrop(dragData,  // The data to be dragged
                            MyDragShadowBuilder(viewHolder.zIconItemHome, resources.getDrawable(R.drawable.icon, null)),  // The drag shadow builder
                            null,      // No need to use local data
                            0          // Flags (not currently used, set to 0)
                        )
                    }
                    true
                }
            }


        }

    }

    override fun getItemCount() = data.size
}