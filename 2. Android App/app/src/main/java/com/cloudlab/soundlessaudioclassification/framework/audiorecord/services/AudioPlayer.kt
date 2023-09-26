package com.cloudlab.soundlessaudioclassification.framework.audiorecord.services

import java.io.File

interface AudioPlayer {
    fun playFile(file: File)
    fun stop()
}