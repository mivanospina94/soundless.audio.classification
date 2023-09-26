package com.cloudlab.soundlessaudioclassification.ui.results

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ListView
import com.cloudlab.soundlessaudioclassification.R
import com.cloudlab.soundlessaudioclassification.framework.sqlite.SoundlessDb

class ResultsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_results)
        val soundRecordingId = intent.getStringExtra("SOUND_RECORDING_ID")

        val soundlessDb = SoundlessDb(this)
        //val dataList = soundlessDb.getSoundClassificationItemsById(soundRecordingId.toString())
        val dataList = soundlessDb.getMostRelevantSoundsById(soundRecordingId.toString())

        val resultsView = findViewById<ListView>(R.id.resultsView)
        val adapter = ResultsAdapter(this, dataList)
        resultsView.adapter = adapter
    }
}