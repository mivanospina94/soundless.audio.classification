package com.cloudlab.soundlessaudioclassification.ui.main

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.cloudlab.soundlessaudioclassification.databinding.ActivityMainBinding
import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Intent
import android.media.AudioRecord
import android.widget.TextView
import com.cloudlab.soundlessaudioclassification.R
import com.cloudlab.soundlessaudioclassification.ui.results.ResultsActivity
import com.cloudlab.soundlessaudioclassification.ui.audioupload.AudioUploadActivity
import com.cloudlab.soundlessaudioclassification.framework.sqlite.entities.SoundRecordingHeader
import com.cloudlab.soundlessaudioclassification.framework.sqlite.entities.SoundRecordingRec
import com.cloudlab.soundlessaudioclassification.framework.sqlite.SoundlessDb
import org.tensorflow.lite.support.audio.TensorAudio
import org.tensorflow.lite.task.audio.classifier.AudioClassifier
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var modelPath = "soundless_model.tflite"
    var probabilityThreshold: Float = 0.3f
    var tvOutput: TextView? = null
    private lateinit var tvTop: TextView
    private lateinit var recorderSpecsTextView: TextView
    lateinit var classifier: AudioClassifier
    lateinit var tensor: TensorAudio
    private lateinit var record: AudioRecord
    private var timer: Timer? = null
    private var _soundRecordingId : String = ""
    private var soundlessDb: SoundlessDb? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setActivityEvents()
        setActivityPermissions()
        initializeActivity()
        soundlessDb = SoundlessDb(applicationContext)
    }

    @SuppressLint("SetTextI18n")
    private fun setActivityEvents(){
        binding.buttonPlay.setOnClickListener { view ->
            timer = Timer()
            record.startRecording()

            timer?.scheduleAtFixedRate(classifyAudio(), 1, 500)

            tvTop.text = "Recording..."
            view.isEnabled = false
            view.alpha = 0.5f
            binding.buttonStop.isEnabled = true
            binding.buttonStop.alpha = 1.0f

            binding.buttonResults.isEnabled = false
            binding.buttonResults.alpha = 0.5f

            insertHeader()
        }

        binding.buttonStop.setOnClickListener { view ->
            timer?.cancel()
            record.stop()

            tvTop.text = "Stopped."
            view.isEnabled = false
            view.alpha = 0.5f
            binding.buttonPlay.isEnabled = true
            binding.buttonPlay.alpha = 1.0f

            // Update header
            //val soundlessDb = SoundlessDb(applicationContext)
            val db = soundlessDb?.writableDatabase

            val values = ContentValues().apply {
                put("EndDate", Calendar.getInstance().time.toString())
            }

            val selection = "SoundRecordingId = ?"
            val selectionArgs = arrayOf(_soundRecordingId)

            db?.update("SoundRecordingHeader", values, selection, selectionArgs)

            binding.buttonResults.isEnabled = true
            binding.buttonResults.alpha = 1.0f
            //db.close()
        }

        binding.buttonResults.setOnClickListener {
            val intent = Intent(this, ResultsActivity::class.java)
            intent.putExtra("SOUND_RECORDING_ID", _soundRecordingId)
            startActivity(intent)
        }

        binding.buttonUploadActivity.setOnClickListener{
            val intent = Intent(this, AudioUploadActivity::class.java)
            startActivity(intent)
            this.finish()
        }
    }

    private fun setActivityPermissions() {
        val requestRecordAudio = 1337
        requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), requestRecordAudio)
    }

    private fun initializeActivity() {
        tvOutput = findViewById(R.id.output)
        tvTop = findViewById(R.id.textView)
        recorderSpecsTextView = findViewById(R.id.textViewAudioRecorderSpecs)
        classifier = AudioClassifier.createFromFile(this, modelPath)
        tensor = classifier.createInputTensorAudio()

        val format = classifier.requiredTensorAudioFormat
        val recorderSpecs = "Number Of Channels: ${format.channels}\n" +
                "Sample Rate: ${format.sampleRate}"
        recorderSpecsTextView.text = recorderSpecs

        record = classifier.createAudioRecord()
        record.startRecording()

        timer?.scheduleAtFixedRate(classifyAudio(), 1, 500)
        binding.buttonResults.isEnabled = false
        binding.buttonResults.alpha = 0.5f

        binding.buttonPlay.callOnClick()
    }

    private fun classifyAudio(): TimerTask {
        return object : TimerTask() {
            override fun run() {

                //val numberOfSamples = tensor.load(record)
                tensor.load(record)
                val output = classifier.classify(tensor)

                var filteredModelOutput = output[0].categories.filter {
                    (it.label.contains("Train") || it.label.contains("Vehicle") || it.label.contains("Rail") || it.label.contains("Engine"))
                        && it.score > probabilityThreshold
                        && !it.label.contains("Silence")
                }

                if (filteredModelOutput.isNotEmpty()) {

                    val filteredModelOutput2 = output[1].categories.filter {
                        it.score > 0.6f
                    }

                    if (filteredModelOutput2?.maxByOrNull { -it.score }!!.score >= filteredModelOutput.maxByOrNull { -it.score }!!.score){
                        filteredModelOutput = filteredModelOutput2
                    }
                }else {
                    filteredModelOutput = output[0].categories.filter {
                        it.score > probabilityThreshold
                    }
                }

                //test

//                var filteredModelOutput = output[1].categories.filter {it ->
//                    /*(it.label.contains("Train") || it.label.contains("Vehicle") || it.label.contains("Rail"))
//                            && */it.score > 0.6f
//                            //&& !it.label.contains("Silence")
//                }

                val outputStr =
                    filteredModelOutput.sortedBy { -it.score }
                        .joinToString(separator = "\n") { "${it.label.replace("\\s+\\d+$".toRegex(), "")} -> ${it.score} " }

                val list: MutableList<SoundRecordingRec> = mutableListOf()
                filteredModelOutput.forEach{category ->
                    val soundRecordingRecordingId = UUID.randomUUID().toString()
                    val rec = SoundRecordingRec().apply {
                        soundRecordingRecId = soundRecordingRecordingId
                        soundRecordingId = _soundRecordingId
                        startDate = Calendar.getInstance().time.toString()
                        label = category.label.replace("\\s+\\d+$".toRegex(), "")
                        probability = category.score
                    }

                    if (rec.label != "Silence" && rec.label != "Inside, small room")
                        list.add(rec)
                }
                soundlessDb?.insertRecordingRecs(list)


                if (outputStr.isNotEmpty())
                    runOnUiThread {
                        tvOutput?.text = outputStr
                    }
            }
        }
    }

    private fun insertHeader(){
        _soundRecordingId = UUID.randomUUID().toString()

        val header = SoundRecordingHeader().apply {
            soundRecordingId = _soundRecordingId
            startDate = Calendar.getInstance().time.toString()
        }
        soundlessDb?.insertSoundRecordingHeader(header)
    }
}