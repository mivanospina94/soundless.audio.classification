package com.cloudlab.soundlessaudioclassification.framework.sqlite

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.cloudlab.soundlessaudioclassification.framework.sqlite.entities.SoundRecordingHeader
import com.cloudlab.soundlessaudioclassification.framework.sqlite.entities.SoundRecordingRec
import java.util.*

class SoundlessDb(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    companion object {
        private const val DATABASE_NAME = "Soundless.db"
        private const val DATABASE_VERSION = 2
    }

    override fun onCreate(db: SQLiteDatabase) {
        //db.setForeignKeyConstraintsEnabled(true)
        db.execSQL("CREATE TABLE IF NOT EXISTS SoundRecordingHeader ( SoundRecordingId TEXT NOT NULL, StartDate DATE, EndDate DATE, PRIMARY KEY(SoundRecordingId) );")
        db.execSQL("CREATE TABLE IF NOT EXISTS SoundRecordingRec ( SoundRecordingRecId TEXT NOT NULL, SoundRecordingId TEXT NOT NULL, StartDate DATE, EndDate DATE, Label TEXT, Probability REAL, IsLabelCorrect BOOLEAN, ActualLabel TEXT, PRIMARY KEY(SoundRecordingRecId), FOREIGN KEY (SoundRecordingId) REFERENCES SoundRecordingHeader(SoundRecordingId) );")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS SoundRecordingHeader")
        db.execSQL("DROP TABLE IF EXISTS SoundRecordingRec")
        onCreate(db)
    }

    override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        onUpgrade(db, oldVersion, newVersion)
    }

    fun closeDB() {
        val db = this.readableDatabase
        if (db != null && db.isOpen) db.close()
    }

    //CRUD
    fun insertSoundRecordingHeader(row: SoundRecordingHeader): Long {
        val db = this.writableDatabase;

        val values = ContentValues().apply {
            put("SoundRecordingId", row.soundRecordingId)
            put("StartDate", row.startDate)
            put("EndDate", row.endDate)
        }
        //db.close()
        return db.insert("SoundRecordingHeader", null, values)
    }

    fun insertRecordingRecs(rowList: List<SoundRecordingRec>){
        val db = this.writableDatabase;

        rowList.forEach { row ->
            val values = ContentValues().apply {
                put("soundRecordingRecId", row.soundRecordingRecId)
                put("soundRecordingId", row.soundRecordingId)
                put("startDate", row.startDate)
                put("endDate", row.endDate)
                put("label", row.label)
                put("probability", row.probability)
                put("isLabelCorrect", row.isLabelCorrect)
                put("actualLabel", row.actualLabel)
            }
            db.insert("SoundRecordingRec", null, values)
        }
    }

    @SuppressLint("Range")
    fun getSoundClassificationItemsById(id: String): List<SoundRecordingRec> {
        val rowList = mutableListOf<SoundRecordingRec>()

        val db = readableDatabase
        val cursor: Cursor? = db.rawQuery("SELECT * FROM SoundRecordingRec WHERE SoundRecordingId = '$id' ORDER BY StartDate ASC, Probability DESC", null)

        cursor?.use {
            while (it.moveToNext()) {

                val rec = SoundRecordingRec().apply {
                    soundRecordingRecId = it.getString(it.getColumnIndex("SoundRecordingRecId"))
                    soundRecordingId = it.getString(it.getColumnIndex("SoundRecordingId"))
                    startDate = it.getString(it.getColumnIndex("StartDate"))
                    label = it.getString(it.getColumnIndex("Label"))
                    probability = it.getFloat(it.getColumnIndex("Probability"))
                }
                rowList.add(rec)
            }
        }

        cursor?.close()
        db.close()

        return rowList
    }

    @SuppressLint("Range")
    fun getMostRelevantSoundsById(id: String): List<SoundRecordingRec> {
        val rowList = mutableListOf<SoundRecordingRec>()
        val query = """
                    SELECT	AVG(Probability) AS Probability, SoundRecordingId, Label,
                            '' AS SoundRecordingRecId, '' AS StartDate
                    FROM    SoundRecordingRec
                    WHERE	SoundRecordingId = '$id'
                    GROUP BY SoundRecordingId, Label
                    ORDER BY AVG(Probability) DESC
                    """
        val db = readableDatabase
        //val cursor: Cursor? = db.rawQuery("SELECT * FROM SoundRecordingRec WHERE SoundRecordingId = '$id' ORDER BY StartDate ASC, Probability DESC", null)
        val cursor: Cursor? = db.rawQuery(query, null)

        cursor?.use {
            while (it.moveToNext()) {

                val rec = SoundRecordingRec().apply {
                    soundRecordingRecId = it.getString(it.getColumnIndex("SoundRecordingRecId"))
                    soundRecordingId = it.getString(it.getColumnIndex("SoundRecordingId"))
                    startDate = it.getString(it.getColumnIndex("StartDate"))
                    label = it.getString(it.getColumnIndex("Label"))
                    probability = it.getFloat(it.getColumnIndex("Probability"))
                }
                rowList.add(rec)
            }
        }

        cursor?.close()
        db.close()

        return rowList
    }
}