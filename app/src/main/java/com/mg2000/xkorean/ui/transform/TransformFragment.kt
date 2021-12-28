package com.mg2000.xkorean.ui.transform

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SwitchCompat
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.MenuCompat
import androidx.lifecycle.MutableLiveData
import androidx.preference.PreferenceManager
import com.android.volley.toolbox.*
import com.c.progress_dialog.BlackProgressDialog
import com.mg2000.xkorean.IntentRepo
import com.mg2000.xkorean.MainActivity
import com.mg2000.xkorean.MainViewModel
import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat
import org.json.JSONObject
import java.io.*
import java.text.DecimalFormat
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
    private var mLanguage = "Korean"
    private var mShowNewTitle = true
    private var mSearchKeyword = ""

    private var mShowRecommendTag = false
    private var mShowDiscount = true
    private var mShowGamepass = true
    private var mShowName = true
    private var mShowReleaseTime = false

    private val mNewTitleList = mutableListOf<String>()

    private val mMessageTemplateMap = mapOf("dlregiononly" to "다음 지역의 스토어에서 다운로드 받아야 한국어가 지원됩니다: [name]",
        "packageonly" to "패키지 버전만 한국어를 지원합니다.",
        "usermode" to "이 게임은 유저 모드를 설치하셔야 한국어가 지원됩니다.",
        "360market" to "360 마켓플레이스를 통해서만 구매하실 수 있습니다.",
        "windowsmod" to "이 게임은 윈도우에서 한글 패치를 설치하셔야 한국어가 지원됩니다.")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        transformViewModel = ViewModelProvider(this).get(TransformViewModel::class.java)
        _binding = FragmentTransformBinding.inflate(inflater, container, false)
        val root: View = binding.root

        mainViewModel = ViewModelProvider(requireActivity(), MainViewModel.Factory(IntentRepo())).get(MainViewModel::class.java)
        mainViewModel.intent.get.observe(viewLifecycleOwner, {
            mSearchKeyword = it
            updateList()
        })

        val preferenceManager = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val preFilterDevice = JSONArray(preferenceManager.getString("filterDevice", "[ false, false, false, false, false, false, false ]"))

        for (i in 0 until preFilterDevice.length()) {
            mFilterDeviceArr[i] = preFilterDevice.getBoolean(i)
        }

        mSort = preferenceManager.getInt("sort", 0)
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

            println("화면 너비 ${root.width} ${convertDPToPixels(columnWidth)}")

            val spanCount = floor(root.width / convertDPToPixels(columnWidth).toDouble()).toInt()
            ((root as RecyclerView).layoutManager as GridLayoutManager).spanCount = spanCount
        }

        val recyclerView = binding.recyclerviewTransform
        val adapter = TransformAdapter()
        recyclerView.adapter = adapter
        transformViewModel.filteredGames.observe(viewLifecycleOwner, {
            adapter.submitList(it)
            adapter.notifyDataSetChanged()

            mHandler.postDelayed({
                recyclerView.scrollToPosition(0)
            }, 500)
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

        if (transformViewModel.gameList == null)
            downloadData()

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

        MenuCompat.setGroupDividerEnabled(menu, true)

        if (mFilterDeviceArr.any { it })
            (menu.findItem(R.id.filter_device)).title = "기종 [적용]"
        else
            (menu.findItem(R.id.filter_device)).title = "기종"

        return result
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.filter_device -> {
                val devices = arrayOf("Xbox Series X|S", "Xbox One X Enhanced", "Xbox One", "Xbox 360", "Original Xbox", "Windows", "Xbox Cloud Gaming")
                AlertDialog.Builder(requireContext())
                    .setTitle("기종 선택")
                    .setMultiChoiceItems(devices, mFilterDeviceArr) { dialog, which, isChecked ->
                        mFilterDeviceArr[which] = isChecked
                    }
                    .setPositiveButton("확인") { dialog, which ->
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
                            item.title = "기종 [적용]"
                        else
                            item.title = "기종"
                    }
                    .show()
            }
            R.id.filter_capability -> {
                val capabilities = arrayOf("게임패스", "할인", "플레이 애니웨어", "돌비 애트모스", "키보드/마우스", "로컬 협동", "온라인 협동", "최대 120프레임", "프레임 부스트", "무료(F2P)")
                AlertDialog.Builder(requireContext())
                    .setTitle("특성 선택")
                    .setMultiChoiceItems(capabilities, mFilterCapabilityArr) { dialog, which, isChecked ->
                        mFilterCapabilityArr[which] = isChecked
                    }
                    .setPositiveButton("확인") { dialog, which ->
                        updateList()

                        if (mFilterCapabilityArr.any { it })
                            item.title = "특성 [적용]"
                        else
                            item.title = "특성"
                    }
                    .show()
            }
            R.id.filter_category -> {
                val categories = arrayOf("가족 & 아이들", "격투", "교육", "레이싱 & 비행", "롤 플레잉", "멀티플레이 온라인 배틀 아레나", "슈터", "스포츠", "시뮬레이션", "액션 & 어드벤처", "음악", "전략", "카드 + 보드", "클래식", "퍼즐 & 상식", "플랫포머", "도박", "기타")
                AlertDialog.Builder(requireContext())
                    .setTitle("장르 선택")
                    .setMultiChoiceItems(categories, mFilterCategoryArr) { dialog, which, isChecked ->
                        mFilterCategoryArr[which] = isChecked
                    }
                    .setPositiveButton("확인") { dialog, which ->
                        updateList()

                        if (mFilterCategoryArr.any { it })
                            item.title = "장르 [적용]"
                        else
                            item.title = "장르"
                    }
                    .show()
            }
            R.id.filter_korean -> {
                val korean = arrayOf("한국어 지원", "한국어 음성 지원", "한국어 자막 지원")
                AlertDialog.Builder(requireContext())
                    .setTitle("지원 범위 선택")
                    .setSingleChoiceItems(korean, mFilterKorean) { dialog, which ->
                        mFilterKorean = which
                    }
                    .setPositiveButton("확인") { dialog, which ->
                        updateList()
                    }
                    .show()
            }
            R.id.sort -> {
                val korean = arrayOf("이름 순 오름차순", "이름 순 내림차순", "출시일 오름차순", "출시일 내림차순")
                AlertDialog.Builder(requireContext())
                    .setTitle("정렬 방식 선택")
                    .setSingleChoiceItems(korean, mSort) { dialog, which ->
                        mSort = which
                    }
                    .setPositiveButton("확인") { dialog, which ->
                        val preferenceManager = PreferenceManager.getDefaultSharedPreferences(requireContext())
                        preferenceManager.edit().putInt("sort", mSort).apply()
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
                    .setTitle("설정")
                    .setView(settingView)
                    .setNegativeButton("취소", null)
                    .setPositiveButton("확인") { dialog, which ->
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

//                val binding = DataBindingUtil.inflate<SettingDialogBinding>(settingDialog.layoutInflater, R.layout.setting_dialog, null, false)
//                settingDialog.setContentView(binding.root)

                settingDialog.show()
            }
            R.id.about -> {
                val pInfo = requireContext().packageManager.getPackageInfo(requireContext().packageName, 0)

                AlertDialog.Builder(requireContext())
                    .setTitle("xKorean 정보")
                    .setMessage("한글화 정보는 엑스박스 정보 카페에서 제공한 데이터를 이용합니다.\n" +
                    "https://cafe.naver.com/xboxinfo\n\n" +
                    "앱의 UI 소스 코드는 Gamepass Scores 앱을 활용하였습니다.\n" +
                    "https://github.com/XeonKHJ/GamePassScores\n\n" +
                    "키보드 & 마우스 지원 여부는 XboxKBM 사이트에서 제공받고 있습니다.\n" +
                    "https://xboxkbm.herokuapp.com\n\n" +
                    "버전: ${pInfo.versionName}")
                    .setPositiveButton("확인", null)
                    .create().show()
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
                if (game.discount.indexOf("출시") >= 0)
                    return@forEach

                if ((game.discount == "" || game.discount == "판매 중지" || game.discount.indexOf("무료") >= 0) && game.bundle.isEmpty())
                    return@forEach

                if (game.discount.indexOf("할인") == -1 && game.bundle.isNotEmpty()) {
                    var discount = false
                    for (bundle in game.bundle) {
                        if (bundle.discountType.indexOf("할인") >= 0) {
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
                mFilterCapabilityArr[9] && game.discount.indexOf("무료") == -1)
                return@forEach

            if (useCategoryFilter) {
                var hasCategory = false
                for (category in game.categories) {
                    if (mFilterCategoryArr[0] && category == "family & kids" ||
                        mFilterCategoryArr[1] && category == "fighting" ||
                        mFilterCategoryArr[2] && category == "educational" ||
                        mFilterCategoryArr[3] && category == "racing & flying" ||
                        mFilterCategoryArr[4] && category == "role playing" ||
                        mFilterCategoryArr[4] && category == "multi-player online battle arena" ||
                        mFilterCategoryArr[5] && category == "shooter" ||
                        mFilterCategoryArr[6] && category == "sports" ||
                        mFilterCategoryArr[7] && category == "simulation" ||
                        mFilterCategoryArr[8] && category == "action & adventure" ||
                        mFilterCategoryArr[9] && category == "music" ||
                        mFilterCategoryArr[10] && category == "strategy" ||
                        mFilterCategoryArr[11] && category == "card + board" ||
                        mFilterCategoryArr[12] && category == "classics" ||
                        mFilterCategoryArr[13] && category == "puzzle & trivia" ||
                        mFilterCategoryArr[14] && category == "platformer" ||
                        mFilterCategoryArr[15] && category == "casino" ||
                        mFilterCategoryArr[16] && category == "other") {
                        hasCategory = true
                        break
                    }
                }

                if (!hasCategory)
                    return@forEach
            }

            if (mFilterKorean == 1 && game.localize.indexOf("음성") == -1 ||
                mFilterKorean == 2 && game.localize.indexOf("자막") == -1)
                return@forEach

            if (mSearchKeyword != "" &&
                game.name.lowercase().indexOf(mSearchKeyword.lowercase()) == -1 &&
                game.koreanName.lowercase().indexOf(mSearchKeyword.lowercase()) == -1) {
                return@forEach
            }

            filteredList.add(game)

            if (mSort == 0) {
                if (mLanguage == "Korean")
                    filteredList.sortBy { it.koreanName }
                else
                    filteredList.sortBy { it.name }
            }
            else if (mSort == 1) {
                if (mLanguage == "Korean")
                    filteredList.sortByDescending { it.koreanName }
                else
                    filteredList.sortByDescending { it.name }
            }
            else if (mSort == 2) {
                if (mLanguage == "Korean")
                    filteredList.sortWith(compareBy<Game> { it.releaseDate }.thenBy { it.koreanName })
                else
                    filteredList.sortWith(compareBy<Game> { it.releaseDate }.thenBy { it.name })
            }
            else {
                if (mLanguage == "Korean")
                    filteredList.sortWith(compareByDescending<Game> { it.releaseDate }.thenBy { it.koreanName })
                else
                    filteredList.sortWith(compareByDescending<Game> { it.releaseDate }.thenBy { it.name })
            }
        }

        transformViewModel.update(filteredList)
        (requireActivity() as MainActivity).setTitle("한국어 지원 타이틀: ${DecimalFormat("#,###").format(filteredList.size)}개")

        if (mNewTitleList.isNotEmpty()) {
            val newTitleBuilder = StringBuilder()
            mNewTitleList.forEach {
                newTitleBuilder.append(it).append("\n")
            }
            mNewTitleList.clear()

            AlertDialog.Builder(requireContext())
                .setTitle("새로 확인된 타이틀")
                .setMessage(newTitleBuilder.toString().trim())
                .setNeutralButton("앞으로 표시하지 않음") { _, _ ->
                    mShowNewTitle = false
                    val preferenceManager = PreferenceManager.getDefaultSharedPreferences(requireContext())
                    preferenceManager.edit()
                        .putBoolean("showNewTitle", mShowNewTitle)
                        .apply()
                }
                .setPositiveButton("확인", null)
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
                    .setTitle("데이터 수신 오류")
                    .setMessage("한국어 정보를 확인할 수 없습니다. 잠시 후 다시 시도해 주십시오.")
                    .setPositiveButton("확인", null)
                    .create().show()
                false
            }
        }

        val progressDialog = BlackProgressDialog(requireContext(), "한국어 데이터 확인중...")
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
                        Toast.makeText(requireContext(), "서버에서 데이터를 가져올 수 없습니다. 잠시 후 다시 시도해 주세요.", Toast.LENGTH_SHORT).show()
                    }
                    finally {
                        progressDialog.dismiss()
                    }
                }, {
                    val loadSuccess = loadCacheList()
                    progressDialog.dismiss()
                    if (loadSuccess)
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
            val loadSuccess = loadCacheList()
            progressDialog.dismiss()
            if (loadSuccess)
                Toast.makeText(requireContext(), "서버 정보를 확인할 수 없어서, 기존 데이터를 보여줍니다.", Toast.LENGTH_SHORT).show()
        })
        updateTimeRequest.tag = "update"

        mRequestQueue.add(updateTimeRequest)
    }

    fun getImage(id: String, playAnywhere: String, seriesXS: String, oneS: String, pc: String, thumbnail: String, onLoadedImageListener: (Bitmap?) -> Unit) {
        val dataFolder = requireContext().getDir("xKorean", Context.MODE_PRIVATE)
        val cacheFolder = File(dataFolder, "cache")

        val cacheName = StringBuilder(id)
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

                onLoadedImageListener.invoke(titleImage)
            }, 0, 0, ImageView.ScaleType.FIT_XY, null, {
                println("이미지 다운로드 에러")
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
                    gamePassTag = "게임패스"

                if (edition.gamePassNew == "O")
                    gamePassTag += " 신규"
                else if (edition.gamePassEnd == "O")
                    gamePassTag += " 만기"

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
                        textViewEditionGamePassPC.text = "피"
                    else
                        textViewEditionGamePassPC.text = ""
                    textViewEditionGamePassPC.visibility = View.VISIBLE

                    if (edition.gamePassConsole != "")
                        textViewEditionGamePassConsole.text = "엑"
                    else
                        textViewEditionGamePassConsole.text = ""
                    textViewEditionGamePassConsole.visibility = View.VISIBLE

                    if (edition.gamePassCloud != "")
                        textViewEditionGamePassCloud.text = "클"
                    else
                        textViewEditionGamePassCloud.text = ""
                    textViewEditionGamePassCloud.visibility = View.VISIBLE
                }

                val textViewEditionMessage = v.findViewById<TextView>(R.id.text_view_edition_message)
                if (edition.discountType != "" && mShowDiscount) {
                    var discount = edition.discountType
                    if (discount == "곧 출시" || (mShowReleaseTime && discount != "출시 예정" && discount.indexOf(" 출시") >= 0))
                        discount = getReleaseTime(edition.releaseDate)

                    if (discount != "") {
                        textViewEditionMessage.text = discount
                        textViewEditionMessage.visibility = View.VISIBLE
                    }
                    else
                        textViewEditionMessage.visibility = View.INVISIBLE
                } else
                    textViewEditionMessage.visibility = View.INVISIBLE


                val imageViewItemEdition = v.findViewById<ImageView>(R.id.image_view_item_edition)

                getImage(edition.id, playAnywhere, edition.seriesXS, edition.oneS, edition.pc, edition.thumbnail) {
                    imageViewItemEdition.setImageBitmap(it)
                }

                imageViewItemEdition.setOnClickListener {
                    goToStore(languageCode, edition.id)
                }

                v
            }
            else
                null
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
                    localize.indexOf("음성") >= 0 -> holder.localizeTextView.setBackgroundColor(Color.argb(0xCC, 0x44, 0x85, 0x0E))
                    localize.indexOf("자막") >= 0 -> holder.localizeTextView.setBackgroundColor(Color.argb(0xCC, 0xA6, 0x88, 0x19))
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
                    holder.gamePassPCTextView.text = "피"
                else {
                    var support = false
                    for (bundle in game.bundle) {
                        if (bundle.gamePassPC == "O") {
                            holder.gamePassPCTextView.text = "피"
                            support = true
                            break
                        }
                    }

                    if (!support)
                        holder.gamePassPCTextView.text = ""
                }
                holder.gamePassPCTextView.visibility = View.VISIBLE

                if (game.gamePassConsole != "")
                    holder.gamePassConsoleTextView.text = "엑"
                else {
                    var support = false
                    for (bundle in game.bundle) {
                        if (bundle.gamePassConsole == "O") {
                            holder.gamePassConsoleTextView.text = "엑"
                            support = true
                            break
                        }
                    }

                    if (!support)
                        holder.gamePassConsoleTextView.text = ""
                }
                holder.gamePassConsoleTextView.visibility = View.VISIBLE

                if (game.gamePassCloud != "")
                    holder.gamePassCloudTextView.text = "클"
                else {
                    var support = false
                    for (bundle in game.bundle) {
                        if (bundle.gamePassCloud == "O") {
                            holder.gamePassCloudTextView.text = "클"
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

            if (discount == "곧 출시" || (mShowReleaseTime && discount != "출시 예정" && discount.indexOf(" 출시") >= 0))
                discount = getReleaseTime(game.releaseDate)

            if (discount != "" && mShowDiscount) {
                holder.messageTextView.text = discount
                holder.messageTextView.visibility = View.VISIBLE
            }
            else
                holder.messageTextView.visibility = View.INVISIBLE

            holder.imageView.setImageBitmap(null)

            getImage(game.id, game.playAnywhere, game.seriesXS, game.oneS, game.pc, game.thumbnail) {
                holder.imageView.setImageBitmap(it)
            }

            val onClickListener = View.OnClickListener {
                fun getLanguageCodeFromUrl(url: String) : String {
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

                fun checkEdition() {
                    if (game.bundle.isEmpty())
                        goToStore(getLanguageCodeFromUrl(game.storeLink), game.id)
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
                                    if (game.discount == "곧 출시") getReleaseTime(game.releaseDate) else game.discount,
                                    game.thumbnail,
                                    game.seriesXS,
                                    game.oneS,
                                    game.pc,
                                    game.gamePassPC,
                                    game.gamePassConsole,
                                    game.gamePassCloud,
                                    game.gamePassNew,
                                    game.gamePassEnd,
                                    game.releaseDate
                                ))
                            }

                            game.bundle.forEach { edition ->
                                editionList.add(edition)
                            }

                            val adapter = EditionAdapter(editionList, game.playAnywhere, getLanguageCodeFromUrl(game.storeLink))
                            editionGridView.adapter = adapter

                            mEditionDialog = AlertDialog.Builder(requireContext())
                                .setTitle("에디션 선택")
                                .setView(editionView)
                                .setPositiveButton("닫기") { _, _ ->
                                    mEditionDialog!!.dismiss()
                                }
                                .create()
                            mEditionDialog!!.show()
                            mEditionDialog!!.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                        }
                        else
                            goToStore(getLanguageCodeFromUrl(game.storeLink), game.bundle[0].id)
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
                                    "kr" -> "한국"
                                    "us" -> "미국"
                                    "jp" -> "일본"
                                    "hk" -> "홍콩"
                                    "gb" -> "영국"
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
                    }

                    val messageDialogBuilder = AlertDialog.Builder(requireContext())
                        .setTitle("스토어 이동전에...")
                        .setMessage(messageBuilder.toString().trim())
                        .setPositiveButton("스토어 이동") { _, _ ->
                            checkEdition()
                        }
                        .setNegativeButton("닫기", null)

                    if (store360Url != "") {
                        messageDialogBuilder.setNeutralButton("360 마켓 이동") { _, _ ->
                            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(store360Url)))
                        }
                    }

                    messageDialogBuilder.create().show()
                }

            }

            holder.imageView.setOnClickListener(onClickListener)

            holder.imageView.setOnLongClickListener {
                val inflater = requireContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
                val errorReportView = inflater.inflate(R.layout.error_report_dialog, null)

                AlertDialog.Builder(requireContext())
                    .setTitle("오류 신고")
                    .setView(errorReportView)
                    .setPositiveButton("신고") { _, _ ->
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
//                                Toast.makeText(requireContext(), "오류를 개발자에게 전달할 수 없습니다. 잠시 후 다시 시도해 주십시오.", Toast.LENGTH_SHORT).show()
//                            else
                                Toast.makeText(requireContext(), "오류가 전송되었습니다.", Toast.LENGTH_SHORT).show()
                        }, {
                            Toast.makeText(requireContext(), "오류 내용을 전송할 수 없습니다. 잠시 후 다시 시도해 주십시오.", Toast.LENGTH_SHORT).show()
                        }))
                    }
                    .setNegativeButton("취소", null)
                    .create().show()

                true
            }
        }

//        fun updateData(updateList: List<Game>) {
//            mItems.addAll(updateList)
//        }
    }

    fun getReleaseTime(releaseDate: String) : String {
        val parser = ISODateTimeFormat.dateTime()
        val releaseTime = parser.parseDateTime(releaseDate)

        return if (releaseTime.isAfterNow)
            releaseTime.toString("M월 d일 H시 출시")
        else
            ""
    }

    fun goToStore(languageCode: String, id: String) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.microsoft.com/$languageCode/p/xkorean/$id")))
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