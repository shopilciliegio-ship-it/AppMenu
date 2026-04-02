package com.example.menciliegio

import com.microsoft.graph.requests.GraphServiceClient

class SharePointService(private val mClient: GraphServiceClient<okhttp3.Request>) {

    private val siteDomain = "ilciliegio.sharepoint.com"
    private val folderPath = "OneDriveAndroid"

    fun uploadJsonToSharePoint(jsonString: String, fileName: String) {
        val bytes = jsonString.toByteArray(Charsets.UTF_8)

        mClient.sites(siteDomain)
            .drive()
            .root()
            .itemWithPath("$folderPath/$fileName")
            .content()
            .buildRequest()
            .put(bytes)
    }

    fun uploadImageToSharePoint(imageBytes: ByteArray, fileName: String) {
        mClient.sites(siteDomain)
            .drive()
            .root()
            .itemWithPath("$folderPath/$fileName")
            .content()
            .buildRequest()
            .put(imageBytes)
    }
}