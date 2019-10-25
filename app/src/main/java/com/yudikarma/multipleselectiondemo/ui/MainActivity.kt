package com.yudikarma.multipleselectiondemo.ui

import android.Manifest
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.provider.MediaStore
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProviders
import androidx.loader.content.CursorLoader
import androidx.recyclerview.selection.Selection
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.selection.StorageStrategy
import com.company107.zonapets.utils.mulitple_select_recycleview.KeyProviders
import com.company107.zonapets.utils.mulitple_select_recycleview.Predicates
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import com.yudikarma.multipleselectiondemo.R
import com.yudikarma.multipleselectiondemo.model.EventBusModel
import com.yudikarma.multipleselectiondemo.model.GaleryFragmentModel
import com.yudikarma.multipleselectiondemo.utils.NpaGridLayoutManager
import kotlinx.android.synthetic.main.activity_main.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.jetbrains.anko.toast
import permissions.dispatcher.*
import java.util.*

@RuntimePermissions
class MainActivity : AppCompatActivity(), Player.EventListener, MainRvAdapter.Interaction {

    companion object {
        /**
         * DEFAULT Value from system Media Store for type image is 1
        ! please dont change this value
         */
        //default by media store for images
        const val IMAGE_TYPE = 1 //dont change please

        /**
         * DEFAULT Value from system Media Store for type Video is 3
        ! please dont change this value
         */
        //default by media store for videos
        const val VIDEO_TYPE = 3 //dont change please
    }

    private lateinit var viewModel: MainViewModel
    private var images: ArrayList<GaleryFragmentModel> = arrayListOf()
    lateinit var adapter: MainRvAdapter
    private lateinit var selected_file: GaleryFragmentModel
    private var selected_filePosition: Int? = null
    private var old_selected_filePosition: Int? = null
    private var bus: EventBus = EventBus.getDefault()
    private var MultipleSupportState = true //for multiple select photos
    private var listFileSelect: ArrayList<GaleryFragmentModel> = arrayListOf()
    private var listSizeNow = 0
    private var tracker: SelectionTracker<Long>? = null
    var galeryFragmentModel: GaleryFragmentModel? = null

    private var player: SimpleExoPlayer? = null
    internal var mHandler: Handler? = null
    internal var mRunnable: Runnable? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupViewModel()

        //setup adapter
        setupAdapterWithPermissionCheck()



        icon_multiple_select.setImageResource(R.drawable.ic_multipleselection_notselect)
        container_icon_multiple_select.background =
            resources.getDrawable(R.drawable.circle_shape_grey)
        icon_multiple_select.setOnClickListener {
            actionOnSupportMultipleClick()
        }


    }


    fun actionOnSupportMultipleClick() {
        //restart bindadapter
        adapter.setActionMode(0)
        viewModel.selectedFile.value = selected_file
        if (MultipleSupportState) {
            actionIsSupportMultiple()
        } else {
            actionIsNotSupportMultiple()
        }
    }

    private fun actionIsNotSupportMultiple() {

        setupAdapterImagesVideo()

        //tell activity for disable swipe to camera
        bus.post(EventBusModel("ActionModeSupportMultiple"))
        viewModel.isSupportMultipleSelect.value = MultipleSupportState
        badge_selected.visibility = View.GONE

        val holder =
            recycleview_galery_fragment.findViewHolderForAdapterPosition(selected_filePosition ?: 0)
        if (holder is MainRvAdapter.GaleryViewHolder) {
            adapter.setVisibleSelecttedBadge(MultipleSupportState, holder)
        }

        icon_multiple_select.setImageResource(R.drawable.ic_multipleselection_notselect)
        container_icon_multiple_select.background =
            resources.getDrawable(R.drawable.circle_shape_grey)


        tracker?.let {
            it.clearSelection()
            adapter.tracker?.clearSelection()
            if (selected_filePosition != 0) {
                it.deselect(0)
                adapter.tracker?.deselect(0)
            }
            //adapter.clearTrackker((adapter.ViewHolder(view)),selected_filePosition?.toInt()?:0,MultipleSupportState)
        }
        MultipleSupportState = true
    }

    private fun actionIsSupportMultiple() {

        setupAdapterOnlyImages()

        //tell activity for disable swipe to camera
        bus.post(EventBusModel("ActionModeNotSupportMultiple"))

        viewModel.isSupportMultipleSelect.value = MultipleSupportState
        badge_selected.visibility = View.VISIBLE
        badge_selected.setText("1")
        val holder =
            recycleview_galery_fragment.findViewHolderForAdapterPosition(selected_filePosition ?: 0)
        if (holder is MainRvAdapter.GaleryViewHolder) {
            adapter.setVisibleSelecttedBadge(MultipleSupportState, holder)
        }
        imageview_insert_post.isEnabled = false
        imageview_insert_post.setImageFilePath(selected_file.fileName)

        //adapter.clearTrackker((adapter.ViewHolder(view)),selected_filePosition?.toInt()?:0,MultipleSupportState)
        tracker?.let {
            it.clearSelection()
            adapter.tracker?.clearSelection()
            adapter.tracker?.select(selected_filePosition?.toLong() ?: 0)
            it.select(selected_filePosition?.toLong() ?: 0)
            if (selected_filePosition != 0) {
                it.deselect(0)
                adapter.tracker?.deselect(0)
            }
        }
        //clear first list crop info
        // listCropInfo.clear()

        icon_multiple_select.setImageResource(R.drawable.ic_multipleselection_notselect)
        container_icon_multiple_select.background = resources.getDrawable(R.drawable.circle_shape)

        MultipleSupportState = false
    }


    override fun onStart() {
        super.onStart()
        bus.register(this)
    }

    override fun onPause() {
        super.onPause()
        bus.unregister(this)
        pausePlayer()
        if (mRunnable != null) {
            mHandler?.removeCallbacks(mRunnable)
        }
    }

    override fun onResume() {
        super.onResume()
        resumePlayer()

    }

    override fun onDestroy() {
        super.onDestroy()
        releasePlayer()
    }

    fun isShowVideo() {
        videoFullScreenPlayer?.visibility = View.VISIBLE
        imageview_insert_post?.visibility = View.GONE
        icon_multiple_select?.visibility = View.GONE
        container_icon_multiple_select?.visibility = View.GONE
    }

    fun isHowImage() {
        videoFullScreenPlayer?.visibility = View.GONE
        spinnerVideoDetails.visibility = View.GONE
        imageview_insert_post?.visibility = View.VISIBLE
        icon_multiple_select.visibility = View.VISIBLE
        container_icon_multiple_select?.visibility = View.VISIBLE

    }

    //subcribe detect news foto
    @Subscribe
    fun onMessageEvent(eventBusModel: EventBusModel) {

        if (eventBusModel.response.equals("disableMultiplePhotos")) {
            actionOnSupportMultipleClick()
        }
    }

    @NeedsPermission(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    fun setupAdapter() {
        adapter = MainRvAdapter(this, this, viewModel, tracker)
        adapter.submitList(getImageListWithVideo())
        recycleview_galery_fragment.layoutManager = NpaGridLayoutManager(this, 4)
        recycleview_galery_fragment.setHasFixedSize(false)
        recycleview_galery_fragment.adapter = adapter


        //for getracker selected item
        getTrackerItemSelect()

    }

    @OnShowRationale(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )
    fun onShowRationaleDialog(request: PermissionRequest) {
        toast("acces storage needed for show image gallery")
    }

    @OnPermissionDenied(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )
    fun onPermissionCameraDenied() {
        toast("Permission Denied, App failed to launch")
        finish()
    }

    @OnNeverAskAgain(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )
    fun onPermissionCameraNeverAskAgain() {
        toast("Permission Denied never Ask Again, App failed to launch")
        finish()
    }

    private fun setupAdapterOnlyImages() {
        images.clear()
        callSettImageList()

        recycleview_galery_fragment.invalidate()
        adapter.submitList(images)

    }

    private fun setupAdapterImagesVideo() {
        images.clear()
        callSettImageListwithVideo()

        recycleview_galery_fragment.invalidate()
        adapter.submitList(images)

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        onRequestPermissionsResult(requestCode, grantResults)
    }


    fun getTrackerItemSelect() {
        tracker = SelectionTracker.Builder<Long>(
            "mySelection",
            recycleview_galery_fragment,
            KeyProviders(recycleview_galery_fragment),
            MainRvAdapter.DetailsLookUp(recycleview_galery_fragment, tracker),
            StorageStrategy.createLongStorage()
        ).withSelectionPredicate(
            Predicates()
        ).build()

        tracker?.let {
            adapter.setSelectionTracker(it)
        }

        var firstObserver = false
        tracker?.addObserver(object : SelectionTracker.SelectionObserver<Long>() {
            override fun onSelectionChanged() {
                super.onSelectionChanged()
                if (!MultipleSupportState && tracker?.selection?.size() ?: 0 <= 8) {
                    val itemposition: Selection<Long>? = tracker?.selection

                    if (!(itemposition?.size() != 0 && itemposition!!.size() <= (8 - listSizeNow))) {
                        //context.toast("max Upload is ${AppConstants.MAXPhoto.toString()}")
                    }
                    if (listFileSelect.size >= 0) {
                        listFileSelect.clear()
                    }
                    val list =
                        itemposition?.map { adapter.differ.currentList[it.toInt()] }?.toList()

                    if (list.size == 0 && firstObserver) {
                        badge_selected.setText("1")
                        firstObserver = false
                    } else {
                        badge_selected.setText("${list.size}")
                    }

                    if (list.size != 0) {
                        listFileSelect = ArrayList(list)

                    }
                }

            }

        })
    }

    private fun setupViewModel() {
        viewModel = ViewModelProviders.of(this).get(MainViewModel::class.java)

        viewModel.selectedFile.observe(this, androidx.lifecycle.Observer {
            selected_file = it

            //for image selected
            if (selected_file.type_file == IMAGE_TYPE) {

                isHowImage()
                pausePlayer()

                imageview_insert_post.setImageFilePath(selected_file.fileName)

                imageview_insert_post.setAspectRatio(1, 1)


            } else { //for video selected

                isShowVideo()

                buildMediaSource(Uri.parse(selected_file.fileName/*"https://androidwave.com/media/androidwave-video-5.mp4"*/))

            }

        })

        viewModel.selectedPosition.observe(this, androidx.lifecycle.Observer {
            if (selected_filePosition == null) {
                old_selected_filePosition = 0
            } else {
                old_selected_filePosition = selected_filePosition
            }
            selected_filePosition = it
        })

        viewModel.isSupportMultipleSelect.observe(this, androidx.lifecycle.Observer {

        })

    }

    fun callSettImageListwithVideo() {
        images = getImageListWithVideo()
    }

    fun callSettImageList() {
        images = getImagelist()
    }


    /**
     * Funciton for get Only Image
     */
    fun getImagelist(): ArrayList<GaleryFragmentModel> {
        var dataLocal = ArrayList<GaleryFragmentModel>()
        var absolutePathOfFile: String? = null
        val projection = arrayOf(
            MediaStore.Files.FileColumns.DATA,
            MediaStore.Files.FileColumns.DATE_ADDED,
            MediaStore.Files.FileColumns.MEDIA_TYPE
        )

        val selection: String = (MediaStore.Files.FileColumns.MEDIA_TYPE + "="
                + MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE
                /*+ " OR "
                + MediaStore.Files.FileColumns.MEDIA_TYPE + "="
                + MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO*/)

        val queryUri = MediaStore.Files.getContentUri("external")

        val cursorLoader = CursorLoader(
            this,
            queryUri,
            projection,
            selection,
            null,
            MediaStore.Files.FileColumns.DATE_ADDED + " DESC"
        )

        val cursor = cursorLoader.loadInBackground()


        cursor?.let {
            while (it.moveToNext()) {
                val dataColumnIndex = it.getColumnIndex(MediaStore.Files.FileColumns.DATA)
                val typeMedia = it.getColumnIndex(MediaStore.Files.FileColumns.MEDIA_TYPE)

                absolutePathOfFile = it.getString(dataColumnIndex)
                val mediaType = it.getInt(typeMedia)/*1 == image , 3 == video*/

                absolutePathOfFile?.let { pathfile ->
                    galeryFragmentModel =
                        GaleryFragmentModel(fileName = pathfile, type_file = mediaType)
                    galeryFragmentModel?.let {
                        dataLocal.add(it)
                    }
                }
            }
        }

        cursor?.close()

        return dataLocal
    }


    /**
     * function for get Image & Video
     */
    fun getImageListWithVideo(): ArrayList<GaleryFragmentModel> {
        var dataLocal = ArrayList<GaleryFragmentModel>()
        var absolutePathOfFile: String? = null
        val projection = arrayOf(
            MediaStore.Files.FileColumns.DATA,
            MediaStore.Files.FileColumns.DATE_ADDED,
            MediaStore.Files.FileColumns.MEDIA_TYPE
        )

        val selection: String = (MediaStore.Files.FileColumns.MEDIA_TYPE + "="
                + MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE
                + " OR "
                + MediaStore.Files.FileColumns.MEDIA_TYPE + "="
                + MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO)

        val queryUri = MediaStore.Files.getContentUri("external")

        val cursorLoader = CursorLoader(
            this,
            queryUri,
            projection,
            selection,
            null,
            MediaStore.Files.FileColumns.DATE_ADDED + " DESC"
        )

        val cursor = cursorLoader.loadInBackground()


        cursor?.let {
            while (it.moveToNext()) {
                val dataColumnIndex = it.getColumnIndex(MediaStore.Files.FileColumns.DATA)
                val typeMedia = it.getColumnIndex(MediaStore.Files.FileColumns.MEDIA_TYPE)

                absolutePathOfFile = it.getString(dataColumnIndex)
                val mediaType = it.getInt(typeMedia)/*1 == image , 3 == video*/

                absolutePathOfFile?.let { pathfile ->
                    galeryFragmentModel =
                        GaleryFragmentModel(fileName = pathfile, type_file = mediaType)
                    galeryFragmentModel?.let {
                        dataLocal.add(it)
                    }
                }
            }
        }

        cursor?.close()

        return dataLocal
    }

    private fun initExoPlayer() {
        if (player == null) {
            // 1. Create a default TrackSelector
            /*val loadControl = DefaultLoadControl(
                DefaultAllocator(true, 16),
                AppConstants.MIN_BUFFER_DURATION,
                AppConstants.MAX_BUFFER_DURATION,
                AppConstants.MIN_PLAYBACK_START_BUFFER,
                AppConstants.MIN_PLAYBACK_RESUME_BUFFER, -1, true
            )*/

            val bandwidthMeter = DefaultBandwidthMeter()
            val videoTrackSelectionFactory = AdaptiveTrackSelection.Factory(bandwidthMeter)
            val trackSelector = DefaultTrackSelector(videoTrackSelectionFactory)
            // 2. Create the player
            // player = ExoPlayerFactory.newSimpleInstance(context,DefaultRenderersFactory(context), trackSelector, loadControl)
            player = ExoPlayerFactory.newSimpleInstance(
                this,
                DefaultRenderersFactory(this), trackSelector
            )
            videoFullScreenPlayer?.setPlayer(player)
        }

    }

    private fun buildMediaSource(mUri: Uri) {
        initExoPlayer()

        if (mUri == null) {
            return
        }
        // Measures bandwidth during playback. Can be null if not required.
        val bandwidthMeter = DefaultBandwidthMeter()
        // Produces DataSource instances through which media data is loaded.
        val dataSourceFactory = DefaultDataSourceFactory(
            this,
            Util.getUserAgent(this, getString(R.string.app_name)), bandwidthMeter
        )
        // This is the MediaSource representing the media to be played.
        /*val videoSource = ExtractorMediaSource.Factory(dataSourceFactory)
            .createMediaSource(mUri)*/
        val videoSource =
            ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(mUri) as MediaSource
        // Prepare the player with the source.
        player?.prepare(videoSource)
        player?.playWhenReady = true
        player?.addListener(this)
    }

    private fun releasePlayer() {
        if (player != null) {
            player?.release()
            player = null
        }
    }

    private fun pausePlayer() {
        if (player != null) {
            player?.playWhenReady = false
            player?.playbackState
        }
    }

    private fun resumePlayer() {
        if (player != null) {
            player?.playWhenReady = true
            player?.playbackState
        }
    }

    override fun onTimelineChanged(timeline: Timeline?, manifest: Any?, reason: Int) {

    }

    override fun onTracksChanged(
        trackGroups: TrackGroupArray?,
        trackSelections: TrackSelectionArray?
    ) {

    }

    override fun onLoadingChanged(isLoading: Boolean) {

    }

    override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
        when (playbackState) {

            Player.STATE_BUFFERING -> spinnerVideoDetails.visibility = View.VISIBLE
            Player.STATE_ENDED -> {
            }
            Player.STATE_IDLE -> {
            }
            Player.STATE_READY -> spinnerVideoDetails.visibility = View.GONE
            else -> {
            }
        }// Activate the force enable
        // status = PlaybackStatus.IDLE;
    }

    override fun onRepeatModeChanged(repeatMode: Int) {

    }

    override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {

    }

    override fun onPlayerError(error: ExoPlaybackException?) {

    }

    override fun onPositionDiscontinuity(reason: Int) {

    }

    override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters?) {

    }

    override fun onSeekProcessed() {

    }


    override fun onItemSelected(position: Int, item: GaleryFragmentModel) {

    }

    override fun onLongClickItemView() {
        actionOnSupportMultipleClick()
    }

    override fun onClickItemInMultipleSupport() {

    }
}
