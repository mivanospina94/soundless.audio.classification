package com.cloudlab.soundlessaudioclassification.ui.results

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.cloudlab.soundlessaudioclassification.R
import com.cloudlab.soundlessaudioclassification.framework.sqlite.entities.SoundRecordingRec

class ResultsAdapter(context: Context, val data: List<SoundRecordingRec>) : ArrayAdapter<SoundRecordingRec>(context,
    R.layout.result_item, data) {
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var listItemView = convertView
        if (listItemView == null) {
            listItemView = LayoutInflater.from(context).inflate(R.layout.result_item, parent, false)
        }

        val itemLabel = listItemView!!.findViewById<TextView>(R.id.itemLabel)
        itemLabel.text = data[position].label

        val itemProbability = listItemView!!.findViewById<TextView>(R.id.itemProbability)
        itemProbability.text = data[position].probability.toString()

        val itemDate = listItemView!!.findViewById<TextView>(R.id.itemDate)
        itemDate.text = data[position].startDate

        val itemIsLabelCorrect = listItemView!!.findViewById<TextView>(R.id.itemIsLabelCorrect)
        itemIsLabelCorrect.text = data[position].isLabelCorrect?.let {
            if (it) "Yes" else "No"
        } ?: "-"

        return listItemView
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }
}
