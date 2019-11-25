package com.iskmz.textify

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setOnClicks()
    }

    private fun setOnClicks() {

        btnExit.setOnClickListener { finish() }
        btnAbout.setOnClickListener { showAboutDialog() }
        btnShoot.setOnClickListener { shootAndTextify() }
    }

    private fun showAboutDialog() {
        val alert:AlertDialog = AlertDialog.Builder(this)
            .setTitle("About ...")
            .setMessage("Textify!\n\n\tBy Iskandar Mazzawi\n\n\tiskandar1123@hotmail.com\n")
            .setIcon(R.drawable.ic_info_orange)
            .setPositiveButton("OK"){d,_->d.dismiss()}
            .create()
        alert.setCanceledOnTouchOutside(false)
        alert.show()
    }


    private fun shootAndTextify() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}
