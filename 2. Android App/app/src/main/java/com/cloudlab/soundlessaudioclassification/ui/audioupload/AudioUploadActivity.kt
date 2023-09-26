package com.cloudlab.soundlessaudioclassification.ui.audioupload

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.view.isVisible
import com.cloudlab.soundlessaudioclassification.R
import com.cloudlab.soundlessaudioclassification.databinding.ActivityTrainingBinding
import com.cloudlab.soundlessaudioclassification.framework.audiorecord.entities.TrainingAudio
import com.cloudlab.soundlessaudioclassification.framework.audiorecord.AndroidAudioPlayer
import com.cloudlab.soundlessaudioclassification.framework.audiorecord.AndroidAudioRecorder
import com.cloudlab.soundlessaudioclassification.framework.retrofit.entities.ApiResponse
import com.cloudlab.soundlessaudioclassification.framework.retrofit.services.ApiService
import com.google.android.material.bottomnavigation.BottomNavigationView
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import okhttp3.MediaType

class AudioUploadActivity : ComponentActivity() {

    private lateinit var binding: ActivityTrainingBinding
    private lateinit var bottomNavigationView: BottomNavigationView
    private var numItems: Int = 0
    private var audioList: MutableList<TrainingAudio> = mutableListOf()

    private val recorder by lazy {
        AndroidAudioRecorder(applicationContext)
    }

    private val player by lazy {
        AndroidAudioPlayer(applicationContext)
    }

    private var audioFile: File? = null

    private val retrofit = Retrofit.Builder()
        .baseUrl("http://192.168.0.17:5000")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val apiService = retrofit.create(ApiService::class.java)

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTrainingBinding.inflate(layoutInflater)
        setContentView(R.layout.activity_training)

        val containerLayout = findViewById<LinearLayout>(R.id.containerLayout)

        bottomNavigationView = findViewById(R.id.bottomNavigationView)
        bottomNavigationView.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_add -> {
                    val audioItem = layoutInflater.inflate(R.layout.activity_training_item, containerLayout, false)
                    val btnRemove = audioItem.findViewById<ImageButton>(R.id.btnRemove)
                    val btnRecord = audioItem.findViewById<ImageButton>(R.id.btnRecord)
                    val btnPause = audioItem.findViewById<ImageButton>(R.id.btnPause)
                    val btnStop = audioItem.findViewById<ImageButton>(R.id.btnStop)
                    val btnPlay = audioItem.findViewById<ImageButton>(R.id.btnPlay)
                    val tvAudioStatus = audioItem.findViewById<TextView>(R.id.tvAudioStatus)
                    val tvPath = audioItem.findViewById<TextView>(R.id.tvPath)
                    val layoutItemFooter = audioItem.findViewById<LinearLayout>(R.id.layoutItemFooter)

                    btnRemove.setOnClickListener {
                        containerLayout.removeView(it.parent.parent as View)
                        countLinearLayoutsWithId((containerLayout as View))
                        numItems--
                    }

                    btnRecord.setOnClickListener { it ->
                        val i = getItemIndex(it)
                        val audioFileName = "Audio_$i.mp3"

                        it.isEnabled=false
                        it.alpha=0.5f
                        tvAudioStatus.text="Recording..."

                        val audioFile = File(cacheDir, audioFileName).also {
                            recorder.start(it)
                            audioFile = it
                        }

                        val audio = TrainingAudio().apply {
                            file = audioFile
                            filename = audioFile.absolutePath
                        }
                        tvPath.text=audio.filename

                        addIfNotExists(audio)
                        btnStop.alpha=1.0f
                        btnStop.visibility=View.VISIBLE
                    }

                    btnPause.setOnClickListener {
                        // TODO: Implement
                        Toast.makeText(this, "Pause listener", Toast.LENGTH_SHORT).show()
                        player.stop()
                    }

                    btnStop.setOnClickListener {
                        recorder.stop()
                        btnPlay.isVisible = true
                        btnRecord.isVisible = false
                        btnPause.isEnabled = false
                        btnPause.alpha=0.5f
                        btnStop.isEnabled = false
                        btnStop.alpha=0.5f
                        tvAudioStatus.text="Recorded."
                        layoutItemFooter.visibility=View.VISIBLE
                    }

                    btnPlay.setOnClickListener {
                        var audio = TrainingAudio()

                        for (i in 0 until audioList.count()) {
                            if (audioList[i].filename == tvPath.text){
                                audio = audioList[i]
                                break
                            }
                        }

                        player.playFile(audio.file ?: return@setOnClickListener)
                    }

                    containerLayout.addView(audioItem)
                    countLinearLayoutsWithId((containerLayout as View))
                    numItems++
                    true
                }
                R.id.nav_upload -> {
                    if (!validateForm(containerLayout))
                        Toast.makeText(this, "ERROR: Record all audios and fill labels!", Toast.LENGTH_SHORT).show()
                    else {
                        //ping()
                        uploadFiles()
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun addIfNotExists(audio: TrainingAudio) {
        var exists = false
        for (i in 0 until audioList.count()) {
            if (audioList[i].filename == audio.filename){
                exists=true
                audioList[i] = audio
                break
            }
        }

        if (!exists)
            audioList.add(audio)
    }

    private fun countLinearLayoutsWithId(view: View, count: Int = 0): Int {
        var currentCount = count

        if (view is TextView) {
            if (view.id == R.id.tvIndex) {
                currentCount++
                view.text = currentCount.toString()
            }
        }

        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                currentCount = countLinearLayoutsWithId(view.getChildAt(i), currentCount)
            }
        }

        return currentCount
    }

    private fun getItemIndex(view: View): Int {

        if (view is ImageButton && (view.id == R.id.btnPause || view.id == R.id.btnStop))
            return (((view.parent.parent.parent as LinearLayout)
                .getChildAt(0) as LinearLayout)
                .getChildAt(0) as TextView)
                .text.toString().toInt()

        if (view is ImageButton && (view.id == R.id.btnRecord || view.id == R.id.btnPlay))
            return (((view.parent.parent as LinearLayout)
                .getChildAt(0) as LinearLayout)
                .getChildAt(0) as TextView)
                .text.toString().toInt()

        if (view is ImageButton && view.id == R.id.btnRemove)
            return ((view.parent as LinearLayout)
                .getChildAt(0) as TextView)
                .text.toString().toInt()

        if (view is EditText && view.id == R.id.etLabel)
            return (((view.parent.parent as LinearLayout)
                .getChildAt(0) as LinearLayout)
                .getChildAt(0) as TextView)
                .text.toString().toInt()

        return -1
    }

    private fun validateForm(view: View): Boolean {
        var isValid = true

        if (view is EditText && view.id == R.id.etLabel) {
            val text = view.text.toString()
            if (text.isEmpty()) {
                isValid = false
            }else{
                val idx = getItemIndex(view) - 1
                audioList[idx].label = text
            }
        }

        if (view is TextView && view.id == R.id.tvPath) {
            val text = view.text.toString()
            if (text.isEmpty()) {
                isValid = false
            }
        }

        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                val childView = view.getChildAt(i)
                isValid = isValid && validateForm(childView)
            }
        }

        return isValid
    }

    private fun ping(){
        val call = apiService.ping()

        call.enqueue(object : Callback<String> {
            override fun onResponse(call: Call<String>, response: Response<String>) {
                if (response.isSuccessful) {
                    val result = response.body()
                    Toast.makeText(applicationContext, "Response: $result", Toast.LENGTH_SHORT).show()
                    println("Response: $result")
                } else {
                    Toast.makeText(applicationContext, "Response error: ${response.code()}", Toast.LENGTH_SHORT)
                        .show()
                }
            }

            override fun onFailure(call: Call<String>, t: Throwable) {
                Toast.makeText(applicationContext, "Request error: ${t.message}", Toast.LENGTH_SHORT)
                    .show()
            }
        })
    }

    private fun uploadFiles(){
        for (audio in audioList) {
            val requestFile =
                audio.file?.let { RequestBody.create(MediaType.parse("multipart/form-data"), it) }
            val body = MultipartBody.Part.createFormData("file", audio.file?.name, requestFile)
            val labelsRequestBody = RequestBody.create(MediaType.parse("text/plain"), audio.label)

            apiService.uploadFile(
                file = body,
                labels = labelsRequestBody
            ).enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    if (response.isSuccessful) {
                        //val apiResponse = response.body()
                        Toast.makeText(applicationContext, "Files uploaded successfully", Toast.LENGTH_SHORT)
                            .show()
                    } else {
                        val errorBody = response.errorBody()?.string()
                        Toast.makeText(applicationContext, "Error uploading files $errorBody", Toast.LENGTH_SHORT)
                            .show()
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    Toast.makeText(applicationContext, "Request error: ${t.message}", Toast.LENGTH_SHORT)
                        .show()
                }
            })
        }
    }
}