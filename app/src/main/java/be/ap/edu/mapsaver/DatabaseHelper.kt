package be.ap.edu.mapsaver

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper


class DatabaseHelper (context: Context):
    SQLiteOpenHelper(context, DATABASE_NAME,null, DATABASE_VERSION){

    override fun onCreate(db: SQLiteDatabase?) {
        db?.execSQL(SQL_CREATE_ENTRIES)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL(SQL_DELETE_TABLE)
        onCreate(db)
    }

    fun getAll():ArrayList<String>{
        val jsonArrayList = ArrayList<String>()
        var file:String
        val db = this.readableDatabase
        val c = db.rawQuery(SQL_SELECT,null)
        if (c.moveToFirst()){
            do {
                file = c.getString(c.getColumnIndex(FILE))
                jsonArrayList.add(file)
            } while (c.moveToNext())
        }
        return jsonArrayList
    }


    fun add(json: String): Long{
        val db = this.writableDatabase
        val values = ContentValues()
        values.put(FILE,json)
        return db.insert(TABLE,null,values)
    }
    fun deleteAll(){
        val db = this.writableDatabase
        db.execSQL(SQL_DELETE_TABLE)
        db.execSQL(SQL_CREATE_ENTRIES)
        onDestroy()
    }

    fun onDestroy(){
        this.close()
        super.close()
    }

    companion object {
        var DATABASE_NAME = "geodatabase"
        private val DATABASE_VERSION = 1
        private val TABLE = "geoJson"
        private val KEY_ID = "id"
        private val FILE = "file"


        private val SQL_CREATE_ENTRIES =
            "CREATE TABLE $TABLE (" +
                    "$KEY_ID INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "$FILE TEXT);"

        private val SQL_DELETE_TABLE = "DROP TABLE IF EXISTS $TABLE"
        private val SQL_SELECT = "SELECT * FROM $TABLE ORDER BY $KEY_ID DESC"
    }

}