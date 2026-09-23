package com.yusufjamil.aicallassistant

import android.content.Context

object AppSettings {
 private const val PREFS="app_settings"
 fun name(c:Context)=c.getSharedPreferences(PREFS,0).getString("name","").orEmpty()
 fun language(c:Context)=c.getSharedPreferences(PREFS,0).getString("language","English").orEmpty()
 fun voice(c:Context)=c.getSharedPreferences(PREFS,0).getString("voice","Female").orEmpty()
 fun saveProfile(c:Context,name:String,language:String,voice:String)=c.getSharedPreferences(PREFS,0).edit().putString("name",name).putString("language",language).putString("voice",voice).apply()
 fun primary(c:Context)=c.getSharedPreferences(PREFS,0).getString("primary","groq").orEmpty()
 fun backup(c:Context)=c.getSharedPreferences(PREFS,0).getString("backup","gemini").orEmpty()
 fun saveRouting(c:Context,primary:String,backup:String)=c.getSharedPreferences(PREFS,0).edit().putString("primary",primary).putString("backup",backup).apply()
 fun azureRegion(c:Context)=c.getSharedPreferences(PREFS,0).getString("azure_region","centralindia").orEmpty()
 fun saveAzureRegion(c:Context,region:String)=c.getSharedPreferences(PREFS,0).edit().putString("azure_region",region.trim()).apply()
}