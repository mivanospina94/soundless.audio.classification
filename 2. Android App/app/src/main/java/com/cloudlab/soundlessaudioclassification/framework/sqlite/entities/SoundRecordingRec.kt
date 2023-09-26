package com.cloudlab.soundlessaudioclassification.framework.sqlite.entities

class SoundRecordingRec{
    var soundRecordingRecId: String = ""
    var soundRecordingId: String = ""
    var startDate: String = ""
    var endDate: String = ""
    var label: String? = ""
    var probability: Float? = null
    var isLabelCorrect: Boolean? = null
    var actualLabel: String? = ""
}