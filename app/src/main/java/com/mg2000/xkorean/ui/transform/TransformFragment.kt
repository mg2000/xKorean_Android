package com.mg2000.xkorean.ui.transform

import android.content.Context
import android.graphics.*
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.NetworkResponse
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.google.gson.Gson
import com.mg2000.xkorean.R
import com.mg2000.xkorean.databinding.FragmentTransformBinding
import com.mg2000.xkorean.databinding.ItemTransformBinding
import org.json.JSONArray
import java.util.zip.GZIPInputStream
import androidx.recyclerview.widget.GridLayoutManager
import android.util.DisplayMetrics
import android.view.*
import android.widget.Toast
import androidx.core.view.MenuCompat
import androidx.preference.PreferenceManager
import com.android.volley.toolbox.*
import com.mg2000.xkorean.MainActivity
import com.techiness.progressdialoglibrary.ProgressDialog
import org.joda.time.format.ISODateTimeFormat
import java.io.*

/**
 * Fragment that demonstrates a responsive layout pattern where the format of the content
 * transforms depending on the size of the screen. Specifically this Fragment shows items in
 * the [RecyclerView] using LinearLayoutManager in a small screen
 * and shows items using GridLayoutManager in a large screen.
 */
class TransformFragment : Fragment() {

    private lateinit var transformViewModel: TransformViewModel
    private var _binding: FragmentTransformBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private lateinit var mRequestQueue: RequestQueue

    private lateinit var mOneTitleHeader: Bitmap
    private lateinit var mSeriesTitleHeader: Bitmap
    private lateinit var mPlayAnywhereTitleHeader: Bitmap
    private lateinit var mPlayAnywhereSeriesTitleHeader: Bitmap
    private lateinit var mWindowsTitleHeader: Bitmap

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        fun loadJSONArray(jsonList: JSONArray) {
            val gson = Gson()

            val gameList = mutableListOf<Game>()
            for (i in 0 until jsonList.length()) {
                gameList.add(gson.fromJson<Game>(jsonList.getString(i), Game::class.java))
            }

            transformViewModel.update(gameList)
            (requireActivity() as MainActivity).setTitle("한국어 지원 타이틀")
        }

        fun loadCacheList() {
            val dataFolder = requireContext().getDir("xKorean", Context.MODE_PRIVATE)
            val dataFile = File(dataFolder, "games.json")
            loadJSONArray(JSONArray(dataFile.readText()))
        }

        transformViewModel = ViewModelProvider(this).get(TransformViewModel::class.java)
        _binding = FragmentTransformBinding.inflate(inflater, container, false)
        val root: View = binding.root

        var inputStream = requireContext().assets.open("xbox_one_title.png")
        var size = inputStream.available()
        var buffer = ByteArray(size)
        inputStream.read(buffer)

        mOneTitleHeader = BitmapFactory.decodeByteArray(buffer, 0, buffer.size)

        inputStream.close()

        inputStream = requireContext().assets.open("xbox_series_xs_title.png")
        size = inputStream.available()
        buffer = ByteArray(size)
        inputStream.read(buffer)

        mSeriesTitleHeader = BitmapFactory.decodeByteArray(buffer, 0, buffer.size)

        inputStream.close()

        inputStream = requireContext().assets.open("xbox_playanywhere_title.png")
        size = inputStream.available()
        buffer = ByteArray(size)
        inputStream.read(buffer)

        mPlayAnywhereTitleHeader = BitmapFactory.decodeByteArray(buffer, 0, buffer.size)

        inputStream.close()

        inputStream = requireContext().assets.open("xbox_playanywhere_xs_title.png")
        size = inputStream.available()
        buffer = ByteArray(size)
        inputStream.read(buffer)

        mPlayAnywhereSeriesTitleHeader = BitmapFactory.decodeByteArray(buffer, 0, buffer.size)

        inputStream.close()

        inputStream = requireContext().assets.open("windows_title.png")
        size = inputStream.available()
        buffer = ByteArray(size)
        inputStream.read(buffer)

        mWindowsTitleHeader = BitmapFactory.decodeByteArray(buffer, 0, buffer.size)

        inputStream.close()

        root.viewTreeObserver.addOnGlobalLayoutListener {
            fun convertDPToPixels(dp: Int) : Float {
                val metrics = DisplayMetrics()
                requireActivity().windowManager.defaultDisplay.getMetrics(metrics)
                val logicalDensity = metrics.density
                return dp * logicalDensity
            }

            val columnWidth = 190
            val spanCount = Math.floor(root.getWidth() / convertDPToPixels(columnWidth).toDouble()).toInt()
            ((root as RecyclerView).getLayoutManager() as GridLayoutManager).spanCount = spanCount
        }

        val recyclerView = binding.recyclerviewTransform
        val adapter = TransformAdapter()
        recyclerView.adapter = adapter
        transformViewModel.games.observe(viewLifecycleOwner, {
            adapter.submitList(it)
            adapter.notifyDataSetChanged()
        })

        val dataFolder = requireContext().getDir("xKorean", Context.MODE_PRIVATE)
        val cacheFolder = File(dataFolder, "cache")
        if (!cacheFolder.exists())
            cacheFolder.mkdir()

        mRequestQueue = Volley.newRequestQueue(requireContext())

//        val progressDialog = ProgressDialog(requireContext()).apply {
//            theme = ProgressDialog.THEME_LIGHT
//            mode = ProgressDialog.MODE_INDETERMINATE
//            //setMessage("한국어 지원 타이틀 확인중...")
//            //setCancelable(false)
//        }
        val progressDialog = ProgressDialog(requireContext())
        progressDialog.show()

        val updateTimeRequest = JsonObjectRequest(Request.Method.POST, "http://192.168.2.37:3000/last_modified_time", null, { updateInfo ->
            val preferenceManager = PreferenceManager.getDefaultSharedPreferences(requireContext())
            val lastModifiedTime = preferenceManager.getString("lastModifiedTime", "")
            if (lastModifiedTime == "" || lastModifiedTime != updateInfo.getString("lastModifiedTime")) {
                val request = BinaryArrayRequest("http://192.168.2.37:3000/title_list_zip", {
                    val gzipInputStream = GZIPInputStream(ByteArrayInputStream(it))
                    val outputStream = ByteArrayOutputStream()

                    val dataBuffer = ByteArray(32768)
                    var len = gzipInputStream.read(dataBuffer)
                    while (len > 0) {
                        outputStream.write(dataBuffer, 0, len)
                        len = gzipInputStream.read(dataBuffer)
                    }
                    gzipInputStream.close()

                    val str = outputStream.toString("utf-8")
                    outputStream.close()

                    val newDataFile = File(dataFolder, "games.json")
                    newDataFile.writeText(str)

                    val jsonList = JSONArray(str)

                    loadJSONArray(jsonList)

//            adapter.updateData(gameList)
//            adapter.notifyDataSetChanged()
                    preferenceManager.edit().putString("lastModifiedTime", updateInfo.getString("lastModifiedTime")).apply()

                    progressDialog.dismiss()
                    println("성공")
                }, {
                    loadCacheList()
                    progressDialog.dismiss()
                    Toast.makeText(requireContext(), "한국어 타이틀 정보를 확인할 수 없어서, 기존 데이터를 보여줍니다.", Toast.LENGTH_SHORT).show()
                })
                request.tag = "update"
                mRequestQueue.add(request)
            }
            else {
                loadCacheList()
                progressDialog.dismiss()
            }
        }, {
            loadCacheList()
            progressDialog.dismiss()
            Toast.makeText(requireContext(), "서버 정보를 확인할 수 없어서, 기존 데이터를 보여줍니다.", Toast.LENGTH_SHORT).show()
        })
        updateTimeRequest.tag = "update"

        mRequestQueue.add(updateTimeRequest)

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        val result = super.onCreateOptionsMenu(menu, inflater)

        inflater.inflate(R.menu.overflow, menu)

        MenuCompat.setGroupDividerEnabled(menu, true)

        return result
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {

        }

        return super.onOptionsItemSelected(item)
    }

    private fun updateList(gameList: List<Game>) {

    }

    inner class TransformAdapter :
        ListAdapter<Game, TransformViewHolder>(object : DiffUtil.ItemCallback<Game>() {

            override fun areItemsTheSame(oldItem: Game, newItem: Game): Boolean =
                oldItem == newItem

            override fun areContentsTheSame(oldItem: Game, newItem: Game): Boolean =
                oldItem == newItem
        }) {

        //private val mItems = mutableListOf<Game>()

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransformViewHolder {
            val binding = ItemTransformBinding.inflate(LayoutInflater.from(parent.context))
            return TransformViewHolder(binding)
        }

        override fun onBindViewHolder(holder: TransformViewHolder, position: Int) {
            val game = getItem(position)
            holder.textView.text = game.koreanName

            val localize = game.localize.replace("/", "\n")
            holder.localizeTextView.text = localize

            when {
                localize.indexOf("음성") >= 0 -> holder.localizeTextView.setBackgroundColor(Color.argb(0xCC, 0x44, 0x85, 0x0E))
                localize.indexOf("자막") >= 0 -> holder.localizeTextView.setBackgroundColor(Color.argb(0xCC, 0xA6, 0x88, 0x19))
                else -> holder.localizeTextView.setBackgroundColor(Color.argb(0xCC, 0x95, 0x0D, 0x00))
            }

            var gamePassTag = ""
            if (game.gamePassPC != "" || game.gamePassConsole != "" || game.gamePassCloud != "")
                gamePassTag = "게임패스"
            else if (game.bundle.isNotEmpty()) {
                for (bundle in game.bundle) {
                    if (bundle.gamePassPC != "" || bundle.gamePassConsole != "" || bundle.gamePassCloud != "") {
                        gamePassTag = "게임패스"
                        break
                    }
                }
            }

            if (game.gamePassNew == "O")
                gamePassTag += " 신규"
            else if (game.gamePassEnd == "O")
                gamePassTag += " 만기"
            else {
                for (bundle in game.bundle)
                {
                    if (bundle.gamePassNew != "")
                    {
                        gamePassTag += " 신규"
                        break
                    }
                    else if (game.gamePassEnd != "")
                    {
                        gamePassTag += " 만기"
                        break
                    }
                }
            }

            holder.gamePassTextView.text = gamePassTag
            if (gamePassTag == "")
                holder.gamePassBackTextView.visibility = View.INVISIBLE
            else
                holder.gamePassBackTextView.visibility = View.VISIBLE

            if (game.gamePassPC != "")
                holder.gamePassPCTextView.text = "피"
            else
                holder.gamePassPCTextView.text = ""

            if (game.gamePassConsole != "")
                holder.gamePassConsoleTextView.text = "엑"
            else
                holder.gamePassConsoleTextView.text = ""

            if (game.gamePassCloud != "")
                holder.gamePassCloudTextView.text = "클"
            else
                holder.gamePassCloudTextView.text = ""

            var discount = game.discount

            if (!game.isAvailable() && game.bundle.size == 1)
                discount = game.bundle[0].discountType;
            else if (game.bundle.isNotEmpty())
            {
                for (bundle in game.bundle)
                {
                    if (bundle.discountType.indexOf("할인") >= 0)
                    {
                        discount = "에디션 할인";
                        break;
                    }
                    else if (discount == "판매 중지" && bundle.discountType != "판매 중지")
                        discount = "";
                }

                if (!game.isAvailable() && discount == "")
                    discount = game.bundle[0].discountType;
            }

            if (discount == "곧 출시") {
                val parser = ISODateTimeFormat.dateTime()
                val releaseDate = parser.parseDateTime(game.releaseDate)

                discount = releaseDate.toString("MM월 dd일 HH시 출시")
            }

            if (discount != "") {
                holder.messageTextView.text = discount
                holder.messageTextView.visibility = View.VISIBLE
            }
            else
                holder.messageTextView.visibility = View.INVISIBLE

            holder.imageView.setImageBitmap(null)

            val dataFolder = requireContext().getDir("xKorean", Context.MODE_PRIVATE)
            val cacheFolder = File(dataFolder, "cache")

            val cacheName = StringBuilder(game.id)
            when {
                game.playAnywhere == "O" -> {
                    if (game.seriesXS == "O")
                        cacheName.append("_playanywhere_xs")
                    else
                        cacheName.append("_playanywhere_os")
                }
                game.seriesXS == "O" -> cacheName.append("_xs")
                game.oneS == "O" -> cacheName.append("_os")
                game.pc == "O" -> cacheName.append("_pc")
            }
            cacheName.append(".jpg")

            val cacheFile = File(cacheFolder, cacheName.toString())
            if (cacheFile.exists())
                holder.imageView.setImageBitmap(BitmapFactory.decodeFile(cacheFile.absolutePath))
            else {
                val request = ImageRequest(game.thumbnail, {
                    val titleImage = Bitmap.createBitmap(584, 800, Bitmap.Config.ARGB_8888)

                    var header: Bitmap? = null
                    when {
                        game.playAnywhere == "O" -> {
                            header = if (game.seriesXS == "O")
                                mPlayAnywhereSeriesTitleHeader
                            else
                                mPlayAnywhereTitleHeader
                        }
                        game.seriesXS == "O" -> header = mSeriesTitleHeader
                        game.oneS == "O" -> header = mOneTitleHeader
                        game.pc == "O" -> header = mWindowsTitleHeader
                    }

                    val oriImage = Bitmap.createScaledBitmap(it, 584, 800, true)
                    val c = Canvas(titleImage)
                    c.drawBitmap(oriImage, 0f, 0f, Paint(Paint.FILTER_BITMAP_FLAG))
                    if (header != null)
                        c.drawBitmap(header, 0f, 0f, Paint(Paint.FILTER_BITMAP_FLAG))

                    FileOutputStream(cacheFile).use { fos ->
                        titleImage.compress(Bitmap.CompressFormat.JPEG, 75, fos)
                    }

                    holder.imageView.setImageBitmap(titleImage)
                }, 0, 0, ImageView.ScaleType.FIT_XY, null, {
                    println("이미지 다운로드 에러")
                })

                mRequestQueue.add(request)
            }
        }

//        fun updateData(updateList: List<Game>) {
//            mItems.addAll(updateList)
//        }
    }

    class TransformViewHolder(binding: ItemTransformBinding) :
        RecyclerView.ViewHolder(binding.root) {

        val imageView: ImageView = binding.imageViewItemTransform
        val textView: TextView = binding.textViewItemTransform
        val localizeTextView: TextView = binding.textViewLocalize
        val gamePassBackTextView: TextView = binding.textViewGamePassBack
        val gamePassTextView: TextView = binding.textViewGamePass
        val gamePassPCTextView: TextView = binding.textViewGamePassPc
        val gamePassConsoleTextView: TextView = binding.textViewGamePassConsole
        val gamePassCloudTextView: TextView = binding.textViewGamePassCloud
        val messageTextView: TextView = binding.textViewMessage
    }

    private class BinaryArrayRequest(url: String, listener: Response.Listener<ByteArray>, errorListener: Response.ErrorListener) : Request<ByteArray>(Method.POST, url, errorListener) {
        private val mListener = listener

        override fun parseNetworkResponse(response: NetworkResponse?): Response<ByteArray> {
            return Response.success(response!!.data, HttpHeaderParser.parseCacheHeaders(response))
        }

        override fun deliverResponse(response: ByteArray?) {
            mListener.onResponse(response)
        }
    }
}