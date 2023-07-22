package com.example.persona3reloadscreensaver

import android.app.Activity
import android.content.Context
import android.util.Log
import android.widget.TextView
import com.android.volley.AuthFailureError
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.spotify.sdk.android.auth.AuthorizationClient
import com.spotify.sdk.android.auth.AuthorizationRequest
import com.spotify.sdk.android.auth.AuthorizationResponse
import org.json.JSONObject


class SpotifyAPI(var activity: Activity, var context: Context, var clientId: String, var clientSecret: String, var playingText: TextView) {
    private val volleyQueue = Volley.newRequestQueue(context)


    var accessToken = ""
    var currentlyPlaying = ""


    fun getAccessToken() {
        val url =
            "https://accounts.spotify.com/api/token?grant_type=client_credentials&client_id=$clientId&client_secret=$clientSecret"

        val request: JsonObjectRequest = object : JsonObjectRequest(
            Method.POST, url,null,
            Response.Listener<JSONObject?> { response ->
                if (response != null) {
                    accessToken = response.get("access_token") as String
                    Log.d("Access Token", accessToken)
                    getPlaybackState()
                } else {
                    Log.d("Your Array Response", "Data Null")
                }
            },
            Response.ErrorListener { error -> Log.e("error is ", "" + error) }) {
                @Throws(AuthFailureError::class)
                override fun getHeaders(): Map<String, String> {
                    val params: MutableMap<String, String> = HashMap()
                    params["Content-Type"] = "application/x-www-form-urlencoded"
                    return params
                }
            }
        volleyQueue.add(request)
    }

    fun nativeGetAccessToken() {
        val builder = AuthorizationRequest.Builder(clientId, AuthorizationResponse.Type.TOKEN, "p3s://callback")
        val scopes = arrayOf("user-read-playback-state", "user-read-currently-playing")
        builder.setScopes(scopes)

        val request = builder.build()
        AuthorizationClient.openLoginInBrowser(activity, request)
    }

    fun getPlaybackState() {
        val url = "https://api.spotify.com/v1/me/player"
        val request: JsonObjectRequest = object : JsonObjectRequest(
            Request.Method.GET, url, null,
            Response.Listener<JSONObject?> { response ->
                val device = response.get("device") as JSONObject
                val isActive = device.get("is_active") as Boolean
                if (isActive) {
                    getCurrentlyPlaying()
                }
            },
            Response.ErrorListener { error ->
                if (error.networkResponse.statusCode == 401) {
                    nativeGetAccessToken()
                } else {
                    Log.d("Playback Error", error.toString())
                }
            }) {
            @Throws(AuthFailureError::class)
            override fun getHeaders(): Map<String, String> {
                val params: MutableMap<String, String> = HashMap()
                params["Authorization"] = "Bearer $accessToken"
                return params
            }
        }
        volleyQueue.add(request)
    }

    fun getCurrentlyPlaying() {
        val url = "https://api.spotify.com/v1/me/player/currently-playing"
        val request = object : JsonObjectRequest(
            Request.Method.GET, url, null,
            Response.Listener<JSONObject?> { response ->
                val item = response.get("item") as JSONObject
                val name = item.get("name") as String
                Log.d("Playing", name)
                if (currentlyPlaying != "Playing - $name") {
                    currentlyPlaying = "Playing - $name"
                    playingText.text = currentlyPlaying

                }
            },
            Response.ErrorListener { error ->
                if (error.networkResponse.statusCode == 401) {
                    nativeGetAccessToken()
                } else {
                    Log.d("Currently Playing Error", error.toString())
                }
            }) {
                @Throws(AuthFailureError::class)
                override fun getHeaders(): Map<String, String> {
                    val params: MutableMap<String, String> = HashMap()
                    params["Authorization"] = "Bearer $accessToken"
                    return params
                }
        }
        volleyQueue.add(request)
    }
}