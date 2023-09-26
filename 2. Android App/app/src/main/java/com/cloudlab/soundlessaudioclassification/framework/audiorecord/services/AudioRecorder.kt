package com.cloudlab.soundlessaudioclassification.framework.audiorecord.services

import java.io.File

interface AudioRecorder {
    fun start(outputFile: File)
    fun stop()
}