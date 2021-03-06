package com.mg2000.xkorean.ui.transform

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.view.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SwitchCompat
import androidx.core.view.MenuCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.NetworkResponse
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.HttpHeaderParser
import com.android.volley.toolbox.ImageRequest
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.c.progress_dialog.BlackProgressDialog
import com.google.gson.Gson
import com.mg2000.xkorean.IntentRepo
import com.mg2000.xkorean.MainActivity
import com.mg2000.xkorean.MainViewModel
import com.mg2000.xkorean.R
import com.mg2000.xkorean.databinding.FragmentTransformBinding
import com.mg2000.xkorean.databinding.ItemTransformBinding
import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat
import org.json.JSONArray
import org.json.JSONObject
import java.io.*
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.*
import java.util.zip.GZIPInputStream
import kotlin.math.floor

/**
 * Fragment that demonstrates a responsive layout pattern where the format of the content
 * transforms depending on the size of the screen. Specifically this Fragment shows items in
 * the [RecyclerView] using LinearLayoutManager in a small screen
 * and shows items using GridLayoutManager in a large screen.
 */
class TransformFragment : Fragment() {

	private lateinit var transformViewModel: TransformViewModel
	private lateinit var mainViewModel: MainViewModel
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

	private var mEditionDialog: AlertDialog? = null

	private val mHandler = Handler(Looper.getMainLooper())

	private val mFilterDeviceArr = booleanArrayOf(false, false, false, false, false, false, false)
	private val mFilterCapabilityArr = booleanArrayOf(false, false, false, false, false, false, false, false, false, false)
	private val mFilterCategoryArr = booleanArrayOf(false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false)
	private var mFilterKorean = 0
	private var mSort = 0
	private var mSortPriority = 0
	private var mLanguage = "Korean"
	private var mShowNewTitle = true
	private var mSearchKeyword = ""

	private var mShowRecommendTag = false
	private var mShowDiscount = true
	private var mShowGamepass = true
	private var mShowName = true
	private var mShowReleaseTime = false

	private val mNewTitleList = mutableListOf<String>()

	private val mThumbnailInfoMap = Collections.synchronizedMap(mutableMapOf<String, JSONObject>())

	private val mMessageTemplateMap = mapOf("dlregiononly" to "?????? ????????? ??????????????? ???????????? ????????? ???????????? ???????????????: [name]",
		"packageonly" to "????????? ????????? ???????????? ???????????????.",
		"usermode" to "??? ????????? ?????? ????????? ??????????????? ???????????? ???????????????.",
		"360market" to "360 ????????????????????? ???????????? ???????????? ??? ????????????.",
		"windowsmod" to "??? ????????? ??????????????? ?????? ????????? ??????????????? ???????????? ???????????????.")

	private val mKorChr = charArrayOf('???', '???', '???', '???', '???', '???', '???', '???', '???', '???', '???', '???', '???', '???', '???', '???', '???', '???', '???')
	private val mKorStr = arrayOf("???", "???", "???", "???", "???", "???", "???", "???", "???", "???", "???", "???", "???", "???", "???","???","???", "???", "???")
	private val mKorChrInt = intArrayOf(44032, 44620, 45208, 45796, 46384, 46972, 47560, 48148, 48736, 49324, 49912, 50500, 51088, 51676, 52264, 52852, 53440, 54028, 54616, 55204)

	private var mFullFeature = false
	private var mFullFeatureReady = 0

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setHasOptionsMenu(true)
	}

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View {
		transformViewModel = ViewModelProvider(this)[TransformViewModel::class.java]
		_binding = FragmentTransformBinding.inflate(inflater, container, false)
		val root: View = binding.root

		val preferenceManager = PreferenceManager.getDefaultSharedPreferences(requireContext())
		mFullFeature = preferenceManager.getBoolean("fullFeature", false)

		mainViewModel = ViewModelProvider(requireActivity(), MainViewModel.Factory(IntentRepo()))[MainViewModel::class.java]
		mainViewModel.intent.get.observe(viewLifecycleOwner) {
			mSearchKeyword = it
			updateList()
		}

		val preFilterDevice = JSONArray(preferenceManager.getString("filterDevice", "[ false, false, false, false, false, false, false ]"))

		for (i in 0 until preFilterDevice.length()) {
			mFilterDeviceArr[i] = preFilterDevice.getBoolean(i)
		}

		mSort = preferenceManager.getInt("sort", 0)
		mSortPriority = preferenceManager.getInt("sortPriority", 0)
		mLanguage = preferenceManager.getString("language", "Korean")!!
		mShowNewTitle = preferenceManager.getBoolean("showNewTitle", true)
		mShowDiscount = preferenceManager.getBoolean("showDiscount", true)
		mShowGamepass = preferenceManager.getBoolean("showGamepass", true)
		mShowName = preferenceManager.getBoolean("showName", true)
		mShowReleaseTime = preferenceManager.getBoolean("showReleaseTime", false)

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

			val columnWidth = 160

			println("?????? ?????? ${root.width} ${convertDPToPixels(columnWidth)}")

			val spanCount = floor(root.width / convertDPToPixels(columnWidth).toDouble()).toInt()
			((root as RecyclerView).layoutManager as GridLayoutManager).spanCount = spanCount
		}

		val recyclerView = binding.recyclerviewTransform
		val adapter = TransformAdapter()
		recyclerView.adapter = adapter
		transformViewModel.filteredGames.observe(viewLifecycleOwner) {
			adapter.submitList(it)
			adapter.notifyDataSetChanged()

			mHandler.postDelayed({
				recyclerView.scrollToPosition(0)
			}, 500)
		}

		val dataFolder = requireContext().getDir("xKorean", Context.MODE_PRIVATE)
		val cacheFolder = File(dataFolder, "cache")
		if (!cacheFolder.exists())
			cacheFolder.mkdir()

		mRequestQueue = Volley.newRequestQueue(requireContext())

//        val progressDialog = ProgressDialog(requireContext()).apply {
//            theme = ProgressDialog.THEME_LIGHT
//            mode = ProgressDialog.MODE_INDETERMINATE
//            //setMessage("????????? ?????? ????????? ?????????...")
//            //setCancelable(false)
//        }

		if (transformViewModel.gameList == null)
		{
			ThumbnailDBLoadTask {
				downloadData()
			}.execute()
		}

		return root
	}

	override fun onDestroyView() {
		super.onDestroyView()

		if (mEditionDialog != null) {
			mEditionDialog!!.dismiss()
		}

		_binding = null
	}

	override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
		val result = super.onCreateOptionsMenu(menu, inflater)

		inflater.inflate(R.menu.overflow, menu)

		val searchManager = requireContext().getSystemService(Context.SEARCH_SERVICE) as SearchManager
		(menu.findItem(R.id.search).actionView as SearchView).apply {
			setSearchableInfo(searchManager.getSearchableInfo(requireActivity().componentName))
		}
		(menu.findItem(R.id.search).actionView as SearchView).setOnCloseListener {
			mSearchKeyword = ""
			updateList()

			false
		}

		menu.findItem(R.id.donation).isVisible = mFullFeature

		MenuCompat.setGroupDividerEnabled(menu, true)

		if (mFilterDeviceArr.any { it })
			(menu.findItem(R.id.filter_device)).title = "?????? [??????]"
		else
			(menu.findItem(R.id.filter_device)).title = "??????"

		return result
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		when (item.itemId) {
			R.id.filter_device -> {
				val devices = arrayOf("Xbox Series X|S", "Xbox One X Enhanced", "Xbox One", "Xbox 360", "Original Xbox", "Windows", "Xbox Cloud Gaming")
				AlertDialog.Builder(requireContext())
					.setTitle("?????? ??????")
					.setMultiChoiceItems(devices, mFilterDeviceArr) { dialog, which, isChecked ->
						mFilterDeviceArr[which] = isChecked
					}
					.setPositiveButton("??????") { dialog, which ->
						val preferenceManager = PreferenceManager.getDefaultSharedPreferences(requireContext())
						val newFilterDevice = JSONArray()
						var mUseFilter = false
						mFilterDeviceArr.forEach {
							newFilterDevice.put(it)
							if (it && !mUseFilter)
								mUseFilter = true
						}
						preferenceManager.edit().putString("filterDevice", newFilterDevice.toString()).apply()
						updateList()

						if (mUseFilter)
							item.title = "?????? [??????]"
						else
							item.title = "??????"
					}
					.show()
			}
			R.id.filter_capability -> {
				val capabilities = arrayOf("????????????", "??????", "????????? ????????????", "?????? ????????????", "?????????/?????????", "?????? ??????", "????????? ??????", "?????? 120?????????", "????????? ?????????", "??????(F2P)")
				AlertDialog.Builder(requireContext())
					.setTitle("?????? ??????")
					.setMultiChoiceItems(capabilities, mFilterCapabilityArr) { dialog, which, isChecked ->
						mFilterCapabilityArr[which] = isChecked
					}
					.setPositiveButton("??????") { dialog, which ->
						updateList()

						if (mFilterCapabilityArr.any { it })
							item.title = "?????? [??????]"
						else
							item.title = "??????"
					}
					.show()
			}
			R.id.filter_category -> {
				val categories = arrayOf("?????? & ?????????", "??????", "??????", "????????? & ??????", "??? ?????????", "??????????????? ????????? ?????? ?????????", "??????", "?????????", "???????????????", "?????? & ????????????", "??????", "??????", "?????? + ??????", "?????????", "?????? & ??????", "????????????", "??????", "??????")
				AlertDialog.Builder(requireContext())
					.setTitle("?????? ??????")
					.setMultiChoiceItems(categories, mFilterCategoryArr) { dialog, which, isChecked ->
						mFilterCategoryArr[which] = isChecked
					}
					.setPositiveButton("??????") { dialog, which ->
						updateList()

						if (mFilterCategoryArr.any { it })
							item.title = "?????? [??????]"
						else
							item.title = "??????"
					}
					.show()
			}
			R.id.filter_korean -> {
				val korean = arrayOf("????????? ??????", "????????? ?????? ??????", "????????? ?????? ??????")
				AlertDialog.Builder(requireContext())
					.setTitle("?????? ?????? ??????")
					.setSingleChoiceItems(korean, mFilterKorean) { dialog, which ->
						mFilterKorean = which
					}
					.setPositiveButton("??????") { dialog, which ->
						updateList()
					}
					.show()
			}
			R.id.sort -> {
				val korean = arrayOf("?????? ??? ????????????", "?????? ??? ????????????", "????????? ????????????", "????????? ????????????")
				AlertDialog.Builder(requireContext())
					.setTitle("?????? ?????? ??????")
					.setSingleChoiceItems(korean, mSort) { dialog, which ->
						mSort = which
					}
					.setPositiveButton("??????") { dialog, which ->
						val preferenceManager = PreferenceManager.getDefaultSharedPreferences(requireContext())
						preferenceManager.edit().putInt("sort", mSort).apply()
						updateList()
					}
					.show()
			}
			R.id.sort_priority -> {
				val korean = arrayOf("?????? ?????? ??????", "???????????? ??????", "????????? ??????")
				AlertDialog.Builder(requireContext())
					.setTitle("?????? ?????? ?????? ??????")
					.setSingleChoiceItems(korean, mSortPriority) { dialog, which ->
						mSortPriority = which
					}
					.setPositiveButton("??????") { dialog, which ->
						val preferenceManager = PreferenceManager.getDefaultSharedPreferences(requireContext())
						preferenceManager.edit().putInt("sortPriority", mSortPriority).apply()
						updateList()
					}
					.show()
			}
			R.id.refresh -> {
				downloadData()
			}
			R.id.setting -> {
				val inflater = requireContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
				val settingView = inflater.inflate(R.layout.setting_dialog, null)

				val optKorean = settingView.findViewById<RadioButton>(R.id.radio_language_korean)
				val optEnglish = settingView.findViewById<RadioButton>(R.id.radio_language_english)

				if (mLanguage == "Korean")
					optKorean.isChecked = true
				else
					optEnglish.isChecked = true

				val switchNewTitle = settingView.findViewById<SwitchCompat>(R.id.switch_show_new_title)
				switchNewTitle.isChecked = mShowNewTitle

				val switchShowDiscount = settingView.findViewById<SwitchCompat>(R.id.switch_show_discount)
				switchShowDiscount.isChecked = mShowDiscount

				val switchShowGamepass = settingView.findViewById<SwitchCompat>(R.id.switch_show_gamepass)
				switchShowGamepass.isChecked = mShowGamepass

				val switchShowName = settingView.findViewById<SwitchCompat>(R.id.switch_show_name)
				switchShowName.isChecked = mShowName

				val switchShowReleaseTime = settingView.findViewById<SwitchCompat>(R.id.switch_release_time)
				switchShowReleaseTime.isChecked = mShowReleaseTime

				val settingDialog = AlertDialog.Builder(requireContext())
					.setTitle("??????")
					.setView(settingView)
					.setNegativeButton("??????", null)
					.setPositiveButton("??????") { dialog, which ->
						val prevLanguage = mLanguage
						mLanguage = if (optKorean.isChecked)
							"Korean"
						else
							"English"

						mShowNewTitle = switchNewTitle.isChecked

						val prevShowDiscount = mShowDiscount
						val prevShowGamepass = mShowGamepass
						val prevShowName = mShowName
						val prevShowReleaseTime = mShowReleaseTime

						mShowDiscount = switchShowDiscount.isChecked
						mShowGamepass = switchShowGamepass.isChecked
						mShowName = switchShowName.isChecked
						mShowReleaseTime = switchShowReleaseTime.isChecked

						val preferenceManager = PreferenceManager.getDefaultSharedPreferences(requireContext())
						preferenceManager.edit()
							.putString("language", mLanguage)
							.putBoolean("showNewTitle", mShowNewTitle)
							.putBoolean("showDiscount", mShowDiscount)
							.putBoolean("showGamepass", mShowGamepass)
							.putBoolean("showName", mShowName)
							.putBoolean("showReleaseTime", mShowReleaseTime)
							.apply()

						if (prevLanguage != mLanguage || prevShowDiscount != mShowDiscount || prevShowGamepass != mShowGamepass || prevShowName != mShowName || prevShowReleaseTime != mShowReleaseTime)
							updateList()
					}
					.create()

				settingDialog.show()
			}
			R.id.about -> {
				val pInfo = requireContext().packageManager.getPackageInfo(requireContext().packageName, 0)

				AlertDialog.Builder(requireContext())
					.setTitle("xKorean ??????")
					.setMessage("????????? ????????? ???????????? ?????? ???????????? ????????? ???????????? ???????????????.\n" +
							"https://cafe.naver.com/xboxinfo\n\n" +
							"????????? & ????????? ?????? ????????? XboxKBM ??????????????? ???????????? ????????????.\n" +
							"https://xboxkbm.herokuapp.com\n\n" +
							"??????: ${pInfo.versionName}")
					.setPositiveButton("??????", null)
					.create().show()
			}
			R.id.donation -> {
				startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://toon.at/donate/637852371342632860")))
			}
		}

		return super.onOptionsItemSelected(item)
	}

	private fun updateList() {
		val filteredList = mutableListOf<Game>()

		var useDeviceFilter = false
		for (i in 0 until mFilterDeviceArr.size - 1)
		{
			if (mFilterDeviceArr[i])
			{
				useDeviceFilter = true
				break
			}
		}

		var useCategoryFilter = false
		for (i in mFilterCategoryArr.indices) {
			if (mFilterCategoryArr[i]) {
				useCategoryFilter = true
				break
			}
		}

		var searchText = ""

		if (mSearchKeyword != "")
		{
			val searchPattern = mSearchKeyword.trim().replace(" ", "").lowercase()

			for (i in searchPattern.indices) {
				when {
					searchPattern[i] in '???'..'???' -> {
						for (j in mKorChr.indices) {
							if (searchPattern[i] == mKorChr[j])
								searchText += "[${mKorStr[j]}-${(mKorChrInt[j + 1] - 1).toChar()}]"
						}
					}
					searchPattern[i] >= '???' -> {
						var magic = (searchPattern[i] - '???') % 588

						searchText += if (magic == 0)
							"[${searchPattern[i]}-${(searchPattern[i] + 27)}]"
						else {
							magic = 27 - (magic % 28)
							"[${searchPattern[i]}-${(searchPattern[i] + magic)}]"
						}
					}
					else -> searchText += searchPattern[i]
				}
			}
		}

		transformViewModel.gameList?.forEach { game ->
			if (useDeviceFilter) {
				if (!(mFilterDeviceArr[0] && game.seriesXS == "O" ||
							mFilterDeviceArr[1] && game.oneXEnhanced == "O" ||
							mFilterDeviceArr[2] && game.oneS == "O" ||
							mFilterDeviceArr[3] && game.x360 == "O" ||
							mFilterDeviceArr[4] && game.og == "O" ||
							mFilterDeviceArr[5] && game.pc == "O"))
					return@forEach
			}

			if ((mFilterDeviceArr[0] || mFilterDeviceArr[1] || mFilterDeviceArr[2] || mFilterDeviceArr[3] || mFilterDeviceArr[4] || mFilterDeviceArr[6]) && !mFilterDeviceArr[5] && game.message.indexOf("windowsMod") >= 0)
				return@forEach

			if (mFilterDeviceArr[6]) {
				if (game.gamePassCloud == "") {
					if (game.bundle.isEmpty())
						return@forEach
					else {
						var supportCloud = false
						for (bundle in game.bundle) {
							if (bundle.gamePassCloud == "O") {
								supportCloud = true
								break
							}
						}

						if (!supportCloud)
							return@forEach
					}
				}
			}

			if (mFilterCapabilityArr[0]) {
				if (game.gamePassPC == "" && game.gamePassConsole == "" && game.gamePassCloud == "") {
					if (game.bundle.isNotEmpty()) {
						var gamePass = false
						for (bundle in game.bundle) {
							if (bundle.gamePassPC == "O" || bundle.gamePassConsole == "O" || bundle.gamePassCloud == "O") {
								gamePass = true
								break
							}
						}

						if (!gamePass)
							return@forEach
					}
					else
						return@forEach
				}
			}

			if (mFilterCapabilityArr[1]) {
				if (game.discount.indexOf("??????") >= 0)
					return@forEach

				if ((game.discount == "" || game.discount == "?????? ??????" || game.discount.indexOf("??????") >= 0) && game.bundle.isEmpty())
					return@forEach

				if (game.discount.indexOf("??????") == -1 && game.bundle.isNotEmpty()) {
					var discount = false
					for (bundle in game.bundle) {
						if (bundle.discountType.indexOf("??????") >= 0) {
							discount = true
							break
						}
					}

					if (!discount)
						return@forEach
				}
			}

			if (mFilterCapabilityArr[2] && game.playAnywhere == "" ||
				mFilterCapabilityArr[3] && game.dolbyAtmos == "" ||
				mFilterCapabilityArr[4] && game.consoleKeyboardMouse == "" ||
				mFilterCapabilityArr[5] && game.localCoop == "" ||
				mFilterCapabilityArr[6] && game.onlineCoop == "" ||
				mFilterCapabilityArr[7] && game.fps120 == "" ||
				mFilterCapabilityArr[8] && game.fpsBoost == "" ||
				mFilterCapabilityArr[9] && game.discount.indexOf("??????") == -1)
				return@forEach

			if (useCategoryFilter) {
				var hasCategory = false
				for (category in game.categories) {
					if (mFilterCategoryArr[0] && category == "family & kids" ||
						mFilterCategoryArr[1] && category == "fighting" ||
						mFilterCategoryArr[2] && category == "educational" ||
						mFilterCategoryArr[3] && category == "racing & flying" ||
						mFilterCategoryArr[4] && category == "role playing" ||
						mFilterCategoryArr[5] && category == "multi-player online battle arena" ||
						mFilterCategoryArr[6] && category == "shooter" ||
						mFilterCategoryArr[7] && category == "sports" ||
						mFilterCategoryArr[8] && category == "simulation" ||
						mFilterCategoryArr[9] && category == "action & adventure" ||
						mFilterCategoryArr[10] && category == "music" ||
						mFilterCategoryArr[11] && category == "strategy" ||
						mFilterCategoryArr[12] && category == "card + board" ||
						mFilterCategoryArr[13] && category == "classics" ||
						mFilterCategoryArr[14] && category == "puzzle & trivia" ||
						mFilterCategoryArr[15] && category == "platformer" ||
						mFilterCategoryArr[16] && category == "casino" ||
						mFilterCategoryArr[17] && category == "other") {
						hasCategory = true
						break
					}
				}

				if (!hasCategory)
					return@forEach
			}

			if (mFilterKorean == 1 && game.localize.indexOf("??????") == -1 ||
				mFilterKorean == 2 && game.localize.indexOf("??????") == -1)
				return@forEach

			if (searchText != "" &&
				searchText.toRegex().find(game.name.trim().replace(" ", "").lowercase()) == null &&
				searchText.toRegex().find(game.koreanName.trim().replace(" ", "").lowercase()) == null) {
				return@forEach
			}

			filteredList.add(game)
		}

		if (mSortPriority == 0) {
			if (mSort == 0) {
				if (mLanguage == "Korean")
					filteredList.sortBy { it.koreanName }
				else
					filteredList.sortBy { it.name }
			} else if (mSort == 1) {
				if (mLanguage == "Korean")
					filteredList.sortByDescending { it.koreanName }
				else
					filteredList.sortByDescending { it.name }
			} else if (mSort == 2) {
				if (mLanguage == "Korean")
					filteredList.sortWith(compareBy<Game> { it.releaseDate }.thenBy { it.koreanName })
				else
					filteredList.sortWith(compareBy<Game> { it.releaseDate }.thenBy { it.name })
			} else {
				if (mLanguage == "Korean")
					filteredList.sortWith(compareByDescending<Game> { it.releaseDate }.thenBy { it.koreanName })
				else
					filteredList.sortWith(compareByDescending<Game> { it.releaseDate }.thenBy { it.name })
			}
		}
		else if (mSortPriority == 1) {
			val gamePassComingList = mutableListOf<Game>() 
			val gamePassNewList = mutableListOf<Game>()
			val gamePassList = mutableListOf<Game>()
			val gamePassEndList = mutableListOf<Game>()

			filteredList.forEach {
				if (it.gamePassPC == "" && it.gamePassConsole == "" && it.gamePassCloud == "") {
					if (it.bundle.isNotEmpty()) {
						for (bundle in it.bundle) {
							if (bundle.gamePassCloud == "O" || bundle.gamePassConsole == "O" || bundle.gamePassPC == "O") {
								when {
									bundle.gamePassNew == "O" -> gamePassNewList.add(it)
									bundle.gamePassEnd == "O" -> gamePassEndList.add(it)
									bundle.gamePassComing == "O" -> gamePassComingList.add(it)
									else -> gamePassList.add(it)
								}

								break
							}
						}
					}
				}
				else {
					when {
						it.gamePassNew == "O" -> gamePassNewList.add(it)
						it.gamePassEnd == "O" -> gamePassEndList.add(it)
						it.gamePassComing == "O" -> gamePassComingList.add(it)
						else -> gamePassList.add(it)
					}
				}
			}

			gamePassComingList.forEach {
				filteredList.remove(it)
			}

			gamePassNewList.forEach {
				filteredList.remove(it)
			}

			gamePassList.forEach {
				filteredList.remove(it)
			}

			gamePassEndList.forEach {
				filteredList.remove(it)
			}

			if (mSort == 0) {
				if (mLanguage == "Korean") {
					gamePassComingList.sortBy { it.koreanName }
					gamePassNewList.sortBy { it.koreanName }
					gamePassList.sortBy { it.koreanName }
					gamePassEndList.sortBy { it.koreanName }
					filteredList.sortBy { it.koreanName }
				}
				else {
					gamePassComingList.sortBy { it.name }
					gamePassNewList.sortBy { it.name }
					gamePassList.sortBy { it.name }
					gamePassEndList.sortBy { it.name }
					filteredList.sortBy { it.name }
				}
			} else if (mSort == 1) {
				if (mLanguage == "Korean") {
					gamePassComingList.sortByDescending { it.koreanName }
					gamePassNewList.sortByDescending { it.koreanName }
					gamePassList.sortByDescending { it.koreanName }
					gamePassEndList.sortByDescending { it.koreanName }
					filteredList.sortByDescending { it.koreanName }
				}
				else {
					gamePassComingList.sortByDescending { it.name }
					gamePassNewList.sortByDescending { it.name }
					gamePassList.sortByDescending { it.name }
					gamePassEndList.sortByDescending { it.name }
					filteredList.sortByDescending { it.name }
				}
			} else if (mSort == 2) {
				if (mLanguage == "Korean") {
					gamePassComingList.sortWith(compareBy<Game> { it.releaseDate }.thenBy { it.koreanName })
					gamePassNewList.sortWith(compareBy<Game> { it.releaseDate }.thenBy { it.koreanName })
					gamePassList.sortWith(compareBy<Game> { it.releaseDate }.thenBy { it.koreanName })
					gamePassEndList.sortWith(compareBy<Game> { it.releaseDate }.thenBy { it.koreanName })
					filteredList.sortWith(compareBy<Game> { it.releaseDate }.thenBy { it.koreanName })
				}
				else {
					gamePassComingList.sortWith(compareBy<Game> { it.releaseDate }.thenBy { it.name })
					gamePassNewList.sortWith(compareBy<Game> { it.releaseDate }.thenBy { it.name })
					gamePassList.sortWith(compareBy<Game> { it.releaseDate }.thenBy { it.name })
					gamePassEndList.sortWith(compareBy<Game> { it.releaseDate }.thenBy { it.name })
					filteredList.sortWith(compareBy<Game> { it.releaseDate }.thenBy { it.name })
				}
			} else {
				if (mLanguage == "Korean") {
					gamePassComingList.sortWith(compareByDescending<Game> { it.releaseDate }.thenBy { it.koreanName })
					gamePassNewList.sortWith(compareByDescending<Game> { it.releaseDate }.thenBy { it.koreanName })
					gamePassList.sortWith(compareByDescending<Game> { it.releaseDate }.thenBy { it.koreanName })
					gamePassEndList.sortWith(compareByDescending<Game> { it.releaseDate }.thenBy { it.koreanName })
					filteredList.sortWith(compareByDescending<Game> { it.releaseDate }.thenBy { it.koreanName })
				}
				else {
					gamePassComingList.sortWith(compareByDescending<Game> { it.releaseDate }.thenBy { it.name })
					gamePassNewList.sortWith(compareByDescending<Game> { it.releaseDate }.thenBy { it.name })
					gamePassList.sortWith(compareByDescending<Game> { it.releaseDate }.thenBy { it.name })
					gamePassEndList.sortWith(compareByDescending<Game> { it.releaseDate }.thenBy { it.name })
					filteredList.sortWith(compareByDescending<Game> { it.releaseDate }.thenBy { it.name })
				}
			}

			gamePassEndList.asReversed().forEach {
				filteredList.add(0, it)
			}

			gamePassList.asReversed().forEach {
				filteredList.add(0, it)
			}

			gamePassNewList.asReversed().forEach {
				filteredList.add(0, it)
			}

			gamePassComingList.asReversed().forEach {
				filteredList.add(0, it)
			}
		}
		else {
			val comparator = compareByDescending<Game> {
				fun getMaxDiscount(game: Game) : Int {
					fun extractDiscount(str: String) : Int {
						var v = 0

						val idx = str.indexOf("%")
						if (idx > 0) {
							var startIdx = idx
							while (startIdx > 0) {
								if (str[startIdx] == ' ') {
									startIdx++
									break
								}

								startIdx--
							}

							v = str.substring(startIdx, idx).toInt()
						}

						return v
					}

					var discount = extractDiscount(game.discount)

					if (game.bundle.isNotEmpty()) {
						game.bundle.forEach {
							val bundleDiscount = extractDiscount(it.discountType)
							if (bundleDiscount > discount)
								discount = bundleDiscount
						}
					}

					return discount
				}

				getMaxDiscount(it)
			}

			if (mSort == 0) {
				if (mLanguage == "Korean")
					filteredList.sortWith(comparator.thenBy { it.koreanName })
				else
					filteredList.sortWith(comparator.thenBy { it.name })
			} else if (mSort == 1) {
				if (mLanguage == "Korean")
					filteredList.sortWith(comparator.thenByDescending { it.koreanName })
				else
					filteredList.sortWith(comparator.thenByDescending { it.name })
			} else if (mSort == 2) {
				if (mLanguage == "Korean")
					filteredList.sortWith(comparator.thenBy { it.releaseDate }.thenBy { it.koreanName })
				else
					filteredList.sortWith(comparator.thenBy { it.releaseDate }.thenBy { it.name })
			} else {
				if (mLanguage == "Korean")
					filteredList.sortWith(comparator.thenByDescending { it.releaseDate }.thenBy { it.koreanName })
				else
					filteredList.sortWith(comparator.thenByDescending { it.releaseDate }.thenBy { it.name })
			}
		}

		transformViewModel.update(filteredList)
		(requireActivity() as MainActivity).setTitle("????????? ?????? ?????????: ${DecimalFormat("#,###").format(filteredList.size)}???")

		if (mNewTitleList.isNotEmpty()) {
			val newTitleBuilder = StringBuilder()
			mNewTitleList.forEach {
				newTitleBuilder.append(it).append("\n")
			}
			mNewTitleList.clear()

			AlertDialog.Builder(requireContext())
				.setTitle("?????? ????????? ?????????")
				.setMessage(newTitleBuilder.toString().trim())
				.setNeutralButton("????????? ???????????? ??????") { _, _ ->
					mShowNewTitle = false
					val preferenceManager = PreferenceManager.getDefaultSharedPreferences(requireContext())
					preferenceManager.edit()
						.putBoolean("showNewTitle", mShowNewTitle)
						.apply()
				}
				.setPositiveButton("??????", null)
				.create().show()
		}
	}

	private fun downloadData() {
		fun loadJSONArray(jsonList: JSONArray) {
			val gson = Gson()

			val gameList = mutableListOf<Game>()
			for (i in 0 until jsonList.length()) {
				gameList.add(gson.fromJson(jsonList.getString(i), Game::class.java))
			}
			transformViewModel.gameList = gameList

			updateList()
		}

		fun loadCacheList() : Boolean {
			val dataFolder = requireContext().getDir("xKorean", Context.MODE_PRIVATE)
			val dataFile = File(dataFolder, "games.json")
			return if (dataFile.exists()) {
				loadJSONArray(JSONArray(dataFile.readText()))
				true
			}
			else
			{
				AlertDialog.Builder(requireContext())
					.setTitle("????????? ?????? ??????")
					.setMessage("????????? ????????? ????????? ??? ????????????. ?????? ??? ?????? ????????? ????????????.")
					.setPositiveButton("??????", null)
					.create().show()
				false
			}
		}

		val progressDialog = BlackProgressDialog(requireContext(), "????????? ????????? ?????????...")
		progressDialog.setCancelable(false)
		progressDialog.setCanceledOnTouchOutside(false)
		progressDialog.show()

		val preferenceManager = PreferenceManager.getDefaultSharedPreferences(requireContext())
		val dataFolder = requireContext().getDir("xKorean", Context.MODE_PRIVATE)

		val updateTimeRequest = JsonObjectRequest(Request.Method.POST, "https://xbox-korean-viewer-server2.herokuapp.com/last_modified_time", null, { updateInfo ->
			val lastModifiedTime = preferenceManager.getString("lastModifiedTime", "")
			if (lastModifiedTime == "" || lastModifiedTime != updateInfo.getString("lastModifiedTime")) {
				val request = BinaryArrayRequest("https://xbox-korean-viewer-server2.herokuapp.com/title_list_zip", {
					try {
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

						val dataFile = File(dataFolder, "games.json")

						val jsonList = JSONArray(str)

						mNewTitleList.clear()
						if (mShowNewTitle && dataFile.exists()) {
							val oldDataList = JSONArray(dataFile.readText())
							for (i in 0 until jsonList.length()) {
								var oldTitle = false
								for (j in 0 until oldDataList.length()) {
									if (jsonList.getJSONObject(i)
											.getString("id") == oldDataList.getJSONObject(j)
											.getString("id")
									) {
										oldDataList.remove(j)
										oldTitle = true
										break
									}
								}

								if (!oldTitle) {
									if (mLanguage == "Korean")
										mNewTitleList.add(
											jsonList.getJSONObject(i).getString("koreanName")
										)
									else
										mNewTitleList.add(
											jsonList.getJSONObject(i).getString("name")
										)
								}
							}
						}

						dataFile.writeText(str)

						loadJSONArray(jsonList)

//            adapter.updateData(gameList)
//            adapter.notifyDataSetChanged()
						preferenceManager.edit()
							.putString("lastModifiedTime", updateInfo.getString("lastModifiedTime"))
							.apply()


					}
					catch (e: EOFException) {
						Toast.makeText(requireContext(), "???????????? ???????????? ????????? ??? ????????????. ?????? ??? ?????? ????????? ?????????.", Toast.LENGTH_SHORT).show()
					}
					finally {
						progressDialog.dismiss()
					}
				}, {
					val loadSuccess = loadCacheList()
					progressDialog.dismiss()
					if (loadSuccess)
						Toast.makeText(requireContext(), "????????? ????????? ????????? ????????? ??? ?????????, ?????? ???????????? ???????????????.", Toast.LENGTH_SHORT).show()
				})
				request.tag = "update"
				mRequestQueue.add(request)
			}
			else {
				loadCacheList()
				progressDialog.dismiss()
			}
		}, {
			val loadSuccess = loadCacheList()
			progressDialog.dismiss()
			if (loadSuccess)
				Toast.makeText(requireContext(), "?????? ????????? ????????? ??? ?????????, ?????? ???????????? ???????????????.", Toast.LENGTH_SHORT).show()
		})
		updateTimeRequest.tag = "update"

		mRequestQueue.add(updateTimeRequest)
	}

	fun getImage(id: String, thumbnailID: String, playAnywhere: String, seriesXS: String, oneS: String, pc: String, thumbnail: String, onLoadedImageListener: (Bitmap?) -> Unit) {
		val dataFolder = requireContext().getDir("xKorean", Context.MODE_PRIVATE)
		val cacheFolder = File(dataFolder, "cache")

		val cacheName = StringBuilder("${id}_${thumbnailID}")
		when {
			playAnywhere == "O" -> {
				if (seriesXS == "O")
					cacheName.append("_playanywhere_xs")
				else
					cacheName.append("_playanywhere_os")
			}
			seriesXS == "O" -> cacheName.append("_xs")
			oneS == "O" -> cacheName.append("_os")
			pc == "O" -> cacheName.append("_pc")
		}
		cacheName.append(".jpg")

		val cacheFile = File(cacheFolder, cacheName.toString())
		if (cacheFile.exists())
			onLoadedImageListener.invoke(BitmapFactory.decodeFile(cacheFile.absolutePath))
		else {
			mThumbnailInfoMap[id]?.let { info ->
				val oldName = StringBuilder("${id}_${info.getString("thumbnailID")}")
				when {
					info.getString("playAnywhere") == "O" -> {
						if (info.getString("seriesXS") == "O")
							oldName.append("_playanywhere_xs")
						else
							oldName.append("_playanywhere_os")
					}
					info.getString("seriesXS") == "O" -> oldName.append("_xs")
					info.getString("oneS") == "O" -> oldName.append("_os")
					info.getString("pc") == "O" -> oldName.append("_pc")
				}
				oldName.append(".jpg")

				val oldFile = File(cacheFolder, cacheName.toString())
				if (oldFile.exists())
					oldFile.delete()
			}

			val request = ImageRequest(thumbnail, {
				val titleImage = Bitmap.createBitmap(584, 800, Bitmap.Config.ARGB_8888)

				var header: Bitmap? = null
				when {
					playAnywhere == "O" -> {
						header = if (seriesXS == "O")
							mPlayAnywhereSeriesTitleHeader
						else
							mPlayAnywhereTitleHeader
					}
					seriesXS == "O" -> header = mSeriesTitleHeader
					oneS == "O" -> header = mOneTitleHeader
					pc == "O" -> header = mWindowsTitleHeader
				}

				val oriImage = Bitmap.createScaledBitmap(it, 584, 800, true)
				val c = Canvas(titleImage)
				c.drawBitmap(oriImage, 0f, 0f, Paint(Paint.FILTER_BITMAP_FLAG))
				if (header != null)
					c.drawBitmap(header, 0f, 0f, Paint(Paint.FILTER_BITMAP_FLAG))

				FileOutputStream(cacheFile).use { fos ->
					titleImage.compress(Bitmap.CompressFormat.JPEG, 75, fos)
				}

				val newInfo = JSONObject()
				newInfo.put("thumbnailID", thumbnailID)
				newInfo.put("playAnywhere", playAnywhere)
				newInfo.put("seriesXS", seriesXS)
				newInfo.put("oneS", oneS)
				newInfo.put("pc", pc)

				ThumbnailDBUpdateTask(id, newInfo).execute()

				onLoadedImageListener.invoke(titleImage)
			}, 0, 0, ImageView.ScaleType.FIT_XY, null, {
				println("????????? ???????????? ??????")
				onLoadedImageListener.invoke(null)
			})

			mRequestQueue.add(request)
		}
	}

	inner class EditionAdapter(private val editionList: List<Edition>, private val playAnywhere: String, private val languageCode: String) : BaseAdapter() {
		override fun getCount(): Int {
			return editionList.size
		}

		override fun getItem(position: Int): Any? {
			return null
		}

		override fun getItemId(p0: Int): Long {
			return 0
		}

		override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View? {
			val context = context

			return if (context != null) {
				val vi = requireContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
				val v = vi.inflate(R.layout.item_edition, null)

				val edition = editionList[position]

				val textViewItemEdition = v.findViewById<TextView>(R.id.text_view_item_edition)

				if (mShowName) {
					textViewItemEdition.text = edition.name
					textViewItemEdition.visibility = View.VISIBLE
				}
				else
					textViewItemEdition.visibility = View.INVISIBLE

				var gamePassTag = ""
				if (edition.gamePassPC != "" || edition.gamePassConsole != "" || edition.gamePassCloud != "")
					gamePassTag = "????????????"

				if (edition.gamePassNew == "O")
					gamePassTag += " ??????"
				else if (edition.gamePassEnd == "O")
					gamePassTag += " ??????"
				else if (edition.gamePassComing == "O")
					gamePassTag += " ??????"

				val textViewEditionGamePassBack = v.findViewById<TextView>(R.id.text_view_edition_game_pass_back)
				val textViewEditionGamePass = v.findViewById<TextView>(R.id.text_view_edition_game_pass)
				val textViewEditionGamePassPC = v.findViewById<TextView>(R.id.text_view_edition_game_pass_pc)
				val textViewEditionGamePassConsole = v.findViewById<TextView>(R.id.text_view_edition_game_pass_console)
				val textViewEditionGamePassCloud = v.findViewById<TextView>(R.id.text_view_edition_game_pass_cloud)

				if (gamePassTag == "" || !mShowGamepass) {
					textViewEditionGamePassBack.visibility = View.INVISIBLE
					textViewEditionGamePass.visibility = View.INVISIBLE
					textViewEditionGamePassPC.visibility = View.INVISIBLE
					textViewEditionGamePassConsole.visibility = View.INVISIBLE
					textViewEditionGamePassCloud.visibility = View.INVISIBLE
				}
				else {
					textViewEditionGamePassBack.visibility = View.VISIBLE

					textViewEditionGamePass.text = gamePassTag
					textViewEditionGamePass.visibility = View.VISIBLE

					if (edition.gamePassPC != "")
						textViewEditionGamePassPC.text = "???"
					else
						textViewEditionGamePassPC.text = ""
					textViewEditionGamePassPC.visibility = View.VISIBLE

					if (edition.gamePassConsole != "")
						textViewEditionGamePassConsole.text = "???"
					else
						textViewEditionGamePassConsole.text = ""
					textViewEditionGamePassConsole.visibility = View.VISIBLE

					if (edition.gamePassCloud != "")
						textViewEditionGamePassCloud.text = "???"
					else
						textViewEditionGamePassCloud.text = ""
					textViewEditionGamePassCloud.visibility = View.VISIBLE
				}

				val textViewEditionMessage = v.findViewById<TextView>(R.id.text_view_edition_message)
				if (edition.discountType != "" && mShowDiscount) {
					var discount = edition.discountType
					if (discount == "??? ??????" || (mShowReleaseTime && discount != "?????? ??????" && discount.indexOf(" ??????") >= 0))
						discount = getReleaseTime(edition.releaseDate)

					if (discount != "") {
						textViewEditionMessage.text = discount
						textViewEditionMessage.visibility = View.VISIBLE

						if (discount.contains("??????") && edition.lowestPrice == edition.price)
							textViewEditionMessage.setTextColor(Color.YELLOW)
						else
							textViewEditionMessage.setTextColor(Color.WHITE)
					}
					else
						textViewEditionMessage.visibility = View.INVISIBLE
				} else
					textViewEditionMessage.visibility = View.INVISIBLE


				val imageViewItemEdition = v.findViewById<ImageView>(R.id.image_view_item_edition)

				getImage(edition.id, edition.thumbnailID, playAnywhere, edition.seriesXS, edition.oneS, edition.pc, edition.thumbnail) {
					imageViewItemEdition.setImageBitmap(it)
				}

				imageViewItemEdition.setOnClickListener {
					goToStore(languageCode, edition.id)
				}

				imageViewItemEdition.setOnLongClickListener {
					val popupMenu = PopupMenu(requireContext(), it)
					requireActivity().menuInflater.inflate(R.menu.bundle_popup, popupMenu.menu)

					popupMenu.menu.getItem(1).isVisible = edition.discountType.contains("??????")

					popupMenu.setOnMenuItemClickListener { item ->
						when (item.itemId) {
							R.id.popup_bundle_price -> {
								showPriceInfo(edition.price, edition.lowestPrice, edition.languageCode)
							}
							R.id.popup_bundle_immigration -> {
								showImmigrantResult(edition.nzReleaseDate, edition.releaseDate)
							}
						}

						false
					}

					popupMenu.show()

					true
				}

				v
			}
			else
				null
		}

	}

	private inner class ThumbnailDBLoadTask(private val onLoadCompleteListener: () -> Unit) : AsyncTask<Void, Void, Void?>() {
		override fun doInBackground(vararg p0: Void?): Void? {
			val thumbnailDao = XKoreanDatabase.getInstance(requireContext()).thumbnailDao()
			val thumbnailInfoArr = thumbnailDao.loadThumbnailInfo()

			for (i in thumbnailInfoArr.indices) {
				mThumbnailInfoMap[thumbnailInfoArr[i].id] = JSONObject(thumbnailInfoArr[i].info)
			}

			return null
		}

		override fun onPostExecute(result: Void?) {
			onLoadCompleteListener.invoke()
		}
	}

	private inner class ThumbnailDBUpdateTask(private val id: String, private val info: JSONObject) : AsyncTask<Void, Void, Void?>() {
		override fun doInBackground(vararg p0: Void?): Void? {
			val thumbnailDao = XKoreanDatabase.getInstance(requireContext()).thumbnailDao()
			synchronized(mThumbnailInfoMap) {
				if (mThumbnailInfoMap.containsKey(id))
					thumbnailDao.updateThumbnailInfo(ThumbnailInfo(id, info.toString()))
				else
					thumbnailDao.insertThumbnailInfo(ThumbnailInfo(id, info.toString()))
			}

			mThumbnailInfoMap[id] = info

			return null
		}
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

			if (mShowName) {
				holder.textView.text = if (mLanguage == "Korean")
					game.koreanName
				else
					game.name
				holder.textView.visibility = View.VISIBLE

				val localize = game.localize.replace("/", "\n")
				holder.localizeTextView.text = localize

				when {
					localize.indexOf("??????") >= 0 -> holder.localizeTextView.setBackgroundColor(Color.argb(0xCC, 0x44, 0x85, 0x0E))
					localize.indexOf("??????") >= 0 -> holder.localizeTextView.setBackgroundColor(Color.argb(0xCC, 0xA6, 0x88, 0x19))
					else -> holder.localizeTextView.setBackgroundColor(Color.argb(0xCC, 0x95, 0x0D, 0x00))
				}

				holder.localizeTextView.visibility = View.VISIBLE
			}
			else {
				holder.textView.visibility = View.INVISIBLE
				holder.localizeTextView.visibility = View.INVISIBLE
			}

			var gamePassTag = ""
			if (game.gamePassPC != "" || game.gamePassConsole != "" || game.gamePassCloud != "")
				gamePassTag = "????????????"
			else if (game.bundle.isNotEmpty()) {
				for (bundle in game.bundle) {
					if (bundle.gamePassPC != "" || bundle.gamePassConsole != "" || bundle.gamePassCloud != "") {
						gamePassTag = "????????????"
						break
					}
				}
			}

			if (game.gamePassNew == "O")
				gamePassTag += " ??????"
			else if (game.gamePassEnd == "O")
				gamePassTag += " ??????"
			else if (game.gamePassComing == "O")
				gamePassTag += " ??????"
			else {
				for (bundle in game.bundle)
				{
					if (bundle.gamePassNew != "")
					{
						gamePassTag += " ??????"
						break
					}
					else if (game.gamePassEnd != "")
					{
						gamePassTag += " ??????"
						break
					}
					else if (game.gamePassComing != "")
					{
						gamePassTag += " ??????"
						break
					}
				}
			}


			if (gamePassTag == "" || !mShowGamepass) {
				holder.gamePassTextView.visibility = View.INVISIBLE
				holder.gamePassBackTextView.visibility = View.INVISIBLE
				holder.gamePassPCTextView.visibility = View.INVISIBLE
				holder.gamePassConsoleTextView.visibility = View.INVISIBLE
				holder.gamePassCloudTextView.visibility = View.INVISIBLE
			}
			else {
				holder.gamePassTextView.text = gamePassTag
				holder.gamePassTextView.visibility = View.VISIBLE
				holder.gamePassBackTextView.visibility = View.VISIBLE

				if (game.gamePassPC != "")
					holder.gamePassPCTextView.text = "???"
				else {
					var support = false
					for (bundle in game.bundle) {
						if (bundle.gamePassPC == "O") {
							holder.gamePassPCTextView.text = "???"
							support = true
							break
						}
					}

					if (!support)
						holder.gamePassPCTextView.text = ""
				}
				holder.gamePassPCTextView.visibility = View.VISIBLE

				if (game.gamePassConsole != "")
					holder.gamePassConsoleTextView.text = "???"
				else {
					var support = false
					for (bundle in game.bundle) {
						if (bundle.gamePassConsole == "O") {
							holder.gamePassConsoleTextView.text = "???"
							support = true
							break
						}
					}

					if (!support)
						holder.gamePassConsoleTextView.text = ""
				}
				holder.gamePassConsoleTextView.visibility = View.VISIBLE

				if (game.gamePassCloud != "")
					holder.gamePassCloudTextView.text = "???"
				else {
					var support = false
					for (bundle in game.bundle) {
						if (bundle.gamePassCloud == "O") {
							holder.gamePassCloudTextView.text = "???"
							support = true
							break
						}
					}

					if (!support)
						holder.gamePassCloudTextView.text = ""
				}
				holder.gamePassCloudTextView.visibility = View.VISIBLE
			}

			var discount = game.discount

			if (!game.isAvailable() && game.bundle.size == 1)
				discount = game.bundle[0].discountType
			else if (game.bundle.isNotEmpty())
			{
				for (bundle in game.bundle)
				{
					if (bundle.discountType.indexOf("??????") >= 0)
					{
						discount = "????????? ??????"
						break
					}
					else if (discount == "?????? ??????" && bundle.discountType != "?????? ??????")
						discount = ""
				}

				if (!game.isAvailable() && discount == "")
					discount = game.bundle[0].discountType
			}

			if (discount == "??? ??????" || (mShowReleaseTime && discount != "?????? ??????" && discount.indexOf(" ??????") >= 0))
				discount = getReleaseTime(game.releaseDate)

			if (discount != "" && mShowDiscount) {
				holder.messageTextView.text = discount
				holder.messageTextView.visibility = View.VISIBLE

				holder.messageTextView.setTextColor(Color.WHITE)
				if ((game.isAvailable() && game.discount.contains("??????") && game.lowestPrice == game.price) || (!game.isAvailable() && game.bundle.size == 1 && game.bundle[0].discountType.contains("??????") && game.bundle[0].lowestPrice == game.bundle[0].price))
					holder.messageTextView.setTextColor(Color.YELLOW)
				else if (game.bundle.isNotEmpty())
				{
					for (bundle in game.bundle) {
						if (bundle.discountType.contains("??????") && bundle.lowestPrice == bundle.price) {
							holder.messageTextView.setTextColor(Color.YELLOW)
							break
						}
					}
				}
			}
			else
				holder.messageTextView.visibility = View.INVISIBLE

			holder.imageView.setImageBitmap(null)

			getImage(game.id, game.thumbnailID, game.playAnywhere, game.seriesXS, game.oneS, game.pc, if (game.thumbnail == null) "" else game.thumbnail!!) {
				holder.imageView.setImageBitmap(it)
			}

			val onClickListener = View.OnClickListener {
				fun checkEdition() {
					if (game.bundle.isEmpty())
						goToStore(game.languageCode, game.id)
					else {
						if (game.isAvailable() || game.bundle.size > 1) {
							val inflater = requireContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
							val editionView = inflater.inflate(R.layout.edition_dialog, null)

							val editionGridView = editionView.findViewById<GridView>(R.id.edition_grid_view)

							val editionList = mutableListOf<Edition>()
							if (game.isAvailable()) {
								editionList.add(Edition(game.id,
									if (mLanguage == "Korean") game.koreanName else game.name,
									game.price,
									game.lowestPrice,
									if (game.discount == "??? ??????") getReleaseTime(game.releaseDate) else game.discount,
									if (game.thumbnail == null) "" else game.thumbnail!!,
									game.thumbnailID,
									game.seriesXS,
									game.oneS,
									game.pc,
									game.gamePassPC,
									game.gamePassConsole,
									game.gamePassCloud,
									game.gamePassNew,
									game.gamePassEnd,
									game.gamePassComing,
									game.releaseDate,
									game.nzReleaseDate,
									game.languageCode
								))
							}

							game.bundle.forEach { edition ->
								edition.languageCode = game.languageCode
								editionList.add(edition)
							}

							val adapter = EditionAdapter(editionList, game.playAnywhere, game.languageCode)
							editionGridView.adapter = adapter

							mEditionDialog = AlertDialog.Builder(requireContext())
								.setTitle("????????? ??????")
								.setView(editionView)
								.setPositiveButton("??????") { _, _ ->
									mEditionDialog!!.dismiss()
								}
								.create()
							mEditionDialog!!.show()
							mEditionDialog!!.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
						}
						else
							goToStore(game.languageCode, game.bundle[0].id)
					}
				}

				if (game.message == "")
					checkEdition()
				else {
					val messageData = game.message.split("\n")

					val messageBuilder = StringBuilder()
					var store360Url = ""
					for (messagePart in messageData) {
						val parsePart = messagePart.split("=")
						val code = parsePart[0].lowercase()

						if (mMessageTemplateMap.containsKey(code)) {
							fun convertToCountryCodeToStr(code: String) : String {
								return when(code.lowercase()) {
									"kr" -> "??????"
									"us" -> "??????"
									"jp" -> "??????"
									"hk" -> "??????"
									"gb" -> "??????"
									else -> ""
								}
							}

							var messageStr = mMessageTemplateMap.getValue(code)
							if (messageStr.indexOf("[name]") >= 0 && parsePart.size > 1) {
								messageStr = if (code == "dlregiononly")
									messageStr.replace("[name]", convertToCountryCodeToStr(parsePart[1]))
								else
									messageStr.replace("[name]", parsePart[1])
							}

							messageBuilder.append("* ").append(messageStr).append("\n")

							if (code == "360market" && parsePart.size > 1)
								store360Url = parsePart[1]
						}
						else
							messageBuilder.append("* ").append(messagePart).append("\n")
					}

					val messageDialogBuilder = AlertDialog.Builder(requireContext())
						.setTitle("????????? ????????????...")
						.setMessage(messageBuilder.toString().trim())
						.setPositiveButton("????????? ??????") { _, _ ->
							checkEdition()
						}
						.setNegativeButton("??????", null)

					if (store360Url != "") {
						messageDialogBuilder.setNeutralButton("360 ?????? ??????") { _, _ ->
							startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(store360Url)))
						}
					}

					messageDialogBuilder.create().show()
				}

			}

			holder.imageView.setOnClickListener(onClickListener)

			holder.imageView.setOnLongClickListener {
				val popupMenu = PopupMenu(requireContext(), it)
				requireActivity().menuInflater.inflate(R.menu.game_popup, popupMenu.menu)

				popupMenu.menu.getItem(2).isVisible = false
				if (game.discount.contains("??????"))
					popupMenu.menu.getItem(2).isVisible = true
				else {
					if (game.bundle.isNotEmpty()) {
						for (bundle in game.bundle) {
							if (bundle.discountType.contains("??????")) {
								popupMenu.menu.getItem(2).isVisible = true
								break
							}
						}
					}
				}

				popupMenu.setOnMenuItemClickListener { item ->
					when (item.itemId) {
						R.id.popup_package -> {
							val supportPackageBuilder = StringBuilder()
							if (game.packages != "")
								supportPackageBuilder.append("* ????????? ?????? ?????????: ").append(game.packages)
							else
								supportPackageBuilder.append("* ????????? ???????????? ????????? ?????? ?????? ???????????? ???????????? ???????????????. ???????????? ???????????? ?????????, ?????? ?????? ????????? ????????? ????????? ????????????.").append(game.packages)

							if (game.message.contains("dlregiononly", true))
								supportPackageBuilder.append("\n").append("* ???????????? ???????????? ?????? ????????? ????????????. ?????? ????????? ????????? ????????? ?????? ????????? ????????? ????????????.")

							AlertDialog.Builder(requireContext())
								.setTitle("????????? ?????? ????????? ??????")
								.setMessage(supportPackageBuilder.toString())
								.setPositiveButton("??????", null)
								.create()
								.show()
						}
						R.id.popup_price -> {
							if (game.bundle.isEmpty())
								showPriceInfo(game.price, game.lowestPrice, game.languageCode)
							else {
								if (game.isAvailable() || game.bundle.size > 1) {
									AlertDialog.Builder(requireContext())
										.setTitle("?????? ??????")
										.setMessage("* ?????? ????????? ?????? ???????????? ????????????. ????????? ???????????? ????????? ????????? ????????????.")
										.setPositiveButton("??????", null)
										.create()
										.show()
								}
								else
									showPriceInfo(game.bundle[0].price, game.bundle[0].lowestPrice, game.languageCode)
							}
						}
						R.id.popup_immigration -> {
							if (game.bundle.isEmpty())
								showImmigrantResult(game.nzReleaseDate, game.releaseDate)
							else {
								if (game.isAvailable() || game.bundle.size > 1) {
									AlertDialog.Builder(requireContext())
										.setTitle("?????? ????????? ?????? ????????? ?????? ??????")
										.setMessage("* ??? ????????? ?????? ???????????? ????????????. ???????????? ???????????? ????????? ????????????.")
										.setPositiveButton("??????", null)
										.create()
										.show()
								}
								else
									showImmigrantResult(game.nzReleaseDate, game.releaseDate)
							}
						}
						R.id.popup_report -> {
							val inflater = requireContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
							val errorReportView = inflater.inflate(R.layout.error_report_dialog, null)

							AlertDialog.Builder(requireContext())
								.setTitle("?????? ??????")
								.setView(errorReportView)
								.setPositiveButton("??????") { _, _ ->
									val report = JSONObject()
									report.put("name", if (mLanguage == "Korean") game.koreanName else game.name)
									report.put("cantBuy", errorReportView.findViewById<CheckBox>(R.id.chk_cant_buy).isChecked.toString())
									report.put("noSupportRegion", errorReportView.findViewById<CheckBox>(R.id.chk_no_support_region).isChecked.toString())
									report.put("message", errorReportView.findViewById<EditText>(R.id.txt_error_report_etc).text.toString())
									report.put("deviceType", "Android")
									report.put("deviceRegion", "Mobile")

									mRequestQueue.add(JsonObjectRequest(Request.Method.POST, "https://xbox-korean-viewer-server2.herokuapp.com/report_error", report, {
										//mRequestQueue.add(JsonObjectRequest(Request.Method.POST, "http://192.168.200.8:3000/report_error", report, {
										//                            if (it.has("error"))
										//                                Toast.makeText(requireContext(), "????????? ??????????????? ????????? ??? ????????????. ?????? ??? ?????? ????????? ????????????.", Toast.LENGTH_SHORT).show()
										//                            else
										Toast.makeText(requireContext(), "????????? ?????????????????????.", Toast.LENGTH_SHORT).show()
									}, {
										Toast.makeText(requireContext(), "?????? ????????? ????????? ??? ????????????. ?????? ??? ?????? ????????? ????????????.", Toast.LENGTH_SHORT).show()
									}))
								}
								.setNegativeButton("??????", null)
								.create().show()
						}
					}

					false
				}

				popupMenu.show()


//
				true
			}
		}

//        fun updateData(updateList: List<Game>) {
//            mItems.addAll(updateList)
//        }
	}

	private fun getLanguageCodeFromUrl(url: String) : String {
		var startIdx = url.indexOf("com/")

		var endIdx = -1
		if (startIdx > 0) {
			startIdx += "/com".length
			endIdx = url.indexOf("/", startIdx)
		}

		return if (endIdx > 0)
			url.substring(startIdx, endIdx)
		else
			""
	}

	private fun showPriceInfo(price: Float, lowestPrice: Float, regionCode: String) {
		val priceInfoBuilder = StringBuilder()
		val regionCodeParts = regionCode.split("-")
		val locale = if (regionCodeParts.size == 2)
			Locale(regionCodeParts[0], regionCodeParts[1])
		else
			Locale("ko", "KR")

		val currencyFormatter = NumberFormat.getCurrencyInstance(locale)
		if (price >= 0)
		{
			priceInfoBuilder.append("* ?????? ?????????: ").append(currencyFormatter.format(price))

			if (lowestPrice > 0)
				priceInfoBuilder.append("\n* ?????? ?????????: ").append(currencyFormatter.format(lowestPrice))
		}
		else
			priceInfoBuilder.append("* ????????? ???????????? ????????? ????????? ????????? ??????????????????.")

		priceInfoBuilder.append("\n\n* xKorean?????? ???????????? ?????? ????????? ????????? ?????? ????????? ???????????? ?????? ??? ????????????. ?????? ?????? ?????? ????????? ????????? ????????? ????????????.")

		AlertDialog.Builder(requireContext())
			.setTitle("?????? ??????")
			.setMessage(priceInfoBuilder.toString())
			.setPositiveButton("??????", null)
			.create()
			.show()
	}

	private fun showImmigrantResult(nzReleaseDate: String, releaseDate: String) {
		val message = if (nzReleaseDate != "" && DateTime.parse(nzReleaseDate) < DateTime.parse(releaseDate)) {
			val nzReleaseTime = DateTime.parse(nzReleaseDate)
			"* ???????????? ??????????????? ????????? ?????? ??????: ${nzReleaseTime.toString("yyyy.MM.dd aa hh:mm")}"
		} else
			"* ??????????????? ?????? ????????? ????????? ?????? ??????????????? ??? ????????????."

		AlertDialog.Builder(requireContext())
			.setTitle("?????? ????????? ?????? ????????? ?????? ??????")
			.setMessage(message)
			.setPositiveButton("??????", null)
			.create()
			.show()
	}

	fun getReleaseTime(releaseDate: String) : String {
		val parser = ISODateTimeFormat.dateTime()
		val releaseTime = parser.parseDateTime(releaseDate)

		return if (releaseTime.isAfterNow)
			releaseTime.toString("M??? d??? H??? ??????")
		else
			""
	}

	fun goToStore(languageCode: String, id: String) {
		if (mFullFeature)
			startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.microsoft.com/$languageCode/p/xkorean/$id")))
		else if ((id == "BPN08ZDRPSFK" && mFullFeatureReady == 0) ||
			(id == "C1SDBNRFXT1D" && mFullFeatureReady == 1))
			mFullFeatureReady++
		else if (id == "BPKDQSSFQ9WV" && mFullFeatureReady == 2) {
			mFullFeature = true

			val preferenceManager = PreferenceManager.getDefaultSharedPreferences(requireContext())
			preferenceManager.edit().putBoolean("fullFeature", true).apply()

			Toast.makeText(context, "????????? ?????????????????????.", Toast.LENGTH_SHORT).show()
		}
		else
			mFullFeatureReady = 0
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