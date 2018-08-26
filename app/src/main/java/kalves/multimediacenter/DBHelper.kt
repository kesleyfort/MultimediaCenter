package kalves.multimediacenter

import java.util.ArrayList
import java.util.HashMap
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.DatabaseUtils
import android.database.sqlite.SQLiteOpenHelper
import android.database.sqlite.SQLiteDatabase
import android.util.Log


class DBHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_title, null, 1) {
    private val hp: HashMap<*, *>? = null

    //hp = new HashMap();
    val allentrys: ArrayList<String>
        get() {
            val arraylist = ArrayList<String>()
            val db = this.readableDatabase
            val res = db.rawQuery("select * from MusicLibrary", null)
            res.moveToFirst()

            while (!res.isAfterLast) {
                arraylist.add(res.getString(res.getColumnIndex(COLUMN_TITLE)))
                res.moveToNext()
            }
            return arraylist
        }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
                "create table MusicLibrary (id integer primary key, title text,artist text,album text, artwork blob,uri text)"
        )
        Log.e("On created", "Table Created")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS MusicLibrary")
        onCreate(db)
    }

    fun insert(title: String, artist: String, album: String, artwork: ByteArray, uri: String): Boolean {
        val db = this.writableDatabase
        val contentValues = ContentValues()
        contentValues.put("title", title)
        contentValues.put("artist", artist)
        contentValues.put("album", album)
        contentValues.put("artwork", artwork)
        contentValues.put("uri", uri)
        db.insert("MusicLibrary", null, contentValues)
        return true
    }

    fun getData(id: Int): Cursor {
        val db = this.readableDatabase
        return db.rawQuery("select * from MusicLibrary where id=$id", null)
    }

    fun numberOfRows(): Int {
        val db = this.readableDatabase
        return DatabaseUtils.queryNumEntries(db, TABLE_title).toInt()
    }

    fun updateEntry(id: Int?, title: String, artist: String, album: String, artwork: ByteArray, uri: String): Boolean {
        val db = this.writableDatabase
        val contentValues = ContentValues()
        contentValues.put("title", title)
        contentValues.put("artist", artist)
        contentValues.put("album", album)
        contentValues.put("artwork", artwork)
        contentValues.put("uri", uri)
        db.update("MusicLibrary", contentValues, "id = ? ", arrayOf(Integer.toString(id!!)))
        return true
    }

    fun deleteContact(id: Int?): Int? {
        val db = this.writableDatabase
        return db.delete("MusicLibrary",
                "id = ? ",
                arrayOf(Integer.toString(id!!)))
    }
    fun getall(): MutableList<String>{
        val arraylist: MutableList<String> = mutableListOf()
        val db = this.readableDatabase
        val res = db.rawQuery("select * from MusicLibrary", null)
        res.moveToFirst()

        while (!res.isAfterLast) {
            arraylist.add(res.getString(res.getColumnIndex(COLUMN_TITLE)) + "\n" +res.getString(res.getColumnIndex(COLUMN_ARTIST)) + " - " + res.getString(res.getColumnIndex(COLUMN_ALBUM)))
            res.moveToNext()
        }
        return arraylist
    }
    fun getSorted(sort: String): MutableList<String>{
        val arraylist: MutableList<String> = mutableListOf()
        val db = this.readableDatabase
        if(sort.equals("Artist (A - Z)", true)){
            val res = db.rawQuery("select * from MusicLibrary order by artist ASC", null)
            res.moveToFirst()

            while (!res.isAfterLast) {
                arraylist.add(res.getString(res.getColumnIndex(COLUMN_TITLE)) + "\n" +res.getString(res.getColumnIndex(COLUMN_ARTIST)) + " - " + res.getString(res.getColumnIndex(COLUMN_ALBUM)))
                res.moveToNext()
            }
        }
        else if(sort.equals("Album (A - Z)", true)){
            val res = db.rawQuery("select * from MusicLibrary order by album ASC", null)
            res.moveToFirst()

            while (!res.isAfterLast) {
                arraylist.add(res.getString(res.getColumnIndex(COLUMN_TITLE)) + "\n" +res.getString(res.getColumnIndex(COLUMN_ARTIST)) + " - " + res.getString(res.getColumnIndex(COLUMN_ALBUM)))
                res.moveToNext()
            }
        }
        else if(sort.equals("Song (A - Z)", true)){
            val res = db.rawQuery("select * from MusicLibrary order by title ASC", null)
            res.moveToFirst()

            while (!res.isAfterLast) {
                arraylist.add(res.getString(res.getColumnIndex(COLUMN_TITLE)) + "\n" +res.getString(res.getColumnIndex(COLUMN_ARTIST)) + " - " + res.getString(res.getColumnIndex(COLUMN_ALBUM)))
                res.moveToNext()
            }
        }
        return arraylist
    }
    fun deleteAll(){
        val db = this.writableDatabase
        db.execSQL("delete from MusicLibrary")
    }
    fun getMusicTitles(): MutableList<String> {
        val arraylist: MutableList<String> = mutableListOf()
        val db = this.readableDatabase
        val res = db.rawQuery("select * from MusicLibrary", null)
        res.moveToFirst()

        while (!res.isAfterLast) {
            arraylist.add(res.getString(res.getColumnIndex(COLUMN_TITLE)))
            res.moveToNext()
        }
        return arraylist
    }
    fun getMusicUri(): MutableList<String> {
        val arraylist: MutableList<String> = mutableListOf()
        val db = this.readableDatabase
        val res = db.rawQuery("select * from MusicLibrary", null)
        res.moveToFirst()

        while (!res.isAfterLast) {
            arraylist.add(res.getString(res.getColumnIndex(COLUMN_URI)))
            res.moveToNext()
        }
        return arraylist
    }

    companion object {

        val DATABASE_title = "Data.db"
        val TABLE_title = "MusicLibrary"
        val COLUMN_ID = "id"
        val COLUMN_TITLE = "title"
        val COLUMN_ARTIST = "artist"
        val COLUMN_ALBUM = "album"
        val COLUMN_ARTWORK = "artwork"
        val COLUMN_URI = "uri"
    }
}
