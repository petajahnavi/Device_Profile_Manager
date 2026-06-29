package com.profiler.deviceprofiler

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {

    private lateinit var btnStartProfiling: Button
    private lateinit var btnSendToServer:   Button
    private lateinit var progressBar:       ProgressBar
    private lateinit var scrollView:        ScrollView

    private lateinit var cardDeviceInfo:  CardView
    private lateinit var cardLiveStatus:  CardView
    private lateinit var cardBenchmark:   CardView
    private lateinit var cardScores:      CardView

    private lateinit var tvDeviceInfo:  TextView
    private lateinit var tvLiveStatus:  TextView
    private lateinit var tvBenchmark:   TextView
    private lateinit var tvScores:      TextView
    private lateinit var tvStatus:      TextView

    private lateinit var deviceInfo: DeviceInfo
    private lateinit var liveStatus: LiveStatus
    private lateinit var benchmark:  Benchmark
    private lateinit var scoring:    Scoring
    private lateinit var apiSender:  ApiSender

    private var lastDeviceInfo: DeviceInfoData? = null
    private var lastLiveStatus: LiveStatusData? = null
    private var lastBenchmark:  BenchmarkResult? = null
    private var lastScores:     ScoreResult?     = null

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        bindViews()
        initModules()
        setupButtons()
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    private fun bindViews() {
        btnStartProfiling = findViewById(R.id.btnStartProfiling)
        btnSendToServer   = findViewById(R.id.btnSendToServer)
        progressBar       = findViewById(R.id.progressBar)
        scrollView        = findViewById(R.id.scrollView)
        cardDeviceInfo    = findViewById(R.id.cardDeviceInfo)
        cardLiveStatus    = findViewById(R.id.cardLiveStatus)
        cardBenchmark     = findViewById(R.id.cardBenchmark)
        cardScores        = findViewById(R.id.cardScores)
        tvDeviceInfo      = findViewById(R.id.tvDeviceInfo)
        tvLiveStatus      = findViewById(R.id.tvLiveStatus)
        tvBenchmark       = findViewById(R.id.tvBenchmark)
        tvScores          = findViewById(R.id.tvScores)
        tvStatus          = findViewById(R.id.tvStatus)
    }

    private fun initModules() {
        deviceInfo = DeviceInfo(this)
        liveStatus = LiveStatus(this)
        benchmark  = Benchmark()
        scoring    = Scoring()
        apiSender  = ApiSender(this)
    }

    private fun setupButtons() {
        btnStartProfiling.setOnClickListener { runProfiling() }
        btnSendToServer.setOnClickListener   { sendToServer() }
    }

    private fun runProfiling() {
        scope.launch {
            setUiBusy(true)
            setStatus("Collecting device info...")

            // ---------- DEVICE INFO ----------
            lastDeviceInfo = deviceInfo.collect()
            tvDeviceInfo.text = deviceInfo.format(lastDeviceInfo!!)
            cardDeviceInfo.visibility = View.VISIBLE

            // ---------- LIVE STATUS ----------
            setStatus("Reading live status...")
            lastLiveStatus = liveStatus.collect()
            tvLiveStatus.text = liveStatus.format(lastLiveStatus!!)
            cardLiveStatus.visibility = View.VISIBLE

            // ---------- BENCHMARK ----------
            setStatus("Running benchmark (up to 2s)...")
            lastBenchmark = withContext(Dispatchers.Default) { benchmark.run() }
            tvBenchmark.text = benchmark.format(lastBenchmark!!)
            cardBenchmark.visibility = View.VISIBLE

            // ---------- SCORING (GPU INCLUDED) ----------
            setStatus("Calculating scores...")
            lastScores = scoring.calculate(
                benchmarkTimeMs = lastBenchmark!!.totalTimeMs,
                cpuCores        = lastDeviceInfo!!.cpuCores,
                totalRamMB      = lastDeviceInfo!!.totalRamMB,
                batteryLevel    = lastLiveStatus!!.batteryPercent,
                gpuVersion      = lastDeviceInfo!!.gpuVersion   // ✅ FIX ADDED
            )
            tvScores.text = scoring.format(lastScores!!)
            cardScores.visibility = View.VISIBLE

            setStatus("Profiling complete!")
            btnSendToServer.visibility = View.VISIBLE
            scrollView.post { scrollView.fullScroll(View.FOCUS_DOWN) }
            setUiBusy(false)
        }
    }

    private fun sendToServer() {
        val di = lastDeviceInfo ?: run { toast("Run profiling first!"); return }
        val ls = lastLiveStatus ?: run { toast("Run profiling first!"); return }
        val bm = lastBenchmark  ?: run { toast("Run profiling first!"); return }
        val sc = lastScores     ?: run { toast("Run profiling first!"); return }

        scope.launch {
            setUiBusy(true)
            setStatus("Sending data to server...")

            val result = apiSender.sendProfile(di, ls, bm, sc)

            result.onSuccess { msg ->
                setStatus("✓ $msg")
                toast("Data sent successfully!")
            }.onFailure { err ->
                setStatus("✗ Failed: ${err.message}")
                toast("Failed: ${err.message}")
            }

            setUiBusy(false)
        }
    }

    private fun setUiBusy(busy: Boolean) {
        btnStartProfiling.isEnabled = !busy
        btnSendToServer.isEnabled   = !busy
        progressBar.visibility = if (busy) View.VISIBLE else View.GONE
    }

    private fun setStatus(msg: String) {
        tvStatus.text = msg
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}