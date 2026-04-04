package com.example.menciliegio

import com.microsoft.graph.requests.GraphServiceClient

class SharePointService(private val mClient: GraphServiceClient<okhttp3.Request>) {

    private val folderPath = "OneDriveAndroid"

    // UPLOAD JSON SU ONEDRIVE
    fun uploadJsonToOneDrive(jsonString: String, fileName: String) {
        val bytes = jsonString.toByteArray(Charsets.UTF_8)
        mClient.me()
            .drive()
            .root()
            .itemWithPath("$folderPath/$fileName")
            .content()
            .buildRequest()
            .put(bytes)
    }

    // UPLOAD IMMAGINE SU ONEDRIVE
    fun uploadImageToOneDrive(imageBytes: ByteArray, fileName: String) {
        mClient.me()
            .drive()
            .root()
            .itemWithPath("$folderPath/$fileName")
            .content()
            .buildRequest()
            .put(imageBytes)
    }

    // DOWNLOAD JSON DA ONEDRIVE
    fun downloadJsonFromOneDrive(fileName: String): String? {
        return try {
            val stream = mClient.me()
                .drive()
                .root()
                .itemWithPath("$folderPath/$fileName")
                .content()
                .buildRequest()
                .get()
            stream?.bufferedReader()?.use { it.readText() }
        } catch (e: Exception) {
            null
        }
    }
}