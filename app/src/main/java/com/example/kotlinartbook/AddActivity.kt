package com.example.kotlinartbook

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapFactory.decodeByteArray
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat.*
import java.io.ByteArrayOutputStream
import java.lang.Exception
import androidx.core.content.ContextCompat as ContextCompat


class AddActivity : AppCompatActivity() {
    lateinit var selectedPicture: Uri
    var selectedBitmap: Bitmap? = null

    lateinit var imageView: ImageView
    lateinit var artNameText: EditText
    lateinit var artistNameText: EditText
    lateinit var yearText: EditText
    lateinit var button: Button
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add)

        imageView = findViewById(R.id.imageView)
        artNameText = findViewById(R.id.artNameText)
        artistNameText = findViewById(R.id.artistNameText)
        yearText = findViewById(R.id.yearText)
        button = findViewById(R.id.saveButton)

        val intent = intent
        val info = intent.getStringExtra("info")

        if (info.equals("new")) {

            artNameText.setText("")
            artistNameText.setText("")
            yearText.setText("")
            button.visibility = View.VISIBLE

            val imageBackground = BitmapFactory.decodeResource(applicationContext.resources, R.drawable.selectimage)
            imageView.setImageBitmap(imageBackground)

        } else {

            button.visibility = View.INVISIBLE
            val selected = intent.getIntExtra("id", 1)

            val database = this.openOrCreateDatabase("Book", Context.MODE_PRIVATE, null)

            try {
                val cursor = database.rawQuery("SELECT * FROM arts WHERE id = ?", arrayOf(selected.toString()))

                val artNameIx = cursor.getColumnIndex("artname")
                val artistNameIx = cursor.getColumnIndex("artistname")
                val yearIx = cursor.getColumnIndex("year")
                val imageIx = cursor.getColumnIndex("image")

                while (cursor.moveToNext()) {

                    artNameText.setText(cursor.getString(artNameIx))
                    artistNameText.setText(cursor.getString(artistNameIx))
                    yearText.setText(cursor.getString(yearIx))

                    val byteArray=cursor.getBlob(imageIx)
                    val bitmap= BitmapFactory.decodeByteArray(byteArray,0,byteArray.size)
                    imageView.setImageBitmap(bitmap)
                }
                cursor.close()

            } catch (e: Exception) {
                println("hata var babba")
                e.printStackTrace()
            }
        }
    }

    fun save(view: View) {

        val artName = artNameText.text.toString()
        val artistName = artistNameText.text.toString()
        val year = yearText.text.toString()

        if (selectedBitmap != null) {
            val smallBitmapImage = smallBitmap(selectedBitmap!!, 300)
            val outputStream = ByteArrayOutputStream()
            smallBitmapImage?.compress(Bitmap.CompressFormat.PNG, 50, outputStream)
            val byteArray = outputStream.toByteArray()

            try {
                val database = this.openOrCreateDatabase("Book", Context.MODE_PRIVATE, null)
                database.execSQL("CREATE TABLE IF NOT EXISTS arts(id INTEGER PRIMERY KEY, artname VARCHAR,artistname VARCHAR, year VARCHAR, image BLOB)")

                val sqlString = "INSERT INTO arts(artname,artistname,year,image) VALUES (?,?,?,?)"
                val statement = database.compileStatement(sqlString)
                statement.bindString(1, artName)
                statement.bindString(2, artistName)
                statement.bindString(3, year)
                statement.bindBlob(4, byteArray)

                statement.execute()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            //finish()
            val intent=Intent(this, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intent)
        } else {
            Toast.makeText(applicationContext, "Image seciniz", Toast.LENGTH_SHORT)
        }

    }

    fun selectImage(view: View) {//this, Manifest.permisson.RREAD_EXTERNAL_STORAGE-> izin erişim işleri için

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {// Kullanıcı erişim izni sorgusu
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 1)//Erişim izni yoksa izin alınıyor
        } else {
            val intentToGallery = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intentToGallery, 2)
        }
    }

    fun smallBitmap(image: Bitmap, size: Int): Bitmap {

        var width = image.width
        var height = image.height

        val bitmapRatio: Double = width.toDouble() / height.toDouble()
        if (bitmapRatio > 1) {  // yatay ayarlama
            width = size
            val scalefHeight = width / bitmapRatio
            height = scalefHeight.toInt()
        } else {  //Dikey ayarlama
            height = size
            val scaleWidth = height * bitmapRatio
            width = scaleWidth.toInt()
        }

        return Bitmap.createScaledBitmap(image, width, height, true)
    }

    override fun onRequestPermissionsResult( //İzin istedikten sonra direk galeri açılması
            requestCode: Int,
            permissions: Array<out String>,
            grantResults: IntArray) {

        if (requestCode == 1) {

            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                val intentToGallery = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                startActivityForResult(intentToGallery, 2)
            }
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == 2 && resultCode == Activity.RESULT_OK && data != null) {
            selectedPicture = data.data!! // çift önlem kullanılmak gerektiğinde if kontrolü yapılmalı sağlıklı olur
            try {
                if (selectedPicture != null) {
                    if (Build.VERSION.SDK_INT >= 28) {
                        val source = ImageDecoder.createSource(this.contentResolver, selectedPicture)
                        selectedBitmap = ImageDecoder.decodeBitmap(source)
                        imageView.setImageBitmap(selectedBitmap)

                    } else {
                        selectedBitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, selectedPicture)
                        imageView.setImageBitmap(selectedBitmap)
                    }
                }
            } catch (ex: Exception) {
                println(ex)
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }
}