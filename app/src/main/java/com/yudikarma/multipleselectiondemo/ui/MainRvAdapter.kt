package com.yudikarma.multipleselectiondemo.ui

import android.content.Context
import android.graphics.Color
import android.media.MediaMetadataRetriever
import android.os.Handler
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.recyclerview.selection.ItemDetailsLookup
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import com.bumptech.glide.Glide
import com.company107.zonapets.utils.mulitple_select_recycleview.Details
import com.yudikarma.multipleselectiondemo.R
import com.yudikarma.multipleselectiondemo.model.GaleryFragmentModel
import android.util.Log

class MainRvAdapter(private val interaction: Interaction? = null,
                    var context: Context?,
                    val viewModel: MainViewModel,
                    var tracker: SelectionTracker<Long>? = null
                    ) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    val DIFF_CALLBACK = object : DiffUtil.ItemCallback<GaleryFragmentModel>() {

        override fun areItemsTheSame(
            oldItem: GaleryFragmentModel,
            newItem: GaleryFragmentModel
        ): Boolean {
            return oldItem.fileName == newItem.fileName
        }

        override fun areContentsTheSame(
            oldItem: GaleryFragmentModel,
            newItem: GaleryFragmentModel
        ): Boolean {
            return oldItem == newItem
        }

    }
    val differ = AsyncListDiffer(this, DIFF_CALLBACK)
    private lateinit var selectedFile: GaleryFragmentModel
    private var state = false
    var lastClickPosition: Int? = null
    private var actionMode: Int = 0

    private var isSupportMultipleSelect = false
    init {
        setHasStableIds(true)

        viewModel.isSupportMultipleSelect.observeForever {
            isSupportMultipleSelect = it
        }
    }

    override fun getItemViewType(position: Int): Int {
        when(position){
            0 -> return 0
            else -> return 1
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val holder = if(viewType == 0){ GaleryViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_image_galery,
                parent,
                false
            ),
            interaction
        )}else{
            GaleryViewHolder(
                LayoutInflater.from(parent.context).inflate(
                    R.layout.item_image_galery,
                    parent,
                    false
                ),
                interaction
            )
        }

        return holder
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is GaleryViewHolder -> {
                Log.d("begin bind","galery holder")
                holder.bind(differ.currentList.get(position),context)
                holder.setVisibleSelecttedBadge(isSupportMultipleSelect)
                //set first file to show first
                if (!state) {
                    state = true
                    selectedFile = differ.currentList[0]
                    //lastClickPosition = 0
                    viewModel.selectedFile.value = differ.currentList[0]
                    viewModel.selectedPosition.value = 0
                    if (!isSupportMultipleSelect){
                        tracker?.clearSelection()
                    }
                }
                //enable multiple support
                holder.itemView.setOnLongClickListener {
                    if (differ.currentList[position].type_file != MainActivity.VIDEO_TYPE && !isSupportMultipleSelect ){
                        interaction?.onLongClickItemView()
                    }
                    true
                }

                holder.setupTracker(position,context!!,isSupportMultipleSelect)


            }
        }
    }

    override fun getItemCount(): Int {
        return differ.currentList.size
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    fun submitList(list: List<GaleryFragmentModel>) {
        differ.submitList(list)
    }

    fun setSelectionTracker(selectionTracker: SelectionTracker<Long>){
        this.tracker = selectionTracker
    }



    fun setActionMode(actionMode: Int) {
        this.actionMode = actionMode
        notifyDataSetChanged()
    }
    fun setVisibleSelecttedBadge(isvisible: Boolean,holder: GaleryViewHolder){
        holder.setVisibleSelecttedBadge(isvisible)
    }

    inner class GaleryViewHolder
    constructor(
        itemView: View,
        private val interaction: Interaction?
    ) : RecyclerView.ViewHolder(itemView) {
        internal var imageView: ImageView = itemView.findViewById(R.id.imagedisplay)
        internal var view: View = itemView.findViewById(R.id.view_image_galery)
        internal var durationtxt : TextView = itemView.findViewById(R.id.duration)
        internal var imgBtnPlay : ImageView = itemView.findViewById(R.id.ic_play_button)
        internal var textview_selected: TextView = itemView.findViewById(R.id.badge_selected)
        // internal var textview_not_selected:TextView  = itemView.findViewById(R.id.badge_not_selected)
        internal var details: Details = Details()


        fun bind(item: GaleryFragmentModel,context: Context?) = with(itemView){
            itemView.setOnClickListener {
                interaction?.onItemSelected(adapterPosition, item)
            }

            context?.let { Glide.with(it).load(item.fileName).into(imageView) }

            if (differ.currentList[position].type_file == MainActivity.VIDEO_TYPE){
                durationtxt.text = convertMilsToSecMinuteFromVideo(item.fileName?:"")
                visibleDurationtxt()
            }else{
                unvisibleDurationtxt()
            }
        }

        fun unvisibleDurationtxt(){
            durationtxt.visibility = View.GONE
        }
        fun visibleDurationtxt(){
            durationtxt.visibility = View.VISIBLE
        }

        fun setvisible_viewImage(isvisible: Boolean) {
            if (isvisible) {
                view.setBackgroundColor(Color.parseColor("#CCFFFFFF"))
            } else { //transparent
                view.setBackgroundColor(Color.parseColor("#00FFFFFF"))
            }
        }
        fun getItemDetails(): Details {
            return details
        }

        fun setVisibleSelecttedBadge(isvisible: Boolean){
            if (isvisible){
                textview_selected.visibility = View.VISIBLE
            }else{
                textview_selected.visibility = View.GONE
            }
        }

        fun setupTracker(position: Int,context: Context,isSupporMultipeSelect:Boolean){
            Log.d("setup tracker","show")
            details.position = position.toLong()
            if (isSupporMultipeSelect && tracker?.selection?.size() ?: 0 <= 8){
                if (tracker != null){
                    tracker?.let {
                        if (!it.selection.isEmpty){
                            if (it.isSelected(details.selectionKey) ) {
                                //tell data to fragment
                                //get item selection and show in fragment
                                getSelectItemAndShowInGalery(position)

                                //for selected item show Badge
                                Log.d("selection adapter size ","it.selection.size()")
                                textview_selected.visibility = View.VISIBLE
                                textview_selected.background =
                                    ActivityCompat.getDrawable(context, R.drawable.badge_background_green)

                                //badge for not select
                                itemView.rootView.isActivated = true

                            } else {/*for not selected items*/
                                textview_selected.visibility = View.VISIBLE
                                textview_selected.background =
                                    ActivityCompat.getDrawable(context, R.drawable.badge_background_softrransparent)

                                //badge for select
                                itemView.rootView.isActivated = false
                            }

                        }else{
                            //tell data to fragment
                            //get item selection and show in fragment
                            getSelectItemAndShowInGalery(position)

                            textview_selected.visibility = View.VISIBLE
                            textview_selected.background =
                                ActivityCompat.getDrawable(context, R.drawable.badge_background_softrransparent)
                        }
                    }
                }
            }else{
                if (tracker != null){
                    tracker?.let {
                        if (!it.selection.isEmpty) {
                            if (it.isSelected(details.selectionKey)) {
                                getSelectItemAndShowInGalery(position)

                            }
                        }

                    }
                }
            }
        }

        fun getSelectItemAndShowInGalery(position: Int) {
            Log.d("click at ","$position")
            lastClickPosition = position
            state = true

            if (!isSupportMultipleSelect) {
                Handler().post {
                    tracker?.clearSelection() //clear all selected when single select photos

                    selectedFile = differ.currentList[position]
                    viewModel.selectedFile.value = differ.currentList[position]
                    viewModel.selectedPosition.value = position

                    if (selectedFile == differ.currentList[position]) {
                        setvisible_viewImage(true)
                    } else {
                        setvisible_viewImage(false)
                    }

                    notifyItemChanged(position)
                }

            } else {
                tracker?.selection?.size()?.let {
                    if (it > 8) {
                        Handler().post {
                            //deselect click after more than max photo
                            lastClickPosition?.let {
                                tracker?.deselect(it.toLong())
                            }
                        }
                    } else {
                        //just show selected photos when selected size <= max photo
                        viewModel.selectedPosition.value = position
                        selectedFile = differ.currentList[position]
                        viewModel.selectedFile.value = differ.currentList[position]
                        interaction?.onClickItemInMultipleSupport()
                    }
                }
            }

        }


        /** function get duration from local file
         * @param path uri or Path of local video file
         * @return String of duration
         */
        private fun getDurationFromVideo(path: String): String {
            var durationstring:String?=null
            var r = MediaMetadataRetriever()
            if (!path.isEmpty() || path.equals("")) {
                r.setDataSource(path)
                durationstring = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            }
            return durationstring?:""
        }

        /** convert mili secont to duration format from path video file
         * @param path video uri
         * @return duration of vidfeo in format $minute : $seconds
         */
        fun convertMilsToSecMinuteFromVideo(path: String):String{
            val milliseconds = getDurationFromVideo(path)
            val minutes = milliseconds.toLong() / 1000 / 60
            val seconds = milliseconds.toLong() / 1000 % 60
            val secondsStr = java.lang.Long.toString(seconds)
            val secs: String
            if (secondsStr.length >= 2) {
                secs = secondsStr.substring(0, 2)
            } else {
                secs = "0$secondsStr"
            }


            return "$minutes : $secs"
        }

    }

    class DetailsLookUp(private val recycleview: RecyclerView,private val tracker: SelectionTracker<Long>?) : ItemDetailsLookup<Long>() {
        override fun getItemDetails(e: MotionEvent): ItemDetails<Long>? {
            val view = recycleview.findChildViewUnder(e.x, e.y)
            if (view != null) {
                return (recycleview.getChildViewHolder(view) as GaleryViewHolder).getItemDetails()
            }
            return null
        }

    }
    interface Interaction {
        fun onItemSelected(position: Int, item: GaleryFragmentModel)
        fun onLongClickItemView()
        fun onClickItemInMultipleSupport()
    }
}