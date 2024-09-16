package com.alienesyorulmaz.yemektarifleri.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Tarif (

    @ColumnInfo(name = "yemek_ismi")
    var isim: String,

    @ColumnInfo(name = "malzemeler")
    var malzeme: String,

    @ColumnInfo(name = "görsel")
    var gorsel: ByteArray
){
    @PrimaryKey(autoGenerate = true)
    var id = 0
}
