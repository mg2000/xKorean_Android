package com.sindohtechno.xkorean.ui.transform

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.NetworkResponse
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.HttpHeaderParser
import com.android.volley.toolbox.ImageRequest
import com.android.volley.toolbox.Volley
import com.google.gson.Gson
import com.sindohtechno.xkorean.R
import com.sindohtechno.xkorean.databinding.FragmentTransformBinding
import com.sindohtechno.xkorean.databinding.ItemTransformBinding
import org.json.JSONArray
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.zip.GZIPInputStream

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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        transformViewModel = ViewModelProvider(this).get(TransformViewModel::class.java)
        _binding = FragmentTransformBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val recyclerView = binding.recyclerviewTransform
        val adapter = TransformAdapter()
        recyclerView.adapter = adapter
        transformViewModel.games.observe(viewLifecycleOwner, {
            adapter.submitList(it)
            adapter.notifyDataSetChanged()
        })

        mRequestQueue = Volley.newRequestQueue(context)
        val request = BinaryArrayRequest("http://192.168.2.37:3000/title_list_zip", {
            val gzipInputStream = GZIPInputStream(ByteArrayInputStream(it))
            val outputStream = ByteArrayOutputStream()

            val buffer = ByteArray(32768)
            var len = gzipInputStream.read(buffer)
            while (len > 0) {
                outputStream.write(buffer, 0, len)
                len = gzipInputStream.read(buffer)
            }

            val str = outputStream.toString("utf-8")

            context?.let { context ->
                val dataFolder = context.getDir("xKorean", Context.MODE_PRIVATE)
                val newDataFile = File(dataFolder, "games.json")
                newDataFile.writeText(str)
            }

            val gson = Gson()
            val jsonList = JSONArray(str)

            val gameList = mutableListOf<Game>()
            for (i in 0 until jsonList.length()) {
                gameList.add(gson.fromJson<Game>(jsonList.getString(i), Game::class.java))
            }

//            adapter.updateData(gameList)
//            adapter.notifyDataSetChanged()
            transformViewModel.update(gameList)

            println("성공")
        }, {
            println("실패")
        })
        request.tag = "data"
        mRequestQueue.add(request)

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
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

            if (localize.indexOf("음성") >= 0)
                holder.localizeTextView.setBackgroundColor(Color.argb(0xCC, 0x44, 0x85, 0x0E))
            else if (localize.indexOf("자막") >= 0)
                holder.localizeTextView.setBackgroundColor(Color.argb(0xCC, 0xA6, 0x88, 0x19))
            else
                holder.localizeTextView.setBackgroundColor(Color.argb(0xCC, 0x95, 0x0D, 0x00))

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

            holder.imageView.setImageBitmap(null)

            val request = ImageRequest(game.thumbnail,{
                holder.imageView.setImageBitmap(it)
            }, 0, 0, ImageView.ScaleType.FIT_XY, null, {
                println("이미지 다운로드 에러")
            })

            mRequestQueue.add(request)
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